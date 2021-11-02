(ns core2.api
  "TODO: add docstring"
  {:author "Chad Angelelli"
   :added "0.1"}

  (:require

   [xtdb.api :as xt]
   [malli.core :as m]

   [core2.db :as db]
   [core2.model :as model]
   [core2.users :as user]
   [core2.utils :as util]
   [core2.validation :as v :refer (validate)]
   [core2.validation.builtins :as vb]
   [core2.error :refer (err)]))

(def permissions #{:read :update :state :owner :all})

;;TODO: validate input
(defn make-perms-key
  [typ perm]
  (keyword
   (format "core2/%s-%s"
           (case typ :user "upm" :group "gpm")
           (str perm))))

(defn make-user-id-lookup
  [users]
  (into {} (for [{:keys [:user/email :xt/id]} (seq users)] [email id])))

(defn make-user-email-lookup
  [users]
  (into {} (for [{:keys [:user/email :xt/id]} (seq users)] [id email])))

(defn make-grant-user-perms
  [id-lookup email-lookup perms grant-user]
  (if-not perms
    {}
    (reduce
     (fn [o* [u* p*]]
       (let [user-id (if (v/valid-email? u*) (get id-lookup u*) u*)]
         (merge
          o*
          (reduce (fn [o** p**]
                    (let [k (make-perms-key :user p**)
                          v (conj (or (get o* k) #{}) user-id)]
                      (assoc o** k v)))
                  o*
                  p*))))
     perms
     grant-user)))

;;TODO: imlement set-owner on create!
(defn make-new-document-permissions
  ""
  {:added "0.1"}
  [{:keys [user grant-user grant-group set-owner] :as args}]

  (let [user-list (-> (filter identity
                              (concat (keys grant-user)
                                      (keys grant-group)
                                      [set-owner]))
                      (user/get-users [:xt/id :user/email]))
        id-lu     (make-user-id-lookup user-list)
        email-lu  (make-user-email-lookup user-list)
        perms     {:core2/upm-owner user}
        perms     (make-grant-user-perms id-lu email-lu perms grant-user)

        ]
    {:___ID_LU___ id-lu
     :___EMAIL_LU___ email-lu
     :___PERMS___ perms}
    ))

;;TODO: finish grant, set-state
(defn make-new-document
  ""
  {:added "0.1"}
  [{:keys [xt/id schema data] :as args}]

  (let [id (or id (util/uuid))]
    (merge data
           {:xt/id id, :core2/schema schema}
           (make-new-document-permissions args))))

(def valid-create-args
  (m/schema
   [:map {:closed true}
    [:user                         vb/valid-user-target]
    [:schema                       vb/valid-schema]
    [:data                         map?]
    [:grant-user  {:optional true} vb/valid-user-perms-map]
    [:grant-group {:optional true} vb/valid-group-perms-map]
    [:set-state   {:optional true} vb/valid-state-set]]))

(defn invalidate-create-args
  [{:keys [user schema data] :as args}]

  (if-let [args-err (validate valid-create-args args)]
    (err {:error/type :core2/args-error
          :error/fatal? false
          :error/message "Invalid create! arguments"
          :error/data {:args args :validation-error args-err}})

    (if (and (= schema :User) (user/get-user (:user/email data)))
      (err {:error/type :core2/validation-error
            :error/fatal? false
            :error/message (str "Cannot create duplicate User: "
                                (:user/email data))
            :error/data {:args args}})

      (let [form      (:form (model/get-schema schema))
            model-err (not form)
            user-err  (not user)
            data-err  (validate form data)]

        (cond
          user-err
          (err {:error/type :core2/auth-error
                :error/fatal? false
                :error/message (str "Unknown user: " user)
                :error/data {:args args}})

          model-err
          (err {:error/type :core2/model-error
                :error/fatal? false
                :error/message (str "Unknown schema: " schema)
                :error/data {:args args}})

          data-err
          (err {:error/type :core2/validation-error
                :error/fatal? false
                :error/message "Invalid data"
                :error/data {:args args :validation-error data-err}})

          ::else-api-create-call-is-valid
          nil)))))

(defn create!
  "Creates a document. Returns Core2 Data Event."
  {:added "0.1"}
  [{:keys [user] :as args}]

  (let [event   (db/make-data-event)
        user-id (:xt/id (user/get-user user [:xt/id]))
        args*   (assoc args :user user-id)]

    (if-let [err (invalidate-create-args args*)]
      (db/make-data-event-response event err)

      (let [doc (make-new-document args*)
            ;; res (db/put {:event event :doc doc})
            res doc
            ]
        res))))

  (println "\n\n\n")
  (time
   (clojure.pprint/pprint
    (let [args {:user "chad@shorttrack.io"
                :schema :User
                :data {:user/first_name "Chris"
                       :user/last_name "Hacker"
                       :user/email "chris@shorttrack.io"}
                :grant-user {"chad@shorttrack.io" #{:read :update :state}}
                :set-state #{:hidden}}]

      ;; (make-new-document args)
      (create! args))))

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

;;TODO: validate datalog query
;;TODO: enforce logic vars only start with "?"
(def valid-q-args
  (m/schema
   [:map {:closed true}
    [:q any?]
    [:user vb/valid-user-target]]))

(defn get-q-logic-vars
  [q]
  (distinct (re-seq #"\?[a-z\-_]+" (str q))))

(defn q
  "Runs a query with Core2 API permissions injected.
  Returns Core2 Data Event."
  {:added "0.1"}
  [{:keys [user q] :as args}]

  (if-let [args-err (validate valid-q-args args)]
    (err {:error/type :core2/args-error
          :error/fatal? false
          :error/message "Invalid query arguments"
          :error/data {:args args :validation-error args-err}})

    (if-let [{user-id :xt/id :as user*
              } (user/get-user user [:xt/id :user/email])]

      (let [logic-vars (get-q-logic-vars q)]
        (if-not (seq logic-vars)
          (err {:error/type :core2/validation-error
                :error/fatal? false
                :error/message "No valid logic vars found in query"
                :error/data {:args args}})

          (let [{:keys [find where]} q
                where* (reduce
                        #(conj
                          %1
                          (list 'or
                                [(symbol (name %2)) :core2/upm-all user-id]
                                [(symbol (name %2)) :core2/upm-read user-id]))
                        where
                        logic-vars)
                q* (assoc q :where where*)]
            (db/q q*))))

      (err {:error/type :core2/auth-error
            :error/fatal? false
            :error/message (str "Unknown user provided to q function: " user)
            :error/data {:args args}}))))

(defn create-initial-user!
  [{:keys [email] :as user}]

  (if-let [args-err (validate (:form model/User) user)]
    (err {:error/type :core2/args-error
          :error/fatal? false
          :error/message "Invalid user provided to create-initial-user!"
          :error/data {:args user :validation-error args-err}})

    (let [users-exist? (-> '{:find [?e] :where [[?e :core2/schema :User]]}
                           db/q
                           :event/result
                           seq
                           boolean)]
      (if users-exist?
        (err {:error/type :core2/auth-error
              :error/fatal? false
              :error/message "One or more users already exist."
              :error/data {:args user}})

        (let [uid (util/uuid)
              doc (merge user
                         {:xt/id uid
                          :core2/schema :User
                          :core2/owner uid
                          :core2/perms-all #{uid}})]
          (db/put {:doc doc}))))))

(comment

  (println
   (create-initial-user! {:user/email "chad@shorttrack.io"
                          :user/first_name "Chad"
                          :user/last_name "Angelelli"}))

  (time
   (clojure.pprint/pprint
    (let [args {:user "chad@shorttrack.io"
                :schema :User
                :data {:user/first_name "Chris"
                       :user/last_name "Hacker"
                       :user/email "chris@shorttrack.io"}
                :grant-user {"chad@shorttrack.io" #{:all}}
                :set-state #{:hidden}}]

      ;; (make-new-document args)
      (create! args))))

  (println (apply str (repeat 3 "\n")))
  (clojure.pprint/pprint
   (q {:user "chad@shorttrack.io"
       :q '{:find [(pull ?e [*])]
            :where [[?e :core2/schema :User]]}}))

  ); /comment
