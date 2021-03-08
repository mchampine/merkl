(ns merkl.root
  (:require [buddy.core.hash :as hash]))

;; Streaming Merkle Root

;; from Luke Champine's Paper: 
;;    "Streaming Merkle Proofs within Binary Numeral Trees"
;;    https://eprint.iacr.org/2021/038.pdf

;; utils

;; More compact display of byte arrays (hash function output)
(defn ba->hex [s] (format "%X" (java.math.BigInteger. s)))

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

;; arbitrary example block stream with 12 'blocks'
(def blkstream (mapv str (range 12)))

(ba->hex (merkle-root blkstream))
;; => "-27AFA269B49555FF7D351735C350FCBFC9D21EC12BC3DB0C4A6D7BB5DC56E328"

;;;;;;;;; ALTERNATIVE 'no delete' on INSERTs ;;;;;;;;;;

;; no-delete inserts
(defn insert-nodel
  ([s v] (insert s v 0))
  ([s v n]
   (if (get s n)
     (insert-nodel s (parent-hash (get s n) v) (inc n))
     (assoc s n v))))

;; determine which nodes are 'live' from stream size
(defn live? [stream-size idx] (bit-test stream-size idx))
(defn live-nodes [ssize strm] (filter #(live? ssize (key %)) strm))

(defn merkle-root-nodel
  "Calculate the Merkle root of nodes in stream"
  [stream]
  (if (nil? (first stream)) nil
      (->> (reduce (fn [s v] (insert-nodel s (leaf-hash v))) {} stream)
           (live-nodes (count stream))
           finalize)))

(ba->hex (merkle-root-nodel blkstream))
;; => "-27AFA269B49555FF7D351735C350FCBFC9D21EC12BC3DB0C4A6D7BB5DC56E328"
