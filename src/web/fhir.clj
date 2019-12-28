(ns web.fhir
  (:require [db.core]
            [clojure.string :as str]
            [clojure.java.io :as io]))


;; Basic

(defn select-to-bundle [items]
  {:resourceType "Bundle"
   :type "searchset"
   :entry  (map (fn [row] (assoc (cheshire.core/parse-string (:resource row))
                                 :id (:id row)
                                 :resourceType (:rt row)))
                items)})

(defn get-resources [ctx resourceType]
  (select-to-bundle
   (db.core/query (get-in ctx [:db :master])
                  (format "select *, '%s' rt from %s"
                          (str/capitalize resourceType) (str/lower-case resourceType)))))


(defn get-resource [db {id :id rt :resourceType}]
  (let [res (db.core/query-first db (format "select *, %s rt from %s where id = '%s'"
                                            (str/capitalize rt)
                                            (str/lower-case rt)
                                            id))]
    (println res)
    (assoc (cheshire.core/parse-string (:resource res))
           :id (:id res)
           :resourceType (str/capitalize rt))))

(defn conflict-if-exists [db {rt :resourceType id :id}]
  (if-not (empty? (db.core/query db (format "select id from %s where id = '%s'"
                                            (str/lower-case rt)
                                            id)))
    {:resourceType "OperationOutcome"
     :id "dublicate"
     :text {:status "generated" :div "Resource with same id already exists"}
     :issue [{:severity "fatal" :code "duplicate" :diagnostics "Resource with same id already exists"}]}))

(defn create-resource [{{db :master} :db :as ctx} resource]
  (if-let [conflict (conflict-if-exists db resource)]
    conflict
    (let [resourceType (:resourceType resource)]
      (db.core/exec! db (format "insert into %s (id, resource) values ('%s', $$%s$$)"
                                (str/lower-case resourceType)
                                (:id resource)
                                (cheshire.core/generate-string (dissoc resource :id :resourceType))))
      {:status 201
       :json (get-resource db resource)})))


(comment

  (get-resource db-ctx {:resourceType "Patient" :id "123"})

  (conflict-if-exists db-ctx {:resourceType "Patient" :id "qq"})

  (try
    (create-resource {:db {:master db-ctx}} {:id "qq" :resourceType "Patient" :name "wow"})
    (catch Exception e
      (println (:error e))))


  )

;; Patient

(defn get-patients [ctx]
  (println (get-in ctx [:db :master]))
  (def db-ctx (get-in ctx [:db :master]))
  {:status 200
   :json (get-resources ctx "Patient")})

(defn find-patient-by-name [ctx]
  )


(defn body->map [body]
  (cheshire.core/parse-stream (io/reader body)))

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(defn enhance-with-id [resource]
  (if (nil? (:id resource))
    (assoc resource :id (uuid))
    resource))

(defn create-patient [ctx]
  (let [resource (body->map (:body (:request ctx)))
        resource (assoc resource :resourceType "Patient")
        resource (enhance-with-id resource)]
    (create-resource ctx resource)))

(defn delete-patient [ctx]
  {:status 200
   :json {:delete :patient}})

(defn update-patient [ctx]
  {:status 200
   :json {:update :patient}})

(defn get-patient [ctx]
  (println (get-in ctx [:db :master]))
  (let [id (keyword (:id (:route-params ctx)))]
    {:status 200
     :json {:id id}}))


;; Observation

(defn get-observations [ctx]
  {:status 200
   :json (select-to-bundle {:observation :get})})

(defn get-observation [ctx]
  (let [id (keyword (:id (:route-params ctx)))]
    {:status 200
     :json {:id id}}))


;; Encounter

(defn get-encounters [ctx]
  {:status 200
   :json (select-to-bundle {:encounter :get})})

(defn get-encounter[ctx]
  (let [id (keyword (:id (:route-params ctx)))]
    {:status 200
     :json {:id id}}))


;; Practitioner

(defn get-practitioners [ctx]
  {:status 200
   :json (select-to-bundle {:practitioner :get})})

(defn get-practitioner [ctx]
  (let [id (keyword (:id (:route-params ctx)))]
    {:status 200
     :json {:id id}}))


;; Group

(defn get-groups [ctx]
  {:status 200
   :json (select-to-bundle {:group :get})})

(defn get-group [ctx]
  (let [id (keyword (:id (:route-params ctx)))]
    {:status 200
     :json {:id id}}))
