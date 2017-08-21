(ns sms-onboard.outgoing
  (:require [sms-onboard.helpers :refer [get-env format]]))

(def ORG_NAME (get-env "ORG_NAME"))

(def messages
  {:retry  "Not a valid response. Please try again. \n\nRespuesta incorrecta, por favor vuelva a intentar."
   :step-1 "Please reply \"1\" for English or \"2\" for Spanish. \n\nPor favor responda \"1\" para inglés o \"2\" para español."
   :step-2 {:spanish "¡Gracias! Y para comunicarnos mejor, responda con su nombre completo."
            :english "Thank you! To best communicate with you, please respond with your full name."}
   :step-3 {:spanish (format "¡Gracias! ¡Este pendiente de la información de %s!" ORG_NAME)
            :english (format "Thank you! Stay tuned for updates from %s!" ORG_NAME)}})
