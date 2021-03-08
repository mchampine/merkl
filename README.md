# merkl
Streaming Merkle Roots and Proofs in Clojure

## Usage

```clojure
(ns merkl.yourmodule
  (:require [merkl.root :refer :all]
            [merkl.proof :refer :all]))

```

Require merkl.root for calculating roots. Also require merkl.proof to generate and verify streaming merkle proofs.

You can use an in-memory Clojure collection for your block stream, or load it from a file.

### In Memory

```clojure
;; arbitrary example block stream with 12 'blocks'
(def blkstream (mapv str (range 12)))

(merkle-root blkstream)
;; => [-40, 80, 93, -106, 75, 106, -86, 0, -126, -54, -24, -54, 60, -81, 3,
;;     64, 54, 45, -31, 62, -44, 60, 36, -13, -75, -110, -124, 74, 35, -87,
;;     28, -40]
```

### From file

For example, if you have a file of blocks (one per line), you can use line-seq to create a sequence for use with merkle-root:

```clojure
(defn file-stream-merkle-root [f]
  (with-open [rdr (clojure.java.io/reader f)]
    (merkle-root (line-seq rdr))))

(def test-root (file-stream-merkle-root "data/blks3.dat"))
;; => [21, 127, -117, 92, -94, -81, 34, -44, 26, -66, -54, 30, -92, -71,
;;     38, 40, -121, 90, 103, -73, 16, 1, 25, -119, -13, 21, 11, 49, 65,
;;     62, 114, 109]
```

### Streaming Merkle Proofs

Note: The implementation of prove-leaf reads the blocks stream from file incrementally. It doesn't work with an in-memory collection as the stream. The intent was to mirror the incremental reads in the pseudocode from the original paper.

#### Utils
```clojure
;; array equals
(defn array-eq? [ba1 ba2] (java.util.Arrays/equals ba1 ba2))

;; construct a proof for a given file and index
(defn get-proof [file index]
  (with-open [stream (clojure.java.io/reader file)]
    (prove-leaf stream index)))
```

Use prove-leaf (via get-proof which just wraps the file open) to create a proof for the leaf at index 2 in the stream:

```clojure
(def test-proof (get-proof "data/blks3.dat" 2))
```

Use verify-leaf to generate a root from the supplied stream and leaf at index 2.

```clojure
(def test-vl (verify-leaf 2 "2" test-proof))
```

Check that the root returned from verify-leaf matches the known root:

```clojure
(array-eq? test-root test-vl)
;; true
```

Check that supplying a different leaf to the verify produces a different root, and therefore the verification fails:

```clojure
(def test-vl-bad (verify-leaf 2 "x" r3-proof))
;; false
```

