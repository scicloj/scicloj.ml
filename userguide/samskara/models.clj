(ns samskara.models
(:require
 [notespace.api :as note]
 [notespace.kinds :as kind ]
 [notespace.view :as view]
 [tablecloth.api :as tc]
 [samskara.ml]))


^kind/hidden
(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/render-static-html "docs/userguide-models.html"))

^kind/hidden
(def model-keys
  (keys @samskara.ml/model-definitions*))

^kind/hidden
(def model-options
  (map
   :options
   (vals @samskara.ml/model-definitions*)))

^kind/hidden
(defmethod kind/kind->behaviour ::dataset-nocode
  [_]
  {:render-src?   false
   :value->hiccup #'view/dataset->md-hiccup})

^kind/hidden
(defn docu-options [model-key]
  (kind/override
   (tc/dataset
    (get-in @samskara.ml/model-definitions* [model-key :options] ))
   ::dataset-nocode
   )
  )

^kind/hidden
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

["### :xgboost/binary-hinge-loss"]
(docu-options :xgboost/binary-hinge-loss)
["null"]

["### :xgboost/classification"]
(docu-options :xgboost/classification)
["null"]

["### :xgboost/count-poisson"]
(docu-options :xgboost/count-poisson)
["null"]

["### :xgboost/gamma-regression"]
(docu-options :xgboost/gamma-regression)
["null"]

["### :xgboost/gpu-binary-logistic-classification"]
(docu-options :xgboost/gpu-binary-logistic-classification)
["null"]

["### :xgboost/gpu-binary-logistic-raw-classification"]
(docu-options :xgboost/gpu-binary-logistic-raw-classification)
["null"]

["### :xgboost/gpu-linear-regression"]
(docu-options :xgboost/gpu-linear-regression)
["null"]

["### :xgboost/gpu-logistic-regression"]
(docu-options :xgboost/gpu-logistic-regression)
["null"]

["### :xgboost/linear-regression"]
(docu-options :xgboost/linear-regression)
["null"]

["### :xgboost/logistic-binary-classification"]
(docu-options :xgboost/logistic-binary-classification)
["null"]

["### :xgboost/logistic-binary-raw-classification"]
(docu-options :xgboost/logistic-binary-raw-classification)
["null"]

["### :xgboost/logistic-regression"]
(docu-options :xgboost/logistic-regression)
["null"]

["### :xgboost/multiclass-softmax"]
(docu-options :xgboost/multiclass-softmax)
["null"]

["### :xgboost/multiclass-softprob"]
(docu-options :xgboost/multiclass-softprob)
["null"]

["### :xgboost/rank-map"]
(docu-options :xgboost/rank-map)
["null"]

["### :xgboost/rank-ndcg"]
(docu-options :xgboost/rank-ndcg)
["null"]

["### :xgboost/rank-pairwise"]
(docu-options :xgboost/rank-pairwise)
["null"]

["### :xgboost/regression"]
(docu-options :xgboost/regression)
["null"]

["### :xgboost/squared-error-regression"]
(docu-options :xgboost/squared-error-regression)
["null"]

["### :xgboost/survival-cox"]
(docu-options :xgboost/survival-cox)
["null"]

["### :xgboost/tweedie-regression"]
(docu-options :xgboost/tweedie-regression)
["null"]
