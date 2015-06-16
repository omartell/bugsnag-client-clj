(defproject bugsnag-client "0.1.0"
  :description "Track exceptions from your Clojure application in Bugsnag"
  :url "http://github.com/omartell/bugsnag-client"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.1.2"]
                 [cheshire "5.5.0"]
                 [clj-stacktrace "0.2.8"]]
  :profiles
  {:dev {:dependencies [[expectations "2.0.16"]
                        [clj-http "1.1.2"]
                        [ring/ring-jetty-adapter "1.3.2"]
                        [compojure "1.3.4"]
                        [http-kit "2.1.16"]
                        [http-kit.fake "0.2.1"]
                        [ring-mock "0.1.5"]]}})
