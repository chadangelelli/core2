(ns core2.api.permissions
  (:require
   [clojure.string :as string]
   [clojure.set :as set]

   [core2.db :as db]
   [core2.users :as user]
   [core2.utils :as util]
   [core2.error :refer (err)]))

(def permissions #{:read :update :state :owner :all})

(defn make-user-perms-key
  [id]
  (keyword "ss" (str "userperm_" id)))

(defn make-doc-perms
  [new?
   {user-id :xt/id user-email :user/email}
   schema-name
   doc-id
   doc]
  (let [doc (assoc doc
                   :ss/owner user-id
                   (make-user-perms-key user-id) #{:all})]
    doc))

(defn make-valid-perms
  ""
  {:added "0.1"}
  [msg]
  [:set
   [:fn
    {:error/message msg}
    (fn make-valid-perms* [v] (boolean (some permissions [v])))]])

(defn make-grant-perms-data
  [{{:keys [user group]} :grant :as q}]
  (let [d {}
        d (if-not user
            d
            (let [user-ids (keys user)
                  users    (user/get-users user-ids)]

              (if (util/neq count user-ids users)
                (err {:error/type :core2/validation-error
                      :error/fatal? false
                      :error/message "Could not find all users for grant"
                      :error/data
                      {:query q
                       :unknown-users
                       (set/difference (set user-ids)
                                       (set (map :user/email users)))}})

                (into d (map (fn [{:keys [xt/id user/email]}]
                               [(make-user-perms-key id) (get user email)])
                             users)))))
        ;;TODO: implement Group perms
        ]
    d))

(comment
  )
