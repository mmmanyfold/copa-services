(ns sms-onboard.helpers
  (:require [cljs.nodejs :as nodejs]
            [goog.string :as gstring]
            [goog.string.format]))

(defn get-env
  "Retrieve environment variable from process.env"
  [var]
  (-> nodejs/process
      .-env
      (aget var)))

(defn format
  "Formats a string using goog.string.format."
  [fmt & args]
  (apply gstring/format fmt args))