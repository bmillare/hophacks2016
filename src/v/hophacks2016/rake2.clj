(ns v.hophacks2016.rake2)

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
