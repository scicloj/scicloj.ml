(ns scicloj.ml.models
(:require
 [notespace.api :as note]
 [notespace.kinds :as kind ]
 [notespace.view :as view]
 [tablecloth.api :as tc]
 [scicloj.ml.core]
 [scicloj.sklearn-clj.ml]
 [clojure.string :as str]
 [scicloj.ml.ug-utils :refer :all]))

^kind/hidden
(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/render-static-html "docs/userguide-models.html")
  )

["# Models"]

["scicloj.ml uses the plugin `scicloj.ml.smiles` and
`scicloj.ml.xgboost` by default,
which gives access to a lot of model from the java libraries [Smile](https://haifengl.github.io/)
and [Xgboost](https://xgboost.readthedocs.io/en/latest/jvm/index.html)"]

["Below is a list of all such models, and which parameter they take."]

["All models are available in the same way:"]

(comment
  (require '[scicloj.ml.metamorph :as mm])
  ;; last step in pipeline
  (mm/model {:model-type :smile.classification/ada-boost
             :trees 200
             :max-depth 100
             :max-nodes 50
             }))

(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset :refer [dataset add-column] ]
         '[scicloj.ml.dataset :as ds]
         '[tech.v3.dataset.math :as std-math]
         '[tech.v3.datatype.functional :as dtf]
         '[scicloj.metamorph.ml.toydata :as datasets]
         )

["The documentation below points as well to the javadoc and user-guide chapter (for Smile models)"]



["## Smile classification"]
^kind/hiccup-nocode (render-key-info ":smile.classification/ada-boost")
^kind/hiccup-nocode (render-key-info ":smile.classification/decision-tree")
^kind/hiccup-nocode (render-key-info ":smile.classification/discrete-naive-bayes")
^kind/hiccup-nocode (render-key-info ":smile.classification/gradient-tree-boost")
^kind/hiccup-nocode (render-key-info ":smile.classification/knn")

["In this example we use a knn model to classify some dummy data.
The training data is this:
"]
(def df
  (ds/dataset {:x1 [7 7 3 1]
               :x2 [7 4 4 4]
               :y [ :bad :bad :good :good]}))

^kind/dataset
df

["Then we construct a pipeline with the knn model,
using 3 neighbours for decision."]

(def pipe-fn
  (ml/pipeline
   (mm/set-inference-target :y)
   (mm/categorical->number [:y])
   (mm/model
    {:model-type :smile.classification/knn
     :k 3})))

["We run the pipeline in mode fit:"]

(def trained-ctx
  (pipe-fn {:metamorph/data df
            :metamorph/mode :fit}))


["Then we run the pipeline in mode :transform with some test data
and take the prediction and convert it from numeric into categorical:"]

(->
 trained-ctx
 (merge
  {:metamorph/data (ds/dataset
                    {:x1 [3 5]
                     :x2 [7 5]
                     :y [nil nil]})
   :metamorph/mode :transform})
 pipe-fn
 :metamorph/data
 (ds/column-values->categorical :y))



^kind/hiccup-nocode (render-key-info ":smile.classification/logistic-regression")
^kind/hiccup-nocode (render-key-info ":smile.classification/maxent-binomial")
^kind/hiccup-nocode (render-key-info ":smile.classification/maxent-multinomial")
^kind/hiccup-nocode (render-key-info ":smile.classification/random-forest")

["The following code plots the decision surfaces of the random forest
 model on pairs of features."]
["We use the Iris dataset for this."]

(def iris
  (ds/dataset
   "https://raw.githubusercontent.com/scicloj/metamorph.ml/main/test/data/iris.csv" {:key-fn keyword})
  )

(defn stepped-range [start end n-steps]
  (let [diff (- end start)]
    (range start end (/ diff n-steps))))

["Standarise the data: "]
(def iris
  (ml/pipe-it
   iris
   (mm/std-scale [:sepal_length :sepal_width :petal_length :petal_width] {})))


^kind/dataset
iris

["The next function creates a vega specification for the random forest
decision surface for a given pair of column names."]

(defn rf-surface [iris cols model-options]
  (let [pipe-fn ;; pipeline including random forest model
        (ml/pipeline
         (mm/select-columns (concat [:species] cols))
         (mm/set-inference-target :species)
         (mm/categorical->number [:species])
         (mm/model model-options)

         )

        fitted-ctx
        (pipe-fn
         {:metamorph/data iris
          :metamorph/mode :fit})

        ;; getting plot boundaries
        min-x (-  (-> (get iris (first cols))  dtf/reduce-min) 0.2)
        min-y (- (-> (get iris (second cols)) dtf/reduce-min) 0.2)
        max-x (+  (-> (get iris (first cols))  dtf/reduce-max) 0.2)
        max-y (+  (-> (get iris (second cols)) dtf/reduce-max) 0.2)


        ;; make a grid for the decision surface
        grid
        (for [x1 (stepped-range min-x max-x 100)
              x2 (stepped-range min-y max-y 100)
              ]
          {(first cols) x1
           (second cols) x2
           :species nil})

        grid-ds (ds/dataset  grid)

        ;; predict for all grid points
        prediction-grid
        (->
         (pipe-fn
          (merge
           fitted-ctx
           {:metamorph/data grid-ds
            :metamorph/mode :transform}))
         :metamorph/data
         (ds/column-values->categorical :species)
         seq)

        grid-ds-prediction
        (ds/add-column grid-ds :predicted-species prediction-grid)


        ;; predict the iris data points from data set
        prediction-iris
        (->
         (pipe-fn
          (merge
           fitted-ctx
           {:metamorph/data iris
            :metamorph/mode :transform}))
         :metamorph/data

         (ds/column-values->categorical :species)
         seq)

        ds-prediction
        (ds/add-column iris :true-species (:species iris)
                       prediction-iris)]

    ;; create a 2 layer Vega lite specification
    {:layer
     [

      {:data {:values (ds/rows grid-ds-prediction :as-maps)}
       :title (str "Decision surfaces for model: " (:model-type model-options))
       :width 500
       :height 500
       :mark {:type "square" :opacity 0.9 :strokeOpacity 0.1 :stroke nil},
       :encoding {:x {:field (first cols)
                      :type "quantitative"
                      :scale {:domain [min-x max-x]}
                      :axis {:format "2.2"
                             :labelOverlap true}
                      }
                  :y {:field (second cols) :type "quantitative"
                      :axis {:format "2.2"
                             :labelOverlap true}
                      :scale {:domain [min-y max-y]}
                      }
                  :color {:field :predicted-species}
                  }}

      {:data {:values (ds/rows ds-prediction :as-maps)}

       :width 500
       :height 500
       :mark {:type "circle" :opacity 1 :strokeOpacity 1},
       :encoding {:x {:field (first cols)
                      :type "quantitative"
                      :axis {:format "2.2"
                             :labelOverlap true}
                      :scale {:domain [min-x max-x]}
                      }
                  :y {:field (second cols) :type "quantitative"
                      :axis {:format "2.2"
                             :labelOverlap true}
                      :scale {:domain [min-y max-y]}
                      }

                  :fill {:field :true-species ;; :legend nil
                         }
                  :stroke { :value :black }
                  :size {:value 300 }}}]}))


(def rf-model {:model-type :smile.classification/random-forest})

^kind/vega
(rf-surface iris [:sepal_length :sepal_width] rf-model)


^kind/vega
(rf-surface iris [:sepal_length :petal_length] rf-model)


^kind/vega
(rf-surface iris [:sepal_length :petal_width] rf-model)

^kind/vega
(rf-surface iris [:sepal_width :petal_length] rf-model)

^kind/vega
(rf-surface iris [:sepal_width :petal_width] rf-model)

^kind/vega
(rf-surface iris [:petal_length :petal_width] rf-model)



^kind/hiccup-nocode (render-key-info ":smile.classification/sparse-logistic-regression")
^kind/hiccup-nocode (render-key-info ":smile.classification/sparse-svm")
^kind/hiccup-nocode (render-key-info ":smile.classification/svm")


["## Smile regression"]

^kind/hiccup-nocode (render-key-info ":smile.regression/elastic-net")
 ^kind/hiccup-nocode (render-key-info ":smile.regression/gradient-tree-boost")
^kind/hiccup-nocode (render-key-info ":smile.regression/lasso")

["We use the diabetes dataset and will show how Lasso regression
regulates the different variables dependent of lambda."]

["First we make a function to create pipelines with different lambdas"]
(defn make-pipe-fn [lambda]
  (ml/pipeline
   (mm/update-column :disease-progression (fn [col] (map #(double %) col)))
   (mm/convert-types :disease-progression :float32)
   (mm/set-inference-target :disease-progression)
   {:metamorph/id :model} (mm/model {:model-type :smile.regression/lasso
                                     :lambda lambda})))

["No we go over a sequence of lambdas and fit a pipeline for all off them
and store the coefficients for each predictor variable:"]

(def coefs-vs-lambda
  (flatten
   (map
    (fn [lambda]
      (let [fitted
            (ml/fit
             diabetes
             (make-pipe-fn lambda))

            model-instance
            (-> fitted
                :model
                (ml/thaw-model))

            predictors
            (map
             #(first (.variables %))
             (seq
              (.. model-instance formula predictors)))
            ]
        (map
         #(hash-map :log-lambda (dtf/log10 lambda)
                    :coefficient %1
                    :predictor %2)
         (-> model-instance .coefficients seq)
         predictors)))
    (range 1 100000 100))))

["Then we plot the coefficients over the log of lambda."]

^kind/vega
{
 :data {:values coefs-vs-lambda}

 :width 500
 :height 500
 :mark {:type "line"}
 :encoding {:x {:field :log-lambda :type "quantitative"}
            :y {:field :coefficient :type "quantitative"}
            :color {:field :predictor}}}

["This shows that an increasing lambda regulates more and more variables
 to zero. This plot can be used as well to find important variables,
namely the ones which stay > 0 even with large lambda."]

^kind/hiccup-nocode (render-key-info ":smile.regression/ordinary-least-square")

["In this example we will explore the relationship between the
body mass index (bmi) and a diabetes indicator."]

["First we load the data and split into train and test sets."]
(def diabetes (datasets/diabetes-ds))

(def diabetes-train
  (ds/head diabetes 422))

(def diabetes-test
  (ds/tail diabetes 20))



["Next we create the pipeline, converting the target variable to
a float value, as needed by the model."]

(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:bmi :disease-progression])
   (mm/update-column :disease-progression (fn [col] (map #(double %) col)))
   (mm/convert-types :disease-progression :float32)
   (mm/set-inference-target :disease-progression)
   {:metamorph/id :model} (mm/model {:model-type :smile.regression/ordinary-least-square})))

["We can then fit the model, by running the pipeline in mode :fit"]

(def fitted
  (ml/fit
   diabetes-train
   pipe-fn))


["Next we run the pipe-fn in :transform and extract the prediction
for the disease progression:"]
(def diabetes-test-prediction
  (-> diabetes-test
      (ml/transform pipe-fn fitted)
      :metamorph/data
      :disease-progression))

["The truth is available in the test dataset."]
(def diabetes-test-trueth
  (-> diabetes-test
      :disease-progression
      ))



["The smile Java object of the LinearModel is in the pipeline as well:"]

(def model-instance
  (-> fitted
      :model
      (ml/thaw-model)))

["This object contains all information regarding the model fit
such as coefficients and formula:"]
(-> model-instance .coefficients seq)
(-> model-instance .formula str)

["Smile generates as well a String with the result of the linear
regression as part of the toString() method of class LinearModel:"]
^kind/code
(println-str
 (str model-instance))

["This tells us that there is a statistically significant
(positive) correlation between the bmi and the diabetes
disease progression in this data."]


["At the end we can plot the truth and the prediction on the test data,
and observe the linear nature of the model."]

^kind/vega
{:layer [
         {:data {:values (map #(hash-map :disease-progression %1 :bmi %2 :type :truth)
                                 diabetes-test-trueth
                                 (:bmi  diabetes-test))}

          :width 500
          :height 500
          :mark {:type "circle"}
          :encoding {:x {:field :bmi :type "quantitative"}
                     :y {:field :disease-progression :type "quantitative"}
                     :color {:field :type}}}

         {:data {:values (map #(hash-map :disease-progression %1 :bmi %2 :type :prediction)
                  diabetes-test-prediction
                  (:bmi diabetes-test))}

          :width 500
          :height 500
          :mark {:type "line" }
          :encoding {:x {:field :bmi :type "quantitative"}
                     :y {:field :disease-progression :type "quantitative"}
                     :color {:field :type}}}

         ]}

^kind/hiccup-nocode (render-key-info ":smile.regression/random-forest")
 ^kind/hiccup-nocode (render-key-info ":smile.regression/ridge")


["## Xgboost"]
^kind/hiccup-nocode
(render-key-info ":xgboost")

["# Compare decision surfaces of models"]

["In the following we see the decision surfaces of some models on the
same data from the Iris dataset using 2 columns :sepal_width and sepal_length:"]

^kind/vega
(rf-surface iris [:sepal_length :sepal_width]  {:model-type :smile.classification/ada-boost})

^kind/vega
(rf-surface iris [:sepal_length :sepal_width]  {:model-type :smile.classification/decision-tree})


^kind/vega
(rf-surface iris [:sepal_length :sepal_width]  {:model-type :smile.classification/gradient-tree-boost})


^kind/vega
(rf-surface iris [:sepal_length :sepal_width]  {:model-type :smile.classification/knn})


^kind/vega
(rf-surface iris [:sepal_length :sepal_width]  {:model-type :smile.classification/logistic-regression})

^kind/vega
(rf-surface iris [:sepal_length :sepal_width]  {:model-type :smile.classification/random-forest})












^kind/hidden
(println
 (->> @scicloj.ml.core/model-definitions*
      (sort-by first)
      (filter #(str/starts-with? (first %) ":smile.regression" ))
      (map #(str  "^kind/hiccup-nocode (render-key-info \""   (first  %)   "\")\n")
           )
      ))
