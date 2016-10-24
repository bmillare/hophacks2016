(dj.dependencies/add-dependencies '[[clojure-opennlp "0.3.3"]]
                                  {:dj/repositories {"central" "http://repo1.maven.org/maven2/",
                                                     "clojars" "http://clojars.org/repo"}})

(require '[opennlp.nlp :as nlp])

(def get-sentences (nlp/make-sentence-detector "/home/bmillare/dj/usr/src/hophacks2016/en-sent.bin"))

(nlp/make-sentence-detector "v/models/en-sent.bin")
(require '[v.hophacks2016.rake])
(get-sentences "Hello world! I am Mr. Smith. This is cool.")

(v.hophacks2016.rake2/split-sentences "Hello world! I am Mr. Smith. This is cool.")
(slurp "")

(dj.io/eat)

(dj.io/exists? (dj.io/file "v/models/en-sent.bin"))

(System/getProperty "user.dir")

(comment
  "This is the beginning of the depolarization extent of the babel fish. Increasing depolarization extent decreases Oscillation frequency of SO_stim is neutral."
  "Compatibility of systems of linear constraints over the set of natural numbers. Criteria of compatibility of a system of linear Diophantine equations, strict inequations, and nonstrict inequations are considered. Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given. These criteria and the corresponding algorithms for constructing a minimal supporting set of solutions can be used in solving all the considered types of systems and systems of mixed types."
  
  
  (let [stop-word-re (-> (dj.io/file "/home/bmillare/dj/usr/store/code-countries/scratch/hophacks2016/stoplist.txt")
                         (dj.io/eat)
                         v.hophacks2016.rake2/parse-stop-words
                         (v.hophacks2016.rake2/stop-word-regex))
        txt (dj.io/eat (dj.io/file "/home/bmillare/dj/usr/store/code-countries/scratch/hophacks2016/w2167e.txt"))
        sentences (map (fn [s]
                         (subs s 0 (dec (count s)))) (get-sentences txt))
        candidates (v.hophacks2016.rake2/generate-candidate-keywords sentences
                                                stop-word-re)
        w-scores (v.hophacks2016.rake2/word-scores candidates)
        k-scores (v.hophacks2016.rake2/candidate-keyword-scores candidates w-scores)]

    (sort-by second (vec k-scores))
    )


  )

