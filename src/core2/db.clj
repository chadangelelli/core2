(ns core2.db
  "Core2 Database connection"

  {:author "Chad Angelelli"
   :added "0.1"}

  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]

   [xtdb.api :as xt]
   [malli.core :as m]
   [taoensso.timbre :as log]

   [core2.config :as config]
   [core2.validation :as v]
   [core2.utils :as util]
   [core2.model :as model]
   [core2.error :refer (err)]))

(declare node_)

(def DataEvent
  {:event/id nil
   :event/start-time nil
   :event/end-time nil
   :event/logged? false
   :event/log-id nil
   :event/result nil})

(defn make-data-event
  ""
  {:added "0.1"}
  [& [m]]
  (merge DataEvent m {:event/id (util/uuid)
                      :event/start-time (System/nanoTime)}))

(defn make-data-event-response
  ""
  {:added "0.1"}
  [{:keys [:event/start-time] :as event} & [m]]
  (let [end-time      (System/nanoTime)
        total-time    (- end-time start-time)
        total-time-ms (/ (double total-time) 1000000.0)
        total-time-s  (/ (double total-time-ms) 1000.0)
        event         (merge
                       event
                       m
                       {:event/end-time                end-time
                        :event/total-time              total-time
                        :event/total-time-milliseconds total-time-ms
                        :event/total-time-seconds      total-time-s})]

    ;;TODO: implement logging

    event))

(defn start-db
  ""
  {:added "0.1"}
  []
  (defonce node_
    (xt/start-node
     {:ss/rocksdb {:xtdb/module 'xtdb.rocksdb/->kv-store
                   :db-dir (io/file "__db")}
      :xtdb/tx-log {:kv-store :ss/rocksdb}
      :xtdb/document-store {:kv-store :ss/rocksdb}
      :xtdb/index-store {:kv-store :ss/rocksdb}}))
  (log/info "[Substrate] [OK] Database started"))

(defn stop-db
  ""
  {:added "0.1"}
  []
  (try
    (.close node_)
    (catch Throwable t))
  (log/info "[Substrate] [OK] Database stopped"))
