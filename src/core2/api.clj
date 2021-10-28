(ns core2.api
  "TODO: add docstring"
  {:author "Chad Angelelli"
   :added "0.1"}

  (:require
   [clojure.string :as string]
   [clojure.set :as set]

   [xtdb.api :as xt]
   [malli.core :as m]

   [core2.db :as db]
   [core2.model :as model]
   [core2.utils :as util]
   [core2.validation :as v :refer (validate)]
   [core2.validation.builtins :as vb]
   [core2.error :refer (err)]))

;;TODO: optimize get-user (maybe cache all users?)
;;TODO: clean up pull syntax (maybe just desired fields like [:user/email])
;;TODO: move into user lib
(defn get-user
  "Returns user for UUID or email. Optionally takes find pull syntax for
  filtering which fields to return. Default returns all fields.
  ```clojure
  (get-user \"chad.angelelli@gmail.com\")
  (get-user \"c28c3279-7def-4ca4-ae41-bae7bd595e57\")
  (get-user \"chad.angelelli@gmail.com\"
            '(pull ?user [:user/first_name :user/last_name :user/email]))
  ```"
  {:added "0.1"}
  [x & [pull-syntax]]
  (let [k (if (v/valid-email? x) :user/email :xt/id)]
    (-> (xt/q (xt/db db/node_)
              {:find [(or pull-syntax '(pull ?user [:xt/id :user/email]))]
               :where [['?user :ss/schema :User]
                       ['?user k x]]})
        first
        first)))

;;TODO: decide on requiring security permissions to look up users
;;TODO: combine get-user & get-users
(defn get-users
  [user-ids & [pull-syntax]]
  ;;TODO: validate user-ids
  (->> (xt/q (xt/db db/node_)
             {:find [(or pull-syntax '(pull ?user [:xt/id :user/email]))]
              :where ['[?user :ss/schema :User]
                      (apply list
                             'or
                             (map #(vector '?user
                                           (if (v/valid-email? %)
                                             :user/email
                                             :xt/id)
                                           %)
                                  user-ids))]})
       (mapv first)))

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
                  users    (get-users user-ids)]

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

(def valid-create-args
  (m/schema
   [:map {:closed true}
    [:user                         vb/valid-user-target]
    [:schema                       vb/valid-schema]
    [:data                         map?]
    [:grant-user  {:optional true} vb/valid-user-perms-map]
    [:grant-group {:optional true} vb/valid-group-perms-map]
    [:set-state   {:optional true} vb/valid-state-set]]))

(defn create!
  ""
  {:added "0.1"}
  [{:keys [user schema data grant set-state] :as args}]

  (if-let [args-err (validate valid-create-args args)]
    (err {:error/type :core2/args-error
          :error/fatal? false
          :error/message "Invalid create! arguments"
          :error/data {:args args
                       :validation-error args-err}})

    (let [form (:form (model/get-schema schema))]
      (if-let [data-err (validate form data)]
        (err {:error/type :core2/validation-error
              :error/fatal? false
              :error/message "Invalid data"
              :error/data {:args args
                           :validation-error data-err}})

        (let []
          )))))

(clojure.pprint/pprint
 (create! {:user "chad@shorttrack.io"
           :schema :User
           :data {:user/first_name "Chris"
                  :user/last_name "Hacker"
                  :user/email "chris@shorttrack.io"}
           :grant-user {"chad@shorttrack.io" #{:all}}
           :set-state #{:hidden}}))

(defn update!
  ""
  {:added "0.1"}
  []
  )

(defn delete!
  ""
  {:added "0.1"}
  []
  )
