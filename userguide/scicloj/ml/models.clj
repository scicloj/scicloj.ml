(ns scicloj.ml.models
(:require
 [notespace.api :as note]
 [notespace.kinds :as kind ]
 [notespace.view :as view]
 [tablecloth.api :as tc]
 [scicloj.ml.core]
 [scicloj.sklearn-clj.ml]
 [clojure.string :as str]
 [scicloj.ml.ug-utils :refer :all]))

^kind/hidden
(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/render-static-html "docs/userguide-models.html")
  )

["# Models"]

["scicloj.ml uses the plugin `scicloj.ml.smiles` and
`scicloj.ml.xgboost` by default,
which gives access to a lot of model from the java librarys [Smile](https://haifengl.github.io/)
and [Xgboost](https://xgboost.readthedocs.io/en/latest/jvm/index.html)"]

["Below is a list of all such models, and which parameter they take."]

["All models are availanble in the same way:"]

(comment
  (require '[scicloj.ml.metamorph :as mm])
  ;; last step in pipeline
  (mm/model {:model-type :smile.classification/ada-boost
             :trees 200
             :max-depth 100
             :max-nodes 50
             }))

["The documentation below points as well to the javadoc and user-guide chapter (for Smile models)"]

["## Smile classification"]
^kind/hiccup-nocode
(render-key-info ":smile.classification")


["## Smile regression"]
^kind/hiccup-nocode
(render-key-info ":smile.regression")


["## Xgboost"]
^kind/hiccup-nocode
(render-key-info ":xgboost")
