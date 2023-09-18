(ns scicloj.ml.generate-api
  (:require scicloj.ml.template.metamorph
            scicloj.ml.template.core
            scicloj.ml.template.dataset
            [tech.v3.datatype.export-symbols :as exporter]))

(def excluded
  '[])
(defn generate-api! [_]

  (exporter/write-api! 'scicloj.ml.template.metamorph
                       'scicloj.ml.metamorph
                       "src/scicloj/ml/metamorph.clj"
                       excluded)
  (exporter/write-api! 'scicloj.ml.template.dataset
                       'scicloj.ml.dataset
                       "src/scicloj/ml/dataset.clj"
                       excluded)
  (exporter/write-api! 'scicloj.ml.template.core
                       'scicloj.ml.core
                       "src/scicloj/ml/core.clj"
                       excluded))
