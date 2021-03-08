(ns merkl.core-test
  (:require [clojure.test :refer :all]
            [merkl.root :refer :all]
            [merkl.proof :refer :all]))

;; test utils
(defn file-stream-merkle-root [f]
  (with-open [rdr (clojure.java.io/reader f)]
    (merkle-root (line-seq rdr))))

(defn blks [f]
  (with-open [rdr (clojure.java.io/reader f)]
    (count (line-seq rdr))))

;; array equals
(defn array-eq? [ba1 ba2] (java.util.Arrays/equals ba1 ba2))

;; utility: construct a proof for a given file and index
(defn get-proof [file index]
  (with-open [stream (clojure.java.io/reader file)]
    (prove-leaf stream index)))

;; some example roots
(def r3 (file-stream-merkle-root "data/blks3.dat"))
;; => [21, 127, -117, 92, -94, -81, 34, -44, 26, -66, -54, 30, -92, -71,
;;     38, 40, -121, 90, 103, -73, 16, 1, 25, -119, -13, 21, 11, 49, 65,
;;     62, 114, 109]
(def r4 (file-stream-merkle-root "data/blks4.dat"))
;; => [117, 17, 17, 22, -106, -119, -34, 70, -23, 57, -15, -39, 12, -67,
;;     -27, -64, 70, -82, 0, 68, -113, 8, 21, 16, 91, 56, -21, 24, 55, -79,
;;     -69, 81]
(def r5 (file-stream-merkle-root "data/blks5.dat"))
;; => [110, 49, -118, -1, -91, 124, 60, -57, 61, -28, -40, 80, 61, 89, -28,
;;     -83, -78, 108, 28, 29, -52, 16, 51, 64, -1, 26, 2, -84, 111, 37,
;;     124, -109]
(def r12 (file-stream-merkle-root "data/blks12.dat"))
;; => [-40, 80, 93, -106, 75, 106, -86, 0, -126, -54, -24, -54, 60, -81, 3,
;;     64, 54, 45, -31, 62, -44, 60, 36, -13, -75, -110, -124, 74, 35, -87,
;;     28, -40]
(def r13 (file-stream-merkle-root "data/blks13.dat"))
;; => [-62, 30, 59, -88, -110, 127, -72, -65, 44, -39, 116, -106, 118, 94,
;;     112, -125, 77, 127, -34, -92, 89, -33, 82, -64, -37, -118, -85, -48,
;;     -12, 70, -3, 121]
(def r15 (file-stream-merkle-root "data/blks15.dat"))
;; => [35, -42, 84, 73, -113, 27, 1, -47, -4, 115, -109, -64, -14, -122,
;;     88, -58, 67, 51, -54, -80, 95, -24, -122, 33, -12, -103, 74, -36,
;;     -55, -53, 84, -88]
(def r32 (file-stream-merkle-root "data/blks32.dat"))
;; => [-29, 44, -38, 34, 99, 51, 11, -46, -61, -42, -43, 17, 106, -64, -1,
;;     31, 76, -96, 121, 23, -85, -15, -73, -47, 110, -117, -19, 117, -88,
;;     1, -10, 77]
(def r35 (file-stream-merkle-root "data/blks35.dat"))
;; => [101, 50, -85, -39, 82, 104, 78, -18, 6, -93, -127, -108, -12, 60,
;;     114, -109, 34, 76, -27, -45, 79, -51, 66, -89, -51, -58, -21, -58,
;;     26, -73, 95, 62]

;; proof and verify for r3

;; generate a proof for leaf at index 6
(def r3-proof (get-proof "data/blks3.dat" 2))
(def r3-vl (verify-leaf 2 "2" r3-proof))
(def r3-vl-bad (verify-leaf 2 "x" r3-proof))

;; The presence of leaf with value "6" at index 6 is verified.
(array-eq? r3 r3-vl)

(deftest r3-test-2
  (testing "Check leaf index 2 of block stream length 3"
    (is (array-eq? r3 r3-vl))))

;; test out of bounds index fails
(deftest r3-test-badleaf
  (testing "Check leaf index 2 of block stream length 3 - bad leaf"
    (is (not (array-eq? r3 r3-vl-bad)))))

;; test out of bounds index fails
(deftest r3-test-oob
  (testing "Check leaf index 6 of block stream length 3"
    (is (not (array-eq? r3 (verify-leaf 6 "6" r3-proof))))))

;; automate test of proof/verify for all indexes for example block streams
(defn verify-leaf-with-proof [file indx leaf]
  (let [mr (file-stream-merkle-root file)
        p (get-proof file indx)
        v (verify-leaf indx leaf p)]
    (java.util.Arrays/equals mr v)))

(verify-leaf-with-proof "data/blks12.dat" 6 "6")
;; => true

;; positive tests: verify leaf at every index is present as expected.
;; negative tests: attempts to verify 'bad' leaf at each index fails as expected.
(defn verify-all-indexes [file]
  (let [n (blks file)
        nstrs (map str (range n))
        allgood (map (partial verify-leaf-with-proof file) (range n) nstrs)
        allbad (map (partial verify-leaf-with-proof file) (range n) (repeat "x"))]
    (and (every? true? allgood) (every? false? allbad))))

(deftest r3-test
  (testing "Test proof/verify for all leaf indexes"
    (is (verify-all-indexes "data/blks3.dat"))))

(deftest r4-test
  (testing "Test proof/verify for all leaf indexes"
    (is (verify-all-indexes "data/blks4.dat"))))

(deftest r5-test
  (testing "Test proof/verify for all leaf indexes"
    (is (verify-all-indexes "data/blks5.dat"))))

(deftest r12-test
  (testing "Test proof/verify for all leaf indexes"
    (is (verify-all-indexes "data/blks12.dat"))))

(deftest r13-test
  (testing "Test proof/verify for all leaf indexes"
    (is (verify-all-indexes "data/blks13.dat"))))

(deftest r15-test
  (testing "Test proof/verify for all leaf indexes"
    (is (verify-all-indexes "data/blks15.dat"))))

(deftest r32-test
  (testing "Test proof/verify for all leaf indexes"
    (is (verify-all-indexes "data/blks32.dat"))))

(deftest r35-test
  (testing "Test proof/verify for all leaf indexes"
    (is (verify-all-indexes "data/blks35.dat"))))


