(ns cellar.core
  (:refer-clojure :exclude [get])
  (:require [cellar.config :as cfg]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]))

(defn- -to
  [column]
  (if (keyword? column)
    (recur (name column))
    (str/replace column #"-" "_")))

(defn- -from
  [column]
  (if (keyword? column)
    (recur (name column))
    (str/replace column #"_" "-")))

(defn- -transform
  [f columns]
  (mapv (fn [[k v]] [(keyword (f k)) v]) columns))

(defn- -table
  ([spec]
   (cond
     (keyword? spec) (if (= :conditional! spec)
                       {:conditional? true}
                       {spec true})
     (vector? spec) {:columns spec}
     :unknown spec))
  ([specs config]
   (if (empty? specs)
     config
     (->> specs
          (first)
          (-table)
          (merge config)
          (recur (rest specs))))))

(defn- -init-table
  [table-name table]
  (jdbc/db-do-commands
    (cfg/<db-settings-)
    (jdbc/create-table-ddl
      (-to table-name)
      (-transform -to (:columns table))
      (if (:conditional? table)
        {:conditional? true}
        {}))))

(defn- -where-clause
  [kvs]
  (str/join " AND " (map (comp #(format "%s = ?" %) -to key) kvs)))

(defn- -query
  [table kvs limit]
  (let [where-clause (-where-clause kvs)]
    (format "SELECT * FROM %s WHERE %s"
            (-to table)
            (if limit
              (str where-clause " LIMIT " limit)
              where-clause))))

(defmacro deftable
  [name & specs]
  (let [new-table (-table specs {})]
    (cfg/-table> {(keyword name) new-table})))

(comment
  (deftable flow
            :conditional!
            [[:chat "BIGINT"]
             [:domain-id "VARCHAR(32)"]
             [:message "BIGINT"]
             [:step "VARCHAR(64)"]]))

(defn get
  "Returns records related to `table` using `kvs` as filter query part"
  ([table kvs]
   (get table kvs nil))
  ([table kvs limit]
   (->> kvs
        (mapv val)
        (cons (-query table kvs limit))
        (jdbc/query (cfg/<db-settings-))
        (map (comp #(into {} %)
                   #(-transform -from %))))))

(defn exists?
  [table kvs]
  (-> table
      (get kvs 1)
      (seq)
      (some?)))

(defn insert!
  [table kvs]
  (->> kvs
       (-transform -to)
       (into {})
       (jdbc/insert! (cfg/<db-settings-) (-to table))))

(defn update!
  [table kvs where]
  (->> where
       (mapv val)
       (cons (-where-clause where))
       (jdbc/update! (cfg/<db-settings-) (-to table) (into {} (-transform -to kvs)))))

(defn delete!
  [table where]
  (->> where
       (mapv val)
       (cons (-where-clause where))
       (jdbc/delete! (cfg/<db-settings-) (-to table))))

(defn init
  ([] (let [tables (cfg/<tables-)]
        (doseq [table tables]
          (init (first table)))))
  ([table] (->> (cfg/<table- table)
                (-init-table table))))