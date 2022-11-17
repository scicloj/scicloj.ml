(ns scicloj.ml.generate-api
  (:require scicloj.ml.metamorph
            scicloj.ml.core
            scicloj.ml.dataset
            [tech.v3.datatype.export-symbols :as exporter]))

(def generate-api!

  (exporter/write-api! 'scicloj.ml.metamorph
                       'scicloj.ml.core.api.metamorph
                       "src/scicloj/ml/metamorph.clj"
                       [])
  (exporter/write-api! 'scicloj.ml.dataset
                       'scicloj.ml.core.api.dataset
                       "src/scicloj/ml/core/dataset.clj"
                       [])
  (exporter/write-api! 'scicloj.ml.core
                       'scicloj.ml.core.api.core
                       "src/scicloj/ml/core/api/core.clj"
                       []))
