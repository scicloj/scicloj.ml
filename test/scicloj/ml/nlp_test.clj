(ns scicloj.ml.nlp-test
  (:require [tablecloth.api :as tc]
            [clojure.string :as str]
            [tech.v3.dataset.column]
            [scicloj.ml.smile.nlp :as smile-nlp]
            [tech.v3.datatype.functional :as dtf]
            [tech.v3.datatype :as dt]
            [tech.v3.tensor :as tt]
            ;; [dk.cst.tf-idf :as tfidf]
            [scicloj.ml.nlp.tfidf :as tfidf]
            [tech.v3.dataset.tensor :as ds-tensor]
            [clojure.test :as t]))

(t/deftest tfidf
  (let [reviews
        (tc/dataset "https://github.com/scicloj/scicloj.ml.smile/blob/main/test/data/reviews.csv.gz?raw=true"
                    {:key-fn (comp keyword str/lower-case)
                     :gzipped? true
                     :file-type :csv})
        ds-tfidf
        (-> reviews
            (tfidf/texts->tfidf :text :tfidf)
            (smile-nlp/tfidf->dense-array  :tfidf :tfidf-arrays))]
    (t/is (= 7762 (-> ds-tfidf :tfidf-arrays first count)))))


(def corpus [
             "This is the first document."
             "This document is the second document."
             "And this is the third one."
             "Is this the first document?"])

(def a
  (-> (tc/dataset {:text corpus})
      (tfidf/texts->tfidf :text :tfidf)
      (smile-nlp/tfidf->dense-array :tfidf :tfidf-arrays)
      :tfidf-arrays))


(def tensor
  (-> a
      dt/concat-buffers
      tt/ensure-tensor
      (tt/reshape [4 9])))

(tt/compute-tensor tensor (fn [p] (inc p)))

(dt/emap inc nil tensor)
(tt/reduce-axis dtf/sum tensor -1)

;; => #tech.v3.tensor<float64>[4 9]
;;    [[ 0.1176  0.1176  0.000  0.000 0.1622  0.000  0.000  0.1176 0.2197]
;;     [0.09796 0.09796 0.2507  0.000 0.2703  0.000  0.000 0.09796  0.000]
;;     [0.09796 0.09796  0.000 0.2507  0.000 0.2507 0.2507 0.09796  0.000]
;;     [ 0.1176  0.1176  0.000  0.000 0.1622  0.000  0.000  0.1176 0.2197]]
