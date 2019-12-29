(ns fhir.util
  (:require [cheshire.core]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [fhir.util :as u]))

(def patient-id-location
  {"Encounter" [:subject]})

(defn require-patient-id? [{resourceType :resourceType}]
  (contains? patient-id-location (str/capitalize resourceType)))

(defn get-patient-id [resource]
  (when (require-patient-id? resource)
    (let [{resourceType :resourceType id :id} (get-in resource (get patient-id-location (:resourceType resource)))]
      (if (or (nil? resourceType) (= "Patient" resourceType))
        id
        resourceType))))


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
