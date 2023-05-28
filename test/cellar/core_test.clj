(ns cellar.core-test
  (:require [clojure.test :refer :all]
            [cellar.core :refer :all]))

(deftest set-config!-test
  (testing "setting config to valid config"
    (let [new-config {:tables      {:abc {}}
                      :db-settings {}
                      :queries     {}}]
      (set-config! new-config)
      (is (= new-config *config*)))))
