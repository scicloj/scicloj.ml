(ns samskara.metamorph
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



  The namespaces samskara.metamorph and samskara.ml.dataset contain
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
            [samskara.utils :refer :all]
            [tablecloth.pipeline]
            [tech.v3.dataset.metamorph]
            [tech.v3.libs.smile.nlp]
            [scicloj.metamorph.ml]
            )
  )

(exporter/export-symbols tech.v3.libs.smile.metamorph
                         count-vectorize
                         bow->sparse-array
                         bow->SparseArray
                         bow->something-sparse
                         bow->tfidf
                         )

(exporter/export-symbols scicloj.metamorph.ml
                         model
                         )

(-> 'tech.v3.libs.smile.nlp ns-publics keys sort)
(export-all [tech.v3.dataset.metamorph
             tablecloth.pipeline

             ]
            [build-pipelined-function
             k-fold-datasets
             train-test-split
             process-all-api-symbols
             ->pipeline
             pipeline
             split
             split->seq
             ])


;; (ns-publics  'scicloj.ml.mm)
