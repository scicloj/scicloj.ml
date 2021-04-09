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

(def num-grid-points 50)
(def num-train-rows 159571)
;; (def num-train-rows 1000)

(def num-test-rows 153164)
;; (def num-test-rows 1000)

(require '[scicloj.ml.core :as ml]
         '[scicloj.metamorph.ml.gridsearch :as gs]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset :as ds]
         '[scicloj.metamorph.ml.loss :as loss]
         '[scicloj.ml.smile.nlp :as nlp]
         '[tech.v3.datatype.functional :as dtf]
         '[pppmap.core :as ppp]
         )



(def df
  (->
   (ds/dataset "data/toxic/train.csv.gz" {:key-fn keyword :parser-fn :string} )
   ;; (ds/shuffle)
   (ds/head num-train-rows)
   ))





(def text->bow
  (memoize nlp/default-text->bow))


(def ->vocabulary-top-n
  (memoize nlp/->vocabulary-top-n))

(defn make-vocab-fn [n]
  (fn [bows]
    (->vocabulary-top-n bows n)))


(def fixed-pipe
  (ml/pipeline
   (mm/count-vectorize :comment_text :bow {:text->bow-fn text->bow})
   (mm/bow->tfidf :bow :tfidf)))

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
   (mm/bow->SparseArray :tfidf :sparse {:create-vocab-fn (make-vocab-fn  (pipe-opts :n)) })
   (mm/select-columns [:id :sparse (:target pipe-opts)])
   (mm/set-inference-target (:target pipe-opts))
   (mm/categorical->number [(:target pipe-opts)])
   (mm/model (merge pipe-opts
                    {:model-type :smile.classification/sparse-logistic-regression
                     :sparse-column :sparse
                     :n-sparse-columns (pipe-opts :n)
                     }))))

  (def test-ds
    (-> (ds/dataset "data/toxic/test.csv" {:key-fn keyword :parser-fn :string} )
        (ds/rename-columns {:text :comment_text})
        (ds/head num-test-rows)
        ))

  (def test-ids (:id test-ds))

  (def test-ds
    (->
     (fixed-pipe {:metamorph/data test-ds})
     :metamorph/data))


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
         :accuracy)

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

        _ (def test-ds-with-target test-ds-with-target)
        _ (def best best)

        prediction-on-test
        (
         (:pipe-fn best)
         (merge (:fit-ctx best)
                {:metamorph/data test-ds-with-target
                 :metamorph/mode :transform
                 })
         )


        probs (-> prediction-on-test :metamorph/data (#(get % "1")))]
    {target
     {:metric (:metric best)
      :probs probs}}

    )
  )



(def model-results
  (mapv
   train-and-eval


   [:toxic
    :severe_toxic
    :obscene
    :threat
    :insult
    :identity_hate]))

(def model-results
  (apply merge model-results)
  )




(def over-all-accuracy
  (dtf/mean (map :metric (vals model-results))))

(def submission
  (ds/dataset
   (assoc
    (zipmap
     (keys model-results)
     (map :probs  (vals model-results)) )
    :id test-ids)))

(println "accuracy: " over-all-accuracy)
(ds/write-csv! submission "submission.csv" )

(println "Finished")
