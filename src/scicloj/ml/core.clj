(ns scicloj.ml.core
  "Core functions for machine learninig and pipeline execution.

  Functions are re-exported from:

  * scicloj.metamorph.ml.*
  * scicloj.metamorph.core

  "
  (:refer-clojure :exclude [boolean concat drop first group-by last rand-nth shuffle])
  (:require [tech.v3.datatype.export-symbols :as exporter]
            [scicloj.ml.utils :refer :all]
            [scicloj.metamorph.core]
            [scicloj.metamorph.ml]
            [scicloj.metamorph.ml.loss]
            [scicloj.metamorph.ml.gridsearch]
            [scicloj.metamorph.ml.classification]
            ;; register models
            [scicloj.ml.smile.classification]
            [scicloj.ml.smile.regression]
            [scicloj.ml.xgboost]
            ))


(export-all [scicloj.metamorph.ml
             scicloj.metamorph.ml.loss
             scicloj.metamorph.ml.classification
             scicloj.metamorph.ml.gridsearch
             ]
            [model predict train safe-inc])


(exporter/export-symbols scicloj.metamorph.core
                         pipeline
                         ->pipeline
                         lift
                         do-ctx
                         ;; def-ctx  ; cannot be exported, is tehreofre copied below
                         pipe-it
                         )

(defmacro def-ctx
  "Convenience macro for defining pipelined operations that
   bind the current value of the context to a var, for simple
   debugging purposes."
  [varname]
  `(do-ctx (fn [ctx#] (def ~varname ctx#))))
