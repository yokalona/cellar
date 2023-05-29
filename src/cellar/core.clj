(ns cellar.core
  (:require [clojure.test :as test]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [cellar.config :as cfg]))

(defn- -transform
  [columns f]
  (mapv (fn [[k v]] [(f (if (keyword? k) (name k) k)) v]) columns))

(defn- -table
  ([spec]
   (cond
     (keyword? spec) (if (= :conditional! spec)
                       {:conditional? true}
                       {spec true})
     (test/function? spec) {:transform spec}
     (coll? spec) {:columns spec}
     :unknown spec))
  ([specs config]
   (if (empty? specs)
     config
     (recur (rest specs) (merge config (-table (first specs)))))))

(defn- -init-table
  [table-name table]
  (jdbc/db-do-commands
    (cfg/<db-settings-)
    (jdbc/create-table-ddl table-name
                           (-transform (:columns table) (:transform table))
                           (if (:conditional? table)
                             {:conditional? true}
                             {}))))

(defn- -where-clause
  [kvs]
  (str/join " AND " (map #(format "%s = ?" (name %)) (map key kvs))))

(defmacro deftable
  [name & specs]
  (let [new-table (-table specs {:transform identity})
        columns (->> new-table :columns (mapv (comp symbol first)))]
    (cfg/-table> {(keyword name) new-table})
    (list 'defrecord (symbol (str/capitalize name)) columns)))

(comment
  (deftable flow
            :conditional!
            (fn [x] (str/replace (name x) "-" "_"))
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
  ([table kvs]
   (get table kvs nil))
  ([table kvs limit]
   (let [where-clause (-where-clause kvs)
         where-clause (if limit (str where-clause " LIMIT " limit) where-clause)
         query (format "SELECT * FROM %s WHERE %s" (name table) where-clause)]
     (jdbc/query (cfg/<db-settings-) (cons query (mapv val kvs))))))

(defn exists? [table kvs] (some? (seq (get table kvs 1))))

(defn new! [table kvs] (jdbc/insert! (cfg/<db-settings-) table kvs))

(defn update!
  [table kvs where]
  (->> where
       (mapv val)
       (cons (-where-clause where))
       (jdbc/update! (cfg/<db-settings-) table kvs)))

(defn delete!
  [table where]
  (->> where
       (mapv val)
       (cons (-where-clause where))
       (jdbc/delete! (cfg/<db-settings-) table)))
