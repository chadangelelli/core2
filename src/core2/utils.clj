(ns core2.utils)

(defn uuid [] (java.util.UUID/randomUUID))

(defn uuid-from-string [s] (java.util.UUID/fromString s))

(defn eq [f a b] (= (f a) (f b)))

(defn neq [f a b] (not= (f a) (f b)))
