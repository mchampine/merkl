# merkl
Clojure Implementation of Streaming Merkle Root, Proof, and Verify (single leaf) from Luke Champine's paper: **Streaming Merkle Proofs within Binary Numeral Trees** @ https://eprint.iacr.org/2021/038.pdf

## Usage

Require merkl.root for calculating roots. Require merkl.proof to generate and verify streaming merkle proofs.

```clojure
(ns your.ns
  (:require [merkl.root :refer :all]
            [merkl.proof :refer :all]))
```

## Calculate Roots

You can use an in-memory Clojure collection for your block stream, or load it from a file.

### In Memory

```clojure
;; arbitrary example block stream with 12 'blocks'
(def blkstream (mapv str (range 12)))

(merkle-root blkstream)
;; D8505D964B6AAA0082CAE8CA3CAF0340362DE13ED43C24F3B592844A23A91CD8

;; Note: byte arrays are displayed as hex via print-method override in merkl.root
```

### From file

With a file of 'blocks' (sequence of characters, one block per line), you can use line-seq to create a sequence for use with merkle-root:

```clojure
(defn file-stream-merkle-root [f]
  (with-open [rdr (clojure.java.io/reader f)]
    (merkle-root (line-seq rdr))))
	
(def test-root (file-stream-merkle-root "data/blks3.dat"))
;; 157F8B5CA2AF22D41ABECA1EA4B92628875A67B710011989F3150B31413E726D
```

The data directory has several example block files. You can create your own, as long as 'blocks' are separated into lines.

## Streaming Merkle Proofs and Verifies

### Proofs

Use **prove-leaf** to create a proof for the leaf at the given index in the stream. For convenience, you can use the **get-proof** utility function (see below), which wraps the file open. A proof consists of subroots before the leaf, the leaf itself, and subroots after the leaf. For leaf index 2 of a 3 block stream, there is one height-1 subroot before the leaf and no subroots after it:

```clojure
;; construct a proof for a given file and index
(defn get-proof [file index]
  (with-open [stream (clojure.java.io/reader file)]
    (prove-leaf stream index)))

@(def test-proof (get-proof "data/blks3.dat" 2))
;; {:pre ({:i 1,
;;         :subr 57074F6D3B9E06AD0D1729F13BEB33F60CBE676FBA63B82ED1229419039C1501}),
;;  :leaf "2",
;;  :post ()}
```
_**Note**_: The implementation of prove-leaf reads the blocks stream from file incrementally. It doesn't work with an in-memory collection as the stream. The intent was to mirror the incremental reads in the pseudocode from the original paper.

### Verifies

Use **verify-leaf** to verify the presence of a leaf in a block stream. The string "2" in this case is the 'block' that existed the original stream. Verify-leaf uses the known root, the proof we created from the original stream, and a supplied leaf to verify that the supplied leaf was present in that original block stream (and is not a forgery).

```clojure
(verify-leaf test-root "2" test-proof)
;; true
```

Check that supplying a different leaf to the verify fails:

```clojure
(verify-leaf test-root "X" test-proof)
;; false
```
