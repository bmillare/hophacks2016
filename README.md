# hophacks2016
literature exploration utilities

## vision
The long term vision is to build google maps for any body of literature

## implementation
Have keyword phrases of any text piece represent nodes, and all phrases within this piece now have an edge to the other phrases within the piece. When applied to all text pieces, we get a graph of relatedness between keyword phrases. As a simple first step, we will try to see how keyword phrase relatedness changes over time using scientific abstracts pulled from each year from pubmed. ftp://ftp.ncbi.nlm.nih.gov/pub/pmc/oa_bulk/

## issues
- Currently, RAKE is implemented to extract the keywords but the accuracy is low, I think adding some context with parts of speech might help, also we can try to also implement or mix in TextRank
