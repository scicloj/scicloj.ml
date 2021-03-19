(ns samskara.models
(:require
 [notespace.api :as note]
 [notespace.kinds :as kind ]
 [notespace.view :as view]
 [tablecloth.api :as tc]
 [samskara.ml]
 )
)

(comment

  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/render-static-html "docs/userguide-models.html")


  )

(def model-keys
  (keys @samskara.ml/model-definitions*))

(def model-options
  (map
   :options
   (vals @samskara.ml/model-definitions*)))

^kind/dataset
(tc/dataset
 {:model-key model-keys
  :options model-options
  }
 )

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

(comment

  (run!
   (fn [[key definition]]
     (println (format "[\"### %s\"]" key))
     (println (format  "(docu-options %s)" key))
     (println (format "[\"%s\"]" (:javadoc definition)))
     (println)
     )
   (sort-by first
            @samskara.ml/model-definitions*)
   )
  )


["## Models"]

["### :smile.classification/ada-boost"]
(docu-options :smile.classification/ada-boost)
["null"]

["### :smile.classification/decision-tree"]
(docu-options :smile.classification/decision-tree)
["null"]

["### :smile.classification/discrete-naive-bayes"]
(docu-options :smile.classification/discrete-naive-bayes)
["null"]

["### :smile.classification/gradient-tree-boost"]
(docu-options :smile.classification/gradient-tree-boost)
["http://haifengl.github.io/api/java/smile/classification/GradientTreeBoost.html"]

["### :smile.classification/knn"]
(docu-options :smile.classification/knn)
["null"]

["### :smile.classification/logistic-regression"]
(docu-options :smile.classification/logistic-regression)
["null"]

["### :smile.classification/maxent-binomial"]
(docu-options :smile.classification/maxent-binomial)
["null"]

["### :smile.classification/maxent-multinomial"]
(docu-options :smile.classification/maxent-multinomial)
["null"]

["### :smile.classification/random-forest"]
(docu-options :smile.classification/random-forest)
["null"]

["### :smile.classification/sparse-logistic-regression"]
(docu-options :smile.classification/sparse-logistic-regression)
["null"]

["### :smile.classification/sparse-svm"]
(docu-options :smile.classification/sparse-svm)
["null"]

["### :smile.classification/svm"]
(docu-options :smile.classification/svm)
["null"]

["### :smile.regression/elastic-net"]
(docu-options :smile.regression/elastic-net)
["null"]

["### :smile.regression/gradient-tree-boost"]
(docu-options :smile.regression/gradient-tree-boost)
["null"]

["### :smile.regression/lasso"]
(docu-options :smile.regression/lasso)
["null"]

["### :smile.regression/ordinary-least-square"]
(docu-options :smile.regression/ordinary-least-square)
["null"]

["### :smile.regression/random-forest"]
(docu-options :smile.regression/random-forest)
["null"]

["### :smile.regression/ridge"]
(docu-options :smile.regression/ridge)
["null"]
