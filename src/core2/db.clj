(ns core2.db
  "TODO: add docstring"

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
   :event/logged? false
   :event/log-id nil
   :event/result nil
   :event/metrics {:start-time nil :end-time nil}})

(defn make-data-event
  ""
  {:added "0.1"}
  [& [m]]
  (merge DataEvent m {:event/id (util/uuid)
                      :event/metrics {:start-time (System/nanoTime)}}))

(defn make-data-event-response
  ""
  {:added "0.1"}
  [{{:keys [start-time] :as metrics} :event/metrics :as event} & [m]]
  (let [end-time      (System/nanoTime)
        total-time    (- end-time start-time)
        total-time-ms (/ (double total-time) 1000000.0)
        total-time-s  (/ (double total-time-ms) 1000.0)
        metrics       (merge
                        metrics
                        {:end-time                end-time
                         :total-time              total-time
                         :total-time-milliseconds total-time-ms
                         :total-time-seconds      total-time-s}) 
        event         (merge event m {:event/metrics metrics})]

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
  (log/info "[Core2] [OK] Database started")
  node_)

(defn stop-db
  ""
  {:added "0.1"}
  []
  (try
    (.close node_)
    (catch Throwable t))
  (log/info "[Core2] [OK] Database stopped"))

(defn restart-db
  ""
  {:added "0.1"}
  []
  (stop-db)
  (start-db))

(defn put
  "Wraps xtdb.api/put with Core2 Data Event"
  {:added "0.1"}
  [{:keys [event] {:keys [xt/id] :as doc} :doc :as args}]

  (let [event (or event (make-data-event))
        {:keys [query-error] :as tx
         } (try
             (xt/submit-tx node_ [[::xt/put doc]])
             (catch Throwable t
               {:query-error (.getMessage t)}))]

    (if query-error
      (make-data-event-response
       event
       (err {:error/type :core2/query-error
             :error/fatal? false
             :error/message "Query error"
             :error/data {:args args :query-error query-error}}))

      ;;TODO: look into keeping things non-blocking/async
      (let [_ (xt/await-tx node_ tx)
            r (xt/entity (xt/db node_) id)]
        (make-data-event-response
         event
         {:event/result r})))))

(defn q
  "Wraps xtdb.api/q with Core2 Data Event"
  {:added "0.1"}
  [query & [event]]
  (let [event (or event (make-data-event))]
    (let [{:keys [query-error] :as r
           } (try
               (xt/q (xt/db node_) query)

               (catch java.lang.NullPointerException e
                 {:query-error (str "Null pointer. Check that the "
                                    "database is started and "
                                    "accepting connections.")})

               (catch Throwable t
                 {:query-error (.getMessage t)}))]

      (make-data-event-response
       event
       (if-not query-error
         (when (seq r)
           {:event/result r})
         (err {:error/type :core2/query-error
               :error/fatal? false
               :error/message "Query error"
               :error/data {:query query :query-error query-error}}))))))
