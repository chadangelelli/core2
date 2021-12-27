(ns repl
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [rebel-readline.main :as rebel]))

(defn -main []
  (println "nrepl server at port 40000")
  (nrepl-server/start-server :port 40000)
  (rebel/-main)
  (System/exit 0))
