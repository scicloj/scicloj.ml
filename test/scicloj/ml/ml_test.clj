(ns scicloj.ml.ml-test
  (:require  [clojure.test :refer [deftest is]]
             [tech.v3.dataset.modelling :as ds-mod]
             [tablecloth.api :as tc]
             [scicloj.metamorph.ml.gridsearch :as gs]
             [scicloj.metamorph.core :as morph]
             [tech.v3.dataset.metamorph :as ds-mm]
             [tech.v3.dataset.column-filters :as cf]
             [scicloj.metamorph.ml :as ml]
             [scicloj.metamorph.ml.loss :as loss]
             [tech.v3.dataset :as ds]
             [scicloj.metamorph.ml.evaluation-handler :as eval]
             [scicloj.metamorph.ml.evaluation-handler :refer [get-source-information qualify-pipelines qualify-keywords]]
             [scicloj.ml.smile.classification])
  (:import (java.util UUID)))






;;
(def iris (tc/dataset "https://raw.githubusercontent.com/techascent/tech.ml/master/test/data/iris.csv" {:key-fn keyword}))

(deftest grid-search
  (let [
        ds (->
            iris
            (ds-mod/set-inference-target :species))


        grid-search-options
        {:trees (gs/categorical [10 50 100 500])
         :split-rule (gs/categorical [:gini :entropy])
         :model-type :smile.classification/random-forest}

        create-pipe-fn
        (fn[options]
          (morph/pipeline
           (ds-mm/categorical->number cf/categorical)
           {:metamorph/id :model}(ml/model options)))

        all-options-combinations (gs/sobol-gridsearch grid-search-options)

        pipe-fn-seq (map create-pipe-fn (take 7 all-options-combinations))

        train-test-seq (tc/split->seq ds :kfold {:k 10})

        evaluations
        (ml/evaluate-pipelines pipe-fn-seq train-test-seq loss/classification-loss :loss)

        new-ds (->
                (tc/shuffle ds  {:seed 1234})
                (tc/head 10))


        best-pipe-fn         (-> evaluations first first :pipe-fn)

        best-fitted-context  (-> evaluations first first :fit-ctx)

        predictions
        (->
         (best-pipe-fn
          (merge best-fitted-context
                 {:metamorph/data new-ds
                  :metamorph/mode :transform}))
         (:metamorph/data)
         (ds-mod/column-values->categorical :species))]



    (is (= ["versicolor"
            "versicolor"
            "virginica"
            "versicolor"
            "virginica"
            "setosa"
            "virginica"
            "virginica"
            "versicolor"
            "versicolor"]
           predictions))))


(deftest test-model
  (let [
        src-ds  iris  ;; (tc/dataset "test/data/iris.csv")
        ds (->  src-ds
                (ds/categorical->number cf/categorical)
                (ds-mod/set-inference-target :species)

                (tc/shuffle {:seed 1234}))
        feature-ds (cf/feature ds)
        split-data (first (tc/split->seq ds :holdout {:seed 1234}))
        train-ds (:train split-data)
        test-ds  (:test split-data)

        pipeline (fn  [ctx]
                   ((ml/model {:model-type :smile.classification/random-forest})
                    ctx))


        fitted
        (pipeline
         {:metamorph/id "1"
          :metamorph/mode :fit
          :metamorph/data train-ds})


        prediction
        (pipeline (merge fitted
                         {:metamorph/mode :transform
                          :metamorph/data test-ds}))

        predicted-species (ds-mod/column-values->categorical (:metamorph/data prediction)
                                                             :species)]

   (is (= [1.0 0.0 0.0 1.0 2.0]
          (take 5 (-> prediction (get "1") :scicloj.metamorph.ml/target-ds (get :species) seq))))
   (is (= ["setosa" "versicolor" "versicolor"]
          (take 3 predicted-species)))))
       

(defn fit-pipe-in-new-ns [file ds]
  (let [new-ns (create-ns (symbol (str (UUID/randomUUID))))
        _ (intern new-ns 'file file)
        _ (intern new-ns 'ds ds)
        _ (.addAlias new-ns 'morph (the-ns 'scicloj.metamorph.core))
        _ (.addAlias new-ns 'nippy (the-ns 'taoensso.nippy))
        species-freqs (binding [*ns* new-ns]  (do
                                                (eval '(def thawed-result (nippy/thaw-from-file file)))


                                                (eval '(def thawed-pipe-fn (clojure.core/->
                                                                            thawed-result
                                                                             :pipe-decl
                                                                            (morph/->pipeline))))
                                                (eval '(clojure.core/->
                                                        (morph/fit-pipe ds thawed-pipe-fn)
                                                        :metamorph/data
                                                        :species
                                                        (clojure.core/frequencies)))))]
    species-freqs))


(defn do-xxx [col] col)

(deftest round-trip-aliased-names
  (is (= {1.0 50, 0.0 50, 2.0 50}

         (let [

               base-pipe-declr
               (qualify-pipelines
                [
                 [[:ds-mm/set-inference-target [:species]]
                  [:ds-mm/categorical->number [:species]]
                  [:ds-mm/update-column :species ::do-xxx]
                  [:ds-mm/update-column :species :clojure.core/identity]
                  {:metamorph/id :model}[:ml/model {:model-type :smile.classification/random-forest}]]]
                (find-ns 'scicloj.ml.ml-test))


               files (atom [])
               nippy-handler (eval/example-nippy-handler files
                                                         "/tmp"
                                                         identity)



               eval-result (ml/evaluate-pipelines
                            base-pipe-declr
                            (tc/split->seq iris)
                            loss/classification-accuracy
                            :accuracy
                            {:map-fn :mapv
                             :evaluation-handler-fn nippy-handler})]

           (fit-pipe-in-new-ns (first @files) iris)))))
