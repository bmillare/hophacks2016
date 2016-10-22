# hophacks2016
literature exploration utilities

## vision
The long term vision is to build google maps for any body of literature

## implementation
Have keyword phrases of any text piece represent nodes, and all phrases within this piece now have an edge to the other phrases within the piece. The more text pieces relate the keyword pharses, the stronger the weight of the edge. When we apply the technique to all text pieces and aggregate the results, we get a graph of relatedness between keyword phrases. As a simple first step analysis, we will try to see how keyword phrase relatedness changes over time by computing the difference in edge weights over time. Our corpus will be scientific abstracts pulled from each year from pubmed. ftp://ftp.ncbi.nlm.nih.gov/pub/pmc/oa_bulk/

## issues
- Currently, RAKE is implemented to extract the keywords but the accuracy is low, I think adding some context with parts of speech might help, also we can try to also implement or mix in TextRank
  - I would prefer not having to use supervised approaches at the moment
  - We will probably need to look at OpenNLP to take advantage of POS tools in Clojure https://opennlp.apache.org/
    - https://github.com/dakrone/clojure-opennlp
