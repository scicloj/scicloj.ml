[![Clojars Project](https://img.shields.io/clojars/v/scicloj/scicloj.ml.svg)](https://clojars.org/scicloj/scicloj.ml)
[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-908a85?logo=gitpod)](https://gitpod.io/#https://github.com/scicloj/scicloj.ml)
[![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/scicloj/scicloj.ml/HEAD?filepath=docs%2Fquickstart.ipynb)

# scicloj.ml

Proposal for a Clojure machine learning library.

***Everything here is **beta** status. Breaking changes are unlikely but possible***




## Quickstart

Dependencies: 

``` clojure
{:deps
 {scicloj/scicloj.ml {:mvn/version "0.1.0-beta4"}}}
```


Code:

```clojure
(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset :as ds])

;; read train and test datasets
(def titanic-train
  (->
   (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/titanic/train.csv"
               {:key-fn keyword
                :parser-fn :string})))
                
(def titanic-test
  (->
   (ds/dataset "https://github.com/scicloj/metamorph-examples/raw/main/data/titanic/test.csv"
               {:key-fn keyword
                :parser-fn :string})
   (ds/add-column :Survived [""] :cycle)))
   
   
;; construct pipeline function including Logistic Regression model
(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:Survived :Pclass ])
   (mm/add-column :Survived (fn [ds] (map #(case % "1" "yes" "0" "no" nil "") (:Survived ds))))
   (mm/categorical->number [:Survived :Pclass])
   (mm/set-inference-target :Survived)
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




## Documentation


Full documentation is here:
* https://scicloj.github.io/scicloj.ml-tutorials/userguide-intro.html
* https://scicloj.github.io/scicloj.ml-tutorials/userguide-advanced.html
* https://scicloj.github.io/scicloj.ml-tutorials/userguide-models.html
* https://scicloj.github.io/scicloj.ml-tutorials/userguide-transformers.html
* https://scicloj.github.io/scicloj.ml-tutorials/userguide-titanic.html
* https://scicloj.github.io/scicloj.ml-tutorials/userguide-sklearnclj.html
* https://scicloj.github.io/scicloj.ml-tutorials/userguide-third_party.html


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
Functions are re-exported from:

* tabecloth.api
* tech.v3.dataset.modelling
* tech.vhttp://scicloj.ml/3.dataset.column-filters

### namespace scicloj.ml.metamorph
Functions are re-exported from:

* tablecloth.pipeline
* tech.v3.libs.smile.metamorph
* scicloj.metamorph.ml
* tech.v3.dataset.metamorph


In case you are already familar with any of the original namespaces, they can of course be used directly as well:

```clojure
(require '[tablecloth.api :as tc])
(tc/add-column ...)
```
