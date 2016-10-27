(ns v.hophacks2016.keyword-extract
  (:require [opennlp.nlp :as nlp]
            [v.hophacks2016.pagerank :as pr]
            [v.1.algorithms.peg :as peg]
            [v.1.dispatch.treefn :as tf]
            [clojure.set :as cs]))

(defn suffix [model-name]
  (str model-name ".bin"))

(def open-nlp
  {:keyword.opennlp/tokenize-fn
   (tf/fm
    [:keyword.opennlp.input/models-prefix
     :keyword.opennlp.input/tokenize-model-name]
    (nlp/make-tokenizer (dj/str-path models-prefix (suffix tokenize-model-name))))
   :keyword.opennlp/tag-pos-fn
   (tf/fm
    [:keyword.opennlp.input/models-prefix
     :keyword.opennlp.input/pos-model-name]
    (nlp/make-pos-tagger (dj/str-path models-prefix (suffix pos-model-name))))
   :keyword.opennlp/sentences-fn
   (tf/fm
    [:keyword.opennlp.input/models-prefix
     :keyword.opennlp.input/sentences-model-name]
    (nlp/make-sentence-detector (dj/str-path models-prefix (suffix sentences-model-name))))
   :keyword.opennlp/sentences
   (tf/fm [:keyword.input/txt
           :keyword.opennlp/sentences-fn]
          (sentences-fn txt))
   :keyword.opennlp/tokenized-sentences
   (tf/fm [:keyword.opennlp/tokenize-fn
           :keyword.opennlp/sentences]
          (persistent!
           (reduce (fn [ret sentence]
                     (conj! ret
                            (tokenize-fn sentence)))
                   (transient [])
                   sentences)))})

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

(def np-chunking
  {:keyword.np-chunking/parse
   (let [JJ (peg/+ (peg/| (t "JJ") (t "JJR") (t "JJS") (t "VBG") (t "VBN")))
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
                                 nil)))]
     (tf/fm [:keyword.np-chunking.input/pairs]
            (peg/parse GETNP pairs)))})

(def keyword-candidates
  {:keyword.candidates/candidates
   (tf/fm [:keyword.opennlp/tokenized-sentences
           :keyword.opennlp/tag-pos-fn]
          (reduce (fn [ret tokenized-sentence]
                    (into ret
                          (loop [remaining-input (tag-pos-fn tokenized-sentence)
                                 NPs []]
                            (if (empty? remaining-input)
                              NPs
                              (let [{:keys [result unconsumed-input]} (quick-parse-np remaining-input)]
                                (recur unconsumed-input
                                       (if result
                                         (conj NPs (mapv first result))
                                         NPs)))))))
                  []
                  tokenized-sentences))
   :keyword.candidates/set-candidates
   (tf/fm [:keyword.candidates/candidates]
          (set candidates))
   :keyword.candidates/set-candidate-words
   (tf/fm [:keyword.candidates/candidates]
          (set (apply concat candidates)))})

(def keyword-connectivity
  {:keyword.connectivity/connectivity
   (tf/fm [:keyword.opennlp/tokenized-sentences]
          (loop [words (apply concat tokenized-sentences)
                 inrange {}]
            ;; Words are connected if within two tagged items (e.g. a word or punctuation)
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
                                            (conj w1 w3))))))))))
   :keyword.connectivity/filtered
   (tf/fm [:keyword.connectivity/connectivity
           :keyword.candidates/set-candidate-words]
          (reduce-kv (fn [ret k v]
                       (let [remaining (cs/intersection v set-candidate-words)]
                         (if (or (empty? remaining)
                                 (not (set-candidate-words k)))
                           ret
                           (assoc ret
                                  k
                                  remaining))))
                     {}
                     connectivity))})

(def keyword-ranking
  {:keyword.ranking/pagerank
   (tf/fm [:keyword.connectivity/filtered]
          (pr/pagerank pr/simple-score filtered))
   :keyword.ranking/singular-rankings
   (tf/fm [:keyword.ranking/pagerank]
          (sort-by second > (vec pagerank)))
   :keyword.ranking/top-singular
   (tf/fm [:keyword.ranking/singular-rankings]
          ;; takes top third
          (map first (take (long (/ (count singular-rankings) 3.0)) singular-rankings)))
   :keyword.ranking/n-grams-ranking
   (tf/fm [:keyword.candidates/set-candidates
           :keyword.ranking/pagerank]
          (sort-by second >
                   (into []
                         (comp
                          (map (fn [words]
                                 [words (/ (apply + (map pagerank
                                                         words))
                                           (count words))]))
                          (filter (fn [[words _]]
                                    (and (> (count words) 1)
                                         (< (count words) 5)))))
                         set-candidates)))
   :keyword.ranking/keywords
   (tf/fm [:keyword.ranking/top-singular
           :keyword.ranking/n-grams-ranking
           :keyword.candidates/candidates]
          (filter (fn [words]
                         (and (some (set top-singular) words)
                              (> (count words) 1)))
                       candidates))})
