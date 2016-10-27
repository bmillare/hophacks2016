(ns v.hophacks2016.keyword-extract
  (:require [opennlp.nlp :as nlp]
            [v.hophacks2016.pagerank :as pr]
            [v.1.algorithms.peg :as peg]))

(def path-prefix (case (System/getProperty "user.dir")
                   "/home/bmillare/dj/usr/src/dj.scratch"
                   "/home/bmillare/dj/usr/src/hophacks2016/src/v"))

(def tokenize (nlp/make-tokenizer (dj/str-path path-prefix "models/en-token.bin")))

(def pos-tag (nlp/make-pos-tagger (dj/str-path path-prefix "models/en-pos-maxent.bin")))

(def get-sentences (nlp/make-sentence-detector (dj/str-path path-prefix "models/en-sent.bin")))

(defn t
  "peg token maker, returns parser that looks for POS tag that matches
  provided POS"
  [pos]
  (fn [input succeed fail]
    (let [pair (first input)
          [word ipos] pair]
      (if (= ipos pos)
        (succeed
         pair
         (rest input))
        (fail {:pos pos
               :input input}
              input)))))

(defn ct
  "peg token maker, returns parser that succeeds when pred is true,
  else fails"
  [pred]
  (fn [input succeed fail]
    (let [pair (first input)]
      (if (and pair (pred pair))
        (succeed
         pair
         (rest input))
        (fail {:pred pred
               :input input}
              input)))))

(let [corpus
      "With the goal of non-invasively localizing cardiac ischemic disease using body-surface potential recordings, we attempted to reconstruct the transmembrane potential (TMP) throughout the myocardium with the bidomain heart model. The task is an inverse source problem governed by partial differential equations (PDE). Our main contribution is solving the inverse problem within a PDE-constrained optimization framework that enables various physically-based constraints in both equality and inequality forms. We formulated the optimality conditions rigorously in the continuum before deriving finite element discretization, thereby making the optimization independent of discretization choice. Such a formulation was derived for the L2-norm Tikhonov regularization and the total variation minimization. The subsequent numerical optimization was fulfilled by a primal–dual interior-point method tailored to our problem’s specific structure. Our simulations used realistic, fiber-included heart models consisting of up to 18,000 nodes, much finer than any inverse models previously reported. With synthetic ischemia data we localized ischemic regions with roughly a 10% false-negative rate or a 20% false-positive rate under conditions up to 5% input noise. With ischemia data measured from animal experiments, we reconstructed TMPs with roughly 0.9 correlation with the ground truth. While precisely estimating the TMP in general cases remains an open problem, our study shows the feasibility of reconstructing TMP during the ST interval as a means of ischemia localization"
      #_(dj.io/eat (dj.io/file "/home/bmillare/dj/usr/store/code-countries/scratch/hophacks2016/w2167e.txt"))
      JJ (peg/+ (peg/| (t "JJ") (t "JJR") (t "JJS") (t "VBG") (t "VBN")))
      NN (peg/| (t "NN") (t "NNS"))
      NNI (peg/| (peg/+ NN)
                          (peg/alt (peg/s JJ (peg/+ NN))
                                   (fn [[j ns]]
                                     (into j ns))))
      NNP (peg/+ (peg/| (t "NNP") (t "NNPS")))
      ELSE (peg/| (ct (fn [[_ tag]]
                                 (not (#{"JJ" "JJR" "JJS" "VBG" "VBN" "NN" "NNS" "NNP" "NNPS"} tag))))
                           (peg/s (ct (fn [[_ tag]]
                                        (not (#{"NN" "NNS" "NNP" "NNPS"} tag))))
                                  (peg/!? NNI)))
      GETNP (peg/| (peg/alt (peg/s (peg/* ELSE) (peg/| NNI NNP))
                                     (fn [[_ NP]]
                                       NP))
                            (peg/alt (peg/+ ELSE)
                                     (fn [_]
                                       nil)))
      
      parse (fn [pairs]
              (peg/parse GETNP pairs))
      candidates (reduce (fn [ret sentence]
                           (into ret
                                 (loop [remaining-input (pos-tag (tokenize sentence))
                                        NPs []]
                                   (if (empty? remaining-input)
                                     NPs
                                     (let [{:keys [result unconsumed-input]} (parse remaining-input)]
                                       (recur unconsumed-input
                                              (if result
                                                (conj NPs (mapv first result))
                                                NPs)))))))
                         []
                         (get-sentences corpus))
      connectivity (loop [words (apply
                                 concat (map tokenize
                                             (get-sentences corpus)))
                          inrange {}]
                     (if (< (count words) 3)
                       inrange
                       (recur (rest words)
                              (let [w1 (first words)
                                    w2 (second words)
                                    w3 (nth words 2)]
                                (-> inrange
                                    (update w1 (fn [touching]
                                                 (-> (or touching #{})
                                                     (conj w2 w3))))
                                    (update w2 (fn [touching]
                                                 (-> (or touching #{})
                                                     (conj w1 w3))))
                                    (update w3 (fn [touching]
                                                 (-> (or touching #{})
                                                     (conj w1 w3)))))))))
      set-candidates (set candidates)
      set-candidate-words (set (apply concat candidates))
      filtered-connectivity (reduce-kv (fn [ret k v]
                                         (let [remaining (clojure.set/intersection v set-candidate-words)]
                                           (if (or (empty? remaining)
                                                   (not (set-candidate-words k)))
                                             ret
                                             (assoc ret
                                                    k
                                                    remaining))))
                                       {}
                                       connectivity)
      frequencies (reduce (fn [ret candidate]
                            (update ret
                                    candidate
                                    (fn [v]
                                      (inc (or v 0)))))
                          {}
                          candidates)
      rankings-map (pr/pagerank pr/simple-score filtered-connectivity)
      rankings (sort-by second > (vec rankings-map))
      top (map first (take (long (/ (count rankings) 3.0)) rankings))
      keywords-rank (sort-by second >
                             (into []
                                   (comp
                                    (map (fn [words]
                                           [words (/ (apply + (map rankings-map
                                                                   words))
                                                     (count words))]))
                                    (filter (fn [[words _]]
                                              (and (> (count words) 1)
                                                   (< (count words) 5)))))
                                   set-candidates))
      #_#_keywords (filter (fn [words]
                         (and (some (set top) words)
                              (> (count words) 1)))
                       candidates)]
  keywords-rank)
