(ns cellar.config
  (:refer-clojure :exclude [reset! get]))

(declare ^:dynamic ^:private *config*)

(def default {:tables      {}
              :db-settings {}})

(defn <get-
  "Returns config"
  []
  *config*)

(defn -set>
  [new-config]
  (alter-var-root #'*config* (fn [_old] new-config)))

(defn reset!
  "Resets config to default state"
  []
  (-set> default))

(defn <table-
  [table]
  (get-in *config* [:tables table]))

(defn -table>
  [new-table]
  (->> new-table
       (merge (:tables *config*))
       (assoc *config* :tables)
       (-set>)))

(defn <tables-
  []
  (:tables *config*))

(defn -tables>
  [new-tables]
  (-> *config*
      (assoc :tables new-tables)
      (-set>)))

(defn <db-settings-
  []
  (:db-settings *config*))

(defn -db-settings>
  [new-db-settings]
  (-> *config*
      (assoc :db-settings new-db-settings)
      (-set>)))

(defonce ^:dynamic ^:private *config* default)
