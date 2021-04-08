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


["### Special keys in metamorph context map"]
["A metamorph context map can contain arbitrary keys. Three keys are
 special and they enable the functioning of the pipeline . All steps should
handle them in the same way."]

[" `:metamorph/data` contains the main data object, the pipeline
 is supposed to manipulate. The type of object can be anything, but all
functionality on `scicloj.ml` requires it to be a `tech.v3.dataset`
 instance. For further information see in
 [metamorph](https://github.com/scicloj/metamorph)" ]


["`:metamorph/mode` is used by 'model' functions, which get fitted from data or transform data.
Two modes are standardized, namelye: `:fit` and `:transform`. In machine learninig they often are called train / predict.
`sciform.ml` requires them to be `:fit` and `:transform`, and
third party libraries should adhere to this convention.
"]

["`:metamorph/id` contains at every step a different , unique, id. A model function can use it
to store the trained model in :fit and use it it :transform for prediction"]

["Two functions in `scicloj.ml` use two further keys with the purpose of model evaluation, see further down in this guide."]
["`scicloj.ml.core/model` stores the feature-dataset and the inference-target-dataset in the ctx before doing a prediction
at keys `:scicloj.metamorph.ml/feature-ds` and  `:scicloj.metamorph.ml/target-ds`"]

["These are then used by function `scicloj.ml.core/evaluate-pipelines` to do performace measurements of a model"]

["## Debugging a metamorph pipeline"]

["A metamorph pipeline can be debugged by two simple techniques."]

["The first is to comment out parts of the pipeline, run it and
 inspect the result, the context"]


(def train-data
  (ds/dataset {:time [1 2 3]
               :val [1 3 4]}))

(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:time])
   ;; (mm/step-2)
   ;; (mm/step-3)
   ;; (mm/step-4)
   ))

(def trained-ctx
  (pipe-fn {:metamorph/data train-data
            :metamorph/mode :fit}))

trained-ctx

["The second alternative is to capture the state of the ctx in arbitrary
 steps"]

(def pipe-fn
  (ml/pipeline
   (fn [ctx] (def ctx-1 ctx) ctx)
   (mm/select-columns [:time])
   (fn [ctx] (def ctx-2 ctx) ctx)
   ;; (mm/step-2)
   ;; (mm/step-3)
   ;; (mm/step-4)
   ))

(def ctx
  (pipe-fn {:metamorph/data train-data
            :metamorph/mode :fit}))

ctx-1
ctx-2

["The context contains as well the dataset, which could be large.
 So other tools for inspecting Clojure maps, are usefull."]


["### Custom metamorph compliant function"]

["Custom steps in metamorph pipelines are normal Clojure functions."]
["Conceptually we have three types of functions, they differ by which keys in
 the context they manipulate."]

["1. Data manipulation functions. Use only :metamorph/data
  2. Model type of functions. They use :metamorph/data , :metamorph/mode, :metamorph:id
     and behave different in mode :fit  and :mode transform. Eventually they use other keys in the context.
  3. They use other keys in the context to pass auxiliary data
"
 ]

["## Custom dataset->dataset transforming functions "]

["Most steps of a pipeline are about modifying the dataset, so most custom code will be here.
In machine learning, this is as well known as feature engineering, as new features get created from existing features."
 ]

["For a custom data manipulation function to be able to participate in a metamorph pipeline it needs to:

1. Take a context map as input
2. Return a context map
3. Modify the dataset at key :metamorph/data
4. Not use any other key in ctx
5. Not change any other key ctx

"]

["Lets take as an example, a function which encodes a column with a numerical value to 3 categorical values:

 - < 0         -> :negative
 - > 0 - 1000  -> :low
 - > 1000      -> :high

 "]

["Helper function which doe the transformation of a single value"]
(defn ->cat [x]
  (cond (< x 0 )          :negative
        (and (pos? x)
             (< x 1000) ) :low
        true              :high))


["We have now three different ways to write a metamorph compliant function"]
["1. Inline
  2. Lifting
  3. Named function
"]

["### Inline fn"]

["We can define inline a metamorph compliant function"]
(def pipe-fn-inline
  (ml/pipeline
   (fn [{:metamorph/keys [data]}]
     (ds/add-or-replace-column data :val (fn [ds] (map ->cat (:val ds)))))))

["### Lift a dataset->dataset function"]

["First we create a functions which manipulates the dataset as we want"]
(defn ds->cat [ds]
  (ds/add-or-replace-column ds :val (fn [ds] (map ->cat (:val ds)))))

["And then we include it into the pipeline via
lifting the ds->ds function into
a :metamorph/data -> :metamorph/data function"]
(def pipe-fn-lift
  (ml/pipeline
   (ml/lift ds->cat)))


["### Metamorph compliant function"]
["We write directly a metamorph compliant, named , function.
The body of the function is the same as the body of the inline fn from before.
"]

(defn mm->cat []
  (fn [{:metamorph/keys [data]}]
    (ds/add-or-replace-column data :val (fn [ds] (map ->cat (:val ds))))))

(def pipe-fn-mm
  (ml/pipeline
   (mm->cat)))


(pipe-fn-inline {:metamorph/data (ds/dataset {:val [-2 100 2000]})})
(pipe-fn-lift {:metamorph/data (ds/dataset {:val [-2 100 2000]})})
(pipe-fn-mm {:metamorph/data (ds/dataset {:val [-2 100 2000]})})




["### Custom model function"]

["In this chapter we see how to build a custom metamorph compliant function, which behaves like a simple model.
It takes the mean of the training data and applies this the to the test data.
"]
(require  '[scicloj.ml.core :as ml]
          '[scicloj.ml.metamorph :as mm]
          '[scicloj.ml.dataset :as ds]
          '[tech.v3.datatype.functional :as fun]
          )

["Here we create dummy training data, which is like a time series.
We have values for time step 1-10, and want to predict (using the mean),
the value for future timesteps.
"]
(def train-data
  (ds/dataset {:time [1 2 3 4 5 6 7 8 9 10]
               :val [1 3 4 4 20 3 4 18 39 23]}))
(def test-data
  (ds/dataset {:time [11 12 13 14 15]
               :val [nil nil  nil nil nil ]}))

["Next we create the model function. It makes use of namespaced
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

["The pipeline has only one step, the model function itself."]
(def pipe-fn
  (ml/pipeline
   (mean-model)
   ))

["We run the training as usual, passing a map of data and mode. (The id gets added automatically)"]

(def trained-ctx
  (pipe-fn {:metamorph/data train-data
         :metamorph/mode :fit}))

[ "Same for the prediction, in mode :transform, merging in the trained-ctx but overwriting data and mode"]

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


["### Keep auxiliary data in pipeline"]


["### Set custom id"]




["## More advanced use case, as we need to pass the vocab size between steps"]

^kind/hidden
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
