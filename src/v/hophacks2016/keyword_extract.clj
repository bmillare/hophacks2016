(ns v.hophacks2016.keyword-extract
  (:require [dj]
            [opennlp.nlp :as nlp]
            [v.algorithms.pagerank :as pr]
            [v.algorithms.peg :as peg]
            [v.dispatch.treefn :as tf]
            [clojure.set :as cs]))

;; This file utilizes tree-functions, (tf), as a better way to compose
;; complex operations (functions) as simply a merging of maps

;; The user calling code will have to merge the different maps
;; together and build the final composite function like the following:

(comment
  (def main-fn
    (v.dispatch.treefn/treefm
     (merge
      v.hophacks2016.keyword-extract/open-nlp
      v.hophacks2016.keyword-extract/np-chunking
      v.hophacks2016.keyword-extract/keyword-candidates
      v.hophacks2016.keyword-extract/keyword-connectivity
      v.hophacks2016.keyword-extract/keyword-ranking)
     :keyword.ranking/n-grams-ranking))

  ;; And then call it like so
  
  (main-fn
   {:keyword.opennlp.input/tokenize-model-name
    "en-token"
    :keyword.opennlp.input/pos-model-name
    "en-pos-maxent"
    :keyword.opennlp.input/sentences-model-name
    "en-sent"
    :keyword.input/txt
    "The angry dog is walking across the street. Patrick has never had a donut."})

  ;; Note that the output of each sub-function will be stored in the
  ;; output of main-fn, so we might want to only extract the data of
  ;; interest by specifying the correct key like so:

  (:keyword.ranking/pagerank (main-fn input-map))

  )

;; ----------------------------------------------------------------------

(defn suffix [model-name]
  (str model-name ".bin"))

;; Builds open-nlp utilities from provided models
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

;; builds parser based on grammar used for extracting noun phrases (NPs)
;; The grammar is defined using parsing expression grammars (PEGs)
;; - a way to compose mini-parsers that act like regular expressions
(def np-chunking
  {:keyword.np-chunking/parse-fn
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
     (tf/fm []
            (fn [pairs]
              (peg/parse GETNP pairs))))})

(def keyword-candidates
  {:keyword.candidates/candidates
   (tf/fm [:keyword.np-chunking/parse-fn
           :keyword.opennlp/tokenized-sentences
           :keyword.opennlp/tag-pos-fn]
          (reduce (fn add-NPs [ret tokenized-sentence]
                    (into ret
                          (loop [remaining-input (tag-pos-fn tokenized-sentence)
                                 NPs []]
                            (if (empty? remaining-input)
                              NPs
                              (let [{:keys [result unconsumed-input]} (parse-fn remaining-input)]
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
          ;; using a vec up front is SOOO much faster
          (let [all-words (vec (apply concat tokenized-sentences))
                stop-n (- (count all-words) 3)]
            (loop [n 0
                   inrange (reduce (fn [ret w]
                                     (assoc! ret
                                             w
                                             #{}))
                                   (transient {})
                                   all-words)]
              ;; Words are connected if within two tagged items (e.g. a word or punctuation)
              (if (< n stop-n)
                (let [w1 (all-words n)
                      w2 (all-words (inc n))
                      w3 (all-words (+ n 2))]
                  (recur (inc n)
                         (-> inrange
                             (assoc! w1 (conj (inrange w1) w2 w3))
                             (assoc! w2 (conj (inrange w2) w1 w3))
                             (assoc! w3 (conj (inrange w3) w1 w2)))))
                (persistent! inrange)))))
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
                          (filter (fn [words]
                                    (and (> (count words) 1)
                                         (< (count words) 5))))
                          (map (fn [words]
                                 [words (/ (apply + (map pagerank
                                                         words))
                                           (count words))])))
                         set-candidates)))})
