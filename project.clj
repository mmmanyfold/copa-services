(defproject copa-services "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure       "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [io.nervous/cljs-lambda    "0.3.5"]]
  :plugins [[lein-npm                    "0.6.2"]
            [io.nervous/lein-cljs-lambda "0.6.5"]]
  :cljs-lambda {:compiler
                {:inputs  ["src"]
                 :options {:output-to     "target/copa-services/copa_services.js"
                           :output-dir    "target/copa-services"
                           :target        :nodejs
                           :language-in   :ecmascript5
                           :optimizations :none}}})
