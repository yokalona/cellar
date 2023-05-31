(defproject io.github.yokalona/cellar "0.1.3-SNAPSHOT"
  :description "Cellar: smallest ORM possible"
  :url "https://github.com/yokalona/cellar"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [io.github.yokalona/aloop "0.1.0-SNAPSHOT"]]
  :repl-options {:init-ns cellar.core})
