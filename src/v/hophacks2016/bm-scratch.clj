#_ (dj.dependencies/add-dependencies '[[clojure-opennlp "0.3.3"]]
                                     {:dj/repositories {"central" "http://repo1.maven.org/maven2/",
                                                        "clojars" "http://clojars.org/repo"}})

#_ (dj.classloader/add-classpath (dj.io/file "/home/bmillare/dj/usr/src/hophacks2016/src"))
#_ (dj.dependencies/resolve-project "hophacks2016" user/sys)

(require '[v.dispatch.treefn]
         '[v.hophacks2016.keyword-extract])

;; build aggregated get-keywords function who's root is :keyword.ranking/n-grams-ranking
;; see code in v/hophacks2016.keyword-extract.clj
(def get-keywords
  (v.dispatch.treefn/treefm
   (merge
    v.hophacks2016.keyword-extract/open-nlp
    v.hophacks2016.keyword-extract/np-chunking
    v.hophacks2016.keyword-extract/keyword-candidates
    v.hophacks2016.keyword-extract/keyword-connectivity
    v.hophacks2016.keyword-extract/keyword-ranking)
   :keyword.ranking/n-grams-ranking))

(def result
  (get-keywords
   {
    ;; open-nlp specific parameters
    :keyword.opennlp.input/models-prefix
    "/home/bmillare/dj/usr/src/hophacks2016/src/v/models"
    :keyword.opennlp.input/tokenize-model-name
    "en-token"
    :keyword.opennlp.input/pos-model-name
    "en-pos-maxent"
    :keyword.opennlp.input/sentences-model-name
    "en-sent"

   ;; input text
   :keyword.input/txt
    #_(dj.io/eat (dj.io/file "/home/bmillare/dj/usr/store/code-countries/scratch/hophacks2016/w2167e.txt"))
    "Optical control of the heart muscle is a promising strategy for cardiology because it is more specific than traditional electrical stimulation, and allows a higher temporal resolution than pharmacological interventions. Anion channelrhodopsins (ACRs) from cryptophyte algae expressed in cultured neonatal rat ventricular cardiomyocytes produced inhibitory currents at less than one-thousandth of the light intensity required by previously available optogenetic tools, such as the proton pump archaerhodopsin-3 (Arch). Because of their greater photocurrents, ACRs permitted complete inhibition of cardiomyocyte electrical activity under conditions in which Arch was inefficient. Most importantly, ACR expression allowed precisely controlled shortening of the action potential duration by switching on the light during its repolarization phase, which was not possible with previously used optogenetic tools. Optical shortening of cardiac action potentials may benefit pathophysiology research and the development of optogenetic treatments for cardiac disorders such as the long QT syndrome."
    #_"Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given."
    #_"The angry dog is walking across the street. Patrick has never had a donut."

    }))

(comment

  ;; run this in a REPL to get keyword rankings
  (:keyword.ranking/n-grams-ranking result)
  



  ;; run this to get pagerankings for individual words filtered by
  ;; noun phrase candidates
  (:keyword.ranking/pagerank result)

  ;; run this to get timings
  (meta result)

  (meta get-keywords)

  )
