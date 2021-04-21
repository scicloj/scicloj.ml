(ns scicloj.ml.advanced
(:require
 [notespace.api :as note]
 [notespace.kinds :as kind ]
 [scicloj.ml.ug-utils :refer :all]
 )
  )

^kind/hidden
(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))

  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html "docs/userguide-advanced.html")
  (note/init) )



(require  '[scicloj.ml.core :as ml]
          '[scicloj.ml.metamorph :as mm]
          '[scicloj.ml.dataset :as ds]
          '[tech.v3.datatype.functional :as fun]
          )


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
Two modes are standardized, namely: `:fit` and `:transform`. In machine learning they often are called train / predict.
`sciform.ml` requires them to be `:fit` and `:transform`, and
third party libraries should adhere to this convention.
"]

["`:metamorph/id` contains at every step a different , unique, id. A model function can use it
to store the trained model in :fit and use it it :transform for prediction"]


["By default the value of `:metamorph/id` is set to a unique uuid. In some cases it can be useful to overwrite this auto-generated id.
This can be done by pre-pending any step function with a map. The map gets merged with the usual context map for this step, before the
step function is executed:
"]


(def pipe-fn
  (ml/pipeline
;;  step 1
;;  step 2
   {:metamorph/id :my-model} (mm/select-columns [:time])
   ))

["This map can have any key / value, which might be useful for injecting other static data into the pipeline."]

["Two functions in `scicloj.ml` use two further keys with the purpose of model evaluation, see further down in this guide."]
["The function `scicloj.ml.core/model` stores the feature-dataset and the inference-target-dataset in the ctx before doing a prediction
at keys `:scicloj.metamorph.ml/feature-ds` and  `:scicloj.metamorph.ml/target-ds`"]


["These are then used by function `scicloj.ml.core/evaluate-pipelines` to do performance measurements of a model"]



["## Debugging a metamorph pipeline"]

["A metamorph pipeline can be debugged by two simple techniques."]

["The first is to comment out parts of the pipeline, run it and
 inspect the pipe-fn result, namely the context."]



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
 steps."]

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
 So other tools for inspecting Clojure maps are usefull."]


["### Custom metamorph compliant function"]

["Custom steps in metamorph pipelines are normal Clojure functions."]
["Conceptually we have three types of functions, they differ by which keys in
 the context they manipulate."]

["1. Data manipulation functions. Use only :metamorph/data .
  2. Model type of functions. They use :metamorph/data , :metamorph/mode and :metamorph/id  and behave different in mode :fit and :mode transform. Eventually they use other keys in the context.
  2a. This variants of type 2), might use non standard keys to pass data between different steps and therefore collaborate.
"
 ]

["## Custom dataset->dataset transforming functions "]

["Most steps of a pipeline are about modifying the dataset, so most custom code will be here.
In machine learning, this is as well known as feature engineering, as new features get created
from existing features."
 ]

["For a custom data manipulation function to be able to participate in a metamorph pipeline
it needs to:

1. Take a context map as input
2. Return a context map
3. Modify the dataset at key :metamorph/data
4. Not change any other key ctx

"]

["Lets take as an example, a function which encodes a column with a numerical value to 3 categorical values:


 - < 0         -> :negative
 - > 0 - 1000  -> :low
 - > 1000      -> :high

 "]

["First a helper function which does the above transformation of a single value"]
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

["We can define inline a metamorph compliant function as anonymous function."]
(def pipe-fn-inline
  (ml/pipeline
   (fn [{:metamorph/keys [data]}]
     (ds/add-or-replace-column data :val (fn [ds] (map ->cat (:val ds)))))))

["### Lift a dataset->dataset function"]

["Here we first create a function which manipulates the dataset as we want"]
(defn ds->cat [ds]
  (ds/add-or-replace-column ds :val (fn [ds] (map ->cat (:val ds)))))

["and then we include it into the pipeline via
lifting the ds->ds function into
a :metamorph/data -> :metamorph/data function"]

(def pipe-fn-lift
  (ml/pipeline
   (ml/lift ds->cat)))


["### Metamorph compliant function"]
["We write directly a metamorph compliant, named , function.
The body of the function is the same as the body of the inline function from before.
"]

(defn mm->cat []
  (fn [{:metamorph/keys [data]}]
    (ds/add-or-replace-column data :val (fn [ds] (map ->cat (:val ds))))))

(def pipe-fn-mm
  (ml/pipeline
   (mm->cat)))


["The results of applying all 3 pipeline functions is the same."]
(pipe-fn-inline {:metamorph/data (ds/dataset {:val [-2 100 2000]})})
(pipe-fn-lift {:metamorph/data (ds/dataset {:val [-2 100 2000]})})
(pipe-fn-mm {:metamorph/data (ds/dataset {:val [-2 100 2000]})})




["### Custom model function"]

["In this chapter we see how to build a custom metamorph compliant function, which behaves like a simple model.
It takes the mean of the training data and applies this the to the test data.
"]


["Here we create dummy training data, which is like a time series.
We have values for time step 1-10, and want to predict (using the mean),
the value for future time steps.
"]
(def train-data
  (ds/dataset {:time [1 2 3 4 5 6 7 8 9 10]
               :val [1 3 4 4 20 3 4 18 39 23]}))
(def test-data
  (ds/dataset {:time [11 12 13 14 15]
               :val [nil nil  nil nil nil ]}))

["Next we create the model function. It makes use of namespaced
key destructuring, which allows very compact code.

The :id,:data and :mode keys from the context,
become local bindings.

In :mode :fit, we calculate the mean of the (training) data and store it in ctx under an `id` which is passed to
the function by `metamorph` and is a unique id of the step.
This we use then as key to store the mean in the context, so that in :transform we can read it from the ctx under the same `id`.
The `id` passed into the function is the same in :fit and :transform (but unique per step)
So we see how to pass data from the same function in the pipeline run in mode :fit to the
run in mode :transform.

Conceptually this function is a pair of train/predict functions, which behaves like `train` in mode :fit and
`predict` in mode :transform.
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

["We run the training as usual, passing a map of data and mode :fit. (The id gets added automatically)"]

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
["As the context is a normal map, we can store arbitrary data in it. This means normally, that two steps work
together in some form, and a former steps writes some data to the ctx under a `known` key, which the later step
reads from the pipeline under this `known, shared` key.
The use cases for this  are left for the user of `scicloj.ml` to decide, the library does not interfere in this.

The moment these functions become re-usable and are eventually added to a library, namespaced keywords should
be used for the keys in order to avoid name collisions.

Typical use cases might be:

1. Some values of the dataset have been converted in some form, and we need to keep some information to convert them back
2. In a Natural Language Processing pipeline, the vocabulary is generated in one pipeline step, and needs to be used later
3. Some non-tabular data is generated during pipeline execution and should be kept somewhere

This features makes sure, that a pipeline can stay 100 % self-contained,
whatever data needed to be stored and makes the metamorph pipeline concept future-proof.

"]

["### Pipeline evaluation and selection"]

["In machine learning we come at a certain moment of the development of
a data pipeline or a model to a point where we do not know any more for sure, if a certain
modification of the pipeline or the model will improve the predictive
performance any further or not."]

["In this situation we can think about trying out all different pipeline
or model parameters and select the best automatically. In the context of the
model, this is typically called `hyper-parameter tuning`"]


["Differently to other machine learning frameworks, `scicloj.ml` does not only
allow to hyper-tune the model parameters but as well the whole transformation pipeline."]

["#### evaluate-pipelines function"]

["The working horse for this is the `scicloj.ml/evaluate-pipelines` function."
 "It takes as basic input a `sequence of pipeline functions` , a `sequence of pairs of train and test datasets`
and  a `metric function`. It will then do a nested loop of all pipelines and all
train/test pairs and calculate the given  model metrics for all combinations.
(which means to `train` and `evaluate` all pipelines using the train/test dataset pairs. "
 ]

["By preparing the seq of pipelines and the seq of train/test pairs accordingly,
various types of grid search with various cross-validation schemes can be realized."
 ]




["By leaving it to the user to provide the seq of pipeline-fns, the sequence of train/test pairs and teh metric function,
the `evaluate-pipelines` function can be applied to a large variety of used cases. We will see below some examples, how to generate this
sequences."]

(def all-pipelines
  ;; whatever needed to get all pipeline fns
  ;; typically these are variations of one single pipeline, where some parameters are different
  ;; (but this is not required, the pipelines can be completely different)
  )

(def train-test-data-pairs
  ;; some form of split of test data, such as:
  ;; holdout
  ;; k-fold
  ;; leave-one-out
  )

;; evaluate all pipelines
(comment
  (ml/evaluate-pipelines all-pipelines train-test-data-pairs ml/classification-accuracy :accuracy))

["This will return the evaluations as a sequence of maps, as explained below."]

["#### Evaluate one pipeline function with Titanic example   "]

["We will reuse the example from the Introduction user guide."]

["First the data:"]
(def titanic-data
  (->   (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/titanic/train.csv"
                    {:key-fn keyword
                     :parser-fn :string
                     })))

["Now we create a seq of pipeline fns, in this case having only **one** pipeline function"]
(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:Survived :Pclass ])
   (mm/categorical->number [:Survived :Pclass])
   (mm/set-inference-target :Survived)
   (mm/model {:model-type :smile.classification/logistic-regression})))


["The sequence of pipeline functions consists for this example of a sequence with a single pipeline."]
(def all-pipelines [pipe-fn])

["For creating train/test pairs, the function `scicloj.ml.dataset/split->seq`
 creates them in the right format (list of maps with keys :train and :test and value being
a `tech.ml.dataset`)"
 ]

(def train-test-data-pairs (ds/split->seq titanic-data :holdout))

(def eval-results (ml/evaluate-pipelines all-pipelines
                                        train-test-data-pairs
                                        ml/classification-accuracy
                                        :accuracy))
["The result contains quite some information, I remove here the binary representation of
the model for pretty printing purposes."]
(remove-deep
 [:model-data]
 eval-results)

["On high level, the result contains for every fold and every pipe-fn
 (in this example we have only one), these keys with the
evaluation metrics and other information"]
(keys (first (first eval-results)))

["By default the `evaluate-pipeline` filters out the datasets already from the result,
which would else wise be in as well. This can be configured in the options when calling it."]

["We can get the accuracy of the one result by doing:"]
(:metric (first (first eval-results)))


["#### Evaluate several pipeline fns using k-fold with Titanic example - 2 pipeline functions"]

["First we will generate seq of 10 pairs of train/test using k-fold"]
(def train-test-data-pairs (ds/split->seq titanic-data :kfold {:k 10}))

["And then we just create two pipeline function via copy/paste/adapt.
(In reality we wanted to do this with a pipeline creating function taking parameters, see below).
"]


(def pipe-fn-1
  (ml/pipeline
   (mm/select-columns [:Survived :Pclass ])
   (mm/categorical->number [:Survived :Pclass])
   (mm/set-inference-target :Survived)
   (mm/model {:model-type :smile.classification/logistic-regression
              :max-iterations 1})))

(def pipe-fn-2
  (ml/pipeline
   (mm/select-columns [:Survived :Pclass ])
   (mm/categorical->number [:Survived :Pclass])
   (mm/set-inference-target :Survived)
   (mm/model {:model-type :smile.classification/logistic-regression
              :max-iterations 1000})))


["Just create seq of 2 pipeline functions"]
(def all-pipelines [pipe-fn-1 pipe-fn-2])


(def eval-results (ml/evaluate-pipelines all-pipelines
                                         train-test-data-pairs
                                         ml/classification-accuracy
                                         :accuracy
                                         ;; we return results of all pipelines and all folds
                                         ;; By default only the best fold of the best pipeline is returned
                                         {:return-best-pipeline-only false
                                          :return-best-crossvalidation-only false
                                          }

                                         ))


["This gives 2 * 10 = 20 results:"]
(map  :metric (flatten eval-results))


["#### Evaluate several pipeline with Titanic example - grid search pipelines"]

["Now we will generate our seq of pipeline functions."
 "First we need a function which creates a pipeline function from parameters:"
 ]

(defn create-pipe-fn [params]
  (ml/pipeline
   (mm/select-columns [:Survived :Pclass ])
   (mm/categorical->number [:Survived :Pclass])
   (mm/set-inference-target :Survived)
   {:metamorph/id :model} (mm/model (merge {:model-type :smile.classification/logistic-regression}
                    params))))


["This function can now be called to produce a pipeline fn:"]

(def pipe-fn (create-pipe-fn {:max-iterations 1}))

["But we go one step further already and grid search over potential values using sobol sequences"]

(def all-options
  (->>
   (ml/sobol-gridsearch {:max-iterations (ml/linear 1 1000)
                         :lambda (ml/linear 0 1)
                          :tolerance (ml/linear 1e-9 1e-1 20)
                         })
   (take 20)))

["This will produce an optimized search grid of all combinations of the options, by taking first larger and then smaller intervals in the boundaries of the options"
 "So taking the first 20 of these covers already the full space roughly. See help of the ml/sobol-gridsearch for more information."
 "This gives us 20 grid points for our parameter search, which we can easily transform in a sequence of 20 pipeline functions:"
 ]

(def all-pipelines
  (map create-pipe-fn all-options))

(def eval-results (ml/evaluate-pipelines all-pipelines
                                         train-test-data-pairs
                                         ml/classification-accuracy
                                         :accuracy
                                         {:return-best-pipeline-only false
                                          :return-best-crossvalidation-only false
                                          }))

["This gives 10 * 20 = 200 model performance results ( 10 folds times 20 option combinations)
 for which I print here the distribution:"]
(frequencies
 (map :metric
      (flatten eval-results)))

["Sorting the result by metric and taking the last, we can get the `best` performing model"]
(def best-result
  (->> eval-results
       flatten
       (sort-by :metric)
       last))

["Out of this can get the trained logistic regression model (in this case a Smile Java object), "]
(def best-logistic-regression-model
  (ml/thaw-model (get-in best-result [:fit-ctx :model] )))

best-logistic-regression-model

["to inspect its internals, like coefficients: "]
(seq
 (.coefficients best-logistic-regression-model))

["Or taking the best pipeleine,"]
(def best-pipe-fn
  (:pipe-fn best-result ))

["the best context"]
(def best-fit-ctx
  (:fit-ctx best-result ))

["and use this for predicting on new data:"]

(->
 (best-pipe-fn
  (merge best-fit-ctx
         {:metamorph/data (-> titanic-data  (ds/shuffle {:seed 123})  (ds/head 10))
          :metamorph/mode :transform
          }))
 :metamorph/data
 (ds/column-values->categorical :Survived))



["### Handling of categorical data"]

^kind/hidden
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
