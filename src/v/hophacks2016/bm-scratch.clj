(dj.dependencies/add-dependencies '[[clojure-opennlp "0.3.3"]]
                                  {:dj/repositories {"central" "http://repo1.maven.org/maven2/",
                                                     "clojars" "http://clojars.org/repo"}})

(dj.classloader/add-classpath (dj.io/file "/home/bmillare/dj/usr/src/hophacks2016/src"))

(require '[opennlp.nlp :as nlp])

(def get-keywords
  (v.1.dispatch.treefn/treefm
   (merge
    v.hophacks2016.keyword-extract/open-nlp
    v.hophacks2016.keyword-extract/np-chunking
    v.hophacks2016.keyword-extract/keyword-candidates
    v.hophacks2016.keyword-extract/keyword-connectivity
    v.hophacks2016.keyword-extract/keyword-ranking)
   :keyword.ranking/keywords))

(def result
  (get-keywords
   {:keyword.opennlp.input/models-prefix
    "/home/bmillare/dj/usr/src/hophacks2016/src/v/models"
    :keyword.input/txt
    "With the goal of non-invasively localizing cardiac ischemic disease using body-surface potential recordings, we attempted to reconstruct the transmembrane potential (TMP) throughout the myocardium with the bidomain heart model. The task is an inverse source problem governed by partial differential equations (PDE). Our main contribution is solving the inverse problem within a PDE-constrained optimization framework that enables various physically-based constraints in both equality and inequality forms. We formulated the optimality conditions rigorously in the continuum before deriving finite element discretization, thereby making the optimization independent of discretization choice. Such a formulation was derived for the L2-norm Tikhonov regularization and the total variation minimization. The subsequent numerical optimization was fulfilled by a primal–dual interior-point method tailored to our problem’s specific structure. Our simulations used realistic, fiber-included heart models consisting of up to 18,000 nodes, much finer than any inverse models previously reported. With synthetic ischemia data we localized ischemic regions with roughly a 10% false-negative rate or a 20% false-positive rate under conditions up to 5% input noise. With ischemia data measured from animal experiments, we reconstructed TMPs with roughly 0.9 correlation with the ground truth. While precisely estimating the TMP in general cases remains an open problem, our study shows the feasibility of reconstructing TMP during the ST interval as a means of ischemia localization"
    :keyword.opennlp.input/tokenize-model-name
    "en-token"
    :keyword.opennlp.input/pos-model-name
    "en-pos-maxent"
    :keyword.opennlp.input/sentences-model-name
    "en-sent"}
   ))

(:keyword.ranking/keywords result)


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

