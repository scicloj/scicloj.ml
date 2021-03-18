(ns scicloj.ml.userguide.models
(:require
 [notespace.api :as note]
 [notespace.kinds :as kind ]
 [scicloj.ml.core]
 [tablecloth.api :as tc]
 )
)

(comment
  (note/init-with-browser)
  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html)
  (note/init)
  )

(def model-keys
  (keys @scicloj.ml.core/model-definitions*))

(def model-options
  (map
   :options
   (vals @scicloj.ml.core/model-definitions*)))

^kind/dataset
(tc/dataset
 {:model-key model-keys
  :options model-options
  }
 )
["## Classification models"]


["### :smile.classification/gradient-tree-boost"]

["### :smile.classification/sparse-logistic-regression"]
["### :smile.classification/decision-tree"]
["### :smile.classification/maxent-multinomial"]
["### :smile.classification/random-forest"]
["### :smile.classification/logistic-regression"]
["### :smile.classification/discrete-naive-bayes"]
["### :smile.classification/knn"]
["### :smile.classification/maxent-binomial"]
["### :smile.classification/sparse-svm"]
["### :smile.classification/svm"]
["### :smile.classification/ada-boost"]

["## Regression models"]
["### :smile.regression/lasso"]
["### :smile.regression/elastic-net"]
["### :smile.regression/ridge"]
["### :smile.regression/gradient-tree-boost"]
["### :smile.regression/ordinary-least-square"]
["### :smile.regression/random-forest"]
