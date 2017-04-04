(ns copa-services.core
  (:require [cljs-lambda.macros :refer-macros [defgateway]]
            [cljs.nodejs :as nodejs]))

(def query-string (nodejs/require "querystring"))

(def http (nodejs/require "request-promise"))

(defonce copa-firebase-endpoint
  "https://copa-services-storage.firebaseio.com")

(defonce outgoing-messages
         {:step-0 "Thank you for signing up to receive COPA’s text messages! To provide you the correct information, please provide your preferred language. Reply “1” for English or “2” for Spanish. \n\n¡Gracias por inscribirse a los mensajes de COPA! Para darle la información correcta, por favor designe su idioma preferido. Responda “1” para inglés o “2” para español."
          :step-1 {:es-US "¡Gracias! Su idioma preferido es español. Y para comunicarnos mejor, responda con su nombre completo."
                   :en-US "Thank you! Your preferred language is English. To best communicate with you, please respond with your full name."}})

(defgateway echo [{:keys [body] :as input} ctx]
            (let [twilio (nodejs/require "twilio")
                  twiml (twilio.TwimlResponse.)
                  parsed-query-str (query-string.parse body)
                  sms-body (aget parsed-query-str "Body")
                  sms-from (aget parsed-query-str "From")
                  url (str copa-firebase-endpoint "/incoming/" sms-from ".json")
                  get-request {:url    url
                               :method "GET"
                               :json   true}
                  put-request {:url    url
                               :method "PUT"
                               :json   true}]

              ;; step-0
              ;; set receive preference

              (-> (http (clj->js get-request))
                  (.then
                    (fn [user]
                      (js/console.log user)

                      ;; if no data then first time user
                      ;; create user record in firebase db

                      (if-not user

                        (do

                          ;; put user at step-0
                          ;; write to firebase

                          (-> (http (clj->js (assoc put-request
                                               :body {:step-0 (clojure.string/upper-case sms-body)})))

                              (.then (fn [_]
                                       ;; reply via sms
                                       {:status  200
                                        :headers {:content-type "text/xml"}
                                        :body    (-> twiml
                                                     (.message (:step-0 outgoing-messages))
                                                     (.toString))}))))

                        ;; step-1
                        ;; set lang preference

                        (do
                          (let [step-0-answer (aget user "step-0")]
                            (if (= step-0-answer "YES")
                              (do
                                (-> (http (clj->js (assoc put-request
                                                     :body {:step-1 (clojure.string/upper-case sms-body)})))
                                    (.then (fn [_]
                                             {:status  200
                                              :headers {:content-type "text/xml"}
                                              :body    (-> twiml
                                                           (.message (-> outgoing-messages :step-1 :en-US))
                                                           (.toString))})))))))))))))