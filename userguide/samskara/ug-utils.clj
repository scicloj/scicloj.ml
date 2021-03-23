(ns samskara.ug-utils
  (:require [clojure.string :as str]
            [notespace.kinds :as kind]
            [notespace.view :as view]
            samskara.ml
            [tablecloth.api :as tc]))

(def model-keys
  (keys @samskara.ml/model-definitions*))

(def model-options
  (map
   :options
   (vals @samskara.ml/model-definitions*)))


(defmethod kind/kind->behaviour ::dataset-nocode
  [_]
  {:render-src?   false
   :value->hiccup #'view/dataset->md-hiccup})

(defn docu-options [model-key]
  (kind/override
   (tc/dataset
    (get-in @samskara.ml/model-definitions* [model-key :options] ))
   ::dataset-nocode
   )
  )
(defn text->hiccup
  "Convert newlines to [:br]'s."
  [text]
  (->> (str/split text #"\n")
       (interpose [:br])
       (map #(if (string? %)
               %
               (with-meta % {:key (gensym "br-")})))))

(defn docu-doc-string [model-key]
  (text->hiccup
   (or
    (get-in @samskara.ml/model-definitions* [model-key :documentation :doc-string] )
    ""
    )

   )
  )

(defn anchor-or-nothing [x text]
  (if (empty? x)
    [:div ""]
    [:div
     [:a {:href x} text]]
    )
  )
(defn render-key-info [prefix]
  (->> @samskara.ml/model-definitions*
       (sort-by first)
       (filter #(str/starts-with? (first %) prefix ))
       (map
        (fn [[key definition]]
          [:div
           [:h3 (str key)]
           (anchor-or-nothing (:javadoc (:documentation definition)) "javadoc")
           (anchor-or-nothing (:user-guide (:documentation definition)) "user guide")


           [:span
            (view/dataset->md-hiccup (docu-options key) )]

           [:span
            (docu-doc-string key)]

           [:hr]
           ]))))
