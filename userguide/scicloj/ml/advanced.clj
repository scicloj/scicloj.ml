(ns scicloj.ml.advanced
(:require
   [notespace.api :as note]
   [notespace.kinds :as kind ])
  )

(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))

  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html "docs/userguide-advanced.html")
  (note/init) )



["### Debugging a metamorph pipeline"]


["### Model selection"]

["### Custom dataset->dataset transforming functions in a metamorph pipeline"]
["### inline fn"]
["### Custom metamorph compliant function"]
["### Custom function which behaves like a model"]
(comment
  (require  '[scicloj.ml.core :as ml]
            '[scicloj.ml.metamorph :as mm]
            '[scicloj.ml.dataset :as ds]
            '[tech.v3.datatype.functional :as fun]
            )

  (def train-data
    (ds/dataset {:time [1 2 3 4 5 6 7 8 9 10]
                 :val [1 3 4 4 20 3 4 18 39 23]}))
  (def test-data
    (ds/dataset {:time [11 12 13 14 15]
                 :val [nil nil  nil nil nil ]}))


  (defn MEAN-model []
    (fn [ctx]
       (case (:metamorph/mode ctx)
         :fit
         (let [vals-so-far (-> ctx :metamorph/data :val seq)
               mean-so-far (fun/mean vals-so-far)
               step-id (:metamorph/id ctx)
               ]
           (assoc ctx step-id mean-so-far)
           )
         :transform
         (let [step-id (:metamorph/id ctx)
               mean-so-far (get ctx step-id)
               ds (:metamorph/data ctx)
               updated-ds (-> ds
                              (ds/add-or-replace-column :val mean-so-far ))]
           (assoc ctx :metamorph/data updated-ds))))
    )

  (def pipe
    (ml/pipeline
     (MEAN-model)
     ))


  (def trained-ctx
    (pipe {:metamorph/data train-data
           :metamorph/mode :fit}))

  (def predicted-ctx
    (pipe
     (merge trained-ctx
            {:metamorph/data test-data
             :metamorph/mode :transform})))

  (def prediction
    (:metamorph/data predicted-ctx))

  prediction
  ;; => _unnamed [5 2]:
  ;;    |  :time |        :val |
  ;;    |--------|-------------|
  ;;    |     11 | 10.66666667 |
  ;;    |     12 | 10.66666667 |
  ;;    |     13 | 10.66666667 |
  ;;    |     14 | 10.66666667 |
  ;;    |     15 | 10.66666667 |
  )
["### Lifting a existing dataset->dataset transformation fn"]
["### Keep auxiliary data in pipeline"]


["### Special keys in metamorph context map"]
:metamorph/data
:metamorph/mode
:metamorph/id
[ "### Set custom id"]




["## More advanced use case, as we need to pass the vocab size between
 steps"]
["Not working yet"]

(comment
  (def reviews
    (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/reviews.csv.gz"
                {:key-fn keyword}))
  (def reviews-split
    (first
     (ds/split->seq reviews :holdout)))

  (def pipe-fn
    (ml/pipeline
     (mm/select-columns [:Text :Score ])
     (mm/count-vectorize :Text :bow)
     (mm/bow->sparse-array :bow :bow-sparse)
     (mm/set-inference-target :Score)
     (mm/select-columns [:bow-sparse :Score])
     ;; It takes key :scicloj.ml.smile.metamorph/bow->sparse-vocabulary
     ;; from ctx and sets it in the next step
     (fn [ctx]
       (let [p (-> ctx :scicloj.ml.smile.metamorph/bow->sparse-vocabulary
                   :vocab
                   count
                   )]
         ((mm/model {:p p
                     :model-type :smile.classification/maxent-multinomial
                     :sparse-column :bow-sparse})
          ctx)
         )
       ctx
       )
     ))

  (def trained-ctx
    (pipe-fn {:metamorph/data (:train reviews-split)
              :metamorph/mode :fit}))

  )
