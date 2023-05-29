(ns cellar.config-test
  (:require [cellar.config :as cfg]
            [clojure.test :refer :all]))

(defn reset-fixture [f]
  (cfg/reset!)
  (f))

(use-fixtures :each reset-fixture)

(deftest <get--test
  (testing "Empty config"
    (is (contains? (cfg/<get-) :tables))
    (is (contains? (cfg/<get-) :db-settings))
    (is (empty? (:tables (cfg/<get-))))
    (is (empty? (:db-settings (cfg/<get-)))))

  (cfg/-set> {:tables      'test-tables
              :db-settings 'test-db-settings})

  (testing "Added to config values persists"
    (is (= (:tables (cfg/<get-)) 'test-tables))
    (is (= (:db-settings (cfg/<get-)) 'test-db-settings)))

  (cfg/reset!)

  (testing "Reset returns config to default"
    (is (contains? (cfg/<get-) :tables))
    (is (contains? (cfg/<get-) :db-settings))
    (is (empty? (:tables (cfg/<get-))))
    (is (empty? (:db-settings (cfg/<get-))))))

(deftest <table--test
  (testing "Non existing tables provide no configuration"
    (is (nil? (cfg/<table- :test-table))))

  (cfg/-table> {:test-table {}})

  (testing "Adding table to config makes it available"
    (is (some? (cfg/<table- :test-table)))
    (is (= 1 (count (cfg/<tables-))))
    (is (contains? (cfg/<tables-) :test-table)))

  (cfg/reset!)
  (cfg/-tables> {:test-table {}})

  (testing "Settings table config"
    (is (some? (cfg/<table- :test-table)))
    (is (= 1 (count (cfg/<tables-))))
    (is (contains? (cfg/<tables-) :test-table)))

  (cfg/reset!)
  (cfg/-table> {:test-table '1})
  (cfg/-table> {:test-table '2})

  (testing "Same tables overrides each other"
    (is (some? (cfg/<table- :test-table)))
    (is (= 1 (count (cfg/<tables-))))
    (is (contains? (cfg/<tables-) :test-table))
    (is (= '2 (cfg/<table- :test-table)))))

(deftest <db-settings--test
  (testing "Empty config have no db configuration"
    (is (empty? (cfg/<db-settings-))))

  (cfg/-db-settings> {:test-db "test-db"})

  (testing "Getting db config"
    (is (not-empty (cfg/<db-settings-)))
    (is (contains? (cfg/<db-settings-) :test-db))))
