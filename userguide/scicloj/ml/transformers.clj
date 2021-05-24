(ns scicloj.ml.transformers
  (:require
   [notespace.api :as note]
   [notespace.kinds :as kind]
   [scicloj.ml.metamorph :as mm]
   )
  )

(comment

  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/render-static-html "docs/userguide-transformers.html")
  )

(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.dataset :as ds]
         '[scicloj.ml.metamorph :as mm]
         )

(defn docu-fn [v]
  (let [m (meta v)]
    (kind/override
     [(str  "## " (:name m))
      (:doc m
            )]
     kind/md-nocode
     )))



(docu-fn (var mm/count-vectorize))

(docu-fn (var mm/bow->SparseArray))

(docu-fn (var mm/bow->sparse-array))

(docu-fn (var mm/bow->tfidf))

(docu-fn (var mm/model))

(docu-fn (var mm/std-scale))

(docu-fn (var mm/min-max-scale))

(docu-fn (var mm/reduce-dimensions))

["#### PCA example"]

["In this example we run PCA on some data."]

(require '[scicloj.metamorph.ml.toydata :as toydata])

["We use the sonar dataset which has 60 columns of quantitative data,
which are certain measurements from a sonar device.
The original purpose of the dataset is to learn to detect rock vs metal
 from the measurements"]
(def sonar
  (toydata/sonar-ds))

^kind/dataset
sonar

(def col-names (map #(keyword (str "x" %))
                    (range 60)))

["First we create and run  a pipeline which does the PCA."
 "In this pipeline we do not fix the number of columns, as we want to
plot the result for all numbers of components (up to 60) "
 ]
(def fitted-ctx
  (ml/pipe-it
   sonar
   [
    (mm/reduce-dimensions :pca-cov 60
                          col-names
                          {}
                          )]))


["The next function transforms the result from the fitted pipeline
into vega lite compatible format for plotting"]
["It accesses the underlying Smile Java object to get the data on
the cumulative variance for each PCA component."]
(defn create-plot-data [ctx ]
  (map
   #(hash-map :principal-component %1
              :cumulative-variance %2
              )
   (range)
   (-> ctx vals (nth 2) :fit-result :model bean :cumulativeVarianceProportion)))

["Next we plot the cummulative variance over the component index:"]
^kind/vega
{:$schema "https://vega.github.io/schema/vega-lite/v5.json"


 :data {:values
        (create-plot-data fitted-ctx)}
 :mark "line" ,
 :encoding
 {:x {:field :principal-component, :type "nominal"},
  :y {:field :cumulative-variance, :type "quantitative"}}}

["From the plot we see, that transforming the data via PCA and reducing
it from 60 dimensions to about 25 would still preserve the full variance."]
["Looking at this plot, we could now make a decision, how many dimensions
 to keep."]
["We could for example decide, that keeping 60 % of the variance
is enough, which would result in keeping the first 2 dimensions."]

["So our pipeline becomes:"]


(def fitted-ctx
  (ml/pipe-it
   sonar
   [
    (mm/reduce-dimensions :pca-cov 2
                          col-names
                          {}
                          )
    (mm/select-columns  [:material "pca-cov-0" "pca-cov-1"])
    (mm/shuffle)
    ]))

^kind/dataset
(:metamorph/data fitted-ctx)

["As the data is now 2-dimensional, it is easy to plot:"]

(def scatter-plot-data
  (-> fitted-ctx
      :metamorph/data
      (ds/select-columns [:material "pca-cov-0" "pca-cov-1"])
      (ds/rows :as-maps)
      ))


^kind/vega
{:$schema "https://vega.github.io/schema/vega-lite/v5.json"
 :data {:values scatter-plot-data}
 :mark "point"
 :encoding
 {:x {:field "pca-cov-0"  :type "quantitative"}
  :y {:field "pca-cov-1"  :type "quantitative"}
  :color {:field :material}}}

["The plot shows that the reduction to 2 dimensions does not create
linear separable areas of `M` and `R`. So a linear model will not be
 able to predict well the material from the 2 PCA components."]

["It even seems, that the reduction to 2 dimensions removes
too much information for predicting of the material for any type of model."]
