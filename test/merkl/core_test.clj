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

;; utility: construct a proof for a given file and index
(defn get-proof [file index]
  (with-open [stream (clojure.java.io/reader file)]
    (prove-leaf stream index)))

;; some example roots
(def r3 (file-stream-merkle-root "data/blks3.dat"))
(ba->hex r3)
;; => "157F8B5CA2AF22D41ABECA1EA4B92628875A67B710011989F3150B31413E726D"

(def r4 (file-stream-merkle-root "data/blks4.dat"))
(ba->hex r4)
;; => "751111169689DE46E939F1D90CBDE5C046AE00448F0815105B38EB1837B1BB51"

(def r5 (file-stream-merkle-root "data/blks5.dat"))
(ba->hex r5)
;; => "6E318AFFA57C3CC73DE4D8503D59E4ADB26C1C1DCC103340FF1A02AC6F257C93"

(def r12 (file-stream-merkle-root "data/blks12.dat"))
(ba->hex r12)
;; => "-27AFA269B49555FF7D351735C350FCBFC9D21EC12BC3DB0C4A6D7BB5DC56E328"

(def r13 (file-stream-merkle-root "data/blks13.dat"))
(ba->hex r13)
;; => "-3DE1C4576D804740D3268B6989A18F7CB280215BA620AD3F2475542F0BB90287"

(def r15 (file-stream-merkle-root "data/blks15.dat"))
(ba->hex r15)
;; => "23D654498F1B01D1FC7393C0F28658C64333CAB05FE88621F4994ADCC9CB54A8"

(def r32 (file-stream-merkle-root "data/blks32.dat"))
(ba->hex r32)
;; => "-1CD325DD9CCCF42D3C292AEE953F00E0B35F86E8540E482E9174128A57FE09B3"

(def r35 (file-stream-merkle-root "data/blks35.dat"))
(ba->hex r35)
;; => "6532ABD952684EEE06A38194F43C7293224CE5D34FCD42A7CDC6EBC61AB75F3E"

;; proof and verify for r3

;; generate a proof for leaf at index 6
(def r3-proof (get-proof "data/blks3.dat" 2))
;; => {:pre
;;     ({:i 1,
;;       :subr
;;       [87, 7, 79, 109, 59, -98, 6, -83, 13, 23, 41, -15, 59, -21, 51,
;;        -10, 12, -66, 103, 111, -70, 99, -72, 46, -47, 34, -108, 25, 3,
;;        -100, 21, 1]}),
;;     :leaf "2",
;;     :post ()}

(verify-leaf r3 "2" r3-proof)
;; => true

;; bad leaf "X" supplied
(verify-leaf r3 "X" r3-proof)
;; => false

(deftest r3-test-2
  (testing "Verify presence of leaf in block stream length 3"
    (is (verify-leaf r3 "2" r3-proof))))

;; test out of bounds index fails
(deftest r3-test-badleaf
  (testing "Check for leaf in block stream length 3 - bad leaf"
    (is (not (verify-leaf r3 "X" r3-proof)))))

;; automate test of proof/verify for all indexes for example block streams
(defn verify-leaf-with-proof [file indx leaf]
  (verify-leaf (file-stream-merkle-root file) leaf (get-proof file indx)))

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
