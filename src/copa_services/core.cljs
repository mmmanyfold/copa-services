(ns copa-services.core
  (:require [cljs-lambda.macros :refer-macros [defgateway]]
            [cljs.core.async :as async]
            [cljs.nodejs :as nodejs]))

(def query-string (nodejs/require "querystring"))

(def buffer (nodejs/require "buffer"))

(def http (nodejs/require "request-promise"))

(def json2csv (nodejs/require "json2csv"))

(def mailgun-js (nodejs/require "mailgun-js"))

(def MAILGUN_KEY (-> nodejs/process .-env .-MAILGUN_KEY))

(def DOMAIN (-> nodejs/process .-env .-DOMAIN))

(def EMAIL_FROM (-> nodejs/process .-env .-EMAIL_FROM))

(def EMAIL_TO (-> nodejs/process .-env .-EMAIL_TO))

(def mailgun (mailgun-js (clj->js {:apiKey MAILGUN_KEY
                                   :domain DOMAIN})))

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

(defn GET [url]
  (http (clj->js {:url url :method "GET" :json true})))

(defn PUT
  [url body]
  (let [opts {:url url :method "PUT" :json true}]
    (http (clj->js (assoc opts :body body)))))


;; TODO: upcase ppl's name, grown up case
;; no yelling!

(defgateway echo [{:keys [body] :as input} ctx]
            (let [twilio (nodejs/require "twilio")
                  twiml (twilio.TwimlResponse.)
                  parsed-query-str (query-string.parse body)
                  sms-body (aget parsed-query-str "Body")
                  sms-from (re-find #"\d+" (aget parsed-query-str "From"))
                  url (str copa-firebase-endpoint "/incoming/" sms-from ".json")
                  body (clojure.string/upper-case sms-body)]

              (-> (GET url)
                  (.then (fn [user]
                           (if (nil? user)
                             ;; firabase has no record
                             (if (= body "YES")
                               (-> (PUT url {:created (.now js/Date)})
                                   (.then #(make-sms twiml (:step-1 outgoing-messages))))
                               (make-sms twiml (:step-0 outgoing-messages)))
                             ;; firebase has record
                             (let [user-map (js->clj user :keywordize-keys true)
                                   user-props ((comp set keys) user-map)]
                               (when-not (= user-props #{:created :name :lang})
                                 (if-let [lang (:lang user-map)]
                                   ;; store name
                                   (-> (PUT url (assoc user-map :name body))
                                       (.then #(make-sms twiml (get-in outgoing-messages [:step-3 (keyword lang)]))))
                                   ;; store lang
                                   (let [lang (cond
                                                (= body "1") "en-US"
                                                (= body "2") "es-US"
                                                :else "bail")]
                                     (if-not (= lang "bail")
                                       (-> (PUT url (assoc user-map :lang lang))
                                           (.then #(make-sms twiml (get-in outgoing-messages [:step-2 (keyword lang)]))))
                                       (make-sms twiml (:step-1 outgoing-messages)))))))))))))


(defgateway email [{:keys [body] :as input} ctx]
            (let [fields [:created :name :lang :number]
                  url (str copa-firebase-endpoint "/incoming.json")]
              (-> (GET url)
                  (.then
                    (fn [users]

                      ;; normalize users map

                      (let [users (js->clj users :keywordize-keys true)
                            k (keys users)
                            v (vals users)
                            flat-users (map #(assoc %1 :number %2) v k)]

                        (let [csv (json2csv (clj->js {:data   flat-users
                                                      :fields fields}))
                              attch (.-Attachment mailgun)
                              attachment (attch. (clj->js {:data     (js/Buffer. csv)
                                                           :filename "members.csv"}))
                              data {:from       EMAIL_FROM
                                    :to         EMAIL_TO
                                    :subject    "new members"
                                    :text       "testing"
                                    :attachment attachment}]
                          (-> mailgun
                              (.messages)
                              (.send (clj->js data)
                                     (fn [err body]
                                       (if err
                                         err
                                         body)))))))))))