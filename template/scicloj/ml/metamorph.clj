(ns scicloj.ml.metamorph
  "This namespace contains functions, which operate on a metamorph context.
  They all return the context as well.

  So all functions in this namespace are metamorph compliant and can
  be placed in a metamorph pipeline.

  Most functions here are only manipulating the dataset, which is in the ctx map
  under the key :metamorph/data.
  And they behave the same in pipeline mode :fit and :transform.


  A few functions manipulate other keys inside the ctx map, and/or behave
  different in :fit and :transform.

  This is documented per function in this form:

  metamorph                            | .
  -------------------------------------|------------------------------
  Behaviour in mode :fit               | .
  Behaviour in mode :transform         | .
  Reads keys from ctx                  | .
  Writes keys to ctx                   | .



  The namespaces scicloj.ml.metamorph and scicloj.ml.dataset contain
  functions with the same name. But they operate on either a context
  map (ns metamorph) or on a dataset (ns dataset)

  The functions in this namesspaces are re-exported from :
  
  * tablecloth.pipeline
  * tech.v3.libs.smile.metamorph
  * scicloj.metamorph.ml
  * tech.v3.dataset.metamorph
  
 " 
  (:refer-clojure :exclude [boolean concat drop first group-by last rand-nth shuffle filter sort-by update take-nth])
  (:require [tech.v3.datatype.export-symbols :as exporter]
            [scicloj.ml.utils :refer :all]
            [tablecloth.pipeline]
            [tech.v3.dataset.metamorph]
            [scicloj.ml.smile.nlp]
            [scicloj.metamorph.ml]
            [scicloj.ml.smile.projections]
            [scicloj.metamorph.ml.preprocessing]
            [scicloj.metamorph.ml.categorical]))

            
  

(exporter/export-symbols scicloj.ml.smile.metamorph
                         count-vectorize
                         bow->sparse-array
                         bow->SparseArray
                         bow->something-sparse
                         bow->tfidf)
                         

(exporter/export-symbols scicloj.metamorph.ml
                         model)
                         

(exporter/export-symbols scicloj.metamorph.ml.preprocessing
                         std-scale
                         min-max-scale)
                         
(exporter/export-symbols scicloj.ml.smile.projections
                         reduce-dimensions)
                         
(exporter/export-symbols scicloj.ml.smile.clustering
                         cluster)

(exporter/export-symbols scicloj.metamorph.ml.categorical
                         transform-one-hot)

(export-all [tech.v3.dataset.metamorph
             tablecloth.pipeline]

             
            [build-pipelined-function
             k-fold-datasets
             train-test-split
             process-all-api-symbols
             ->pipeline
             pipeline
             split
             split->seq])

(comment
  (exporter/write-api! 'scicloj.ml.metamorph
                       'scicloj.ml.core.api.metamorph
                       "/tmp/metamorph.clj"
                       []))


(meta 'tablecloth.pipeline/append)
