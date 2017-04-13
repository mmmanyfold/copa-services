(ns copa-services.core
  (:require [cljs-lambda.macros :refer-macros [defgateway]]
            [cljs.core.async :as async]
            [cljs.nodejs :as nodejs])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def query-string (nodejs/require "querystring"))

(def http (nodejs/require "request-promise"))

(defonce copa-firebase-endpoint
         "https://copa-services-storage.firebaseio.com")

(defonce outgoing-messages
         {:step-0 "Welcome to COPA's text alerts and news! To subscribe to receive important updates reply \"yes.\" Do not respond if you do not want to be added at this time.\n\nBienvenid@ a los alertos de texto de COPA! Para inscribirse a recibir información importante, responda \"yes.\" No necesita responder si no quiere recibir mensajes por el momento."
          :step-1 "Thank you for signing up to receive COPA’s text messages! To provide you the correct information, please provide your preferred language. Reply “1” for English or “2” for Spanish. \n\n¡Gracias por inscribirse a los mensajes de COPA! Para darle la información correcta, por favor designe su idioma preferido. Responda “1” para inglés o “2” para español."
          :step-2 {:es-US "¡Gracias! Su idioma preferido es español. Y para comunicarnos mejor, responda con su nombre completo."
                   :en-US "Thank you! Your preferred language is English. To best communicate with you, please respond with your full name."}
          :step-3 {:es-US "¡Gracias! ¡Este pendiente de la información de COPA!"
                   :en-US "Thank you! Stay tuned for updates from COPA!"}})

(defn make-sms [twiml msg]
  {:status  200
   :headers {:content-type "text/xml"}
   :body    (-> twiml
                (.message msg)
                (.toString))})

(defn get-user [url]
  (http (clj->js {:url url :method "GET" :json true})))


(defn update-user-record
  "creates or updates user record in firebase"
  [url body]
  (http (clj->js {:url url :method "PUT" :json true :body body})))


;; TODO: upcase ppl's name, grown up case
;; no yelling!

(defgateway echo [{:keys [body] :as input} ctx]
            (let [twilio (nodejs/require "twilio")
                  twiml (twilio.TwimlResponse.)
                  parsed-query-str (query-string.parse body)
                  sms-date (aget parsed-query-str "DateSent")
                  sms-body (aget parsed-query-str "Body")
                  sms-from (re-find #"\d+" (aget parsed-query-str "From"))
                  url (str copa-firebase-endpoint "/incoming/" sms-from ".json")
                  body (clojure.string/upper-case sms-body)]

              (js/console.log sms-from)
              (js/console.log body)

              (-> (get-user url)
                  (.then (fn [user]
                          (js/console.log user)
                          (if (nil? (js->clj user))
                            ;; firabase has no record
                            (if (= body "YES")
                              (-> (update-user-record url {:created sms-date})
                                  (.then #(make-sms twiml (:step-1 outgoing-messages))))
                              (make-sms twiml (:step-0 outgoing-messages)))
                            ;; firebase has record
                            (let [_user (clj->js user)]

                              (if-let [lang (:lang _user)]
                                ;; store name
                                (-> (update-user-record url (assoc _user :name body))
                                    (.then #(make-sms twiml (get-in outgoing-messages [:step-3 (keyword lang)]))))
                                ;; store lang
                                (let [lang (cond
                                            (= body "1") "en-US"
                                            (= body "2") "es-US"
                                            :else "bail")]
                                  (if-not (= lang "bail")
                                    (-> (update-user-record url (assoc _user :lang lang))
                                        (.then #(make-sms twiml (get-in outgoing-messages [:step-2 (keyword lang)]))))
                                    (make-sms twiml (:step-1 outgoing-messages))))))))))))
