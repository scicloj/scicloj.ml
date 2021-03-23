(ns samskara.sklearnclj
  (:require
   [notespace.api :as note]
   [notespace.kinds :as kind]
   [scicloj.sklearn-clj.ml]
   [samskara.ug-utils :refer :all])
  )

(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))

  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html "docs/userguide-sklearnclj.html")
  (note/init)

  )


["# Models"]



["## Sklearn classification"]
^kind/hiccup-nocode
(render-key-info ":sklearn.classification")


["## Sklearn regression"]
^kind/hiccup-nocode
(render-key-info ":sklearn.regression")
