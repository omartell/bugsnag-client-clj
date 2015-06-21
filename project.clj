(defproject bugsnag-client "0.1.0"
  :description "Track exceptions from your Clojure application in Bugsnag"
  :url "http://github.com/omartell/bugsnag-client"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.1.2"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-stacktrace "0.2.8"]]
  :profiles
  {:dev {:dependencies [[expectations "2.0.16"]
                        [environ "1.0.0"]
                        [http-kit "2.1.16"]
                        [javax.servlet/servlet-api "2.5"]
                        [ring/ring-core "1.3.2"]
                        [compojure "1.3.4"]
                        [ring-mock "0.1.5"]]}})
