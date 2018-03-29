(ns v.algorithms.pagerank)

;;; Pagerank
;; - Famous algorithm by Larry Page (Google)
;; - Ranks nodes in a graph by how much other nodes point to it (and recursively what points to those nodes)
;;   - ranks nodes in graph by how much the rest of the graph point to it
;; - measure of node importance/significance

;; Given damping factor d (fraction), an actor will select a random
;; link on page A d times, and will select some other page 1-d
;; times. The actor will then select a node with pagerank X, fraction
;; X times.

;; Aka page rank gives probability distribution a random walk through
;; network will end up on a given node

;; Iterative algorithm
;; (Want basic algorithm description in engligh)
;; - (maybe also include simple math description)
(def d 0.85)

(defn simple-score
  "connectivity: node-id -> (set of node-ids it's connected to)
   old-scores: node-id -> score for that node
   id: id of node to compute score of"
  [connectivity old-scores id]
  (+ (- 1.0 d)
     (* d (reduce (fn [ret nid]
                    (+ ret
                       (/ (old-scores nid)
                          (count (connectivity nid)))))
                  0
                  (connectivity id)))))

(defn weighted-score
  "connectivity: node-id -> {nid -> weight}
   old-scores: node-id -> score for that node
   id: id of node to compute score of"
  [connectivity old-scores id]
  (+ (- 1.0 d)
     (* d (reduce (fn [ret nid]
                    (let [weights (connectivity nid)]
                      (+ ret
                         (/ (* (weights id) (old-scores nid))
                            ;; This should be precomputed and reused
                            (reduce + 0.0 (vals weights))))))
                  0
                  (keys (connectivity id))))))

(defn cached-weighted-score
  [cache]
  (fn [connectivity old-scores id]
    (+ (- 1.0 d)
       (* d (reduce (fn [ret nid]
                      (let [weights (connectivity nid)]
                        (+ ret
                           (/ (* (weights id) (old-scores nid))
                              (let [cache-value (@cache nid)]
                                (if cache-value
                                  cache-value
                                  (reset! cache (reduce + 0.0 (vals weights)))))))))
                    0
                    (keys (connectivity id)))))))

(defn score-graph
  [score-fn connectivity old-scores]
  (persistent!
   (reduce-kv (fn [ret k v]
                (assoc! ret
                        k
                        (score-fn connectivity
                                  old-scores
                                  k)))
              (transient {})
              old-scores)))

(defn pagerank
  "pick a score-fn that must match connectivity format, run pagerank for iterations"
  ([score-fn connectivity iterations]
   (let [initial-score
         (persistent!
          (reduce-kv (fn [ret k v]
                       (assoc! ret
                               k
                               1.0))
                     (transient {})
                     connectivity))]
     (reduce (fn [prev-score n]
               (score-graph score-fn connectivity prev-score))
             initial-score
             (range iterations))))
  ([score-fn connectivity]
   (pagerank score-fn connectivity 20)))

(comment

  (pagerank-debug
   simple-score
   {1 #{2}
    2 #{1 3}
    3 #{2}}
   20)

  (pagerank weighted-score
            {1 {2 1.0}
             2 {1 0.5
                3 1.0}
             3 {2 1.0}})
  )
