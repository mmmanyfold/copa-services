(defproject copa-services "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure       "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [io.nervous/cljs-lambda    "0.3.5"]
                 [org.clojure/core.async    "0.2.395"]
                 [jamesmacaulay/cljs-promises "0.1.0"]]
  :plugins [[lein-npm                    "0.6.2"]
            [io.nervous/lein-cljs-lambda "0.6.5"]]
  :npm {:dependencies [[serverless-cljs-plugin "0.1.2"]
                       [source-map-support "0.4.0"]
                       [twilio "2.11.1"]
                       [request "2.81.0"]
                       [request-promise "4.2.0"]
                       [json2csv "3.7.3"]
                       [mailgun-js "0.10.1"]]}
  :cljs-lambda {:compiler
                {:inputs  ["src"]
                 :options {:output-to     "target/copa-services/copa_services.js"
                           :output-dir    "target/copa-services"
                           :target        :nodejs
                           :language-in   :ecmascript5
                           :optimizations :none}}})
