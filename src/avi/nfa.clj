(ns avi.nfa
  (:require [clojure.set :as set]))

(defn- null-reducer
  [accumulator _]
  accumulator)

(defn match
  ([value]
   (match value null-reducer))
  ([value reducer]
   (let [s1 (gensym)
         s2 (gensym)]
     {:start #{s1}
      :accept #{s2}
      :transitions {value {s1 {s2 [reducer]}}}})))

(defn any
  ([]
   (match ::any null-reducer))
  ([reducer]
   (match ::any reducer)))

(defn opt
  [nfa]
  {:start (:start nfa)
   :accept (set/union (:start nfa) (:accept nfa))
   :transitions (:transitions nfa)})

(defn start
  [nfa]
  (->> (:start nfa)
    (map #(vector % nil))
    (into {})))

(defn accept?
  [nfa state]
  (not (empty? (set/intersection
                 (:accept nfa)
                 (into #{} (keys state))))))

(defn advance
  [nfa state input reject-value]
  (let [state' (->> (for [[s targets] (concat
                                        (get-in nfa [:transitions ::any])
                                        (get-in nfa [:transitions input]))
                          :when (contains? state s)
                          :let [v (get state s)]
                          [s' reducers] targets
                          reducer reducers
                          :let [v' (reducer v input)]]
                      [s' v'])
                    (into {}))]
    (if (empty? state')
      reject-value
      state')))