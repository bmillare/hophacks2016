(ns v.hophacks2016.babluki-np-extractor
  (:require [opennlp.nlp :as nlp]
            [v.hophacks2016.pagerank :as pr]
            [v.1.algorithms.peg :as peg]
            [v.1.dispatch.graphfn :as gf]))

;; trying to make a clojure / open-nlp verison of https://gist.github.com/shlomibabluki/5539628

;; I think the key insights are optimized (faster/simpler) tagging and
;; then custom noun-phrase chunking

;; Main point is to not do full parsing that identifies VP in addition
;; to NP and all the other stuff, but to just quickly scan for NP

(def path-prefix (case (System/getProperty "user.dir")
                   "/home/bmillare/dj/usr/src/dj.scratch"
                   "/home/bmillare/dj/usr/src/hophacks2016/src/v"))

(def tokenize (nlp/make-tokenizer (dj/str-path path-prefix "models/en-token.bin")))

(def pos-tag (nlp/make-pos-tagger (dj/str-path path-prefix "models/en-pos-maxent.bin")))

(def get-sentences (nlp/make-sentence-detector (dj/str-path path-prefix "models/en-sent.bin")))

(pos-tag (tokenize "The granny smith apple gave a car to his son on Friday"))
(pos-tag (tokenize "Mr. Anderson is tall"))
(pos-tag (tokenize "The Andersons will be joining us tonight"))

(pos-tag (tokenize "Many processing techniques are useful for getting straight A's"))
(["Many" "JJ"]
 ["processing" "NN"]
 ["techniques" "NNS"]
 ["are" "VBP"]
 ["useful" "JJ"]
 ["for" "IN"]
 ["getting" "VBG"]
 ["straight" "JJ"]
 ["A" "NNP"]
 ["'s" "POS"])

(pos-tag (tokenize "Tall frightening boyfriends are useful for grabbing tall apples"))
(["Tall" "JJ"]
 ["frightening" "JJ"]
 ["boyfriends" "NNS"]
 ["are" "VBP"]
 ["useful" "JJ"]
 ["for" "IN"]
 ["grabbing" "VBG"]
 ["tall" "JJ"]
 ["apples" "NNS"])

(pos-tag (tokenize "The carefully playing children are sick"))

(pos-tag (tokenize "My jogging shorts are smelly"))
(pos-tag (tokenize "The boy girl shorts are awesome"))

(pos-tag (tokenize "The fat tall dog took a poop"))

(pos-tag (tokenize "In this study, we developed a mitochondrial model exhibiting both IMAC-mediated and MPTP-mediated RIRR, and examined how the dynamics of these two RIRR mechanisms interact to produce Dpsi oscillations and waves in a two-dimensional diffusively coupled mitochondrial network"))
(["In" "IN"]
 ["this" "DT"]
 ["study" "NN"]
 ["," ","]
 ["we" "PRP"]
 ["developed" "VBD"]
 ["a" "DT"]
 ["mitochondrial" "JJ"]
 ["model" "NN"]
 ["exhibiting" "VBG"]
 ["both" "DT"]
 ["IMAC-mediated" "NNP"]
 ["and" "CC"]
 ["MPTP-mediated" "NNP"]
 ["RIRR" "NNP"]
 ["," ","]
 ["and" "CC"]
 ["examined" "VBD"]
 ["how" "WRB"]
 ["the" "DT"]
 ["dynamics" "NNS"]
 ["of" "IN"]
 ["these" "DT"]
 ["two" "CD"]
 ["RIRR" "JJ"]
 ["mechanisms" "NNS"]
 ["interact" "NN"]
 ["to" "TO"]
 ["produce" "VB"]
 ["Dpsi" "NNP"]
 ["oscillations" "NNS"]
 ["and" "CC"]
 ["waves" "NNS"]
 ["in" "IN"]
 ["a" "DT"]
 ["two-dimensional" "JJ"]
 ["diffusively" "JJ"]
 ["coupled" "VBN"]
 ["mitochondrial" "JJ"]
 ["network" "NN"])

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
      #_"Solutions can be used in solving all the considered types systems and systems of mixed types."
      "Compatibility of systems of linear constraints over the set of natural numbers. Criteria of compatibility of a system of linear Diophantine equations, strict inequations, and nonstrict inequations are considered. Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given. These criteria and the corresponding algorithms for constructing a minimal supporting set of solutions can be used in solving all the considered types systems and systems of mixed types."
      #_(dj.io/eat (dj.io/file "/home/bmillare/dj/usr/store/code-countries/scratch/hophacks2016/w2167e.txt"))
      gfns {:JJ (gf/fn #{} #{}
                  (peg/+ (peg/| (t "JJ") (t "JJR") (t "JJS") (t "VBG") (t "VBN"))))
            :NN (gf/fn #{} #{}
                  (peg/| (t "NN") (t "NNS")))
            :NNI (gf/fn #{:NN :JJ} #{}
                   (peg/| (peg/+ NN)
                          (peg/alt (peg/s JJ (peg/+ NN))
                                   (fn [[j ns]]
                                     (into j ns)))))
            :NNP (gf/fn #{} #{}
                   (peg/+ (peg/| (t "NNP") (t "NNPS"))))
            :ELSE (gf/fn #{} #{:NNI}
                    (peg/| (ct (fn [[_ tag]]
                                 (not (#{"JJ" "JJR" "JJS" "VBG" "VBN" "NN" "NNS" "NNP" "NNPS"} tag))))
                           (peg/s (ct (fn [[_ tag]]
                                        (not (#{"NN" "NNS" "NNP" "NNPS"} tag))))
                                  (peg/!? NNI))))
            :GETNP (gf/fn #{} #{:ELSE :NNP :NNI}
                     (peg/| (peg/alt (peg/s (peg/* ELSE) (peg/| NNI NNP))
                                     (fn [[_ NP]]
                                       NP))
                            (peg/alt (peg/+ ELSE)
                                     (fn [_]
                                       nil))))}
      phrase (-> gfns
                 (gf/mutual-graphfn :GETNP)
                 :GETNP)
      parse (fn [pairs]
              (peg/parse phrase pairs))
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
      frequencies (reduce (fn [ret candidate]
                            (update ret
                                    candidate
                                    (fn [v]
                                      (inc (or v 0)))))
                          {}
                          candidates)]
  (reduce (fn [ret sentence]
                           (into ret
                                 (loop [remaining-input (pos-tag (tokenize sentence))
                                        NPs []]
                                   (if (empty? remaining-input)
                                     NPs
                                     (let [{:keys [result unconsumed-input]} (parse remaining-input)]
                                       (recur unconsumed-input
                                              (if result
                                                (conj NPs result)
                                                NPs)))))))
                         []
                         (get-sentences corpus))
  #_(sort-by second (vec frequencies)))

;; Try my own pagerank method

(let [corpus
      #_"Solutions can be used in solving all the considered types systems and systems of mixed types."
      #_"Compatibility of systems of linear constraints over the set of natural numbers. Criteria of compatibility of a system of linear Diophantine equations, strict inequations, and nonstrict inequations are considered. Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given. These criteria and the corresponding algorithms for constructing a minimal supporting set of solutions can be used in solving all the considered types systems and systems of mixed types."
      #_"Periodic cellwide depolarizations of mitochondrial membrane potential (Dpsi ) which are triggered by reactive
oxygen species (ROS) and propagated by ROS-induced ROS release (RIRR) have been postulated to contribute to cardiac
arrhythmogenesis and injury during ischemia/reperfusion. Two different modes of RIRR have been described: Dpsi oscillations
involving ROS-sensitive mitochondrial inner membrane anion channels (IMAC), and slow depolarization waves related to mitochondrial permeability transition pore (MPTP) opening. In this study, we developed a computational model of mitochondria
exhibiting both IMAC-mediated RIRR and MPTP-mediated RIRR, diffusively coupled in a spatially extended network, to study
the spatiotemporal dynamics of RIRR on Dpsi . Our major findings are: 1), as the rate of ROS production increases, mitochondria
can exhibit either oscillatory dynamics facilitated by IMAC opening, or bistable dynamics facilitated by MPTP opening; 2), in
a diffusively-coupled mitochondrial network, the oscillatory dynamics of IMAC-mediated RIRR results in rapidly propagating
(~25 mm/s) cellwide Dpsi oscillations, whereas the bistable dynamics of MPTP-mediated RIRR results in slow (0.1–2 mm/s)
Dpsi depolarization waves; and 3), the slow velocity of the MPTP-mediated depolarization wave is related to competition between
ROS scavenging systems and ROS diffusion. Our observations provide mechanistic insights into the spatiotemporal dynamics
underlying RIRR-induced Dpsi oscillations and waves observed experimentally in cardiac myocytes."
      #_"Loss of mitochondrial function is a fundamental determinant of cell injury and death. In heart cells under metabolic stress,
we have previously described how the abrupt collapse or oscillation of the mitochondrial energy state is synchronized
across the mitochondrial network by local interactions dependent upon reactive oxygen species (ROS). Here, we develop a
mathematical model of ROS-induced ROS release (RIRR) based on reaction-diffusion (RD-RIRR) in one- and two-dimensional
mitochondrial networks. The nodes of the RD-RIRR network are comprised of models of individual mitochondria that
include a mechanism of ROS-dependent oscillation based on the interplay between ROS production, transport, and
scavenging; and incorporating the tricarboxylic acid (TCA) cycle, oxidative phosphorylation, and Ca 2+ handling. Local
mitochondrial interaction is mediated by superoxide (O 2.2 ) diffusion and the O 2.2 -dependent activation of an inner
membrane anion channel (IMAC). In a 2D network composed of 500 mitochondria, model simulations reveal DY m
depolarization waves similar to those observed when isolated guinea pig cardiomyocytes are subjected to a localized laser-
flash or antioxidant depletion. The sensitivity of the propagation rate of the depolarization wave to O 2.2 diffusion,
production, and scavenging in the reaction-diffusion model is similar to that observed experimentally. In addition, we
present novel experimental evidence, obtained in permeabilized cardiomyocytes, confirming that DY m depolarization is
mediated specifically by O 2.2 . The present work demonstrates that the observed emergent macroscopic properties of the
mitochondrial network can be reproduced in a reaction-diffusion model of RIRR. Moreover, the findings have uncovered a
novel aspect of the synchronization mechanism, which is that clusters of mitochondria that are oscillating can entrain
mitochondria that would otherwise display stable dynamics. The work identifies the fundamental mechanisms leading from
the failure of individual organelles to the whole cell, thus it has important implications for understanding cell death during
the progression of heart disease."
      #_"We produced transgenic mice that express dominant
negative Kir6.x pore-forming subunits (Kir6.1-AAA or Kir6.2-AAA)
in cardiac myocytes by driving their expression with the ␣-myosin
heavy chain promoter. Weight gain and development after birth of
these mice were similar to nontransgenic mice, but an increased
mortality was noted after the age of 4 –5 mo. Transgenic mice lacked
cardiac K ATP channel activity as assessed with patch clamp tech-
niques. Consistent with a decreased current density observed at
positive voltages, the action potential duration was increased in these
mice. Some myocytes developed EADs after isoproterenol treatment.
Hemodynamic measurements revealed no significant effects on ven-
tricular function (apart from a slightly elevated heart rate), whereas in
vivo electrophysiological recordings revealed a prolonged ventricular
effective refractory period in transgenic mice. The transgenic mice
tolerated stress less well as evident from treadmill stress tests. The
proarrhythmogenic features and lack of adaptation to a stress response
in transgenic mice suggest that these features are intrinsic to the
myocardium and that K ATP channels in the myocardium have an
important role in protecting the heart from lethal arrhythmias and
adaptation to stress situations."
      #_"ATP-sensitive potassium
(K ATP ) channels are present in the surface and internal membranes of cardiac, skeletal, and smooth muscle cells and
provide a unique feedback between muscle cell metabolism and electrical activity. In so doing, they can play an
important role in the control of contractility, particularly when cellular energetics are compromised, protecting the
tissue against calcium overload and fiber damage, but the cost of this protection may be enhanced arrhythmic
activity. Generated as complexes of Kir6.1 or Kir6.2 pore-forming subunits with regulatory sulfonylurea receptor
subunits, SUR1 or SUR2, the differential assembly of K ATP channels in different tissues gives rise to tissue-specific
physiological and pharmacological regulation, and hence to the tissue-specific pharmacological control of contrac-
tility. The last 10 years have provided insights into the regulation and role of muscle K ATP channels, in large part
driven by studies of mice in which the protein determinants of channel activity have been deleted or modified. As
yet, few human diseases have been correlated with altered muscle K ATP activity, but genetically modified animals
give important insights to likely pathological roles of aberrant channel activity in different muscle types."
      #_"Mitochondrial membrane potential (⌬⌿ m ) depolarization
contributes to cell death and electrical and contractile dysfunc-
tion in the post-ischemic heart. An imbalance between mito-
chondrial reactive oxygen species production and scavenging
was previously implicated in the activation of an inner mem-
brane anion channel (IMAC), distinct from the permeability
transition pore (PTP), as the first response to metabolic stress in
cardiomyocytes. The glutathione redox couple, GSH/GSSG,
oscillated in parallel with ⌬⌿ m and the NADH/NAD ⴙ redox
state. Here we show that depletion of reduced glutathione is an
alternative trigger of synchronized mitochondrial oscillation in
cardiomyocytes and that intermediate GSH/GSSG ratios cause
reversible ⌬⌿ m depolarization, although irreversible PTP acti-
vation is induced by extensive thiol oxidation. Mitochondrial
dysfunction in response to diamide occurred in stages, progress-
ing from oscillations in ⌬⌿ m to sustained depolarization, in
association with depletion of GSH. Mitochondrial oscillations
were abrogated by 4ⴕ-chlorodiazepam, an IMAC inhibitor,
whereas cyclosporin A was ineffective. In saponin-permeabi-
lized cardiomyocytes, the thiol redox status was systematically
clamped at GSH/GSSG ratios ranging from 300:1 to 20:1. At
ratios of 150:1–100:1, ⌬⌿ m depolarized reversibly, and a
matrix-localized fluorescent marker was retained; however,
decreasing the GSH/GSSG to 50:1 irreversibly depolarized ⌬⌿ m
and induced maximal rates of reactive oxygen species produc-
tion, NAD(P)H oxidation, and loss of matrix constituents. Mito-
chondrial GSH sensitivity was altered by inhibiting either GSH
uptake, the NADPH-dependent glutathione reductase, or the
NADH/NADPH transhydrogenase, indicating that matrix GSH
regeneration or replenishment was crucial. The results indicate
that GSH/GSSG redox status governs the sequential opening of
mitochondrial ion channels (IMAC before PTP) triggered by
thiol oxidation in cardiomyocytes."
      "With the goal of non-invasively localizing cardiac ischemic disease using body-surface potential recordings, we attempted to reconstruct the transmembrane potential (TMP) throughout the myocardium with the bidomain heart model. The task is an inverse source problem governed by partial differential equations (PDE). Our main contribution is solving the inverse problem within a PDE-constrained optimization framework that enables various physically-based constraints in both equality and inequality forms. We formulated the optimality conditions rigorously in the continuum before deriving finite element discretization, thereby making the optimization independent of discretization choice. Such a formulation was derived for the L2-norm Tikhonov regularization and the total variation minimization. The subsequent numerical optimization was fulfilled by a primal–dual interior-point method tailored to our problem’s specific structure. Our simulations used realistic, fiber-included heart models consisting of up to 18,000 nodes, much finer than any inverse models previously reported. With synthetic ischemia data we localized ischemic regions with roughly a 10% false-negative rate or a 20% false-positive rate under conditions up to 5% input noise. With ischemia data measured from animal experiments, we reconstructed TMPs with roughly 0.9 correlation with the ground truth. While precisely estimating the TMP in general cases remains an open problem, our study shows the feasibility of reconstructing TMP during the ST interval as a means of ischemia localization"
      #_(dj.io/eat (dj.io/file "/home/bmillare/dj/usr/store/code-countries/scratch/hophacks2016/w2167e.txt"))
      gfns {:JJ (gf/fn #{} #{}
                  (peg/+ (peg/| (t "JJ") (t "JJR") (t "JJS") (t "VBG") (t "VBN"))))
            :NN (gf/fn #{} #{}
                  (peg/| (t "NN") (t "NNS")))
            :NNI (gf/fn #{:NN :JJ} #{}
                   (peg/| (peg/+ NN)
                          (peg/alt (peg/s JJ (peg/+ NN))
                                   (fn [[j ns]]
                                     (into j ns)))))
            :NNP (gf/fn #{} #{}
                   (peg/+ (peg/| (t "NNP") (t "NNPS"))))
            :ELSE (gf/fn #{} #{:NNI}
                    (peg/| (ct (fn [[_ tag]]
                                 (not (#{"JJ" "JJR" "JJS" "VBG" "VBN" "NN" "NNS" "NNP" "NNPS"} tag))))
                           (peg/s (ct (fn [[_ tag]]
                                        (not (#{"NN" "NNS" "NNP" "NNPS"} tag))))
                                  (peg/!? NNI))))
            :GETNP (gf/fn #{} #{:ELSE :NNP :NNI}
                     (peg/| (peg/alt (peg/s (peg/* ELSE) (peg/| NNI NNP))
                                     (fn [[_ NP]]
                                       NP))
                            (peg/alt (peg/+ ELSE)
                                     (fn [_]
                                       nil))))}
      phrase (-> gfns
                 (gf/mutual-graphfn :GETNP)
                 :GETNP)
      parse (fn [pairs]
              (peg/parse phrase pairs))
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

;; seems that keywords-rank with values over 1.0 are good
(pos-tag (tokenize "Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given."))



;; 1. CC    Coordinating conjunction
;; 2. CD    Cardinal number
;; 3. DT    Determiner
;; 4. EX    Existential there
;; 5. FW    Foreign word
;; 6. IN    Preposition or subordinating conjunction
;; 7. JJ    Adjective
;; 8. JJR   Adjective, comparative
;; 9. JJS   Adjective, superlative
;; 10. LS   List item marker
;; 11. MD   Modal
;; 12. NN   Noun, singular or mass
;; 13. NNS  Noun, plural
;; 14. NNP  Proper noun, singular
;; 15. NNPS Proper noun, plural
;; 16. PDT  Predeterminer
;; 17. POS  Possessive ending
;; 18. PRP  Personal pronoun
;; 19. PRP  Possessive pronoun
;; 20. RB   Adverb
;; 21. RBR  Adverb, comparative
;; 22. RBS  Adverb, superlative
;; 23. RP   Particle
;; 24. SYM  Symbol
;; 25. TO   to
;; 26. UH   Interjection
;; 27. VB   Verb, base form
;; 28. VBD  Verb, past tense
;; 29. VBG  Verb, gerund or present participle
;; 30. VBN  Verb, past participle
;; 31. VBP  Verb, non-3rd person singular present
;; 32. VBZ  Verb, 3rd person singular present
;; 33. WDT  Wh-determiner
;; 34. WP   Wh-pronoun
;; 35. WP$  Possessive wh-pronoun
;; 36. WRB  Wh-adverb
