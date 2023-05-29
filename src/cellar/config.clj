(ns cellar.config
  (:refer-clojure :exclude [reset! get]))

(declare ^:dynamic ^:private *config*)

(def default {:tables      {}
              :db-settings {}})

(defn get
  "Returns config"
  []
  *config*)

(defn- -set>!
  [new-config]
  (alter-var-root #'*config* (fn [_old] new-config)))

(defn reset!
  "Resets config to default state"
  []
  (-set>! default))

(defn -table>
  [new-table]
  (->> new-table
       (merge (:tables *config*))
       (assoc *config* :tables)
       (-set>!)))

(defn <tables-
  []
  (:tables *config*))

(defn <db-settings-
  []
  (:db-settings *config*))

(defn -db-settings>
  [new-db-settings]
  (-set>! (assoc *config* :db-settings new-db-settings)))

(defonce ^:dynamic ^:private *config* default)
