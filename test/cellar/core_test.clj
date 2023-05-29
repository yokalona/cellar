(ns cellar.core-test
  (:require [cellar.config :as cfg]
            [clojure.test :refer :all]))

(defn reset-fixture [f]
  (cfg/reset!)
  (f))

(use-fixtures :each reset-fixture)
