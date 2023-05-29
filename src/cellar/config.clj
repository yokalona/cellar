(ns cellar.config
  (:refer-clojure :exclude [reset!]))

(declare ^:dynamic ^:private *config*)

(defn- -default
  []
  {:tables      {}
   :db-settings {}})

(defn <get-
  "Returns config"
  []
  *config*)

(defn -set>
  "Sets config"
  [new-config]
  (alter-var-root #'*config* (fn [_old] new-config)))

(defn reset!
  "Resets config to default state"
  []
  (-set> (-default)))

(defn <table-
  "Returns `table` config if exists"
  [table]
  (get-in *config* [:tables table]))

(defn -table>
  "Adds table `new-table` to config"
  [new-table]
  (->> new-table
       (merge (:tables *config*))
       (assoc *config* :tables)
       (-set>)))

(defn <tables-
  "Returns all tables configs"
  []
  (:tables *config*))

(defn -tables>
  "Sets `new-tables` to config"
  [new-tables]
  (-> *config*
      (assoc :tables new-tables)
      (-set>)))

(defn <db-settings-
  "Returns db settings"
  []
  (:db-settings *config*))

(defn -db-settings>
  "Sets db settings"
  [new-db-settings]
  (-> *config*
      (assoc :db-settings new-db-settings)
      (-set>)))

(defonce ^:dynamic ^:private *config* (-default))
