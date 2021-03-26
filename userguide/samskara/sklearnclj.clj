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


(comment
  (require '[libpython-clj2.require :refer [require-python]]
           '[libpython-clj2.python :refer [py. py.. py.-] :as py]
           '[tech.v3.datatype :as dtype])

  (require-python '[sklearn.datasets :refer [make_classification]]
                  '[sklearn.ensemble :refer [RandomForestClassifier]]
                  '[time :refer [time]]
                  )

  (def classification
    (make_classification :n_samples 10000
                         :n_features 20
                         :n_informative 15
                         :n_redundant 5
                         :random_state 3))

  (def X (first classification))
  (def y (second classification))
  (def model (RandomForestClassifier :n_estimators 500 :n_jobs 1))

  (do
    (def start (time))
    (py. model fit X y)
    (def end (time)))

  (- end start)
;; => 10.843264818191528

(def model (RandomForestClassifier :n_estimators 500 :n_jobs -1))
(do
    (def start (time))
    (py. model fit X y)
    (def end (time)))

(- end start)
