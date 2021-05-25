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
^kind/hidden
(defn docu-fn [v]
  (let [m (meta v)]
    (kind/override
     [
      (str  "## Transformer " "**" (:name m) "**")
      "----------------------------------------------------------"
      "__Clojure doc__:\n"
      (:doc m)
      "----------------------------------------------------------"
      ]
     kind/md-nocode
     )))



(docu-fn (var mm/count-vectorize))

["In the following we transform the text given in a dataset into a
 map of token counts applying some default text normalization." ]
(def data (ds/dataset {:text ["Hello Clojure world, hello ML word !"
                              "ML with Clojure is fun"
                              ]}))

^kind/dataset-grid
data

["_"]

(def fitted-ctx
  (ml/fit data
          (mm/count-vectorize :text :bow)))



fitted-ctx

(def bow-ds
(:metamorph/data fitted-ctx))

^kind/dataset
bow-ds


["A custom tokenizer can be specified by either passing options to
`scicloj.ml.smile.nlp/default-tokenize` "]


(def fitted-ctx
  (ml/fit
   data
   (mm/count-vectorize :text :bow {:stopwords ["clojure"]
                                   :stemmer :none
                                   })))

fitted-ctx

["or passing in a implementation of a tokenizer function"]

(def fitted-ctx
  (ml/fit
   data
   (mm/count-vectorize
    :text :bow
    {:text->bow-fn (fn [text options]
                     {:a 1 :b 2
                      })})))
fitted-ctx



(docu-fn (var mm/bow->SparseArray))
["Now we convert the bag-of-words map to a sparse array of class
 `smile.util.SparseArray`

"]
(def ctx-sparse
  (ml/fit
   bow-ds
   (mm/bow->SparseArray :bow :sparse)))

ctx-sparse


^kind/dataset
(:metamorph/data ctx-sparse)

["The SparseArray instances look like this:"]
(zipmap
 (:text bow-ds)
 (map seq
      (-> ctx-sparse :metamorph/data :sparse)))

(docu-fn (var mm/bow->sparse-array))
["Now we convert the bag-of-words map to a sparse array of class
 `java primitive int array`
"]
(def ctx-sparse
  (ml/fit
   bow-ds
   (mm/bow->sparse-array :bow :sparse)))

ctx-sparse

["We see as well the sparse representation as indices against the vocabulary
of the non-zero counts."]

(zipmap
 (:text bow-ds)
 (map seq
      (-> ctx-sparse :metamorph/data :sparse)))




["In both ->sparse function we can control the vocabulary via
the option to pass in a different / custom functions which creates
the vocabulary from the bow maps."]

(def ctx-sparse
  (ml/fit
   bow-ds
   (mm/bow->SparseArray
    :bow :sparse
    {:create-vocab-fn
     (fn [bow] (scicloj.ml.smile.nlp/->vocabulary-top-n bow 1))
     })))

ctx-sparse

(def ctx-sparse
  (ml/fit
   bow-ds
   (mm/bow->SparseArray
    :bow :sparse
    {:create-vocab-fn
     (fn [_]
       ["hello" "fun"]
       )})))

ctx-sparse


(docu-fn (var mm/bow->tfidf))
["Here we calculate the tf-idf score from the bag of words:"]

^kind/dataset
(ml/pipe-it
 bow-ds
 (mm/bow->tfidf :bow :tfidf))



(docu-fn (var mm/model))
["The `model` transformer allows to execute all machine learning models.clj
which register themself inside the `metamorph.ml` system via the function
`scicloj.metamorph.ml/define-model!`.
The build in models are listed here:
https://scicloj.github.io/scicloj.ml/userguide-models.html

"]

["We use the Iris data for this example:"]

(def iris
  (->
   (ds/dataset
    "https://raw.githubusercontent.com/scicloj/metamorph.ml/main/test/data/iris.csv" {:key-fn keyword})
   (tech.v3.dataset.print/print-range 5)
   )
  )

^kind/dataset
iris

(def train-test
  (ds/train-test-split iris))

["The pipeline consists in specifying the inference target,
 transform target to categorical and the model function"]
(def pipe-fn
  (ml/pipeline
   (mm/set-inference-target :species)
   (mm/categorical->number [:species])
   {:metamorph/id :model} (mm/model {:model-type :smile.classification/logistic-regression})))

["First we run the training "]
(def fitted-ctx
  (ml/fit
   (:train-ds train-test)
   pipe-fn
   ))

^kind/hidden
(defn dissoc-in [m ks]
  (let [parent-path (butlast ks)
        leaf-key (last ks)]
    (if (= (count ks) 1)
      (dissoc m leaf-key)
      (update-in m parent-path dissoc leaf-key))))

(dissoc-in  fitted-ctx [:model :model-data])

["and then prediction on test"]

(def transformed-ctx
  (ml/transform (:test-ds train-test) pipe-fn fitted-ctx ))

(-> transformed-ctx
    (dissoc-in [:model :model-data])
    (update-in [:metamorph/data ] #(tech.v3.dataset.print/print-range % 5))
    )

["and we get the predictions: "]
^kind/dataset
(-> transformed-ctx
    :metamorph/data
    (ds/reverse-map-categorical-xforms)
    (ds/select-columns :species)
    (ds/head))


(docu-fn (var mm/std-scale))
["We can use the std-scale transformer to center and scale data."]
["Lets take some example data:"]
(def data
  (ds/dataset
   [
    [100 0.001]
    [8   0.05]
    [50  0.005]
    [88  0.07]
    [4   0.1]]
   {:layout :as-row}))

^kind/dataset
data

["Now we can center each column arround 0 and scale
it by the standard deviation  of the column"]

^kind/dataset
(ml/pipe-it
 data
 (mm/std-scale [0 1] {}))


(docu-fn (var mm/min-max-scale))

["The min-max scaler scales columns in a specified interval,
by default from -0.5 to 0.5"]

^kind/dataset
(ml/pipe-it
 data
 (mm/min-max-scale [0 1] {}))

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
  (ml/fit
   sonar
   (mm/reduce-dimensions :pca-cov 60
                         col-names
                         {})))


["The next function transforms the result from the fitted pipeline
into vega lite compatible format for plotting"]
["It accesses the underlying Smile Java object to get the data on
the cumulative variance for each PCA component."]
(defn create-plot-data [ctx ]
  (map
   #(hash-map :principal-component %1
              :cumulative-variance %2)
   (range)
   (-> ctx vals (nth 2) :fit-result :model bean :cumulativeVarianceProportion)))

["Next we plot the cumulative variance over the component index:"]
^kind/vega
{:$schema "https://vega.github.io/schema/vega-lite/v5.json"
 :width 850
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
  (ml/fit
   sonar
   (mm/reduce-dimensions :pca-cov 2
                         col-names
                         {}
                         )
   (mm/select-columns  [:material "pca-cov-0" "pca-cov-1"])
   (mm/shuffle)))

^kind/dataset
(:metamorph/data fitted-ctx)

["As the data is now 2-dimensional, it is easy to plot:"]

(def scatter-plot-data
  (-> fitted-ctx
      :metamorph/data
      (ds/select-columns [:material "pca-cov-0" "pca-cov-1"])
      (ds/rows :as-maps)))


^kind/vega
{:$schema "https://vega.github.io/schema/vega-lite/v5.json"
 :data {:values scatter-plot-data}
 :width 500
 :height 500

 :mark :circle
 :encoding
 {:x {:field "pca-cov-0"  :type "quantitative"}
  :y {:field "pca-cov-1"  :type "quantitative"}
  :color {:field :material}}}

["The plot shows that the reduction to 2 dimensions does not create
linear separable areas of `M` and `R`. So a linear model will not be
 able to predict well the material from the 2 PCA components."]

["It even seems, that the reduction to 2 dimensions removes
too much information for predicting of the material for any type of model."]
