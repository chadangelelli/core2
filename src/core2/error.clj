(ns core2.error)

(def error-types
  #{:core2/args-error
    :core2/auth-error
    :core2/model-error
    :core2/query-error
    :core2/schema-error
    :core2/validation-error})

(defmacro err
  [{error-type :error/type :as m}]
  (let [{:keys [line column]} (meta &form)
        file *file*]
    (when-not (some error-types [error-type])
      (throw (Exception. (str "Invalid error type provided: " error-type
                              " (at " file " [" line ":" column "])"))))
    (assoc m
           :core2/error? true
           :error/file file
           :error/line line
           :error/column column)))
