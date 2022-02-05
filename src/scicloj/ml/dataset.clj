(ns scicloj.ml.dataset
  "
  This namespace contains functions which operate on a dataset
  and mostly return a dataset.

  The namespaces scicloj.ml.metamorph and scicloj.ml.dataset contain
  functions with the same name. But they operate on either a context
  map (ns metamorph) or on a dataset (ns dataset)

  The functions in tis namespace are re-exported from:

  * tabecloth.api - docs at https://scicloj.github.io/tablecloth/
  * tech.v3.dataset.modelling
  * tech.v3.dataset.column-filters
"
  (:refer-clojure :exclude [boolean concat drop first group-by last rand-nth shuffle])
  (:require [tech.v3.datatype.export-symbols :as exporter]
            [scicloj.ml.utils :refer :all]
            [tech.v3.dataset.modelling]
            [tech.v3.dataset.column-filters]
            [tech.v3.dataset.categorical]

            [tablecloth.api]
            )
  )

(export-all [tech.v3.dataset.modelling
             tech.v3.dataset.column-filters
             tech.v3.dataset.categorical
             tablecloth.api]
            [let-dataset without-grouping->])


(exporter/export-symbols tech.v3.dataset
                         categorical->number
                         categorical->one-hot
                         )
