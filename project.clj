(defproject sms-onboard "0.1.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [io.nervous/cljs-lambda "0.3.5"]
                 [org.clojure/core.async "0.2.395"]
                 [jamesmacaulay/cljs-promises "0.1.0"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-npm "0.6.2"]
            [lein-doo "0.1.7"]
            [io.nervous/lein-cljs-lambda "0.6.6"]]
  :npm {:dependencies [[serverless-cljs-plugin "0.1.2"]
                       [source-map-support "0.4.0"]
                       [twilio "2.11.1"]
                       [request "2.81.0"]
                       [request-promise "4.2.0"]
                       [json2csv "3.7.3"]
                       [mailgun-js "0.10.1"]
                       [source-map-support "0.4.0"]]}
  :cljs-lambda {:compiler
                {:inputs  ["src"]
                 :options {:output-to     "target/sms-onboard/sms_onboard.js"
                           :output-dir    "target/sms-onboard"
                           :target        :nodejs
                           :language-in   :ecmascript5
                           :optimizations :none}}}
  :cljsbuild
  {:builds [{:id "sms-onboard"
             :source-paths ["src"]
             :compiler {:output-to     "target/sms-onboard/sms_onboard.js"
                        :output-dir    "target/sms-onboard"
                        :source-map    true
                        :target        :nodejs
                        :language-in   :ecmascript5
                        :optimizations :none}}
            {:id "sms-onboard-test"
             :source-paths ["src" "test"]
             :compiler {:output-to     "target/sms-onboard-test/sms_onboard.js"
                        :output-dir    "target/sms-onboard-test"
                        :target        :nodejs
                        :language-in   :ecmascript5
                        :optimizations :none
                        :main          sms-onboard.test-runner}}]})
