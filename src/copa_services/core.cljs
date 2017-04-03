(ns copa-services.core
  (:require [cljs-lambda.macros :refer-macros [defgateway]]
            [cljs.nodejs :as nodejs]))

(def query-string (nodejs/require "querystring"))

(def http (nodejs/require "request-promise"))

(defonce copa-firebase-endpoint
  "https://copa-services-storage.firebaseio.com/members/new-welcome")

(defonce outgoing-messages
  {:step-0 "Thank you for signing up to receive COPA’s text messages! To provide you the correct information, please provide your preferred language. Reply “1” for English or “2” for Spanish. \n\n¡Gracias por inscribirse a los mensajes de COPA! Para darle la información correcta, por favor designe su idioma preferido. Responda “1” para inglés o “2” para español."})

(defgateway echo [{:keys [body] :as input} ctx]
            (let [twilio (nodejs/require "twilio")
                  twiml (twilio.TwimlResponse.)
                  parsed-query-str (query-string.parse body)
                  sms-body (aget parsed-query-str "Body")
                  sms-from (aget parsed-query-str "From")
                  opts-0 {:url    (str copa-firebase-endpoint "/incoming/" sms-from ".json")
                          :method "GET"}
                  opts-1 {:url    (str copa-firebase-endpoint "/incoming/" sms-from ".json")
                          :method "PUT"
                          :json   true
                          ;; TODO:
                          ;; - parse for YES vs *
                          :body   {:step-0 (clojure.string/upper-case sms-body)}}]

              ;; step-0

              (-> (http (clj->js opts-0))
                  (.then
                    (fn [user]
                      ;; if no data then first time user
                      ;; create user record in firebase db
                      (if-not user
                        (do
                          ;; put user at step-0
                          ;; write to firebase
                          (-> (http (clj->js opts-1))
                              (.then (fn [err res fb-body]
                                       ;; reply via sms
                                       {:status  200
                                        :headers {:content-type "text/xml"}
                                        :body    (-> twiml
                                                     (.message (:step-0 outgoing-messages))
                                                     (.toString))}))))
                        ;; if step-0 is yes, then set lang preference
                        (do
                          (js/console.log user))))))))
