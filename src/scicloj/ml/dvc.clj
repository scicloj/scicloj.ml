(ns scicloj.ml.dvc
  (:require
   [clj-yaml.core :as yaml]
   [clojure.java.shell :as sh]
   [clojure.pprint :as pp]))


(defn queue-exp
 "Queue a number of experiments with dvc.
  The provided `params` (list of maps) is converted to yaml and
  written to `params.yaml` and a new job is queued using it.

  The list of params can for example be created from [[scicloj.metamorph.ml.gridsearch/sobol-gridsearch]]
  "
  [params]
  (run!
   #(do (spit "params.yaml" (yaml/generate-string %))
        (pp/pprint
         (sh/sh "dvc" "exp" "run" "--queue")))
   params))


(comment
  (queue-exp [{:a 1 :b 2}]))
