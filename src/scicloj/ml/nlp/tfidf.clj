(ns scicloj.ml.nlp.tfidf
  (:require [tablecloth.api :as tc]
            [clojure.string :as str]
            [tech.v3.dataset.column]
            [scicloj.ml.smile.nlp :as smile-nlp]
            [dk.cst.tf-idf :as tfidf]))


(defn texts->tfidf [ds text-column tfidf-column]
  (let [tfidf (tfidf/tf-idf (get ds text-column))]
    (-> ds
        (tc/add-column tfidf-column
                       (tech.v3.dataset.column/new-column
                        :tfidf tfidf
                        {:tf-map (zipmap
                                  (keys (apply merge tfidf))
                                  (repeat nil))})))))


(comment
  (def ds
    (tc/dataset "https://github.com/scicloj/scicloj.ml.smile/blob/main/test/data/reviews.csv.gz?raw=true"
                {:key-fn (comp keyword str/lower-case)
                 :gzipped? true
                 :file-type :csv}))


  (def tf
    (->>
     (tfidf/tf
      (:text ds))))


  (frequencies
   (tfidf/list-terms tf))

  (def df
    (->>
     (tfidf/df (:text ds))))



  (def idf
    (tfidf/invert df))

  (def tfidf-1000
    (map (partial tfidf/apply-idf idf) tf))

  (count
   (apply merge tfidf-1000))

  (def frequent-more-then-10
    (into (tfidf/vocab (:text ds) 10) #{}))


  (def new-tf-idf
    (binding [tfidf/*tokenizer-xf* (tfidf/->tokenizer-xf :tokenize tfidf/tokenize
                                                         :postprocess (fn [seq-of-s]
                                                                        (filter
                                                                         #(contains?  frequent-more-then-10 %)
                                                                         seq-of-s)))]

      (tfidf/tf-idf (:text ds))))


  (map #(get % "food") new-tf-idf))
