(ns cellar.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [clojure.test :as test]))

(declare ^:dynamic *config*)

(def default {:tables      {}
              :db-settings {}})

(defn set-config! [new-config] (alter-var-root #'*config* (fn [_old] new-config)))

(defn set-db-setting! [new-db-settings] (set-config! (assoc *config* :db-settings new-db-settings)))

(defn reset-config! [] (set-config! default))

(defonce ^:dynamic *config* default)

(defn- -add-table->config!
  [new-table]
  (->> new-table
       (merge (:tables *config*))
       (assoc *config* :tables)
       (set-config!)))

(defn- -table
  ([spec]
   (cond
     (keyword? spec) (if (= :conditional! spec)
                       {:conditional? true}
                       {spec true})
     (test/function? spec) {:entities spec}
     (coll? spec) {:columns spec}
     :unknown spec))
  ([specs config]
   (if (empty? specs)
     config
     (recur (rest specs) (merge config (-table (first specs)))))))

(defn- -init-table
  [table]
  (jdbc/db-do-commands
    (:db-settings *config*)
    (jdbc/create-table-ddl (first table)
                           (:columns (second table))
                           {:conditional? (:conditional? (second table))
                            :entities     (:entities (second table))})))

(defmacro deftable
  [name & specs]
  (let [new-table (-table specs {})
        columns (->> new-table :columns (mapv (comp symbol first)))]
    (-add-table->config! {(keyword name) new-table})
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
  ([]
   (let [tables (:tables *config*)]
     (doseq [table tables]
       (init (first table)))))
  ([table]
   (-init-table (get-in *config* [:tables table]))))

(defn exists?
  [table kvs]
  (let [where-clause (str/join " AND " (map #(format "%s = ?" %) (map key kvs)))
        query (format "SELECT * FROM %s WHERE %s LIMIT 1" (name table) where-clause)]
    (jdbc/query (:db-settings *config*) (cons query (mapv val kvs)))))

(defn new!
  [table kvs]
  (jdbc/insert! (:db-settings *config*) table kvs))