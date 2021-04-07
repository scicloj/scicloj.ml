[![Clojars Project](https://img.shields.io/clojars/v/scicloj/scicloj.ml.svg)](https://clojars.org/scicloj/scicloj.ml)

# scicloj.ml

Proposal for a Clojure machine learning library.

***Everything here is **beta** status. Breaking changes are possible, but unlikely***

Nevertheless,  it will likely not diverge a lot from the reference projects below, so it is worth to have a look at them
for machine learning in Clojure.


## Quickstart

Dependencies: 

``` clojure
{:deps
 {scicloj/scicloj.ml {:mvn/version "0.1.0-alpha1"}}}
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
   (ds/add-column :Survived [""])))
   
   
;; construct pipeline function including Logistic Regression model
(def pipe-fn
  (ml/pipeline
   (mm/select-columns [:Survived :Pclass ])
   (mm/add-column :Survived (fn [ds] (map #(case % "1" "yes" "0" "no" nil "") (:Survived ds))))
   (mm/categorical->number [:Survived :Pclass])
   (mm/set-inference-target :Survived)
   (mm/model {:model-type :smile.classification/logistic-regression})))
   
;;  execute pipeline with train data including model fit
(def trained-ctx
  (pipe-fn {:metamorph/data titanic-train
            :metamorph/mode :fit}))


;; execute pipeline with test data including prediction 
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


Docu is here:
* https://scicloj.github.io/scicloj.ml/userguide-intro.html
* https://scicloj.github.io/scicloj.ml/userguide-models.html
* https://scicloj.github.io/scicloj.ml/userguide-transformers.html
* https://scicloj.github.io/scicloj.ml/userguide-titanic.html
* https://scicloj.github.io/scicloj.ml/userguide-sklearnclj.html
* https://scicloj.github.io/scicloj.ml/userguide-third_party.html

which is based on notespace files in here:
https://github.com/scicloj/scicloj.ml/tree/main/userguide/scicloj/ml

To run them, the alias "test" in deps.edn need to be activated via `clj -Atest` or similar

API documentation:
https://scicloj.github.io/scicloj.ml


## Reference to projects scicloj.ml is using/based on:

* https://github.com/techascent/tech.ml
* https://github.com/scicloj/tablecloth
* https://github.com/scicloj/metamorph
* https://github.com/scicloj/metamorph.ml (branch https://github.com/scicloj/metamorph.ml/tree/mergeTechML)
* https://github.com/techascent/tech.ml.dataset
* https://github.com/haifengl/smile
