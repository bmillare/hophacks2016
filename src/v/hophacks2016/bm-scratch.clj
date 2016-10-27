(dj.dependencies/add-dependencies '[[clojure-opennlp "0.3.3"]]
                                  {:dj/repositories {"central" "http://repo1.maven.org/maven2/",
                                                     "clojars" "http://clojars.org/repo"}})

(dj.classloader/add-classpath (dj.io/file "/home/bmillare/dj/usr/src/hophacks2016/src"))

(require '[opennlp.nlp :as nlp])
(require 'v.1.dispatch.treefn)

(def get-keywords
  (v.1.dispatch.treefn/treefm
   (merge
    v.hophacks2016.keyword-extract/open-nlp
    v.hophacks2016.keyword-extract/np-chunking
    v.hophacks2016.keyword-extract/keyword-candidates
    v.hophacks2016.keyword-extract/keyword-connectivity
    v.hophacks2016.keyword-extract/keyword-ranking)
   :keyword.ranking/n-grams-ranking
   #_:keyword.ranking/pagerank
   #_:keyword.candidates/set-candidates))

(def result
  (get-keywords
   {:keyword.opennlp.input/models-prefix
    "/home/bmillare/dj/usr/src/hophacks2016/src/v/models"
    :keyword.input/txt
    (dj.io/eat (dj.io/file "/home/bmillare/dj/usr/src/hophacks2016/src/v/models/wl.txt"))
    #_"Optical control of the heart muscle is a promising strategy for cardiology because it is more specific than traditional electrical stimulation, and allows a higher temporal resolution than pharmacological interventions. Anion channelrhodopsins (ACRs) from cryptophyte algae expressed in cultured neonatal rat ventricular cardiomyocytes produced inhibitory currents at less than one-thousandth of the light intensity required by previously available optogenetic tools, such as the proton pump archaerhodopsin-3 (Arch). Because of their greater photocurrents, ACRs permitted complete inhibition of cardiomyocyte electrical activity under conditions in which Arch was inefficient. Most importantly, ACR expression allowed precisely controlled shortening of the action potential duration by switching on the light during its repolarization phase, which was not possible with previously used optogenetic tools. Optical shortening of cardiac action potentials may benefit pathophysiology research and the development of optogenetic treatments for cardiac disorders such as the long QT syndrome."
    #_"Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given."
    #_"The angry dog is walking across the street. Patrick has never had a donut."
 
    :keyword.opennlp.input/tokenize-model-name
    "en-token"
    :keyword.opennlp.input/pos-model-name
    "en-pos-maxent"
    :keyword.opennlp.input/sentences-model-name
    "en-sent"}
   ))

(def debug (atom nil))
@debug
(:keyword.ranking/n-grams-ranking result)
(:keyword.ranking/pagerank result)
(:keyword.candidates/set-candidates result)

(meta result)

;; ----------------------------------------------------------------------

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
                         (subs s 0 (dec (count s))))
                       (get-sentences txt))
        candidates (v.hophacks2016.rake2/generate-candidate-keywords
                    sentences
                    stop-word-re)
        w-scores (v.hophacks2016.rake2/word-scores candidates)
        k-scores (v.hophacks2016.rake2/candidate-keyword-scores candidates w-scores)]

    (sort-by second (vec k-scores))
    )


  )

