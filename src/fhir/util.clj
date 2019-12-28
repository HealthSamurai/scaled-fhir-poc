(ns fhir.util
  (:require [cheshire.core]
            [clojure.java.io :as io]))

(defn select-to-bundle [items]
  {:resourceType "Bundle"
   :type "searchset"
   :entry  (map (fn [row] (assoc (cheshire.core/parse-string (:resource row) true)
                                 :id (:id row)
                                 :resourceType (:rt row)))
                items)})

(defn body->map [body]
  (cheshire.core/parse-stream (io/reader body) true))

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(defn enhance-with-id [resource]
  (if (nil? (:id resource))
    (assoc resource :id (uuid))
    resource))
