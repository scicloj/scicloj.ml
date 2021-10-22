(ns scicloj.ml.all-test
  (:require  [scicloj.ml.core :as ml]
             [scicloj.ml.metamorph :as mm]
             [scicloj.ml.dataset :as ds]
             [scicloj.ml.smile.nlp :as nlp]
             [scicloj.ml.smile.maxent]
             [clojure.test :refer [is deftest]])
             
  (:import [ smile.classification Maxent$Multinomial]))
  


(def reviews-split
  (->
   (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/reviews.csv.gz"
               {:key-fn keyword
                :parser-fn :string})
                
   (ds/split->seq :holdout {:shuffle? false})
   first))
(-> reviews-split :train :Score distinct)

(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:Text :Score])
   (mm/count-vectorize :Text :bow {:text->bow-fn nlp/default-text->bow})
   (mm/bow->sparse-array :bow :bow-sparse {:create-vocab-fn #(nlp/->vocabulary-top-n % 1000)})
   (mm/categorical->number [:Score] [ "1" "2"  "3"  "4" "5"])

   (mm/set-inference-target :Score)
   (mm/select-columns [:bow-sparse :Score])
   {:metamorph/id :model}
   (mm/model {:p 1000
              :model-type :smile.classification/maxent-multinomial
              :sparse-column :bow-sparse})))

   

(deftest test-pipeline

  (let  [trained-ctx
         (pipe-fn {:metamorph/data (:train reviews-split)
                   :metamorph/mode :fit})


         predicted-ctx
         (pipe-fn
          (merge trained-ctx
                 {:metamorph/data (:test reviews-split)
                  :metamorph/mode :transform}))]


    (is  (= Maxent$Multinomial
            (-> trained-ctx :model :model-data class)))


   (is (= {"5" 262, "1" 26, "4" 29, "3" 10, "2" 7}
          (-> predicted-ctx :metamorph/data ds/reverse-map-categorical-xforms :Score frequencies)))

   (is (= {"5" 212, "1" 24, "4" 44, "3" 31, "2" 23}
          (-> predicted-ctx :model :scicloj.metamorph.ml/target-ds ds/reverse-map-categorical-xforms :Score frequencies)))

   (is (= [:bow-sparse]
          (-> predicted-ctx :model :scicloj.metamorph.ml/feature-ds ds/column-names)))))
