(ns copa-services.core
  (:require [cljs-lambda.macros :refer-macros [defgateway]]
            [cljs.nodejs :as nodejs]))

(def twilio (nodejs/require "twilio"))

(def request (nodejs/require "request"))

(def query-string (nodejs/require "querystring"))

(def twiml (twilio.TwimlResponse.))

(defonce copa-firebase-endpoint
         "https://copa-services-storage.firebaseio.com/members/onboard.json")

(defonce request-opts {:uri    copa-firebase-endpoint
                       :method "PUT"
                       :json   {:foo "bar"}})

(defgateway echo [{:keys [body] :as input} ctx]
            (let [parsed-query-str (query-string.parse body)
                  sms-body (aget parsed-query-str "Body")
                  sms-from (aget parsed-query-str "From")]
              (js/console.log (str sms-from ":" sms-body))
              (request (clj->js request-opts)
                       (fn [err res res-body]
                         {:status  200
                          :headers {:content-type "text/xml"}
                          :body    (-> twiml
                                       (.message (str sms-from ":" sms-body))
                                       (.toString))}))))
