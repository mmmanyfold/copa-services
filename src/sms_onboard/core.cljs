(ns sms-onboard.core
  (:require [cljs-lambda.macros :refer-macros [defgateway]]
            [cljs.core.async :as async]
            [sms-onboard.outgoing :as outgoing]
            [cljs.nodejs :as nodejs]
            [sms-onboard.helpers :refer [get-env]]))

(def query-string (nodejs/require "querystring"))

(def http (nodejs/require "request-promise"))

(def json2csv (nodejs/require "json2csv"))

(def mailgun-js (nodejs/require "mailgun-js"))

(def MAILGUN_KEY (get-env "MAILGUN_KEY"))

(def DOMAIN (get-env "DOMAIN"))

(def EMAIL_FROM (get-env "EMAIL_FROM"))

(def EMAIL_TO (get-env "EMAIL_TO"))

(def FIREBASE_ENDPOINT (get-env "FIREBASE_ENDPOINT"))

(def EXPORT_FREQUENCY (get-env "EXPORT_FREQUENCY"))

(def ORG_NAME (.toLowerCase (get-env "ORG_NAME")))

(defn make-sms [twiml msg]
  {:status  200
   :headers {:content-type "text/xml"}
   :body    (-> twiml
                (.message msg)
                (.toString))})

(def FINAL-USER-PROPS #{:name :lang :status :timestamp})

(defn GET [url]
  (http (clj->js {:url url :method "GET" :json true})))

(defn PUT
  [url body]
  (let [opts {:url url :method "PUT" :json true}]
    (http (clj->js (assoc opts :body body)))))

(defn includes? [term ctx] (boolean (re-find (re-pattern term) ctx)))

(defgateway sms [{:keys [body] :as input} ctx]
            (let [twilio (nodejs/require "twilio")
                  twiml (twilio.TwimlResponse.)
                  now (.now js/Date)
                  parsed-query-str (query-string.parse body)
                  sms-body (aget parsed-query-str "Body")
                  sms-from (re-find #"\d+" (aget parsed-query-str "From"))
                  url (str FIREBASE_ENDPOINT "/" ORG_NAME "/" sms-from ".json")
                  body (clojure.string/upper-case sms-body)]

              (-> (GET url)
                  (.then (fn [user]
                           (if (nil? user)

                             ;; firabase has no record
                             (if (includes? "YES" body)
                               (-> (PUT url {:status "incomplete" :timestamp now})
                                   (.then #(make-sms twiml (:step-1 outgoing/messages))))
                               (make-sms twiml (:retry outgoing/messages)))

                             ;; firebase has record
                             (let [user-map (js->clj user :keywordize-keys true)
                                   user-props ((comp set keys) user-map)]

                               (cond
                                 (includes? "STOP" body)
                                 (-> (PUT url (assoc user-map :status "REMOVE"))
                                     (.then #(println "User removed.")))

                                 (includes? "START" body)
                                 (let [user-map (assoc user-map :status "new")]
                                   (if (not= user-props #{:name :lang :status})
                                     (if-let [lang (:lang user-map)]
                                       ;; ask name
                                       (-> (PUT url user-map)
                                           (.then #(make-sms twiml (get-in outgoing/messages
                                                                           [:step-2 (keyword lang)]))))
                                       ;; ask lang
                                       (-> (PUT url user-map)
                                           (.then #(make-sms twiml (:step-1 outgoing/messages)))))
                                     (-> (PUT url user-map)
                                         (.then #(println "User opted back in.")))))

                                 :else
                                 (when (and (not= (:status user-map) "REMOVE")
                                            (not= user-props #{:name :lang :status}))
                                   (if-let [lang (:lang user-map)]
                                     ;; store name
                                     ;; guard against overwriting last user-prop :name
                                     (when-not (= user-props FINAL-USER-PROPS)
                                       (-> (PUT url (assoc user-map :name body :status "complete"))
                                           (.then #(make-sms twiml (get-in outgoing/messages
                                                                           [:step-3 (keyword (:lang user-map))])))))
                                     ;; store lang
                                     (let [lang (cond
                                                  (and (includes? "1" body) (nil? (re-find #"2" body))) "english"
                                                  (and (includes? "2" body) (nil? (re-find #"1" body))) "spanish"
                                                  :else :bail)]
                                       (if-not (= lang :bail)
                                         (-> (PUT url (assoc user-map :lang lang))
                                             (.then #(make-sms twiml (get-in outgoing/messages
                                                                             [:step-2 (keyword lang)]))))
                                         (make-sms twiml (:retry outgoing/messages))))))))))))))

(defn update-status [user-records filtered]
  (let [records-to-update (map :number filtered)
        url (str FIREBASE_ENDPOINT "/" ORG_NAME ".json")
        clj->users (js->clj user-records :keywordize-keys true)
        final-records-update
        (->> (map (fn [[k v]]
                    (if (some #(= k %) records-to-update)
                      (hash-map k (update v :status (constantly "exported")))
                      (hash-map k v))) clj->users)
             (into {}))]
    (-> (PUT url final-records-update)
        (.then #(println "User records updated.")))))

(defn older-than-duration?
  [records now duration]
  (filter (fn [{:keys [timestamp]}]
            (let [diff (- now timestamp)]
              (> diff duration)))
          records))

(defgateway email [{:keys [body] :as input} ctx]
            (let [fields [:status :name :lang :number]
                  today (.now js/Date)
                  ;; in millis
                  one-week 604800000
                  url (str FIREBASE_ENDPOINT "/" ORG_NAME ".json")
                  mailgun (mailgun-js (clj->js {:apiKey MAILGUN_KEY
                                                :domain DOMAIN}))]
              (-> (GET url)
                  (.then
                    (fn [json-user-records]
                      ;; normalize users map
                      (let [users (js->clj json-user-records :keywordize-keys true)
                            k (keys users)
                            v (vals users)
                            flat-users (map #(assoc %1 :number %2) v k)

                            incomplete-records (filter #(= (% :status) "incomplete") flat-users)
                            complete-records (filter #(= (% :status) "complete") flat-users)
                            old-incomplete-records (older-than-duration? incomplete-records today one-week)
                            all-records (concat complete-records old-incomplete-records)]

                        (let [csv (json2csv (clj->js {:data   all-records
                                                      :fields fields}))
                              attch (.-Attachment mailgun)
                              attachment (attch. (clj->js {:data     (js/Buffer. csv)
                                                           :filename "members.csv"}))
                              new-member-count (count all-records)
                              data {:from       EMAIL_FROM
                                    :to         EMAIL_TO
                                    :subject    (str "Hello, " new-member-count " new members today.")
                                    :text       (str "Exported: " EXPORT_FREQUENCY ".")
                                    :attachment attachment}]
                          (if (>= new-member-count 1)
                            (-> mailgun
                                (.messages)
                                (.send (clj->js data)
                                       (fn [err body]
                                         (when-not err
                                           (update-status json-user-records all-records)))))
                            (-> mailgun
                                (.messages)
                                (.send (clj->js (dissoc data :attachment))
                                       #(println "No new members today.")))))))))))
