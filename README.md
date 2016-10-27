# hophacks2016
literature exploration utilities

## vision
The long term vision is to build google maps for any body of literature

## implementation
Have keyword phrases of any text piece represent nodes, and all phrases within this piece now have an edge to the other phrases within the piece. The more text pieces relate the keyword pharses, the stronger the weight of the edge. When we apply the technique to all text pieces and aggregate the results, we get a graph of relatedness between keyword phrases. As a simple first step analysis, we will try to see how keyword phrase relatedness changes over time by computing the difference in edge weights over time. Our corpus will be scientific abstracts pulled from each year from pubmed. ftp://ftp.ncbi.nlm.nih.gov/pub/pmc/oa_bulk/

## issues
- none prioritized

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
