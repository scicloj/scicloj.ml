(ns scicloj.ml.utils)


(defn ns-symbols [ns except]
  (let [publics
        (-> ns ns-publics keys)
        to-export (clojure.set/difference
                   (set publics)
                   (set except))]
    (sort to-export)))

(defmacro export-all [spaces except]
  `(do ~@(for [ns spaces]
           `(tech.v3.datatype.export-symbols/export-symbols ~ns ~@(ns-symbols ns except)))))
