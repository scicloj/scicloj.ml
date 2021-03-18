(ns scicloj.ml.userguide.transformers

  (:require
   [notespace.api :as note]
   [notespace.kinds :as kind]
   [scicloj.ml.metamorph :as mm]

   )

  )

(defn docu-fn [v]
  (let [m (meta v)]
    (kind/override
     [(str  "## " (:name m))
      (:doc m
            )]
     kind/md-nocode
     )))


(comment
  (note/init-with-browser)
  (note/eval-this-notespace)
  (note/reread-this-notespace)
  (note/render-static-html)
  (note/init)
  )

(docu-fn (meta (var mm/count-vectorize)))

(docu-fn (var mm/bow->SparseArray))

(docu-fn (var mm/bow->sparse-array))

(docu-fn (var mm/bow->tfidf))

(docu-fn (var mm/model))
