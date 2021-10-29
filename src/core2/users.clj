(ns core2.users
  (:require
   [xtdb.api :as xt]

   [core2.db :as db]
   [core2.utils :as util]
   [core2.validation :as v]))

;;TODO: optimize get-user (maybe cache all users?)
;;TODO: clean up pull syntax (maybe just desired fields like [:user/email])
;;TODO: move into user lib
(defn get-user
  ""
  {:added "0.1"}
  [x & [fields]]
  (let [email? (v/valid-email? x)
        k (if email? :user/email :xt/id)
        v (if email? x (util/uuid-from-string x))
        q {:find [(conj '() (or fields '[*]) '?user 'pull)]
           :where [['?user :core2/schema :User]
                   ['?user k v]]}]
    (-> (xt/q (xt/db db/node_) q) first first)))

;;TODO: decide on requiring security permissions to look up users
;;TODO: combine get-user & get-users
(defn get-users
  [user-ids & [fields]]
  ;;TODO: validate user-ids
  (->> (xt/q (xt/db db/node_)
             {:find [(conj '() (or fields '[*]) '?user 'pull)]
              :where ['[?user :core2/schema :User]
                      (apply list
                             'or
                             (map #(vector '?user
                                           (if (v/valid-email? %)
                                             :user/email
                                             :xt/id)
                                           %)
                                  user-ids))]})
       (mapv first)))

;;TODO: examples
(comment
  )
