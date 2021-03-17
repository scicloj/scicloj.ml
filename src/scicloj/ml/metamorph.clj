(ns scicloj.ml.metamorph
  "This ns contains functions, which operate on a metamorph context.
  They all return the context as well.

 So all functions in this ns are metamorph compliant and can be placed in a
 metamorph pipeline. 

  The functions are re-exported from :
  
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
            [tech.v3.libs.smile.nlp]
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
