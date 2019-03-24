(ns jepsen.kafka-test
  (:require [clojure.test :refer :all]
            [jepsen.core :as jepsen]
            [jepsen.kafka :as kafka]))

(deftest kafka-test
   (is  (:valid?  (:results  (jepsen/run!  (kafka/zookeeper-test-base "2.12" "0.10.2.0"))))))
