(ns bugsnag-client.core-test
  (:use expectations)
  (:require
   [bugsnag-client.core :as bugsnag]
   [org.httpkit.client :as http-client]
   [cheshire.core :as json]
   [ring.mock.request :as mock]
   [org.httpkit.fake :as fake]))

(defn trigger-exception []
  (try
    (.foo nil)
    (catch Exception e
      e)))

(defn erroring-handler [request]
  (throw (Exception. "Boom!")))

(def bugsnag-config
  {:api-key "770c0307e1a8949161ba0a8c904ebd6d"
   :release-stages ["staging" "production"]
   :release-stage "production"})

;;Expect that the bugsnag-ring-handler gets notified when an exception happens
(expect (more-of [[request-info config e]]
                 {:server-port 80
                  :server-name "localhost"
                  :remote-addr "localhost"
                  :uri "/"
                  :query-string nil
                  :scheme :http
                  :request-method :get
                  :headers {"host" "localhost"}} request-info
                  {:api-key "770c0307e1a8949161ba0a8c904ebd6d"} (in config)
                  Exception e)
        (side-effects [bugsnag/notify-bugsnag]
                      (try
                        (let [wrapped-handler (bugsnag/wrap-bugsnag erroring-handler bugsnag-config)]
                          (wrapped-handler (mock/request :get "/")))
                        (catch Exception e))))

;;Expect to rethrow exception after sending request to bugsnag
(expect Exception
        (do
          (let [wrapped-handler (bugsnag/wrap-bugsnag erroring-handler bugsnag-config)]
            (wrapped-handler (mock/request :get "/")))))

;;Expect to send a JSON post request to bugsnag with the exception information
(expect (more-of exception-map
                 {"apiKey" "770c0307e1a8949161ba0a8c904ebd6d"
                  "notifier" {"name" "bugsnag-client"
                              "version" "0.0.1"
                              "url" "https://github.com/omartell/bugsnag-client"}
                  "severity" "error"
                  "app" {"releaseStage" "production"}
                  "device" {"hostname" "sledgehammer.local"}} (in (-> exception-map
                                                                      first
                                                                      last
                                                                      :body
                                                                      json/parse-string)))
        (side-effects [http-client/post]
                      (bugsnag/notify-bugsnag (mock/request :get "/")
                                              bugsnag-config
                                              (trigger-exception))))

