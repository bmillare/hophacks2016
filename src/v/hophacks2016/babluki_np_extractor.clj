(ns v.hophacks2016.babluki-np-extractor
  (:require [opennlp.nlp :as nlp]
            [v.1.algorithms.peg :as peg]
            [v.1.dispatch.graphfn :as gf]))

;; trying to make a clojure / open-nlp verison of https://gist.github.com/shlomibabluki/5539628

;; I think the key insights are optimized (faster/simpler) tagging and
;; then custom noun-phrase chunking

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

;; CFG
;; NNP: NNP+NNP
;; NNI: NN+NN | NNI+NN | JJ+NN
;; JJ: JJ+JJ

(let [corpus (dj.io/eat (dj.io/file "/home/bmillare/dj/usr/store/code-countries/scratch/hophacks2016/w2167e.txt"))
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
                     (peg/| (peg/alt (peg/s (peg/* ELSE) (peg/| NNP NNI))
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
  (sort-by second (vec frequencies)))



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
