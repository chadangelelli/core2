(ns core2.validation.builtins
  "TODO: add docstring"
  {:author "Chad Angelelli"
   :added "0.1"}

  (:require
   [malli.core :as m]

   [core2.model :as model]
   [core2.api.permissions :as perms]
   [core2.validation :as v]))

(def valid-user-target
  (m/schema [:fn {:error/message "Invalid user target"} v/valid-user-target?]))

(def valid-group-target
  (m/schema [:fn {:error/message "Invalid group target"} v/valid-uuid?]))

(def valid-state-set
  (m/schema [:set keyword?]))

(def valid-schema
  (m/schema
   [:and
    keyword?
    [:fn {:error/message "Invalid schema"} #(boolean (model/get-schema %))]]))

(def valid-user-perms
  (m/schema (perms/make-valid-perms "Invalid user permissions")))

(def valid-group-perms
  (m/schema (perms/make-valid-perms "Invalid group permissions")))

(def valid-user-perms-map
  (m/schema
   [:and
    [:map-of valid-user-target valid-user-perms]
    [:fn
     {:error/message "User permissions map cannot be empty"}
     #(> (count (keys %)) 0)]]))

(def valid-group-perms-map
  (m/schema
   [:and
    [:map-of valid-group-target valid-group-perms]
    [:fn
     {:error/message "Group permissions map cannot be empty"}
     #(> (count (keys %)) 0)]]))
