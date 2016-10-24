(ns v.hophacks2016.rake
  (:require [clojure.string :as string]))

(comment
  (do (require '[v.hophacks2016.rake :as rake]
       '[dj.dependencies])
    (dj.dependencies/add-dependencies '[[clojure-opennlp "0.3.3"]]
      {:dj/repositories {"central" "http://repo1.maven.org/maven2/"
                         "clojars" "http://clojars.org/repo"}})
   (require '[opennlp.nlp :as nlp]))

  (nlp/make-sentence-detector "v/models/en-sent.bin")
)

(defn is-number [txt]
  (re-matches #"\d+(?:\.\d+)?" txt))

(defn parse-stop-words [txt]
  (clojure.string/split txt #"\s+"))

(defn big-words [txt min-length]
  (into []
        (comp
         (map clojure.string/trim)
         (map clojure.string/lower-case)
         (filter (fn [w]
                   (and (> (count w) min-length)
                        (not (empty? w))
                        (not (is-number w))))))
        (clojure.string/split txt #"[^a-zA-Z0-9_\+\-/]")))

(defn split-sentences [txt]
  (clojure.string/split txt (re-pattern (str "[" #"\[\]\n.!?,;:\t\-\"\(\)\\\'" \u2019 \u2013 "]"))))

(defn stop-word-regex [stop-words]
  (re-pattern (str #"(?i)"
                   (clojure.string/join "|"
                                        (map (fn [word]
                                               (str #"\b" word #"\b"))
                                             stop-words)))))

(defn generate-candidate-keywords [sentences stop-word-re]
  (reduce (fn [ret sentence]
            (into ret
                  (comp
                   (map clojure.string/trim)
                   (filter (fn [phrase]
                             (not (empty? phrase)))))
                  (-> (clojure.string/replace (clojure.string/trim sentence)
                                              stop-word-re
                                              "|")
                      (clojure.string/split #"\|"))))
          []
          sentences))

(defn word-scores [phrases]
  (let [[word-frequency word-degree]
        (loop [word-frequency {}
               word-degree {}
               phrases' phrases]
          (if (empty? phrases')
            [word-frequency
             (reduce-kv (fn [ret word frequency]
                          (assoc ret
                                 word
                                 (+ (word-degree word) frequency)))
                        {}
                        word-frequency)]
            (let [phrase (first phrases')]
              (let [words (big-words phrase 0)
                    words-degree (dec (count words))]
                (let [[wf' wd']
                      (loop [wf word-frequency
                             wd word-degree
                             words' words]
                        (if (empty? words')
                          [wf wd]
                          (let [word (first words')]
                            (recur (update wf word (fn [w]
                                                     (if w
                                                       (inc w)
                                                       1)))
                                   (update wd word (fn [w]
                                                     (if w
                                                       (+ w words-degree)
                                                       words-degree)))
                                   (rest words')))))]
                  (recur wf' wd' (rest phrases')))))))]
    (reduce-kv (fn [ret word frequency]
                 (assoc ret
                        word
                        (double (/ (word-degree word) frequency))))
               {}
               word-frequency)))

(defn candidate-keyword-scores [phrases scores]
  (loop [keyword-candidates {}
         phrases' phrases]
    (if (empty? phrases')
      keyword-candidates
      (let [phrase (first phrases')
            words (big-words phrase 0)]
        (recur (assoc keyword-candidates
                      phrase
                      (reduce (fn [candidate-score word]
                                (+ candidate-score (scores word)))
                              0
                              words))
               (rest phrases'))))))

(defn extract-text [text]
  ":keywords = vector of keywords.
   :body = most stuff.
   :refs = the citations."
  {:keywords 
    (let [first-section (first (string/split text #"\n==== Body"))
          keyword-line (last (string/split first-section #"\n"))]
      (into [] (re-seq #"[A-Z]+[^A-Z]+" keyword-line)))
   :body (first (string/split text #"\n==== Refs"))
   :refs (let [refblock (second (string/split text #"\n==== Refs\n"))
               reflines (into [] (re-seq #"[0-9]+[^\n]+\n" refblock))]
           (mapv #(subs %1 (inc (count (str (inc %2)))) (dec (count %1))) reflines (range)))}) ; remove the numbers.

(comment
  "This is the beginning of the depolarization extent of the babel fish. Increasing depolarization extent decreases Oscillation frequency of SO_stim is neutral."
  "Compatibility of systems of linear constraints over the set of natural numbers. Criteria of compatibility of a system of linear Diophantine equations, strict inequations, and nonstrict inequations are considered. Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given. These criteria and the corresponding algorithms for constructing a minimal supporting set of solutions can be used in solving all the considered types of systems and systems of mixed types."
  
  
  (let [stop-word-re (-> (dj.io/file "/home/bmillare/dj/usr/store/code-countries/scratch/hophacks2016/stoplist.txt")
                         (dj.io/eat)
                         parse-stop-words
                         (stop-word-regex))
        txt (dj.io/eat (dj.io/file "/home/bmillare/dj/usr/store/code-countries/scratch/hophacks2016/w2167e.txt"))
        sentences (split-sentences txt)
        candidates (generate-candidate-keywords sentences
                                                stop-word-re)
        w-scores (word-scores candidates)
        k-scores (candidate-keyword-scores candidates w-scores)]

    (sort-by second (vec k-scores))
    )


  )

;; My Comments on results
;; - 1-grams don't seem to be that useful
;; - more important to create map of space then pull 100% complete keyword list
;; - from scientific perspective, I feel 2-grams and 3-grams will probably be the most important
;;   - context from the sentence and even grammar could probably be put to good use to trim keyphrase candidates
