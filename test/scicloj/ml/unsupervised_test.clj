(ns scicloj.ml.unsupervised-test
  (:require  [clojure.test :refer [deftest is] :as t]
             [tablecloth.api :as tc]
             [tablecloth.pipeline :as tc-mm]
             [scicloj.metamorph.ml :as ml]
             [scicloj.ml.smile.clustering :as clustering]
             [fastmath.clustering :as cluster]
             [scicloj.metamorph.core :as mm]))



(ml/define-model! :test-unsupervised (fn [feature-ds label-ds options] {})  nil {:unsupervised? true})
(ml/define-model! :test-supervised (fn [feature-ds label-ds options] {})  nil {})

(deftest non-unsupervised
  (is (thrown-with-msg? Exception #"No target columns.*"
               (mm/fit (tc/dataset {:a [0]})
                       (ml/model {:model-type :test-supervised})))))

(deftest unsupervised
  (is (= {}
       (get-in
        (mm/fit (tc/dataset {:a [0]})
                {:metamorph/id :model}(ml/model {:model-type :test-unsupervised}))
        [:model :model-data]))))






(def  iris
  (tc/dataset
   "https://raw.githubusercontent.com/scicloj/metamorph.ml/main/test/data/iris.csv"
   {:key-fn keyword}))

(defn make-pipe-fn [n]
  (mm/pipeline
   (fn [ctx] (assoc ctx :n n))
   (tc-mm/select-columns :type/float)
   {:metamorph/id :model}
   (ml/model {:model-type :fastmath/cluster
              :clustering-method :k-means
              :clustering-method-args [n]})))

(deftest distortion
  (let [eval-result
        (ml/evaluate-pipelines
         (map make-pipe-fn (range 2 10))
         [{:train iris :test (tc/dataset)}]
         (fn [ctx]
           (-> ctx :model :model-data :info :distortion))
         :loss
         {:return-best-crossvalidation-only false
          :return-best-pipeline-only false})

        ellbow-data
        (->>
         (zipmap
          (->> eval-result flatten (map #(get-in % [:fit-ctx :n])))
          (->> eval-result flatten (map #(get-in % [:train-transform :metric]))))
         (sort-by :n)
         reverse)]

    (is  (every? true? (map pos?  (vals ellbow-data))))))



(deftest test-metricfn-collector-fn
    (let [
          iris
          (tc/dataset
           "https://raw.githubusercontent.com/scicloj/metamorph.ml/main/test/data/iris.csv"
           {:key-fn keyword})

          make-pipe (fn [n]
                      (mm/pipeline
                       (tc-mm/select-columns :type/float)
                       {:metamorph/id :model}
                       (ml/model {:model-type :fastmath/cluster
                                  :clustering-method :k-means
                                  :clustering-method-args [n]})))

          distortions (atom [])

          _
          (ml/evaluate-pipelines
           (map make-pipe (range 2 10))
           [{:train iris :test (tc/dataset)}]
           (fn [ctx]
             (let [
                   n  (-> ctx :model :options :clustering-method-args first)
                   distortion (-> ctx :model :model-data :info :distortion)]
               (swap! distortions conj {:n n :distortion distortion})
               0))
           :loss)]
      (is (= 2 (-> @distortions first :n)))
      (is (= 8 (-> @distortions count)))))
