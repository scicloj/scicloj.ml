(ns scicloj.ml.titanic
  (:require
   [notespace.api :as note]
   [notespace.kinds :as kind ]))

(comment
  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html "docs/userguide-titanic.html")

  (note/init)
  )


 (require '[scicloj.ml.dataset :as ds]
          '[tech.v3.dataset.math :as ds-math]
          '[tech.v3.datatype.functional :as dfn]
          '[scicloj.ml.core :as ml]
          '[scicloj.ml.metamorph :as mm]
          '[camel-snake-kebab.core :as csk]
          '[scicloj.metamorph.ml.loss :as loss]
          '[clojure.string :as str]
          '[fastmath.stats :as stats]
          '[fastmath.random :as rnd]
          '[fitdistr.core :as fit]
          )


["## Introduction "]

[" In this example, we will train a model which is able to predict the survival of passengers from the Titanic dataset."
 "In a real analysis, this would contain as well explorative analysis of the data, which I will skip here,
as the purpose is to showcase machine learning with scicloj.ml, which is about model evaluation and selection."
 ]


["### Read data"]

(def data (ds/dataset "data/titanic/train.csv" {:key-fn csk/->kebab-case-keyword}))



["Column names:"]
(ds/column-names data)


["We can explore teh association between the categorical columns of the dataset
with the :survived using cramers-v-corrected:"]
(def categorical-feature-columns [:pclass :sex :age :parch
                                    :embarked])
(map
 #(hash-map
   %
   (stats/cramers-v-corrected
    (get  data %)
    (:survived data)
    ))
 categorical-feature-columns
 )


["Association between the select variables:"]
(for [c1 categorical-feature-columns c2 categorical-feature-columns]
  {[c1 c2]
   (stats/cramers-v-corrected (get data c1) (get data  c2))}
  )




["In this dataset, :sex seems to be the best predictor for survival."]

["### Analyse distribution of :fare"]
(defn find-best
  [seq method ds]
  (let [selector (if (= method :mle) last first)]
    (dissoc (->> (map #(fit/fit method % seq {:stats #{:mle :ad :ks :cvm}}) ds)
                 (sort-by (comp method :stats))
                 (selector))
            :distribution)))




(def hist-fare
  (->>
   (stats/histogram (:fare data))
   :bins
   (map
    #(hash-map :a (Math/round (first %)) :b (second %))
    )

   ))

^kind/vega
{:description "A simple bar chart with embedded data."
 :data {:values hist-fare}
 ;; {:values [{:a "A" :b 28} {:a "B" :b 55} {:a "C" :b 43}
 ;;           {:a "D" :b 91} {:a "E" :b 81} {:a "F" :b 53}
 ;;           {:a "G" :b 19} {:a "H" :b 87} {:a "I" :b 52}]}
 :mark        :bar
 :encoding    {:x {:field :a :type :nominal :axis {:labelAngle 0}
                   }
               :y {:field :b :type :quantitative}}}


(def fare-best-ds
  (find-best
   (map
    inc
    (seq (:fare data)))
   :mle [:normal :exponential :levy :pareto :chi-squared]))






(def simulated-fare
 (rnd/->seq (rnd/distribution :exponential {:mean 32}) 891)
  )

(def hist-simulated-fare
  (->>
   (stats/histogram simulated-fare)
   :bins
   (map
    #(hash-map :a (Math/round (first %)) :b (second %))
    )

   ))

^kind/vega
{:description "A simple bar chart with embedded data."
 :data {:values hist-simulated-fare}
 ;; {:values [{:a "A" :b 28} {:a "B" :b 55} {:a "C" :b 43}
 ;;           {:a "D" :b 91} {:a "E" :b 81} {:a "F" :b 53}
 ;;           {:a "G" :b 19} {:a "H" :b 87} {:a "I" :b 52}]}
 :mark        :bar
 :encoding    {:x {:field :a :type :nominal :axis {:labelAngle 0}
                   }
               :y {:field :b :type :quantitative}}}
;; (rnd/cdf (:fare data))

(defn perc-survived [data]
  (let [survived
        (-> data (ds/select-rows (comp #(= % 1) :survived)) ds/row-count)
        non-survived
        (-> data (ds/select-rows (comp #(= % 0) :survived)) ds/row-count)
        survived-perc (float (/ survived (+ survived non-survived)))

        ]
    survived-perc
    )
  )

(defn perc-survived-grouped [data col]
  (let [grouped
        (-> data
            (ds/group-by col {:result-type :as-map }))]
    (map
     (fn [[key ds]]
       {key (perc-survived ds)})
     grouped)))



(perc-survived-grouped data :pclass)

(perc-survived-grouped data :sex)





(defn categorize-cabin [data]
  (-> data
      (ds/add-or-replace-column
       :cabin
       (map
        #(if (empty? %)
           :unknown
           (keyword (subs
                     %
                     0 1)))
        (:cabin data)
        ))))

(defn categorize-age [data]
  (->
   data
   (ds/add-or-replace-column
    :age-group
    (map
     #(cond
        (< % 10) :child
        (< % 18) :teen
        (< % 60) :adult
        (> % 60) :elderly
        true :other)
     (:age data)))))



(defn name->title [dataset]
  (-> dataset
      (ds/add-or-replace-column
       :title
       (map
        #(-> % (str/split  #"\.")
             first
             (str/split  #"\,")
             last
             str/trim)
        (data :name)))
      (ds/drop-columns :name)))

(def title-map
  {"Major" :sir
   "Col" :sir
   "Rev" :sir
   "Ms" :miss
   "Miss" :miss
   "Jonkheer" :sir
   "Don" :sir
   "Mlle" :miss
   "Mr" :mr
   "Master" :sir
   "Capt" :sir
   "Mrs" :mrs
   "Lady" :mme
   "Sir" :sir
   "Dr" :sir
   "the Countess" :mme
   "Mme" :mme})

(defn categorize-title [data]
(->
   data
   (ds/add-or-replace-column
    :title
    (map title-map (:title data)))))

(def pipeline-fn
  (ml/pipeline
   (mm/replace-missing :embarked :value "S")
   (mm/replace-missing :age :value  tech.v3.datatype.functional/mean)
   (ml/lift categorize-age)
   (ml/lift name->title)
   (ml/lift categorize-title)
   (ml/lift categorize-cabin)
   (mm/select-columns [:survived
                       :pclass
                       :age-group
                       :sex
                       :embarked
                       :title
                       :cabin])
   (fn [ctx]
     (assoc ctx :categorical-ds
            (:metamorph/data ctx)
            ))
   (mm/categorical->number [:survived :pclass :sex :embarked
                            :title :age-group :cabin] {} :int64)

   (mm/set-inference-target :survived)))


(def pipe-ops [
  [mm/replace-missing :embarked :value "S"]
  [mm/replace-missing :age :value tech.v3.datatype.functional/mean]
  [ml/lift categorize-age]
  [ml/lift name->title]
  [ml/lift categorize-title]
  [ml/lift categorize-cabin]
  [mm/select-columns [:survived
                      :pclass
                      :age-group
                      :sex
                      :embarked
                      :title
                      :cabin]]
  ])

(defn pprint-pipe-ops [pipe-ops]
  (run!
   (fn [ops]
     (run!
      (fn [p]
        (print (if  (fn? p)
                 (second (str/split (.getName (class p)) #"\$" ))
                 p)
               " "))
      ops)
     (println))
   pipe-ops))

(str/split "a$b" #"\$")
(pprint-pipe-ops pipe-ops)

(println
 (class
  (first (first pipe-ops))))

(def declartive-pipeline
  (ml/->pipeline
   pipe-ops
   ))

(def ctx
  (declartive-pipeline
   {:metamorph/data data}
   ))




(def ctx
  (pipeline-fn

   {:metamorph/data data}
   ))

^kind/dataset-grid
(:metamorph/data ctx)

[""]

(perc-survived-grouped (:categorical-ds ctx) :sex)
(perc-survived-grouped (:categorical-ds ctx) :pclass)
(perc-survived-grouped (:categorical-ds ctx) :title)
(perc-survived-grouped (:categorical-ds ctx) :age-group)
(perc-survived-grouped (:categorical-ds ctx) :cabin)


(map

 #(let [ds (:categorical-ds ctx)]
    (hash-map
     %
     (stats/cramers-v-corrected
      (get  ds %)
      (:survived ds)
      )))
 [:sex :pclass :title :age-group :cabin]
 )

(->>
 (for [v1 [:sex :pclass :title :age-group :cabin :survived]
       v2 [:sex :pclass :title :age-group :cabin :survived]
       ]

   (let [ds (:categorical-ds ctx)]
     (vector
      [v1 v2]
      (stats/cramers-v-corrected
       (get  ds v1)
       (get ds v2)
       )))


   )
 (sort-by second)
 reverse
 )



(let [ds (:categorical-ds ctx)]
  (stats/cramers-v-corrected
   (:sex ds )
   (:title ds)
   ))

["The following splits the dataset in three pieces,
 train, val and test to predict on later.
"]


(def ds-split (first (ds/split->seq data :holdout {:ratio [0.8 0.2]
                                                   :split-names [:train-val :test]}
                                    )))

["Create a sequence of train/test  (k-fold with k=10) splits used to evaluate the pipeline."]
(def train-val-splits
    (ds/split->seq
     (:train-val ds-split)
     :kfold
     {:k 10}))



["### Use Pclass, Sex, title, age for prediction"]

["We want to create a new column :title which might help in the score.
This is an example of custom function, which creates a new column from existing columns,
which is a typical case of feature engineering."]




["The pipeline definition"]

(def full-pipeline-fn
  (ml/pipeline
   pipeline-fn

   ;; we overwrite the id, so the model function will store
   ;; it's output (the model) in the pipeline ctx under key :model
   {:metamorph/id :model}
   (mm/model {:model-type :smile.classification/random-forest})))





["Evaluate the (single) pipeline function using the train/test split"]
(def evaluations
  (ml/evaluate-pipelines
   [full-pipeline-fn]
   train-val-splits
   ml/classification-accuracy
   :accuracy))


["The default k-fold splits makes 10 folds,
so we train 10 models, each having its own loss."]

["The `evaluate-pipelines` fn averages the models per pipe-fn,
and returns the best.
So we get a single model back, as we only have one pipe fn"]

["Often we consider the model with the lowest loss to be the best."]

["Return a single model only (as a list of 1) , namely the best over all
 pipeline functions
and all cross validations is the default behavoiur, but can be changed
with the `tune options`."]

["They controll as well which information is returned."]

["`tech.ml` stores the models in the context in a serialzed form,
and the function `thaw-model` can be used to get the original model back.
This is a Java class in the case of
 model :smile.classification/random.forest, but this depends on the
which `model` function is in the pipeline"]

["We can get for example,  the models like this:"]

(def models
  (->> evaluations
       flatten
       (map
        #(hash-map :model (ml/thaw-model (get-in % [:fit-ctx :model]))
                   :metric (:metric %)
                   :fit-ctx (:fit-ctx %)
                   ))
       (sort-by :mean)
       reverse))


["The accuracy of the best trained model is:"]
(-> models first :metric )

["The one with the highest accuracy is then:"]
(-> models first :model )


["We can get the predictions on new-data, which for classification contain as well
the posterior probabilities per class."]

["We do this by running the pipeline again, this time with new data and merging
:mode transform"]

(def predictions
  (->
   (full-pipeline-fn
    (assoc
     (:fit-ctx (first models))
     :metamorph/data (:test ds-split)
     :metamorph/mode :transform))
   :metamorph/data))

^kind/dataset
predictions

;; ["We have a helper function, which allows to predict using
;;  the best model from the result to `evaluate-pipelines`,
;; as this is a very common case."]


;; (def predictions
;;   (->
;;    (eval-mm/predict-on-best-model
;;     evaluations
;;     new-data
;;     :accuracy)))

["Out of the predictions and the truth, we can construct the
 confusion matrix."]

(def trueth
  (->
   (full-pipeline-fn {:metamorph/data (:test ds-split) :metamorph/mode :fit })
   :metamorph/data
   tech.v3.dataset.modelling/labels))

^kind/dataset
(->
 (ml/confusion-map (:survived predictions)
                        (:survived trueth)
                        :none)
 (ml/confusion-map->ds))

["### Hyper parameter tuning"]

["This defines a pipeline with options. The options gets passed to the model function,
so become hyper-parameters of the model.

The `use-age?` options is used to make a conditional pipeline. As the use-age? variable becomes part of the grid to search in,
we tune it as well.
This is an example how pipeline-options can be grid searched in the same way then hyper-parameters of the model.

"]
(defn make-pipeline-fn [options]

  (ml/pipeline
   pipeline-fn
   {:metamorph/id :model}
   (mm/model
    (merge options
           {:model-type :smile.classification/random-forest}))))

["Use sobol optimization, to find 10 grid points,
which cover in a smart way the hyper-parameter space."]

(def search-grid
  (->>
   (ml/sobol-gridsearch {:trees (ml/linear 100 500 10)
                           :split-rule (ml/categorical [:gini :entropy])
                           :max-depth (ml/linear 1 50 10 )
                           :node-size (ml/linear 1 10 10)
                         :sample-rate (ml/linear 0.1 1 10)})
   (take 100))
  )

["Generate the 10 pipeline-fn we want to evaluate."]
(def pipeline-fns (map make-pipeline-fn search-grid))


["Evaluate all 10 pipelines and keep results"]
(def evaluations
  (ml/evaluate-pipelines
   pipeline-fns
   train-val-splits
   ml/classification-accuracy
   :accuracy
   {:return-best-pipeline-only false
    :return-best-crossvalidation-only false

    :map-fn :pmap
    :result-dissoc-seq []
    }
   ))

["Get the key information from the evaluations and sort by the metric function used,
 accuracy here."]

(def models
  (->> evaluations
       flatten
       (map
        #(assoc
          (select-keys % [:mean :metric :fit-ctx])
          :model (ml/thaw-model (get-in % [:fit-ctx :model]))))
       (sort-by :metric)
       reverse))




["As we did 10 pipelines and 10 fold cross validation, we have 100 models ttrained in total "]
(count models)

["As we sorted by mean accuracy, the first evaluation result is the best model,"]
(def best-model (first models))

["which is: "]
(:model best-model)

["with a mean accuracy of "  (:mean best-model)]

["and a accuracy of "  (:metric best-model)]



["using options: "]
(-> best-model :fit-ctx :model :options)
