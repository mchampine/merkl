(ns merkl.proof
  (:require [merkl.root :as root]))

;; Streaming Merkle Proof and Verify (single leaf)
;; from Luke Champine's Paper: 
;;    "Streaming Merkle Proofs within Binary Numeral Trees"
;;    https://eprint.iacr.org/2021/038.pdf

;;;; Utility functions

;; stateful read - not the clojure way generally, but meant
;; to show the streaming nature of this proof / verify
(defn limit
  "Read (at most) n lines from rdr. If n=0 read the remainder of the stream"
  [rdr n]
  (loop [indx (dec n) acc [(.readLine rdr)]]
    (if (or (zero? indx) (nil? (last acc)))
      (if (last acc) acc (drop-last acc))
      (recur (dec indx) (conj acc (.readLine rdr))))))

(defn << [n] (bit-shift-left 1 n)) ;; returns 2^^n
(defn subroot [stream k] (root/merkle-root (limit stream (<< k))))
(defn read-leaf [stream] (limit stream 1))

(defn ones
  "Return the bit positions of 1's in i from most to least significant
  Example: 10 = 2r1010 => (3 1)  (ones at indexes 3 and 1)"
  [i]
  (->> (range (count (Integer/toString i 2)))
       (filter (partial bit-test i))
       reverse))

;; zeros must return zero bits up to log2(stream-size).
;; Since stream size may be unknown when streaming
;; choose max supported stream size. In this case 2^32 = ~4B
(defn zeros
  "Return the bit positions of 0's in i from least to most significant
  Example: 10 = 2r0..01010 => (0 2 4 5 6...) (zeros at index 0, 2, 4..31)"
  [i]
  (remove (partial bit-test i) (range 32)))

;;;; Streaming Merkle Proof

(defn prove-leaf
  "Streaming single-leaf merkle proof for leaf at index for nodes in file"
  [stream index]
  (let [pre (doall (for [k (ones index)] {:i k :subr (subroot stream k)}))
        l (first (read-leaf stream))
        post (doall (for [k (zeros index)] {:i k :subr (subroot stream k)}))]
    {:pre pre :leaf l :post (remove #(nil? (:subr %)) post)}))

;;;; Verify Proof (single leaf)

(defn- load-stack [stk blks]
  (reduce (fn [s {:keys [i subr]}] (root/insert s subr i)) stk blks))

(defn root-from-proof-and-leaf
  "Generate a verification hash of leaf (to verify) and the proof
   Proof consists of subroots before the index (pre) and after the index (post)"
  [leaf proof]
  (-> {}
      (load-stack (:pre proof))
      (root/insert (root/leaf-hash leaf) 0)
      (load-stack (:post proof))
      root/finalize))

(defn verify-leaf
  "Use a proof to verify that a given leaf exists in a stream of blocks"
  [knownroot leaf proof]
  (= (root/ba->hex knownroot)
     (root/ba->hex (root-from-proof-and-leaf leaf proof))))
