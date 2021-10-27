(ns core2.error)

(defmacro err
  [m]
  (let [{:keys [line column]} (meta &form)]
    (assoc m
           :core2/error? true
           :error/file *file*
           :error/line line
           :error/column column)))
