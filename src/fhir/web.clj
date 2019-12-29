(ns fhir.web
  (:require [db.core]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [fhir.util :as u]))


(defn get-resource-or-nil [db {id :id rt :resourceType}]
  (when-let [res (db.core/query-first db (format "select *, '%s' rt from %s where id = '%s'"
                                                 (str/capitalize rt)
                                                 (str/lower-case rt)
                                                 id))]
    (assoc (:resource res)
           :id (:id res)
           :resourceType (str/capitalize rt))))

(defn get-resource [resourceType {{db :master} :db :as ctx}]
  (let [id (:id (:route-params ctx))
        res (get-resource-or-nil db {:id id :resourceType resourceType})]
    (if (nil? res)
      {:status 404
       :json {:not :found}}
      {:status 200
       :json res})))

(defn get-resources [resourceType ctx]
  (def db-ctx (get-in ctx [:db :master]))
  (let [name (:name (:params (:request ctx)))]
    (if (not (nil? name))
      {:status 200
       :json (u/select-to-bundle
              (db.core/query (get-in ctx [:db :master])
                             (format "select  *, '%s'
                                                  from %s t,
                                                  jsonb_array_elements(resource#>'{resource, name}') elem
                                                  where elem->>'given' iLIKE '%s';"
                                     (str/capitalize resourceType)
                                     (str/lower-case resourceType)
                                     (format "%s%s%s" "%" name "%"))))}
      {:status 200
       :json (u/select-to-bundle
              (db.core/query (get-in ctx [:db :master])
                             (format "select *, '%s' rt from %s"
                                     (str/capitalize resourceType)
                                 (str/lower-case resourceType))))})))

(defn create-resource [resourceType {{db :master} :db :as ctx}]
  (let [resource (u/body->map (:body (:request ctx)))
        resource (assoc resource :resourceType resourceType)
        resource (u/enhance-with-id resource)
        patient-id (u/get-patient-id resource)]
    (if (u/require-patient-id? resource)
      (db.core/exec! db (format "insert into %s (id, patient_id, resource) values ('%s', '%s', $$%s$$) on conflict do nothing"
                                (str/lower-case resourceType)
                                (:id resource)
                                patient-id
                                (cheshire.core/generate-string (dissoc resource :id :resourceType) true)))
      (db.core/exec! db (format "insert into %s (id, resource) values ('%s', $$%s$$) on conflict do nothing"
                                (str/lower-case resourceType)
                                (:id resource)
                                (cheshire.core/generate-string (dissoc resource :id :resourceType) true))))
    {:status 201
     :json (get-resource-or-nil db resource)}))


(defn update-resource [resourceType {{db :master} :db :as ctx}]
  (let [id (:id (:route-params ctx))
        resource (u/body->map (:body (:request ctx)))]
    (db.core/exec! db
                   (format "insert into %s (id, resource)
values('%s', $$%s$$) on conflict(id) do update set resource = EXCLUDED.resource;"
                           (str/lower-case resourceType) id (cheshire.core/generate-string resource)))
    (get-resource resourceType ctx)))


(defn delete-resource [resourceType {{db :master} :db :as ctx}]
  (let [id (:id (:route-params ctx))]
    (if-let [resource (get-resource-or-nil db {:id id :resourceType resourceType})]
      (do
        (db.core/exec! db (format "delete from %s where id = '%s'" (str/lower-case resourceType) id))
        {:status 200
         :json resource})
      {:status 204
       :json nil})))


;; Shraded

(defn get-sharded-id [id]
  (let [[patient-id id] (str/split id #"_")]
    {:id id
     :patient-id patient-id}))

(defn get-sharded-resource-or-nil [db {id :id rt :resourceType}]
  (let [sharded-id (get-sharded-id id)]
    (println sharded-id)
    (when-let [res (db.core/query-first db (format "select patient_id || '_' || id as id, resource, '%s' rt from %s where id = '%s' and patient_id = '%s'"
                                                   (str/capitalize rt)
                                                   (str/lower-case rt)
                                                   (:id sharded-id)
                                                   (:patient-id sharded-id)))]
      (assoc (:resource res)
             :id (:id res)
             :resourceType (str/capitalize rt)))))


(defn get-sharded-resource [resourceType {{db :master} :db :as ctx}]
  (let [id (:id (:route-params ctx))
        _ (println id)
        res (get-sharded-resource-or-nil db {:id id :resourceType resourceType})]
    (if (nil? res)
      {:status 404
       :json {:not :found}}
      {:status 200
       :json res})))

(defn get-sharded-resources [resourceType ctx]
  (def db-ctx (get-in ctx [:db :master]))
  {:status 200
   :json (u/select-to-bundle
          (db.core/query (get-in ctx [:db :master])
                         (format "select patient_id || '_' || id as id, resource, '%s' rt from %s"
                                 (str/capitalize resourceType)
                                 (str/lower-case resourceType))))})

(defn create-sharded-resource [resourceType {{db :master} :db :as ctx}]
  (let [resource (u/body->map (:body (:request ctx)))
        resource (assoc resource :resourceType resourceType)
        resource (u/enhance-with-id resource)
        patient-id (u/get-patient-id resource)]
    (db.core/exec! db (format "insert into %s (id, patient_id, resource) values ('%s', '%s', $$%s$$) on conflict do nothing"
                              (str/lower-case resourceType)
                              (:id resource)
                              patient-id
                              (cheshire.core/generate-string (dissoc resource :id :resourceType) true)))
    {:status 201
     :json (get-sharded-resource-or-nil db (assoc resource :id (str/join "_" [patient-id (:id resource)])))}))

(defn update-sharded-resource [resourceType {{db :master} :db :as ctx}]
  (let [id (:id (:route-params ctx))
        sharded-id (get-sharded-id id)
        resource (u/body->map (:body (:request ctx)))
        resource (assoc resource :resourceType resourceType)]
    (if (u/require-patient-id? resource)
      (if (zero? (first (db.core/execute! db (format "insert into %s (id, patient_id, resource) values('%s', '%s', $$%s$$) on conflict do nothing;"
                                                     (str/lower-case resourceType) (:id sharded-id) (:patient-id sharded-id) (cheshire.core/generate-string resource)))))
        (if (zero? (first (db.core/execute! db (format "update %s set resource = $$%s$$ where id = '%s' and patient_id in ('%s', 'Group')"
                                                       (str/lower-case resourceType) (cheshire.core/generate-string resource) (:id sharded-id) (:patient-id sharded-id)))))
          {:status 400
           :body "not updated"}
          (get-resource resourceType ctx)))
      (get-sharded-resource resourceType ctx))))

(defn delete-sharded-resource [resourceType {{db :master} :db :as ctx}]
  (let [id (:id (:route-params ctx))
        sharded-id (get-sharded-id id)]
    (if-let [resource (get-sharded-resource-or-nil db {:id id :resourceType resourceType})]
      (do
        (db.core/exec! db (format "delete from %s where id = '%s' and patient_id = '%s'" (str/lower-case resourceType) (:id sharded-id) (:patient-id sharded-id)))
        {:status 200
         :json resource})
      {:status 204
       :json nil})))
