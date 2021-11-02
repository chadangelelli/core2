(ns core2.contrib.db-utils
  "TODO: add docstring"

  {:author "Chad Angelelli"
   :added "0.1"}

  (:require
   [clojure.string :as string]

   [xtdb.api :as xt]

   [core2.db :as db]))

(defn inspect
  [uuid]
  (let [doc-id (if (uuid? uuid) uuid (java.util.UUID/fromString uuid))]
    (when-let [doc (xt/entity (xt/db db/node_) doc-id)]

      (let [str-count #(count (str %))

            extract
            (fn [doc pat]
              (let [x-ks  (filter #(re-find pat (str %)) (keys doc))
                    x     (into (sorted-map) (select-keys doc x-ks))
                    x-max (+ (apply max (map str-count x-ks))
                             (apply max (map str-count (vals x))))]
                [x-ks x x-max]))

            [xt-ks xt xt-max] (extract doc #":xt\/.*")
            doc (dissoc doc :xt/id)

            [sys-ks sys sys-max] (extract doc #":core2\/.*")
            doc (apply dissoc doc sys-ks)

            [data-ks data data-max] (extract doc #".*")
            doc (apply dissoc doc data-ks) ; NOTE: map should be empty now

            max-len (max xt-max sys-max data-max)

            print-table
            (fn [m]
              (let [max-k (apply max (map str-count (keys m)))
                    max-v (apply max (map str-count (vals m)))
                    rows (->> (map (fn [[k v]]
                                     (let [n (- max-k (str-count k))]
                                       [(str k (apply str (repeat n " ")))
                                        v]))
                                   m)
                              (map #(str "| " (string/join " | " %)))
                              (string/join "\n"))
                    header (->> ["| "
                                 (apply str (repeat (+ max-k max-v 3) "-"))]
                                (string/join ""))]

                (println)
                (println header)
                (println rows)))

            top-header (->> [(apply str (repeat 5 "\n"))
                             "|"
                             (apply str (repeat (+ max-len 8) "%"))]
                            (string/join ""))]

        (println top-header)
        (print-table xt)
        (print-table sys)
        (print-table data)

        (println "\n\n")
        (clojure.pprint/pprint doc)))))
