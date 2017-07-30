(ns sms-onboard.test-runner
 (:require [doo.runner :refer-macros [doo-tests]]
           [sms-onboard.core-test]
           [cljs.nodejs :as nodejs]))

(try
  (.install (nodejs/require "source-map-support"))
  (catch :default _))

(doo-tests
 'sms-onboard.core-test)
