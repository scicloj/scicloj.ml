(ns scicloj.ml.smoke-test
  (:require  [clojure.test :as t]
             [scicloj.ml.core :as ml]
             [scicloj.ml.metamorph :as mm]
             [scicloj.ml.dataset :as ds]
             [scicloj.ml.smile.classification]
             [tablecloth.api :as tc]))
             


;; read train and test datasets
(def titanic-train
  (->
   (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/titanic/train.csv"
               {:key-fn keyword
                :parser-fn :string})))


(def titanic-test
  (->
   (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/titanic/test.csv"
               {:key-fn keyword
                :parser-fn :string})
   (tc/add-column :Survived [ "0"] :cycle)))



;; construct pipeline function including Logistic Regression model
(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:Survived :Pclass])
   (mm/add-column :Survived (fn [ds] (map #(case % "1" "yes" "0" "no" ) (:Survived ds))))
   (mm/categorical->number [:Survived :Pclass])
   (mm/set-inference-target :Survived)
   {:metamorph/id :model}
   (mm/model {:model-type :smile.classification/logistic-regression})))

;;  execute pipeline with train data including model in mode :fit
(def trained-ctx
  (pipe-fn {:metamorph/data titanic-train
            :metamorph/mode :fit}))


;; execute pipeline in mode :transform with test data which will do a prediction
(def test-ctx
  (pipe-fn
   (assoc trained-ctx
          :metamorph/data titanic-test
          :metamorph/mode :transform)))

;; extract prediction from pipeline function result
(def yes-no-freqs
  (frequencies
   (-> test-ctx :metamorph/data
       (ds/column-values->categorical :Survived))))


(t/deftest smoke-test
  (t/is (= {"yes" 107 "no" 311} yes-no-freqs)))
