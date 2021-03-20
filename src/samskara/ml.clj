(ns samskara.ml
  "Core functions for machine learninig.

  Functions are re-exported from:

  * scicloj.metamorph.ml
  * scicloj.metamorph.core

  "
  (:refer-clojure :exclude [boolean concat drop first group-by last rand-nth shuffle])
  (:require [tech.v3.datatype.export-symbols :as exporter]
            [samskara.utils :refer :all]
            [scicloj.metamorph.core]
            [scicloj.metamorph.ml]
            [scicloj.metamorph.ml.loss]
            [scicloj.metamorph.ml.gridsearch]
            [scicloj.metamorph.ml.classification]

            ))


;; register models
(require '[tech.v3.libs.smile.classification]
         '[tech.v3.libs.smile.regression]
         '[tech.v3.libs.xgboost]
         )

;; (scicloj.metamorph.ml/)
(export-all [scicloj.metamorph.ml
             scicloj.metamorph.ml.loss
             scicloj.metamorph.ml.classification
             scicloj.metamorph.ml.gridsearch
             ]
            [model])


(exporter/export-symbols scicloj.metamorph.core
                         pipeline
                         ->pipeline
                         lift)
