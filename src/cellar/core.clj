(ns cellar.core
  (:require [cellar.config :as cfg]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]))

(defn- -to
  [column]
  (str/replace column #"-" "_"))

(defn- -from
  [column]
  (str/replace column #"_" "-"))

(defn- -transform
  [columns f]
  (mapv (fn [[k v]] [(keyword (f (if (keyword? k) (name k) k))) v]) columns))

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
     (recur (rest specs) (merge config (-table (first specs)))))))

(defn- -init-table
  [table-name table]
  (jdbc/db-do-commands
    (cfg/<db-settings-)
    (jdbc/create-table-ddl (-to (name table-name))
                           (-transform (:columns table) -to)
                           (if (:conditional? table)
                             {:conditional? true}
                             {}))))

(defn- -where-clause
  [kvs]
  (str/join " AND " (map (comp #(format "%s = ?" %) -to name key) kvs)))

(defn- -query
  [table kvs limit]
  (let [where-clause (-where-clause kvs)]
    (format "SELECT * FROM %s WHERE %s"
            (-to (name table))
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

(defn init
  ([] (let [tables (cfg/<tables-)]
        (doseq [table tables]
          (init (first table)))))
  ([table] (-init-table table (get (cfg/<tables-) table))))

(defn get
  "Returns records related to `table` using `kvs` as filter query part"
  ([table kvs]
   (get table kvs nil))
  ([table kvs limit]
   (->> kvs
        (mapv val)
        (cons (-query table kvs limit))
        (jdbc/query (cfg/<db-settings-))
        (map (comp #(into {} %) #(-transform % -from))))))

(defn exists?
  [table kvs]
  (-> table
      (get kvs 1)
      (seq)
      (some?)))

(defn insert!
  [table kvs]
  (jdbc/insert! (cfg/<db-settings-) (-to (name table)) (into {} (-transform kvs -to)))
  (get table kvs 1))

(defn update!
  [table kvs where]
  (->> where
       (mapv val)
       (cons (-where-clause where))
       (jdbc/update! (cfg/<db-settings-) (-to (name table)) (into {} (-transform kvs -to))))
  (get table (merge kvs where)))

(defn delete!
  [table where]
  (->> where
       (mapv val)
       (cons (-where-clause where))
       (jdbc/delete! (cfg/<db-settings-) (-to (name table)))))
