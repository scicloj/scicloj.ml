(ns scicloj.ml.generate-api
  (:require scicloj.ml.metamorph
            scicloj.ml.core
            scicloj.ml.dataset
            [tech.v3.datatype.export-symbols :as exporter]))

(defn generate-api! []

  (exporter/write-api! 'scicloj.ml.metamorph
                       'scicloj.ml.metamorph
                       "src/scicloj/ml/metamorph.clj"
                       [])
  (exporter/write-api! 'scicloj.ml.dataset
                       'scicloj.ml.dataset
                       "src/scicloj/ml/dataset.clj"
                       [])
  (exporter/write-api! 'scicloj.ml.core
                       'scicloj.ml.core
                       "src/scicloj/ml/core.clj"
                       []))
