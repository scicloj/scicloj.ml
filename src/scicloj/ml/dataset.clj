(ns scicloj.ml.dataset


  "This namespace contains functions which operate on a dataset and mostly return a dataset.

  Functions are re-exported from:

  * tabecloth.api
  * tech.v3.dataset.modelling
  * tech.v3.dataset.column-filters
"
  (:refer-clojure :exclude [boolean concat drop first group-by last rand-nth shuffle])
  (:require [tech.v3.datatype.export-symbols :as exporter]
            [scicloj.ml.utils :refer :all]
            [tech.v3.dataset.modelling]
            [tech.v3.dataset.column-filters]
            [tablecloth.api]
            )
  )

(export-all [tech.v3.dataset.modelling
             tech.v3.dataset.column-filters
             tablecloth.api]
            [let-dataset without-grouping->])


(exporter/export-symbols tech.v3.dataset
                         categorical->number
                         categorical->one-hot
                         )
