(ns scicloj.ml.core
  "iCore functions for machine learninig.

  Functions are re-exported from:

  * scicloj.metamorph.ml
  * scicloj.metamorph.core

  "
  (:refer-clojure :exclude [boolean concat drop first group-by last rand-nth shuffle])
  (:require [tech.v3.datatype.export-symbols :as exporter]
            [scicloj.ml.utils :refer :all]
            [scicloj.metamorph.core]
            [scicloj.metamorph.ml]))


;; register models
(require '[tech.v3.libs.smile.classification]
         '[tech.v3.libs.smile.regression])

;; (scicloj.metamorph.ml/)
(export-all [scicloj.metamorph.ml]
            [model])


(exporter/export-symbols scicloj.metamorph.core
                         pipeline
                         ->pipeline
                         lift)
