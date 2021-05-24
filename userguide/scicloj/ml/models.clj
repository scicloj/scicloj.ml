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

["The documentation below points as well to the javadoc and user-guide chapter (for Smile models)"]

["## Smile classification"]
^kind/hiccup-nocode
(render-key-info ":smile.classification")


["## Smile regression"]
^kind/hiccup-nocode
(render-key-info ":smile.regression")


["## Xgboost"]
^kind/hiccup-nocode
(render-key-info ":xgboost")


["## Random Forrest"]
(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset :refer [dataset add-column] ]
         '[scicloj.ml.dataset :as ds]
         '[tech.v3.dataset.math :as std-math]
         '[tech.v3.datatype.functional :as dtf]

         )


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
  (:metamorph/data
   (ml/pipe-it
    iris
    [
     (mm/std-scale [:sepal_length :sepal_width :petal_length :petal_width] {})])))


^kind/dataset
iris

["The next function creates a vega specification for the random forest
decision surface for a given pair of column names."]
(defn rf-surface [iris cols]
  (let [pipe-fn ;; pipeline including random forest model
        (ml/pipeline
         (mm/select-columns (concat [:species] cols))
         (mm/set-inference-target :species)
         (mm/categorical->number [:species])
         (mm/model {:model-type :smile.classification/random-forest}))

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
        (ds/add-column grid-ds :species prediction-grid)


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
        (ds/add-column iris :species prediction-iris)]

    ;; create a 2 layer Vega lite specification
    {:layer
     [

      {:data {:values (ds/rows grid-ds-prediction :as-maps)}
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
                  :color {:field :species}
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

                  :fill {:field :species :legend nil}
                  :stroke { :value :black }
                  :size {:value 300 }}}]}))


^kind/vega
(rf-surface iris [:sepal_length :sepal_width])


^kind/vega
(rf-surface iris [:sepal_length :petal_length])


^kind/vega
(rf-surface iris [:sepal_length :petal_width])

^kind/vega
(rf-surface iris [:sepal_width :petal_length])

^kind/vega
(rf-surface iris [:sepal_width :petal_width])

^kind/vega
(rf-surface iris [:petal_length :petal_width])
