(ns scicloj.ml.test-all
  (:require  [scicloj.ml.core :as ml]
             [scicloj.ml.mm :as mm]
             [scicloj.ml.ds :as ds]
             [tech.v3.libs.smile.nlp :as nlp]

             ))


(def reviews-split
  (->
   (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/reviews.csv.gz"
               {:key-fn keyword})
   (ds/split->seq :holdout)
   first))

(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:Text :Score])
   (mm/count-vectorize :Text :bow nlp/default-text->bow {})
   (mm/bow->sparse-array :bow :bow-sparse #(nlp/->vocabulary-top-n % 1000))
   (mm/set-inference-target :Score)
   (mm/select-columns [:bow-sparse :Score])
   (mm/model {:p 1000
              :model-type :maxent-multinomial
              :sparse-column :bow-sparse})
   ))

(def trained-ctx
  (pipe-fn {:metamorph/data (:train reviews-split)
            :metamorph/mode :fit}))
