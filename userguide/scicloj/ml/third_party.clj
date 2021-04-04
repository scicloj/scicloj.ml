(ns scicloj.ml.third-party

(:require
 [notespace.api :as note]
 [notespace.kinds :as kind ]

   [scicloj.ml.ug-utils :refer :all]
 )
  )

(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))

  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html "docs/userguide-cljdjl.html")
  (note/init)
  )



(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset  :as ds]
         '[tech.v3.datatype.functional :as dfn])


["# Deep learning model via clj-djl "]



(def train-ds
  (ds/dataset
   "http://d2l-data.s3-accelerate.amazonaws.com/kaggle_house_pred_train.csv"))


(def test-ds
  (->
   (ds/dataset
    "http://d2l-data.s3-accelerate.amazonaws.com/kaggle_house_pred_test.csv")
   (ds/add-column "SalePrice" 0)))

(defn numeric-features [ds]
  (ds/intersection (ds/numeric ds)
                   (ds/feature ds)))

(defn update-columns
  "Update a sequence of columns selected by column name seq or column selector function."
  [dataframe col-name-seq-or-fn update-fn]
  (ds/update-columns dataframe
                     (if (fn? col-name-seq-or-fn)
                       (ds/column-names (col-name-seq-or-fn dataframe))
                       col-name-seq-or-fn)
                     update-fn))

(require
 '[clj-djl.mmml]
 '[clj-djl.nn :as nn]
 '[clj-djl.training :as t]
 '[clj-djl.training.loss :as loss]
 '[clj-djl.training.optimizer :as optimizer]
 '[clj-djl.training.tracker :as tracker]
 '[clj-djl.training.listener :as listener]
 '[clj-djl.ndarray :as nd])

(def  learning-rate 0.05)
(defn net [] (nn/sequential {:blocks (nn/linear {:units 1})
                         :initializer (nn/normal-initializer)}))
(defn cfg [] (t/training-config {:loss (loss/l2-loss)
                             :optimizer (optimizer/sgd
                                         {:tracker (tracker/fixed learning-rate)})
                             :evaluator (t/accuracy)
                             :listeners (listener/logging)}))

(def pipe
  (ml/pipeline
   (mm/drop-columns ["Id" ])
   (mm/set-inference-target "SalePrice")
   (tech.v3.dataset.metamorph/replace-missing ds/numeric :value 0)
   (tech.v3.dataset.metamorph/replace-missing ds/categorical :value "None")
   (ml/lift update-columns numeric-features
            #(dfn// (dfn/- % (dfn/mean %))
                    (dfn/standard-deviation %)))
   (mm/update-column "SalePrice"
                     #(dfn// % (dfn/mean %)))
   (mm/set-inference-target "SalePrice")
   (mm/categorical->one-hot ds/categorical)
   (fn [ctx]
     ((mm/select-rows (:ds-indeces ctx) ) ctx))
   (mm/model {:model-type :clj-djl/djl
              :batchsize 64
              :model-spec {:name "mlp" :block-fn net}
              :model-cfg (cfg)
              :initial-shape (nd/shape 1 310)
              :nepoch 1})))

(def trained-pipeline
  (pipe {:metamorph/data (ds/concat train-ds test-ds)
         :metamorph/mode :fit
         :ds-indeces (range (ds/row-count train-ds))
         }))

(def predicted-pipeline
  (pipe
   (merge trained-pipeline
          {:metamorph/data (ds/concat test-ds train-ds)
           :metamorph/mode :transform
           :ds-indeces (range (ds/row-count test-ds))})))



( get
 (:metamorph/data trained-pipeline)
 "SalePrice"
 )


^kind/hiccup-nocode
(render-key-info ":clj-djl")
