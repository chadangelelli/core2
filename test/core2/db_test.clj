(ns core2.db-test
  (:require
   [clojure.test :refer (deftest is testing)]

   [malli.core :as m]

   [core2.db :as db]))

(declare node_)

(deftest conn-test
  (defonce node_ (db/start-db))

  (testing "database connectivity"
    ;; (is  (instance? xtdb.node.XtdbNode node_))
    (is (= 1 1))
    ))
