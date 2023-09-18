[![Clojars Project](https://img.shields.io/clojars/v/scicloj/scicloj.ml.svg)](https://clojars.org/scicloj/scicloj.ml/)[![cljdoc badge](https://cljdoc.org/badge/scicloj/scicloj.ml)](https://cljdoc.org/d/scicloj/scicloj.ml)
- v0.2.2: [![Gitpod ready-to-code v0.2.2](https://img.shields.io/badge/Gitpod-ready--to--code-908a85?logo=gitpod)](https://gitpod.io/#https://github.com/scicloj/scicloj.ml/tree/v0.2.2)
- latest snapshot: [![Gitpod ready-to-code latest-snapshot](https://img.shields.io/badge/Gitpod-ready--to--code-908a85?logo=gitpod)](https://gitpod.io/#https://github.com/scicloj/scicloj.ml)
- latest snapshot: [![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/scicloj/scicloj.ml/HEAD?filepath=docs%2Fquickstart.ipynb)

# scicloj.ml

A idiomatic Clojure machine learning library.

Main features:
- Harmonized and *idiomatic* use of various classification, regression and unsupervised models 
- Supports creation of machine learning pipelines *as-data*
- Includes easy-to-use, sophisticated *cross-validations* of pipelines
- Includes most important data transformation for data preprocessing
- Experiment tracking can be added by the user via a callback mechanism
- *Open architecture* to allow to plugin any potential ML model, even in non-JVM languages, including deep learning
- Based on well established Clojure/Java Data Science libraries
    - [*tech.ml.dataset*](https://github.com/techascent/tech.ml.dataset) for *very efficient* underlying data storage
    - [*Smile*](https://haifengl.github.io/) for ML *models*
    - [*metamorph.ml*](https://github.com/scicloj/metamorph.ml) as foundation of *higher level ML* functions
       (former: [*tech.ml*](https://github.com/techascent/tech.ml) )

## Quickstart

Dependencies: 

``` clojure
{:deps
 {scicloj/scicloj.ml {:mvn/version "0.2.0"}}}
```


Code:

```clojure
(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset :as ds])

;; read train and test datasets
(def titanic-train
  (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/titanic/train.csv" {:key-fn keyword :parser-fn :string}))

(def titanic-test
  (-> "https://github.com/scicloj/metamorph-examples/raw/main/data/titanic/test.csv"
      (ds/dataset {:key-fn keyword :parser-fn :string})
      (ds/add-column :Survived [""] :cycle)))

;; construct pipeline function including Logistic Regression model
(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:Survived :Pclass ])
   (mm/add-column :Survived (fn [ds] (map #(case % "1" "yes" "0" "no" nil "") (:Survived ds))))
   (mm/categorical->number [:Survived :Pclass])
   (mm/set-inference-target :Survived)
   {:metamorph/id :model}
   (mm/model {:model-type :smile.classification/logistic-regression})))

;;  execute pipeline with train data including model in mode :fit
(def trained-ctx
  (pipe-fn {:metamorph/data titanic-train
            :metamorph/mode :fit}))

;; execute pipeline in mode :transform with test data which will do a prediction 
(def test-ctx
  (pipe-fn
   (assoc trained-ctx
          :metamorph/data titanic-test
          :metamorph/mode :transform)))

;; extract prediction from pipeline function result
(-> test-ctx :metamorph/data
    (ds/column-values->categorical :Survived))
    
;; => #tech.v3.dataset.column<string>[418]
;;    :Survived
;;    [no, no, yes, no, no, no, no, yes, no, no, no, no, no, yes, no, yes, yes, no, no, no...]   
                
```

## Community
For support use Clojurians on Zulip:

[Scicloj.ml on Zulip](https://clojurians.zulipchat.com/#narrow/stream/283491-scicloj.2Eml-dev)

or on Clojurians Slack:

[Scicloj.ml on Slack](https://app.slack.com/client/T03RZGPFR/C02KKT03HV5/thread/CQT1NFF4L-1635769673.041400)


## Documentation


Full documentation is here as [userguides](https://github.com/scicloj/scicloj.ml-tutorials)

API documentation:
https://scicloj.github.io/scicloj.ml


## Reference to projects scicloj.ml is using/based on:

This library itself is a shim, not containing any functions.
The code is present in the following repositories, and the functions get re-exported in `scicloj.ml` in a 
small number of namespaces for user convenience.


* https://github.com/techascent/tech.ml
* https://github.com/scicloj/tablecloth
* https://github.com/scicloj/metamorph
* https://github.com/scicloj/metamorph.ml 
* https://github.com/techascent/tech.ml.dataset
* https://github.com/scicloj/scicloj.ml.smile
* https://github.com/scicloj/scicloj.ml.xgboost
* https://github.com/haifengl/smile


Scicloj.ml organises the existing code in 3 namespaces, as following:

### namespace scicloj.ml.core
Functions are re-exported from:

* scicloj.metamorph.ml.*
* scicloj.metamorph.core

### namespace scicloj.ml.dataset
All functions in this ns take a dataset as first argument.
The functions are re-exported from:

* tabecloth.api
* tech.v3.dataset.modelling
* tech.v3.dataset.column-filters

### namespace scicloj.ml.metamorph
All functions in this ns take a metamorph context as first argument,
so can directly be used in [metamorph](https://github.com/scicloj/metamorph) pipelines.
The functions are re-exported from:

* tablecloth.pipeline
* tech.v3.libs.smile.metamorph
* scicloj.metamorph.ml
* tech.v3.dataset.metamorph


In case you are already familar with any of the original namespaces, they can of course be used directly as well:

```clojure
(require '[tablecloth.api :as tc])
(tc/add-column ...)
```
# Plugins

scicloj.ml can be easely extended by plugins, which contribute models or other algorithms.
By now the following plugins exist:

* Builtin: [scicloj.ml.smile](https://github.com/scicloj/scicloj.ml.smile)
* Builtin: [scicloj.ml.xgboost](https://github.com/scicloj/scicloj.ml.xgboost)
* All [sklearn](https://scikit-learn.org/stable/index.html) models: [sklearn.clj](https://github.com/scicloj/sklearn-clj)
* [top2vec](https://github.com/ddangelov/Top2Vec) model: [scicloj.ml.top2vec](https://github.com/scicloj/scicloj.ml.top2vec)
* [crf](https://github.com/scicloj/scicloj.ml.crf) A NER model from `standfortNLP`
* [clj-djl](https://github.com/scicloj/scicloj.ml.clj-djl) Use fasttext model from djl
