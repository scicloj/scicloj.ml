(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b] ; for b/git-count-revs
            [org.corfield.build :as bb]))

(def lib 'scicloj/scicloj.ml)
; alternatively, use MAJOR.MINOR.COMMITS:
;;(def version (format "0.2.%s" (b/git-count-revs nil)))
(def version "0.2.2")

(defn test "Run the tests." [opts]
  (bb/run-tests opts))

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version :aliases [:test-runner]
             :src-dirs ["src" "template"])
      (bb/run-tests)
      (bb/clean)
      (bb/jar)))

(defn ci-no-test "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version :src-dirs ["src" "template"])
      (bb/clean)
      (bb/jar)))
(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version
             :src-dirs ["src" "template"])
      (bb/install)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version :src-dirs ["src" "template"])
      (bb/deploy)))
