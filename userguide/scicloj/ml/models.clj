(ns scicloj.ml.models
  (:require
   [notespace.api :as note]
   [notespace.kinds :as kind]
   [notespace.view :as view]
   [tablecloth.api :as tc]
   [scicloj.ml.core]
   [scicloj.sklearn-clj.ml]
   [clojure.string :as str]
   [scicloj.ml.ug-utils :refer :all]
   [scicloj.kroki :as kroki]
   [clojure.java.io :as io]))

^kind/hidden
(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/render-static-html "docs/userguide-models.html"))

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



["## Smile classification models"]
^kind/hiccup-nocode (render-key-info ":smile.classification/ada-boost")

["In this example we will use the capapility of the Ada boost classifier
to give us the importance of variables."]

["As data we take the Wiscon Breast Cancer dataset, which has 30 variables."]
(def df
  (datasets/breast-cancer-ds))

["To get an overview of the dataset, we print its summary:"]

^kind/dataset
(ds/info df)

["Then we create a metamorph  pipeline with the ada boost model:"]

(def pipe-fn
  (ml/pipeline
   (mm/set-inference-target :class)
   (mm/categorical->number [:class])
   (mm/model
    {:model-type :smile.classification/ada-boost
     })))

["We run the pipeline in :fit. As we just explore the data,
not train.test split is needed."]

(def trained-ctx
  (ml/fit-pipe df
   pipe-fn))

["Next we take the model out of the pipeline:"]
(def model
  (-> trained-ctx vals (nth 2) ml/thaw-model))

(spit
 "/tmp/test.dot"
 (.dot  (first  (.trees model)))

 )


(with-open [out (io/output-stream
                 (notespace.api/file-target-path "tree1.svg")
                 )]
  (clojure.java.io/copy
   (:body
    (kroki/kroki (.dot  (first  (.trees model))) :graphviz :svg ))
   out
   ))

(with-open [out (io/output-stream
                 (notespace.api/file-target-path "tree2.svg")
                 )]
  (clojure.java.io/copy
   (:body
    (kroki/kroki (.dot (second (.trees model))) :graphviz :svg))
   out))

(defn copy [uri file]
  (with-open [in  (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

;(copy                                   ;
;"https://clojure.org/images/clojure-logo-120b.png"
; "https://kroki.io/plantuml/svg/eNplj0FvwjAMhe_5FVZP40CgaNMuUGkcdttp3Kc0NSVq4lRxGNKm_fe1HULuuD37-bOfuXPUm2QChEjRnlIMCDmdUfHNSYY6xh42a9Fsegflk-yYlOLlcHK2I2SGtX4WZm9sZ1o8uOzxxbuWAlIGj8cshs6M1jDuY2owyU2P8jAezdnn10j53X0hlBsZFW021Pq7HaVSNw-KN-OogG8F8BAGqT8dXhZjxW4cyJEW6kcC-yHWFagHqW0MfaThhYmaVyE26P_x27qaDmXeruqqAMMw1h-ZlRI4aF3dX7hOwm5XzfIKDctlNcshPT1tFa8JPYAj-Zf5F065sqM="
;      (notespace.api/file-target-path "clojure.png")
;      )

(notespace.api/img-file-tag "tree1.svg" {})

(notespace.api/img-file-tag "tree2.svg" {})






["The variable importance can be obtained from the trained model,"]
(def var-importances
  (mapv
   #(hash-map :variable %1
              :importance %2)
   (map
    #(first (.variables %))
    (.. model formula predictors))
   (.importance model)
   ))

["and we plot the variables:"]

 ^kind/vega
 {
 :data {:values
         var-importances}
  :width  800
  :height 500
  :mark {:type "bar"}
  :encoding {:x {:field :variable :type "nominal" :sort "-y"}
             :y {:field :importance :type "quantitative"}}}


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



["Standarise the data: "]
(def iris
  (ml/pipe-it
   iris
   (mm/std-scale [:sepal_length :sepal_width :petal_length :petal_width] {})))


^kind/dataset
iris

["The next function creates a vega specification for the random forest
decision surface for a given pair of column names."]




(def rf-model {:model-type :smile.classification/random-forest})

^kind/vega
(surface-plot iris [:sepal_length :sepal_width] rf-model)


^kind/vega
(surface-plot iris [:sepal_length :petal_length] rf-model)


^kind/vega
(surface-plot iris [:sepal_length :petal_width] rf-model)

^kind/vega
(surface-plot iris [:sepal_width :petal_length] rf-model)

^kind/vega
(surface-plot iris [:sepal_width :petal_width] rf-model)

^kind/vega
(surface-plot iris [:petal_length :petal_width] rf-model)



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
(def diabetes (datasets/diabetes-ds))

(def coefs-vs-lambda
  (flatten
   (map
    (fn [lambda]
      (let [fitted
            (ml/fit-pipe
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
      (ml/transform-pipe pipe-fn fitted)
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
(surface-plot iris [:sepal_length :sepal_width]  {:model-type :smile.classification/ada-boost})

^kind/vega
(surface-plot iris [:sepal_length :sepal_width]  {:model-type :smile.classification/decision-tree})


^kind/vega
(surface-plot iris [:sepal_length :sepal_width]  {:model-type :smile.classification/gradient-tree-boost})


^kind/vega
(surface-plot iris [:sepal_length :sepal_width]  {:model-type :smile.classification/knn})


^kind/vega
(surface-plot iris [:sepal_length :sepal_width]  {:model-type :smile.classification/logistic-regression})

^kind/vega
(surface-plot iris [:sepal_length :sepal_width]  {:model-type :smile.classification/random-forest})












^kind/hidden
(println
 (->> @scicloj.ml.core/model-definitions*
      (sort-by first)
      (filter #(str/starts-with? (first %) ":smile.regression" ))
      (map #(str  "^kind/hiccup-nocode (render-key-info \""   (first  %)   "\")\n")
           )
      ))
