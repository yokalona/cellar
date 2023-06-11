(ns cellar.core
  (:require [cellar.config :as cfg]
            [clojure.string :as str]
            [aloop.core :refer :all]
            [clojure.java.jdbc :as jdbc]))

(declare -to -from -transform
         -table -init-table
         -select -where-clause)

(defn deftable
  [name & specs]
  (let [new-table (-table specs {})]
    (cfg/-table> {(keyword name) new-table})))

(defn select
  "Returns records related to `table` using `kvs` as filter query part"
  ([table kvs]
   (select table kvs nil))
  ([table kvs limit]
   (select table kvs limit nil))
  ([table kvs limit order-map]
   (->> kvs
        (mapv val)
        (reduce (fn [acc x] (if (keyword? x)
                              acc
                              (conj acc x))) [])
        (cons (-select table kvs limit order-map))
        (jdbc/query (cfg/<db-settings-))
        (map (comp #(into {} %)
                   #(-transform -from %))))))

(defn select-first
  ([table kvs]
   (select-first table kvs nil))
  ([table kvs limit]
   (select-first table kvs limit nil))
  ([table kvs limit order-by]
   (first (select table kvs limit order-by))))

(defn exists?
  [table kvs]
  (-> table
      (select kvs 1)
      (seq)
      (some?)))

(defn insert!
  [table kvs]
  (->> kvs
       (-transform -to)
       (into {})
       (jdbc/insert! (cfg/<db-settings-) (-to table))))

(defn insert!!
  [table kvs]
  (with-> table
          (insert! kvs)
          (select kvs)))

(defn update!
  [table kvs where]
  (->> where
       (mapv val)
       (cons (-where-clause where))
       (jdbc/update! (cfg/<db-settings-) (-to table) (into {} (-transform -to kvs)))))

(defn update!!
  [table kvs where]
  (with-> table
          (update! kvs where)
          (select (merge kvs where))))

(defn delete!
  [table where]
  (->> where
       (mapv val)
       (cons (-where-clause where))
       (jdbc/delete! (cfg/<db-settings-) (-to table))))

(defn delete!!
  [table where]
  (delete! table where)
  (vector))

(defn init
  ([] (let [tables (cfg/<tables-)]
        (doseq [table tables]
          (init (first table)))))
  ([table] (->> (cfg/<table- table)
                (-init-table table))))

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

(defn- -varchar
  [spec]
  (let [length (apply str (drop 7 spec))]
    (if (str/blank? length)
      "varchar"
      (str "varchar(" length ")"))))

(defn- -keyword-spec
  [spec]
  (str/join " " (str/split spec #"-")))

(defn -type-variant
  [spec]
  (condp (mix 0 1 str/starts-with?) spec
    "varchar" (-varchar spec)
    (-keyword-spec spec)))

(defn- -type
  [t]
  (if+> (keyword? t)
        name
        str/lower-case
        -type-variant))

(defn- -spec-map
  [spec]
  (mapv (fn [[k v]]
          (if (coll? v)
            (into [] (add->seq (mapv -type v) k))
            [k (-type v)])) spec))

(defn- -table
  ([spec]
   (cond
     (keyword? spec) (if (= :conditional! spec)
                       {:conditional? true}
                       {spec true})
     (vector? spec) {:columns spec}
     (map? spec) {:columns (-spec-map spec)}
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

(defn- -on
  [on]
  (str/join ", " (map #(format "%s = %s" (name (key %)) (name (val %))) on)))

(defn- -table-name
  [table]
  (cond
    (map? table) (let [table (first table)] (str (-to (key table)) " AS " (-to (val table))))
    (coll? table) (str/join ", " (map -table-name table))
    :else (-to table)))

(defn- -select
  [table kvs limit order-map]
  (let [table (-table-name table)
        [where on] (reduce
                     (fn [[where on] x]
                       (if (keyword? (val x))
                         [where (merge on x)]
                         [(merge where x) on]))
                     [{} {}] kvs)]
    (format "SELECT * FROM %s WHERE %s"
            table
            (cond-> (-where-clause where)
                    (not-empty on) (switch->> str (-on on) " AND ")
                    limit (str " LIMIT " limit)
                    order-map (str " ORDER BY "
                                   (str (-> order-map first key -to)
                                        " "
                                        (if (-> order-map first val name str/upper-case (= "ASC"))
                                          "ASC" "DESC")))))))
