(ns core2.api.permissions
  (:require
   [clojure.string :as string]
   [clojure.set :as set]

   [core2.db :as db]
   [core2.users :as user]
   [core2.utils :as util]
   [core2.validation :as v]
   [core2.error :refer (err)]))

(def permissions #{:read :update :state :owner :all})

;;TODO: validate input
(defn make-perms-key
  [typ perm]
  (keyword
   (format "core2/%s-%s"
           (case typ :user "upm" :group "gpm")
           (name perm))))

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
;;TODO: implement grant-group on create!
(defn make-new-document-permissions
  ""
  {:added "0.1"}
  [{:keys [as grant-user grant-group set-owner] :as args}]

  (let [user-list (-> (filter identity
                              (concat (keys grant-user)
                                      (keys grant-group)
                                      [set-owner]))
                      (user/get-users [:xt/id :user/email]))
        id-lu     (make-user-id-lookup user-list)
        email-lu  (make-user-email-lookup user-list)
        perms     {:core2/upm-owner as}
        perms     (make-grant-user-perms id-lu email-lu perms grant-user)]
    perms))

(comment


  ); /comment
