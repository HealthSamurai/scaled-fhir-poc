(ns fhir.util
  (:require [cheshire.core]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [fhir.util :as u]))

(def patient-id-location
  {"Encounter" [:subject :id]})

(defn require-patient-id? [{resourceType :resourceType}]
  (contains? patient-id-location (str/capitalize resourceType)))

(defn get-patient-id [resource]
  (when (require-patient-id? resource)
    (get-in resource (get patient-id-location (:resourceType resource)))))


(comment

  (println (join-refernce-to-patient {:resourceType "Encounter"}))

  )

(defn select-to-bundle [items]
  {:resourceType "Bundle"
   :type "searchset"
   :entry  (map (fn [row] (assoc (:resource row)
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
