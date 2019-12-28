(ns fhir.web
  (:require [db.core]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [fhir.util :as u]))

(comment

  (db.core/query
   db-ctx
   "select * from practitioner")

  )



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
  {:status 200
   :json (u/select-to-bundle
          (db.core/query (get-in ctx [:db :master])
                         (format "select *, '%s' rt from %s"
                                 (str/capitalize resourceType) (str/lower-case resourceType))))})

(defn create-resource [resourceType {{db :master} :db :as ctx}]
  (let [resource (u/body->map (:body (:request ctx)))
        resource (assoc resource :resourceType resourceType)
        resource (u/enhance-with-id resource)]
    (let [resourceType (:resourceType resource)]
      (db.core/exec! db (format "insert into %s (id, resource) values ('%s', $$%s$$) on conflict do nothing"
                                (str/lower-case resourceType)
                                (:id resource)
                                (cheshire.core/generate-string (dissoc resource :id :resourceType) true)))
      {:status 201
       :json (get-resource-or-nil db resource)})))

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
