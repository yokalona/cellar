# cellar

Micro ORM for small apps, that doesn't require fancy, heavy DB related work.
Takes care of creating tables and basic CRUD queries.
Additionally converts db names to clojure name and vice versa, like so:

> domain-id

will become

> domain_id
 
### How to? (short version)

[cellar]

```clojure
(ns example
  (:require
    [cellar.core :as db]
    [cellar.config :as cfg]))

;; providing db settings
(cfg/-db-settings>
  {:classname   "org.h2.Driver"
   :subprotocol "h2:mem"
   :subname     "demo;DB_CLOSE_DELAY=-1"
   :user        "sa"
   :password    ""})

;; creating a new table
(db/deftable cellar-shelves
             :conditional!
             [[:id "BIGINT"]
              [:shelve-height "BIGINT"]
              [:shelve-content "VARCHAR(64)"]])

;; inserting new record
(db/insert! :cellar-shelves
            {:id             1
             :shelve-height  2
             :shelve-content "One small lib"})
;; => {:id 1 :shelve-height 2 :shelve-content "One small lib"}

;; let's check if new record exists
(db/exists? :cellar-shelves {:id 1})
;; => true ; indeed, it was just created

(db/update! :cellar-shelves
            {:shelve-height 3}
            {:id 1})
;; => 1 ; only one record was updated

(db/delete! :cellar-shelves
            {:id 1})
;; => 1 ; only one record was deleted
```

### why?
There are plenty of ORM's out there, but mostly they are much larger. 
For small, close to pet-projects, it is overkill to use anything larger, than this ORM.

Manly it is designed and created for my private pet-projects.

### What's the deal with `-foo>` and `<foo-`
Code notations of this project:

* `-foo>` - alias for `set foo`
* `<foo-` - alias for `get foo`
* `-foo`  - private function

###### underscore is never used!

### Future plans
1. Add table specific columns and values transformers
2. Batch insert/delete
3. Fields emitting fn's