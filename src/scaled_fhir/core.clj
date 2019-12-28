(ns scaled-fhir.core
  (:require [web.core]
            [db.core]
            [hiccup.core]
            [clojure.java.shell :as shell]
            [route-map.core :as route-map]
            [web.fhir]
            [clojure.string :as str])

  (:gen-class))

(defn shell [cmd]
  (println "$ " cmd)
  (let [res (apply shell/sh (str/split cmd #"\s+"))]
    (if (= 0 (:exit res))
      (let [result (str/replace (:out res) #"\s*\n$" "")]
        (println result)
        result)
      (throw (Exception. (pr-str res))))))

(defn conn-by-shard-name [ctx shard]
  (if-let [cfg (get-in ctx [:cfg :db shard])]
    {:cfg cfg
     :conn (get-in ctx [:db shard])}))


(defn db-index [ctx]
  {:status 200
   :body (hiccup.core/html [:body
                      [:p "Shards: "
                       (map (fn [shard] [:a {:href (str "/db/" (name shard) "/status")
                                             :style "padding-left: 5px; color: #333"}
                                         (name shard)]) (keys (:db ctx)))]])})

(defn db-status [{req :request cfg :cfg :as ctx}]
  (let [shard-name (keyword (:shard (:route-params ctx)))]
    (if-let [db-conn (conn-by-shard-name ctx shard-name)]
      (do
        (println (:cfg db-conn))
        {:status 200
         :headers {"content-type" "text/html"}
         :body (hiccup.core/html [:body
                                  [:p "Shards: "
                                   (map (fn [shard]
                                          (if (= shard shard-name)
                                            [:span {:style "padding-left: 5px; color: #333"} (name shard)]
                                            [:a {:href (str "/db/" (name shard) "/status")
                                                 :style "padding-left: 5px; color: #333"}
                                             (name shard)])) (keys (:db ctx)))]
                                  [:pre (shell (str "pgmetrics"
                                                    " -h " (get-in db-conn [:cfg :host])
                                                    " -U " (get-in db-conn [:cfg :user])
                                                    " -p " (get-in db-conn [:cfg :port])))]])})
      {:status 404
       :body (str "Not found shard " (name shard-name))})))



(def routes
  {:GET (fn [_] {:status 200 :body "Hello"})
   "db" {:GET #'db-index
         [:shard] {"status" {:GET #'db-status}}}
   "Patient" {:GET #'web.fhir/get-patients
              [:id] {:GET #'web.fhir/get-patient}}
   "Observation" {:GET #'web.fhir/get-observations
                  [:id] {:GET #'web.fhir/get-observation}}
   "Encounter" {:GET #'web.fhir/get-encounters
                [:id] #'web.fhir/get-encounter}
   "Practitioner" {:GET #'web.fhir/get-practitioner
                   [:id] 'web.fhir/get-practitioner}
   "Group" {:GET #'web.fhir/get-groups
            [:id] #'web.fhir/get-group}})


(defn handler [{req :request :as ctx}]
  (let [route   (route-map/match [(or (:request-method req) :get) (:uri req)] routes)]
    (if-let [handler (:match route)]
      (handler (assoc ctx :route-params (:params route)))
      {:status 404
       :body (str [(or (:request-method req) :get) (:uri req)] "not found" route)})))

(defn start [cfg]
  (let [ctx (atom {:cfg cfg})
        db (when (:db cfg) (reduce (fn [acc [shard-key db-cfg]] (assoc acc shard-key (db.core/datasource db-cfg))) {} (:db cfg))
                 #_{:master (db.core/datasource (get-in cfg [:db :master]))
                            :shards (mapv (fn [db-cfg] (db.core/datasource db-cfg)) (get-in cfg [:db :shards]))})
        _ (swap! ctx assoc :db db)
        disp (fn [req] (handler (assoc @ctx :request req)))
        _ (swap! ctx assoc :dispatch disp)
        web (when (:web cfg) (web.core/start {:port 8887} disp))
        _ (swap! ctx assoc :web web)]
    ctx))

(defn stop [ctx]
  (try
    (when-let [srv (:web @ctx)] (srv))
    (catch Exception e))
  (try
    (when-let [db (:db @ctx)]
      (doseq [conn (vals db)]
        (db.core/shutdown conn)))
    (catch Exception e)))

(defn dispatch [ctx req]
  ((:dispatch @ctx) req))

(defn db-from-env []
  (db.core/db-spec-from-env))

(defn -main [& args]
  (println (db.core/db-spec-from-env :master))
  (println (db.core/db-spec-from-env :alfa))
  (println (db.core/db-spec-from-env :beta))
  (start {:db {:master (db.core/db-spec-from-env :master)
               :alfa (db.core/db-spec-from-env :alfa)
               :beta (db.core/db-spec-from-env :beta)}
          :web {}}))

(comment
  (def *ctx (start {:db {:master (db.core/db-spec-from-env :master)
                         :alfa (db.core/db-spec-from-env :alfa)
                         :beta (db.core/db-spec-from-env :beta)}
                    :web {}}))

  (db.core/query (:master (:db @*ctx)) "select 1")

  (:db @*ctx)

  (dispatch *ctx {:uri "/"})
  (dispatch *ctx {:uri "/db/tables" :params {:q "class"}})

  (stop *ctx)

  )
