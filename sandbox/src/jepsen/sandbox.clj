(ns jepsen.sandbox
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [cli :as cli]
             [client :as client]
             [control :as c]
             [db :as db]
             [generator :as gen]
             [tests :as tests]]
            [jepsen.control.util :as cu]
            [jepsen.os.debian :as debian]))

(def client1 "t1")
(def client2 "t2")

(defn- first-n?
  [test node n]
  (some #{node} (take n (sort (:nodes test)))))

(defn- which-client
  [test node n]
  (if (first-n? test node n)
    client1
    client2))

(defn- w1 [test process] {:type :invoke, :f :write, :value 1})
(defn- w2 [test process] {:type :invoke, :f :write, :value 2})
(defn- enq [test process] {:type :invoke, :f :enqueue, :value 42})

(defn- client
  ""
  [node type]
  (reify client/Client
    (open! [_ test node]
      (let [type (which-client test node (:n test))]
        (info node "open" type)
        (client node type)
        )
      )

    (setup! [_ test]
      (info node "setup" type))

    (invoke! [_ test op]
      (assoc op :type :ok))

    (teardown! [_ test]
      (info node "teardown" type)
      )

    (close! [_ test]
      (info node "close" type))))

(defn- db
  ""
  [n v1 v2]
  (reify db/DB
    (setup! [_ test node]
      (if (first-n? test node n)
        (info node "installing" v1)
        (info node "installing" v2)))

    (teardown! [_ test node]
      (if (first-n? test node n)
        (info node "tearing" v1)
        (info node "tearing" v2))
      )))

(defn- sandbox-test
  ""
  [opts]
  (merge tests/noop-test
         opts
         {
          :n         2
          ;:os        debian/os
          :db        (db 2 "1.0" "2.0")
          :client    (client nil nil)
          :generator (->> (gen/concat (gen/on #{1 2} w1) (gen/on #{3 4} w2) (gen/on #{5} enq))
                          (gen/limit 8))}))

(defn -main
  ""
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn sandbox-test})
                   (cli/serve-cmd))
            args))
