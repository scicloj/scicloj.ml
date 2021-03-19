(ns samskara.transformers
  (:require
   [notespace.api :as note]
   [notespace.kinds :as kind]
   [samskara.metamorph :as mm]
   )
  )

(comment

  (note/init-with-browser)
  (notespace.api/update-config
   #(assoc % :source-base-path "userguide"))
  (note/eval-this-notespace)
  (note/render-static-html "docs/userguide-transformers.html")
  )

(defn docu-fn [v]
  (let [m (meta v)]
    (kind/override
     [(str  "## " (:name m))
      (:doc m
            )]
     kind/md-nocode
     )))



(docu-fn (var mm/count-vectorize))

(docu-fn (var mm/bow->SparseArray))

(docu-fn (var mm/bow->sparse-array))

(docu-fn (var mm/bow->tfidf))

(docu-fn (var mm/model))
