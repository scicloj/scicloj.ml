(ns scicloj.ml.intro
  (:require
   [notespace.api :as note]
   [notespace.kinds :as kind ]))


(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))

  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html "docs/userguide-intro.html")
  (note/init) )

["# Clojure and machine learning "]

["In order to practice machine learning and create an ecosystem of models around it,
we need 3 components."]

["1. A standard way to manage tabular data in memory."]
["2. Various machine learning models"]
["3. A standard way to express steps of data manipulations including train/predict of a model"]


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

["This was addressed by an other library, layering on top of it, called
`tablecloth`. It is available [here](https://github.com/scicloj/tablecloth)"
 ]

["So we have now a very reliable, mature, easy to use library to store and manipulate tabular data, including text."]

["## Models"]
["Models are the core of most machine learning libraries. In scicloj.ml we
 rely on an common **abstraction** for all
machine learning models and one Java library [Smile](https://github.com/haifengl/smile) providing models,
which we bridge into Clojure via the abstraction.
So we use Java models internally, but without the need for Java
interop by the user.

Documentation for existing models is appearing here:
https://scicloj.github.io/scicloj.ml/userguide-models.html

The abstraction is independent from Smile, so we could makes bridges to other libraries, even in non JVM languages (python, R)


"]

["## Data transformation pipelines."]

["In order to apply machine learning, the data needs to be transformed from its original form ,
(often as a data file), into the form required by the model.
 Sometimes these transformation are simple, like re-encode data,
sometimes they are very complex. In some contexts this is as well called
 feature engineering, which can result in arbitrary
complex dataset transformations.
This transformations are mostly dataset to dataset transformations.
"
 ]

["These pipelines need to be repeatable and self-contained,
as they need to run several times with different data or in variants
for either cross validation or hyper-parameter tuning."]

["Clojure and the `tablecloth` library contains already
the concept of running a pipeline"]

["These simpler form of a pipeline in Clojure and Tablecloth, can just make use of the fact that all tablecloth
 functions take a dataset as the first parameter and return a dataset.
So they can be chained together with the pipe (`->`) operator of Clojure,
 example:"]

(require '[scicloj.ml.dataset :as ds])
(def my-data
  (-> (ds/dataset "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv" {:key-fn keyword})
      (ds/select-columns [:symbol :price])
      (ds/add-or-replace-column :symbol (fn [ds] (map clojure.string/lower-case  (ds :symbol)) ))))

["This form of pipeline works to manipulate a dataset,
but has three disadvantages."]

["
1. `->` is a macro, so we cannot compose pipelines easily

2. We move a dataset object through the pipeline steps, so the only object we have nicely inside the pipeline, accessible to all steps, is the dataset itself.  But sometimes we need non-tabular, auxiliary, data to be shared across the pipeline steps, which is not possible with passing a dataset only.Using this simple pipelines, would force to hold auxiliary data in a global state of some form. This makes is very hard to execute pipelines repeatedly, as they are not self-contained.

3. These simpler pipeline concepts have no notion of running a pipeline in several modes. In machine learning a pipeline need to behave differently in `fit` and in `transform`. (often called `train` vs `predict`). The models learns from data in the `fit` and it applies what it has learned in `transform`.
"]

["Due to this, the idea of the `metamorph` pipeline concept was born."]
["It addresses all three shortcomings of the simpler pipeline."]

["Metamorph is documented here: [metamorph](https://github.com/scicloj/metamorph)"]


["As we see in the metamorph documentation, a pipeline can be composed of functions, which adhere to some simple standards
regarding input and output, as explained here: https://github.com/scicloj/metamorph#compliant-operations"]

["Tablecloth contains such operations in the `tablecloth.pipeline`
namespace. All functions of the `tablecloth.api` namespace are replicated
there, but metamorph compliant"]

["## scicloj.ml"]

["The Clojure ML ecosystem is based on different libraries working
together, as typic and idiomatic in Clojure"]

["Some existing libraries are used internally in scicloj.ml, to create a
complete machine learning library, but this is hidden from the user,
and is listed here only for completeness."]

["
1. `tablecloth` - for general manipulation of the dataset
1. `tech.v3.dataset` - to finally prepare a dataset for the machine learing models
1. `metamorph.ml` - for running pipelines and machine learning core functions
1. `Smile`  Java data science library containing lots of models
"
]


["These libraries can be used standalone as well. `tech.ml` was changed  in order
to work with scicloj.ml in a incompatible way.
So it is re-released under a new name `metamorph.ml`.
The others can be used by scicloj.ml without any change.
 "]


["In order to give easier access to the various libraries, the scicloj.ml
 library was created. It unifies the access to the libraries above
in three simple namespaces.
"]

["## Machine learning using scicloj.ml"]

["The setup for the following code needs a single dependencies in deps.edn or project.clj"]

["
{:deps {
        scicloj/scicloj.ml {:mvn/version \"0.1.0-beta2\"}} }
"]


["This library acts as a facade to the four libraries above, and arranges the functions in a simple way in these namespaces:"]

^kind/md-nocode
["

| namespace             | purpose                                                  |
|-----------------------|----------------------------------------------------------|
| scicloj.ml.core       | core functionality for machine learning                  |
| scicloj.ml.dataset    | functions to manipulate a dataset                        |
| scicloj.ml.methamorph | metamorph compliant functions to be used in ml pipelines |

 "]



["To start we need to require a few namespaces"]

(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset :refer [dataset add-column] :as ds]
         )


["First we load the data."]
(def titanic-train
  (->
   (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/titanic/train.csv"
               {:key-fn keyword
                :parser-fn :string
                })))

(def titanic-test
  (->
   (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/titanic/test.csv"
               {:key-fn keyword
                :parser-fn :string})
   (ds/add-column :Survived [""])))

["Then we define the pipeline and it steps. Inside the pipeline we only use functions
from namespace scicloj.ml.metamorph"]

["In scicloj.ml the model functions receives a single dataset,
in which the inference target column is marked as such. The model
to use is a parameter of the `model` function. All built-in
models are listed here: https://scicloj.github.io/scicloj.ml/userguide-models.html"
 ]

["In the titanic dataset the `survived` column is a categorical variable.
All target variables for classification need to be transformed first
into numbers, the model can work with. This is done by the function
`categorical->number`. The mapping for this is stored in the dataset on the column
and can be later retrieved to transform the numeric prediction back to its
categorical form."
 ]

["In `scicloj.ml` we pass a whole dataset to a model, and we need to mark
the inference target via function `set-inference-target`.
All other columns are used then as feature columns.
To restric the feature column, I simply remove most of them and keep only one, :Pclass"]

["Now the dataset is ready for the model, which is called in the last step.
It is a logistic regression model, which gets trained to predict column
:Survived from column :Pclass"]

(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:Survived :Pclass ])
   (mm/categorical->number [:Survived :Pclass])
   (mm/set-inference-target :Survived)
   (mm/model {:model-type :smile.classification/logistic-regression})))

["So the `ml/pipeline` function returns a function, which can be called with the ctx map."]

["We execute the pipeline in mode :fit,
which will execute all pipeline steps and train as well the model. "]

(def trained-ctx
  (pipe-fn {:metamorph/data titanic-train
            :metamorph/mode :fit}))

["Now we have a trained model inside trained-ctx. This is a usual map, so can be inspected in the repl.
 As the model is based on Smile, the trained-ctx contains the java class representing the trained model.
"]

["Now we execute the pipeline in mode :transform,
which will make a prediction "]

["We combine the previously obtained context
 (which contains the trained model)",
 "with the test data and mode :transform"]

(def test-ctx
  (pipe-fn
   (assoc trained-ctx
          :metamorph/data titanic-test
          :metamorph/mode :transform)))



["Prediction is now part of the ctx obtained.
The internally called `predict` function of `metamorph.ml` returns always the raw prediction of the model,
which we can easily transform into the original categories.
"]



;; ^kind/dataset
(-> test-ctx :metamorph/data
    (ds/column-values->categorical :Survived)
    )


["This shows the predicted survival. "]

["The documentation of `mm/model` here https://scicloj.github.io/scicloj.ml/scicloj.ml.metamorph.html#var-model"
 "documents this special behavior of the function, which does somthing different in mode :fit vs mode :transform" ]

["Any form of feature-engineering takes now the same form.
We will successively
add more and more steps into the pipeline to improve the model."]

["This can be build-in functions or custom functions as we see later"]
