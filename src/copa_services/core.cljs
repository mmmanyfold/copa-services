(ns copa-services.core
  (:require [cljs-lambda.macros :refer-macros [defgateway]]
            [cljs.nodejs :as nodejs]))

(def twilio (nodejs/require "twilio"))

(def twiml (twilio.TwimlResponse.))

(defgateway echo [event ctx]
  {:status  200
   :headers {:content-type "text/xml"}
   :body    (-> twiml
                (.message "hola camarada!")
                (.toString))})
