(ns fhir.util
  (:require [cheshire.core]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def patient-id-location
  {"Encounter" [:subject :id]})

(defn get-patient-id [resource]
  (when (require-patient-id? resource)
    (get-in resource (get patient-id-location (:resourceType resource)))))

(defn require-patient-id? [{resourceType :resourceType}]
  (contains? patient-id-location (str/capitalize resourceType)))

(defn join-refernce-to-patient [{resourceType :resourceType :as resource}]
  (if (require-patient-id? resource)
    (json-patient-id (get patient-id-location resourceType))))


(defn json-patient-id [[elem & path]]
  (cond
    (nil? elem)
    "patient_id"

    (nil? path)
    (str "jsonb_build_object('"
         (name elem) "', "
         (json-patient-id path)
         ", 'resourceType', 'Patient')")

    :else
    (str "jsonb_build_object('"
         (name elem)
         "', "
         (json-patient-id path)
         ")")))

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
