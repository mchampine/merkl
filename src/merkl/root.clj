(ns merkl.root
  (:require [buddy.core.hash :as hash]))

;; Streaming Merkle Root

;; from Luke Champine's Paper: 
;;    "Streaming Merkle Proofs within Binary Numeral Trees"
;;    https://eprint.iacr.org/2021/038.pdf

;; utils

;; More compact display of byte arrays (hash function output)
(defn ba->hex [s] (apply str (map #(format "%02X" %) s)))

;; default to ba->hex for printing byte arrays
(defmethod print-method (Class/forName "[B") [v ^java.io.Writer w]
  (.write w (ba->hex v)))

;; blake2b-256 for the hash function as in the Sia blockchain
(defn leaf-hash [n] (hash/blake2b-256 (.getBytes n)))
(defn parent-hash [l r] (hash/blake2b-256 (byte-array (concat l r))))

(defn foldr [f coll]
  "Right Fold: (foldr f [1 2 3 4] => (f 1 (f 2 (f 3 4))"
  (reduce #(f %2 %1) (reverse coll)))

;; Calculate Merkle Root

(defn insert
  "Add node v with key n to map s.
  When a node is already present at key n, merge the new node with the
  existing value at n, by creating a new parent hash of (existing, new)."
  ([s v] (insert s v 0))
  ([s v n]
   (if (get s n)
     (insert (dissoc s n) (parent-hash (get s n) v) (inc n))
     (assoc s n v))))

(defn finalize
  "Heights (sorted high to low) are parent-hashed with a right fold."
  [s]
  (foldr parent-hash (vals (reverse (sort s)))))

(defn merkle-root
  "Calculate the Merkle root of nodes in stream"
  [stream]
  (if (nil? (first stream)) nil
    (->> (reduce (fn [s v] (insert s (leaf-hash v))) {} stream)
         finalize)))

;;;; arbitrary example block stream with 12 'blocks'
(def blkstream (mapv str (range 12)))

;; (merkle-root blkstream)
;;;; D8505D964B6AAA0082CAE8CA3CAF0340362DE13ED43C24F3B592844A23A91CD8


;;;;;;;;; ALTERNATIVE 'no delete' on INSERTs ;;;;;;;;;;

;; no-delete inserts
(defn insert-nodel
  ([[stk i] v] (insert-nodel [stk (inc i)] v 0))
  ([[stk i] v n]
   (if (bit-test i n)
     (insert-nodel [stk i] (parent-hash (get stk n) v) (inc n))
     [(assoc stk n v) i])))

;; get only live nodes

;; v1
;; (defn log2 [n] (int (Math/ceil (/ (Math/log n) (Math/log 2)))))
;; (defn ones' [i] (filter (partial bit-test i) (range (log2 i))))
;; (defn- live-nodes [[stk i]] (select-keys stk (ones' (inc i))))

;; v2 curr
;; (defn- live-nodes [[stk i]] (select-keys stk (merkl.proof/ones (inc i))))

;; v3
(defn- live-nodes [[stk i]]
  (apply merge (keep-indexed #(when (bit-test (inc i) %1) {%1 %2}) stk)))

(defn merkle-root-nodel
  "Calculate the Merkle root of nodes in stream"
  [stream]
  (if (nil? (first stream)) nil
      (-> (reduce (fn [s v] (insert-nodel s (leaf-hash v))) [[] -1] stream)
          live-nodes
          finalize)))

(merkle-root-nodel blkstream)
;; => D8505D964B6AAA0082CAE8CA3CAF0340362DE13ED43C24F3B592844A23A91CD8
;;
;; => [-40, 80, 93, -106, 75, 106, -86, 0, -126, -54, -24, -54, 60, -81, 3,
;;     64, 54, 45, -31, 62, -44, 60, 36, -13, -75, -110, -124, 74, 35, -87,
;;     28, -40]
