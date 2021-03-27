(ns scicloj.ml.titanic
  (:require
   [notespace.api :as note]
   [notespace.kinds :as kind ]))

(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html "docs/userguide-titanic.html")

  (note/init)
  )


(require '[scicloj.ml.dataset :as ds])
(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mm]
         ;; '[samskara.dataset :refer [dataset add-column] ]
         '[camel-snake-kebab.core :as csk]
         '[scicloj.metamorph.ml.loss :as loss]
         '[clojure.string :as str]
         )


["## Introduction "]

[" In this example, we will train a model which is able to predict the survival of passengers from the Titanic dataset."
 "In a real analysis, this would contain as well explorative analysis of the data, which I will skip here,
as the purpose is to showcase machine learning with scicloj.ml, which is about model evaluation and selection."
 ]


["### Read data"]

(def data (ds/dataset "data/titanic/train.csv" {:key-fn csk/->kebab-case-keyword}))


["Column names:"]
(ds/column-names data)

["The following splits the dataset in three pieces,
 train, val and test to predict on later.
"]


(def ds-split (first (ds/split->seq data :holdout {:ratio [0.8 0.2]
                                                               :split-names [:train-val :test]}
                                                      )))

["Create a sequence of train/test  (k-fold with k=10) splits used to evaluate the pipeline."]
(def train-val-splits
    (ds/split->seq
     (:train-val ds-split)
     :kfold
     {:k 10}

     ))



["### Use Pclass, Sex, title, age for prediction"]

["We want to create a new column :title which might help in the score.
This is an example of custom function, which creates a new column from existing columns,
which is a typical case of feature engineering."]

(defn name->title [dataset]
  (-> dataset
      (ds/add-or-replace-column
       :title
       (map
        #(-> % (str/split  #"\.")
             first
             (str/split  #"\,")
             last
             str/trim)
        (data :name)))
      (ds/drop-columns :name)))


["The pipeline definition"]

(def pipeline-fn
  (ml/pipeline
   (mm/select-columns [:survived :pclass :name :sex :age])

   ;; included th custom function via lifting in the pipeline
   (ml/lift name->title)

   (mm/categorical->number [:survived :pclass :sex :title])
   (mm/set-inference-target :survived)

   ;; this key in the data is required by the function
   ;; scicloj.metamorph.ml/evaluate-pipeline
   ;; and need to contain the target variable (the truth)
   ;; as a dataset
   (fn [ctx]
     (assoc ctx
            :scicloj.metamorph.ml/target-ds (ds/target (:metamorph/data ctx))))

   ;; we overwrite the id, so the model function will store
   ;; it's output (the model) in the pipeline ctx under key :model
   {:metamorph/id :model}
   (mm/model {:model-type :smile.classification/random-forest})))

["Evaluate the (single) pipeline function using the train/test split"]
(def evaluations
  (ml/evaluate-pipelines
   [pipeline-fn]
   train-val-splits
   ml/classification-accuracy
   :accuracy))


["The default k-fold splits makes 10 folds,
so we train 10 models, each having its own loss."]

["The `evaluate-pipelines` fn averages the models per pipe-fn,
and returns the best.
So we get a single model back, as we only have one pipe fn"]

["Often we consider the model with the lowest loss to be the best."]

["The evaluation results contains all data sets for test and prediction
, all models, and the loss
for each cross validation"]

["`tech.ml` stores the models ion the context in a serialzed form,
and the function `thaw-model` can be used to get the original model back.
This is a Java class in the case of
 model :smile.classification/random.forest, but this depends on the
which `model` function is in the pipeline"]

["We can get for example,  the models like this:"]

(def models
  (->> evaluations
       flatten
       (map
        #(hash-map :model (ml/thaw-model (get-in % [:fit-ctx :model]))
                   :metric (:metric %)
                   :fit-ctx (:fit-ctx %)
                   ;; :data (get-in % [:fit-ctx :metamorph/data])
                   ))
       (sort-by :mean)
       reverse))


["The accuracy of the 10 trained model is:"]
(map #(dissoc % :data :fit-ctx) models)

["The one with the highest accuracy is then:"]
(:model (first models))

 ["with a  accuracy of :"]
(:metric (first models))

;; ["The pre-processed data is:"]
;; ^kind/dataset-grid
;; (:data (first models))



["We can get the predictions on new-data, which for classification contain as well
the posterior probabilities per class."]

["We do this by running the pipeline again, this time with new data and merging
:mode transform"]

(def predictions
  (->
   (pipeline-fn
    (assoc
     (:fit-ctx (first models))
     :metamorph/data (:test ds-split)
     :metamorph/mode :transform))
   :metamorph/data))

^kind/dataset
predictions

;; ["We have a helper function, which allows to predict using
;;  the best model from the result to `evaluate-pipelines`,
;; as this is a very common case."]


;; (def predictions
;;   (->
;;    (eval-mm/predict-on-best-model
;;     evaluations
;;     new-data
;;     :accuracy)))

["Out of the predictions and the truth, we can construct the
 confusion matrix."]

(def trueth
  (->
   (pipeline-fn {:metamorph/data (:test ds-split) :metamorph/mode :fit })
   :metamorph/data
   tech.v3.dataset.modelling/labels))

^kind/dataset
(->
 (ml/confusion-map (:survived predictions)
                        (:survived trueth)
                        :none)
 (ml/confusion-map->ds))

["### Hyper parameter tuning"]

["This defines a pipeline with options. The options gets passed to the model function,
so become hyper-parameters of the model.

The `use-age?` options is used to make a conditional pipeline. As the use-age? variable becomes part of the grid to search in,
we tune it as well.
This is an example how pipeline-options can be grid searched in the same way then hyper-parameters of the model.

"]
(defn make-pipeline-fn [options]

  (ml/pipeline
   (if (:use-age? options)
     (mm/select-columns [:survived :pclass :name :sex :age])
     (mm/select-columns [:survived :pclass :name :sex])
     )
   (ml/lift name->title)
   (mm/categorical->number [:survived :pclass :sex :title])
   (mm/set-inference-target :survived)
   (fn [ctx]
     (assoc ctx
            :scicloj.metamorph.ml/target-ds (ds/target (:metamorph/data ctx))))
   ;; any plain map in a pipeline definition get merged into the context
   ;; this is used here to configure the if for the next step
   ;; so the id can be set by the user
   {:metamorph/id :model}
   (mm/model
    (merge options
           {:model-type :smile.classification/random-forest}))))

["Use sobol optimization, to find 10 grid points,
which cover in a smart way the hyper-parameter space."]

(def search-grid
  (->>
   (ml/sobol-gridsearch {:trees (ml/linear 100 500 10)
                           :split-rule (ml/categorical [:gini :entropy])
                           :max-depth (ml/linear 1 50 10 )
                           :node-size (ml/linear 1 10 10)
                           :sample-rate (ml/linear 0.1 1 10)
                           :use-age? (ml/categorical [true false])})
   (take 10))
  )

["Generate the 10 pipeline-fn we want to evaluate."]
(def pipeline-fns (map make-pipeline-fn search-grid))


["Evaluate all 10 pipelines"]
(def evaluations
  (ml/evaluate-pipelines
   pipeline-fns
   train-val-splits
   ml/classification-accuracy
   :accuracy

   ))

["Get the key information from the evaluations and sort by the metric function used,
 accuracy here."]

(def models
  (->> evaluations
       flatten
       (map
        #(assoc
          (select-keys % [:metric :mean :fit-ctx])
          :model (ml/thaw-model (get-in % [:fit-ctx :model]))
          :data (get-in % [:fit-ctx :metamorph/data])))
       (sort-by :metric)
       reverse))


["As we sorted by mean accuracy, the first evaluation result is te best model,"]
(def best-model (first models))

["which is: "]
(:model best-model)

 ["with a accuracy of "  (:metric best-model)
  "and a mean accuracy of " (:mean best-model)]

["using options: "]
(-> best-model :fit-ctx :model :options)
