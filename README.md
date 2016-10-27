# hophacks2016
literature exploration utilities

## vision
The long term vision is to build google maps for any body of literature

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
