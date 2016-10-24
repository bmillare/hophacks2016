(comment
  (do (require '[v.hophacks2016.rake :as rake]
       '[dj.dependencies])
    (dj.dependencies/add-dependencies '[[clojure-opennlp "0.3.3"]]
      {:dj/repositories {"central" "http://repo1.maven.org/maven2/"
                         "clojars" "http://clojars.org/repo"}})
   (require '[opennlp.nlp :as nlp]))

  (nlp/make-sentence-detector "v/models/en-sent.bin")
)

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