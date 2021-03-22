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
     (println (format "[\"Javadoc: %s\"]" (:javadoc (:documentation definition))))
     (println (format "[\"User guide: %s\"]" (:user-guide (:documentation definition))))

     (println)
     )
   (sort-by first
            @samskara.ml/model-definitions*)
   )
  )



["## Models"]

["### :smile.classification/ada-boost"]
(docu-options :smile.classification/ada-boost)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/decision-tree"]
(docu-options :smile.classification/decision-tree)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/discrete-naive-bayes"]
(docu-options :smile.classification/discrete-naive-bayes)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/gradient-tree-boost"]
(docu-options :smile.classification/gradient-tree-boost)
["Javadoc: http://haifengl.github.io/api/java/smile/classification/GradientTreeBoost.html"]
["User guide: https://haifengl.github.io/classification.html#gbm"]

["### :smile.classification/knn"]
(docu-options :smile.classification/knn)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/logistic-regression"]
(docu-options :smile.classification/logistic-regression)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/maxent-binomial"]
(docu-options :smile.classification/maxent-binomial)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/maxent-multinomial"]
(docu-options :smile.classification/maxent-multinomial)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/random-forest"]
(docu-options :smile.classification/random-forest)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/sparse-logistic-regression"]
(docu-options :smile.classification/sparse-logistic-regression)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/sparse-svm"]
(docu-options :smile.classification/sparse-svm)
["Javadoc: null"]
["User guide: null"]

["### :smile.classification/svm"]
(docu-options :smile.classification/svm)
["Javadoc: null"]
["User guide: null"]

["### :smile.regression/elastic-net"]
(docu-options :smile.regression/elastic-net)
["Javadoc: null"]
["User guide: null"]

["### :smile.regression/gradient-tree-boost"]
(docu-options :smile.regression/gradient-tree-boost)
["Javadoc: null"]
["User guide: null"]

["### :smile.regression/lasso"]
(docu-options :smile.regression/lasso)
["Javadoc: null"]
["User guide: null"]

["### :smile.regression/ordinary-least-square"]
(docu-options :smile.regression/ordinary-least-square)
["Javadoc: null"]
["User guide: null"]

["### :smile.regression/random-forest"]
(docu-options :smile.regression/random-forest)
["Javadoc: null"]
["User guide: null"]

["### :smile.regression/ridge"]
(docu-options :smile.regression/ridge)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/binary-hinge-loss"]
(docu-options :xgboost/binary-hinge-loss)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/classification"]
(docu-options :xgboost/classification)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/count-poisson"]
(docu-options :xgboost/count-poisson)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/gamma-regression"]
(docu-options :xgboost/gamma-regression)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/gpu-binary-logistic-classification"]
(docu-options :xgboost/gpu-binary-logistic-classification)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/gpu-binary-logistic-raw-classification"]
(docu-options :xgboost/gpu-binary-logistic-raw-classification)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/gpu-linear-regression"]
(docu-options :xgboost/gpu-linear-regression)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/gpu-logistic-regression"]
(docu-options :xgboost/gpu-logistic-regression)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/linear-regression"]
(docu-options :xgboost/linear-regression)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/logistic-binary-classification"]
(docu-options :xgboost/logistic-binary-classification)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/logistic-binary-raw-classification"]
(docu-options :xgboost/logistic-binary-raw-classification)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/logistic-regression"]
(docu-options :xgboost/logistic-regression)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/multiclass-softmax"]
(docu-options :xgboost/multiclass-softmax)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/multiclass-softprob"]
(docu-options :xgboost/multiclass-softprob)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/rank-map"]
(docu-options :xgboost/rank-map)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/rank-ndcg"]
(docu-options :xgboost/rank-ndcg)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/rank-pairwise"]
(docu-options :xgboost/rank-pairwise)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/regression"]
(docu-options :xgboost/regression)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/squared-error-regression"]
(docu-options :xgboost/squared-error-regression)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/survival-cox"]
(docu-options :xgboost/survival-cox)
["Javadoc: null"]
["User guide: null"]

["### :xgboost/tweedie-regression"]
(docu-options :xgboost/tweedie-regression)
["Javadoc: null"]
["User guide: null"]
