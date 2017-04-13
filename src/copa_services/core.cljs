(ns copa-services.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cljs-lambda.macros :refer-macros [defgateway]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(def query-string (nodejs/require "querystring"))

(set! js/XMLHttpRequest (nodejs/require "xhr2")) ;; XMLHttpRequest not present in node,
                                                 ;; therefore we need to load this xhr emulator

(def twilio (nodejs/require "twilio"))

(defonce copa-firebase-endpoint "https://copa-services-storage.firebaseio.com")

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

(defn make-url [from] (str copa-firebase-endpoint "/incoming/" from ".json"))

;; TODO: upcase ppl's name, grown up case
;; no yelling!

(defgateway echo [{:keys [body] :as input} ctx]
            (let [twiml (twilio.TwimlResponse.)
                  query-string (query-string.parse body)
                  sms-date (aget query-string "DateSent")
                  sms-body (aget query-string "Body")
                  sms-from (re-find #"\d+" (aget query-string "From"))
                  url (make-url sms-from)
                  body (clojure.string/upper-case sms-body)]


              (go (let [response (<! (http/get url))
                        user (:body response)]

                    (js/console.log (clj->js response))
                    (js/console.log (type user))

                    (if (nil? (js->clj user))
                      ;; @eemishi this works^ for checking null
                      ;; now its just a matter of porting the
                      ;; rest of the branching logic to
                      ;; this inner expression
                      (js/console.log "got here 1"))))))
