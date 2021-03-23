(ns samskara.models
(:require
 [notespace.api :as note]
 [notespace.kinds :as kind ]
 [notespace.view :as view]
 [tablecloth.api :as tc]
 [samskara.ml]
 [scicloj.sklearn-clj.ml]
 [clojure.string :as str]
 [samskara.utils :refer :all]
 ))


^kind/hidden
(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/render-static-html "docs/userguide-models.html")
  )

["# Models"]

["## Smile classification"]
^kind/hiccup-nocode
(render-key-info ":smile.classification")


["## Smile regression"]
^kind/hiccup-nocode
(render-key-info ":smile.regression")


["## Xgboost"]
^kind/hiccup-nocode
(render-key-info ":xgboost")

