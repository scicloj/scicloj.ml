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
["### Custom model function"]

["In this chapter we see how to build a custom metamorph complinat function, which behaves like a simple model.
It takes the mean of the training data and applies this the to the test data.
"]
(require  '[scicloj.ml.core :as ml]
          '[scicloj.ml.metamorph :as mm]
          '[scicloj.ml.dataset :as ds]
          '[tech.v3.datatype.functional :as fun]
          )

["Here we create dummy training data, which is like a time series.
We have values for time step 1-10, and want to predict (using the mean),
the value for future timestpes.
"]
(def train-data
  (ds/dataset {:time [1 2 3 4 5 6 7 8 9 10]
               :val [1 3 4 4 20 3 4 18 39 23]}))
(def test-data
  (ds/dataset {:time [11 12 13 14 15]
               :val [nil nil  nil nil nil ]}))

["Next we create the moddel function. It makes use of namespaced
key destructuring, which allows very compact code.

The :id,:data and :mode keys from the context ctx,
become local bindings.

In :mode :fit, we calculate the mean of the (training) data and store it in ctx under an `id` which is passed to
the function by `metamorph` and is a unique id of the step.
This we use then as key to store the mean in the context, so that in :transform we can read it from the ctx under the same `id`.
The `id` passed into the function is the same in :fit and :transform (but unique per step)
So we see how to pass data from the pipeline run in mode :fit to the run in mode :transform.
"
 ]

(defn mean-model []
  (fn [{:metamorph/keys [id data mode] :as ctx}]
    (case mode
      :fit
      (let [vals-so-far (-> data :val seq)
            mean-so-far (fun/mean vals-so-far)
            ]
        (assoc ctx id mean-so-far)
        )
      :transform
      (let [mean-so-far (get ctx id)
            updated-ds (-> data
                           (ds/add-or-replace-column :val mean-so-far ))]
        (assoc ctx :metamorph/data updated-ds)))))

["The piplein has only one step, the model function itself."]
(def pipe-fn
  (ml/pipeline
   (mean-model)
   ))

["We run the training as usual, passing a map of data and mode. (The id gets added automatically)"]

(def trained-ctx
  (pipe-fn {:metamorph/data train-data
         :metamorph/mode :fit}))

[ "Same for the prediction, in mode :transform, merging in the trained-ctx but overwritting data and mode"]

(def predicted-ctx
  (pipe-fn
   (merge trained-ctx
          {:metamorph/data test-data
           :metamorph/mode :transform})))

["This runs the pipeline again and we have the prediction available in :metamorph/data"]

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
