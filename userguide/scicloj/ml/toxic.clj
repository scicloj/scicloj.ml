(ns scicloj.ml.toxic
(:require
   [notespace.api :as note]
   [notespace.kinds :as kind ])
)
;; https://www.kaggle.com/sermakarevich/sklearn-pipelines-tutorial
;;
(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))

  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html "docs/userguide-tocix.html")
  (note/init)



  )

(def num-grid-points 1)
(def num-test-rows 1000)
;; (def num-test-rows 1000)


(require '[scicloj.ml.core :as ml]
         '[scicloj.metamorph.ml.gridsearch :as gs]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset :as ds]
         '[scicloj.metamorph.ml.loss :as loss]
         '[tech.v3.libs.smile.nlp :as nlp]
         '[pppmap.core :as ppp]
         )



(def df
  (->
   (ds/dataset "data/toxic/train.csv.gz" {:key-fn keyword :parser-fn :string} )
   ;; (ds/shuffle)
   (ds/head num-test-rows)
   ))





(frequencies
 (:toxic df))

(def freqs->SparseArray
  (memoize nlp/freqs->SparseArray)
  )

(defn tfidf->sparse [tfidf-col sparse-col]
  (fn [ctx]
    (assoc ctx
           :metamorph/data
           (ds/add-column
            (:metamorph/data ctx)
            tfidf-col
            (fn [ds]
              (ppp/ppmap-with-progress "tfidf->sparse" 1000
               #(freqs->SparseArray
                 %
                 (-> ctx :tech.v3.libs.smile.metamorph/bow->sparse-vocabulary :vocab->index-map))
               (get ds sparse-col ds)
               )
              )

            ))))





(def text->bow
  (memoize nlp/default-text->bow))


(def ->vocabulary-top-n
  (memoize nlp/->vocabulary-top-n)
  )

(defn make-vocab-fn [n]
  (fn [bows]
    (->vocabulary-top-n bows n)
    ))

;; (def pipe-opts {:n 10})


(def fixed-pipe
  (ml/pipeline
   (mm/count-vectorize :comment_text :comment_text {:text->bow-fn text->bow})
   (mm/bow->tfidf :comment_text :comment_text))

  )

(def splits
  (->
   (fixed-pipe {:metamorph/data df})
   :metamorph/data
   (ds/split->seq  :kfold {:k 3})))

(defn make-pipe [pipe-opts]
  (ml/pipeline
   (fn [ctx]
     (assoc ctx :pipe-opts pipe-opts)
     )
   (mm/select-columns [:id :comment_text (:target pipe-opts)])
   (mm/bow->sparse-array :comment_text :dummy {:create-vocab-fn (make-vocab-fn  (pipe-opts :n)) })
   (tfidf->sparse :comment_text :comment_text)
   (mm/select-columns [:id :comment_text (:target pipe-opts)])
   (mm/set-inference-target (:target pipe-opts))
   (mm/categorical->number [(:target pipe-opts)])
   (mm/model (merge pipe-opts
                    {:model-type :smile.classification/sparse-logistic-regression
                     :sparse-column :comment_text
                     :n-sparse-columns (pipe-opts :n)
                     })
             )))

(def test-ds
  (-> (ds/dataset "data/toxic/test.csv" {:key-fn keyword :parser-fn :string} )
      (ds/rename-columns {:text :comment_text})

      ;; (ds/drop-columns [:id])
      ))

(def test-ids (:id test-ds))

(def test-ds
  (->
   (fixed-pipe {:metamorph/data test-ds})
   :metamorph/data)
  )



(defn train-and-eval [target]

  (let [pipes
        (map
         make-pipe
         (take  num-grid-points
                (gs/sobol-gridsearch {:target target
                                      :lambda (gs/linear 0 1)
                                      :n (gs/categorical [100 500 2500 5000])})))

        evals
        (ml/evaluate-pipelines
         pipes
         splits
         loss/classification-accuracy
         :accuracy
         )

        _ (def evals evals)
        m
        (map
         #(merge (select-keys % [:metric :pipe-fn])
                 (get-in % [:fit-ctx :pipe-opts]))
         (flatten evals))

        _
        (->
         (ds/dataset m)
         (ds/write-csv! (str  "eval_result_"  (name target)   ".csv"))
         )


        best
        (select-keys
         (->>
          (flatten evals)
          (sort-by :metric)
          first)
         [:pipe-fn :fit-ctx :metric])


        test-ds-with-target
        (ds/add-column test-ds target  ["a"])

        prediction-on-test
        (
         (:pipe-fn best)
         (merge (:fit-ctx best)
                {:metamorph/data test-ds-with-target
                 :metamorph/mode :transform
                 })
         )


        probs (-> prediction-on-test :metamorph/data (#(get % "1")))]

    probs

    )
  )






(def submission
  (ds/dataset
   {:id test-ids
    :toxic (train-and-eval :toxic)
    :severe_toxic (train-and-eval :severe_toxic)
    :obscene (train-and-eval :obscene)
    :threat (train-and-eval :threat)
    :insult (train-and-eval :insult)
    :identity_hate (train-and-eval :identity_hate)
    }
   )
  )

(ds/write-csv! submission "submission.csv" )



(println "Finished")
