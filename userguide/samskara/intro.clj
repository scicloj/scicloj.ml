(ns samskara.intro

  (:require
   [notespace.api :as note]
   [notespace.kinds :as kind ]))


(comment
  (note/init-with-browser)
  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html)
  (note/init)
  (note/update-config)

(notespace.api/update-config
  #(assoc % :source-base-path "userguide"))

  )

["# Clojure and machine learning "]

["In order to practice machine learning and create an ecosystem of models around it,
we need 2 pieces."]

["1. A standard way to store data."]
["2. A standard way to express steps of data manipulations including train/predict of a model"]

["The Clojure language and core libraries do not have build-in, specific support for this,
so some libraries are required. "]

["## Representing training data"]

["In the last 2 years the Clojure data science landscape was shaped
by the appearance and maturation of a new library to manage tabular data."]

["This library is [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset).
 It defines a in-memory tabular data structure and operations on it. It is a remarkable piece of software,
highly optimized and linking in its root to native memory and allow zero-copy integration's outside Clojure."]

["As it was organically growing over time, it's API is functional and complete,
but lacks consistency in some parts.
"]

["This was addressed by an other library, layering on top of it, called `tablecloth`"
 "It is available [here](https://github.com/scicloj/tablecloth)"
 ]

["So we have now a very reliable, mature, easy to use library to store and manipulate tabular data, including text."]


["## Data transformation pipelines."]

["In order to apply machine learning, the data needs to be transformed from its original form ,
(often as a data file), into the form required my the model.
 Sometimes these transformation are simple, like chaning encodins, sometimes very complex.
 In some contexts this is as well called feature engineering, which can result in arbitrary
complex data transformations.
This transformations are dataset->dataset transformations.
"
 ]

["This pipelines need to be repeatable and self-contained, as they need to run several times
with different data or in variants"]

["The `tablecoth` library contains already the concept of running a  pipeline on a dataset"]

["The simplest form of a pipeline in Clojure and Tablecloth, can just make use of the fact that all tablecloth functions take a dataset as
 the first parameter and return a dataset. So they can be chained together with the pipe (`->`) operator of Clojure, example:"]

(require '[samskara.dataset :as ds])
(def my-data
  (-> (ds/dataset "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv" {:key-fn keyword})
      (ds/select-columns [:symbol :price])
      (ds/add-or-replace-column :symbol (fn [ds] (map clojure.string/lower-case  (ds :symbol)) ))
      ))

["This form of pipeline works to manipulate a dataset, but has 3 disadvantages."]

["
1. `->` is a macro, so we cannot compose pipelines easily
2. We move a dataset object through the pipeline steps, the so only object we have nicely inside the pipeline is the dataset itself.
 But sometimes we need non-tabular, auxiliary data to be shared across the pipeline steps,
which is not possible with passing a dataset only.

Using this simple pipelines, would force
to hold auxiliary data in a global state of some form.
This makes is very hard to execute pipelines repeatedly, as they are not self-contained.
"]

["Due to this, the idea of the `metamorph` pipeline concept was born."]
["It addresses both shortcomings of the simpler pipeline."]

["Metamorph is documented here: [metamorph](https://github.com/scicloj/metamorph)"]

["It adds as well a third missing feature to the simple pipeline concept, mainly to run a pipeline
in 2 modes :fit and :transform, which is required for machine learning"]


["So a pipeline can be composed of functions, which adhere to the simple standards regarding input and output, as explained here:
https://github.com/scicloj/metamorph#compliant-operations"]

["Tablecloth contains such operations in the `tablecloth.pipeline` name space. All functions of the `tablecloth.api` ns are replicated there,
 but metamorph compliant"]

["The Clojure ML ecosystem is based on different libraries working together, as
idiomatic in Clojure"]

["Three different libraries are used internally, to create a working ml / nlp pipleine,
but this is hidden from the user, but is listed here for completeness."]

["
1. `Tabelcloth` to manipulate the dataset
2. `Metamorph.ml` Running pipelines and machine learning core functions
3. `tech.ml.smile` containing ML models and NLP functions based on Smile
"]


["[Smile](https://haifengl.github.io/) is a complete data science package for Java.
Various smile models are made available to Clojure by `tech.ml.smile` " ]

["The setup for the following code needs a single dependencies in deps.edn or project.clj"]


["
{:deps {
        scicloj.ml/scicloj.ml {:mvn/version \"0.1.0\"}} }
"]


["This library acts as a facade to the three libraries above, and arranges the functions in a simple way in these namespaces:"]

^kind/md-nocode
["

| namespace           | purpose                                                  |
|---------------------|----------------------------------------------------------|
| samskara.ml         | core functionality for machine learning                  |
| samskara.dataset    | functions to manipulate a dataset                        |
| samskara.methamorph | metamorph compliant functions to be used in ml pipelines |

 "]



["The we need to require s few namespaces from the three libraries"]

(require '[samskara.ml :as ml]
         '[samskara.metamorph :as mm]
         '[samskara.dataset :as ds]
         '[tech.v3.libs.smile.nlp :as nlp]
         )
["First we load and split the data."]
(def reviews
  (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/reviews.csv.gz"
              {:key-fn keyword}))
(def reviews-split
  (first
   (ds/split->seq reviews :holdout)))


["Then we define the pipeline and it steps:"]

(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:Text :Score])
   (mm/count-vectorize :Text :bow nlp/default-text->bow {})
   (mm/bow->sparse-array :bow :bow-sparse #(nlp/->vocabulary-top-n % 1000))
   (mm/set-inference-target :Score)
   (mm/select-columns [:bow-sparse :Score])
   (mm/model {:p 1000
              :model-type :smile.classification/maxent-multinomial
              :sparse-column :bow-sparse})
   ))

["Now we execute the pipeline, which will as well train the model. "]
(def trained-ctx
  (pipe-fn {:metamorph/data (:train reviews-split)
            :metamorph/mode :fit}))
