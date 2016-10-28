# hophacks2016
literature exploration utilities

## vision
The long term vision is to build google maps for any body of literature

## how to run

From the command line:
```bash
cd cooldirectory
git clone https://github.com/bmillare/hophacks2016.git
cd hophacks2016
lein trampoline repl
```

Within the clojure REPL:
```clojure
(load "v/hophacks2016/bm-scratch")
(:keyword.ranking/n-grams-ranking result)
```
The output will be:

```clojure
([["cardiac" "action" "potentials"] 1.3994729154205503] [["used" "optogenetic" "tools"] 1.3778488435346672] [["available" "optogenetic" "tools"] 1.3778488435346672] [["cardiomyocyte" "electrical" "activity"] 1.358787446857419] [["optogenetic" "treatments"] 1.3269831640459446] [["Optical" "shortening"] 1.2548805858319163] [["cardiac" "disorders"] 1.1870146482689359] [["proton" "pump" "archaerhodopsin-3"] 1.144782537379599] [["ventricular" "cardiomyocytes"] 1.1235921601992676] [["promising" "strategy"] 1.1104127565646165] [["traditional" "electrical" "stimulation"] 1.0869285126694592] [["cultured" "neonatal" "rat"] 1.0831789157772558] [["higher" "temporal" "resolution"] 1.0331384003345274] [["light" "intensity"] 1.0] [["cryptophyte" "algae"] 1.0] [["pathophysiology" "research"] 1.0] [["repolarization" "phase"] 1.0] [["heart" "muscle"] 1.0] [["complete" "inhibition"] 0.9538610206445015] [["pharmacological" "interventions"] 0.9005847989964181] [["channelrhodopsins" "(ACRs"] 0.9005847989964181] [["greater" "photocurrents"] 0.8832599225183109] [["controlled" "shortening"] 0.8826140009659376] [["Optical" "control"] 0.8777915766206601] [["potential" "duration"] 0.8168945261873111] [["inhibitory" "currents"] 0.7516394661348487])
```
Feel free to modify v/hophacks2016/bm-scratch.clj to play with different inputs and parameters

## implementation
Have keyword phrases of any text piece represent nodes, and all phrases within this piece now have an edge to the other phrases within the piece. The more text pieces relate the keyword pharses, the stronger the weight of the edge. When we apply the technique to all text pieces and aggregate the results, we get a graph of relatedness between keyword phrases. As a simple first step analysis, we will try to see how keyword phrase relatedness changes over time by computing the difference in edge weights over time. Our corpus will be scientific abstracts pulled from each year from pubmed. ftp://ftp.ncbi.nlm.nih.gov/pub/pmc/oa_bulk/

## issues
- using pagerank to seed k-means might be suboptimal compared to divrank
  - http://clair.si.umich.edu/~radev/papers/SIGKDD2010.pdf
  - basically, imagine a "complex" city which comprises multiple sub-districts with no dominating district and that contrasts to a "simple" city which only has a singular dominant district. Divrank ranks the two regions as two cities where pagerank would provide a ranking of districts. This can cause missing the forest among the trees. In human communications, we typically refer to regions through the city name, not through the group of districts as this enables compression of information into a smaller term. Divrank is thus better at aggregating dense regions into a singular cluster, which makes it more appropriate for k-means, and perhaps recursive k-means.

## tasks
- [X] Re-write src/hophacks2016/rake.clj as rake2.clj but using opennlp instead of the homebrew code for sentence breakdown
- [X] use openNLP POS to filter out bad keyword phrase candidates
      - use https://github.com/davidadamojr/TextRank to see how they use POS to make better decisions
- [ ] Add normalization such as stemming and lemmatisation to keyword extractor (low priority)

## phases
- [X] 1. Implement an accurate enough keyword phrase extraction method
- [ ] 2. Download corpus to desktop computer
- [ ] 3. Parse & generate keyword phrase graph edge data and write to disk (design custom output format? or use EDN?)
- [ ] 4. Graph analysis (rank relatedness delta per year)
