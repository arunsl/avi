(ns avi.buffer.t-locations
  (:require [avi.buffer.locations :refer :all]
            [avi.scan :as scan]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.gfredericks.test.chuck.generators :as gen']
            [com.gfredericks.test.chuck.properties :as prop']
            [midje.sweet :refer :all]))

(facts "about comparing simple locations"
  (location< [1 2] [1 4]) => true
  (location< [1 2] [2 2]) => true
  (location< [1 4] [2 2]) => true
  (location<= [1 2] [1 2]) => true)

(def simple-location-generator
  (gen/tuple
    (gen/choose 1 50)
    (gen/choose 0 50)))

(defspec location<-location>-symmetry 25
  (prop/for-all [a simple-location-generator
                 b simple-location-generator]
   (= (location< a b) (location> b a))))

(defspec location<-implies-location<= 25
  (prop'/for-all [a simple-location-generator
                  b simple-location-generator]
   (if (location< a b)
     (location<= a b)
     true)))

(defspec location>-implies-location>= 25
  (prop'/for-all [a simple-location-generator
                  b simple-location-generator]
   (if (location> a b)
     (location>= a b)
     true)))

(def line-generator (gen/such-that #(= -1 (.indexOf % "\n")) gen/string-ascii))
(def lines-generator (gen/such-that #(not (zero? (count %))) (gen/vector line-generator)))
(def lines-and-position-generator
  (gen'/for [lines lines-generator
             i-base gen/pos-int
             :let [i (mod i-base (count lines))]
             j-base gen/pos-int
             :let [j (mod j-base (inc (count (get lines i))))]]
    {:lines lines
     :position [i j]}))

(def line-length-generator
  (gen/such-that
    (fn [v]
      (>= (count v) 1))
    (gen/vector gen/pos-int)))

(defspec retreat-from-0-0-is-always-nil 100
  (prop/for-all [line-length line-length-generator]
    (nil? (retreat [0 0] line-length))))

(defspec advance-at-eof-is-always-nil 100
  (prop'/for-all [line-length line-length-generator
                  :let [i (dec (count line-length))
                        j (last line-length)]]
    (nil? (advance [i j] #(get line-length %)))))

(defspec retreat-position-always-decreases 100
  (prop/for-all [{:keys [lines position]} lines-and-position-generator]
    (or (nil? (retreat position (scan/line-length lines)))
        (location< (retreat position (scan/line-length lines)) position))))

(defspec advance-position-always-increases 100
  (prop/for-all [{:keys [lines position]} lines-and-position-generator]
    (or (nil? (advance position (scan/line-length lines)))
        (location< position (advance position (scan/line-length lines))))))

(defspec retreat-at-beginning-of-line-goes-to-newline-position 100
  (prop'/for-all [lines lines-generator
                  :when (>= (count lines) 2)
                  i (gen'/bounded-int 1 (dec (count lines)))]
    (= (retreat [i 0] (scan/line-length lines))
       [(dec i) (count (get lines (dec i)))])))

(defspec advance-on-last-character-of-any-line-but-last-goes-to-newline-position 100
  (prop'/for-all [line-lengths (gen/vector (gen'/bounded-int 1 25))
                  :when (>= (count line-lengths) 2)
                  i (gen'/bounded-int 0 (- (count line-lengths) 2))
                  :let [j (dec (line-lengths i))]]
    (= (advance [i j] line-lengths) [i (inc j)])))

(defspec retreat-never-skips-a-line 100
  (prop/for-all [{lines :lines [i j] :position} lines-and-position-generator]
    (or (nil? (retreat [i j] (scan/line-length lines)))
        (= i (first (retreat [i j] (scan/line-length lines))))
        (= (dec i) (first (retreat [i j] (scan/line-length lines)))))))

(defspec advance-never-skips-a-line 100
  (prop/for-all [{lines :lines [i j] :position} lines-and-position-generator]
    (or (nil? (advance [i j] (scan/line-length lines)))
        (= i (first (advance [i j] (scan/line-length lines))))
        (= (inc i) (first (advance [i j] (scan/line-length lines)))))))
