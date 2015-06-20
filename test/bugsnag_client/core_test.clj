(ns bugsnag-client.core-test
  (:use expectations)
  (:require
   [bugsnag-client.core :as bugsnag]
   [clj-http.client :as http-client]
   [cheshire.core :as json]
   [clojure.pprint :as pprint]
   [ring.mock.request :as mock]))

(defn trigger-exception []
  (try
    (.foo nil)
    (catch Exception e
      e)))

(defn erroring-handler [request]
  (throw (ex-info "Boom!" {})))

(def bugsnag-config
  {:api-key "770c0307e1a8949161ba0a8c904ebd6d"
   :notify-release-stages ["staging" "production"]
   :release-stage "production"})

;;The bugsnag-ring-handler gets notified when an exception happens
(expect (more-of [[e config request-info]]
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
        (side-effects [bugsnag/report-web-exception]
                      (try
                        (let [wrapped-handler (bugsnag/wrap-bugsnag erroring-handler bugsnag-config)]
                          (wrapped-handler (mock/request :get "/")))
                        (catch Exception e))))

;;Rethrow exception after sending request to bugsnag
(expect Exception
        (do
          (let [wrapped-handler (bugsnag/wrap-bugsnag erroring-handler bugsnag-config)]
            (wrapped-handler (mock/request :get "/")))))

;;Send a JSON post request to bugsnag with the exception information
(expect (more-of [[exception-map]]
                 {:apiKey "770c0307e1a8949161ba0a8c904ebd6d"
                  :notifier {:name "bugsnag-client"
                             :version "0.0.1"
                             :url "https://github.com/omartell/bugsnag-client"}} (in exception-map)
                  {:severity "error"
                   :app {:releaseStage "production"}
                   :device {:hostname "sledgehammer.local"}} (in (-> exception-map :events first)))
        (side-effects [bugsnag/post-json-to-bugsnag]
                      (bugsnag/report (trigger-exception) bugsnag-config)))

;;It does not send exception to bugsnag if release stage is not in notify release stages
(expect []
        (side-effects [bugsnag/post-json-to-bugsnag]
                      (with-redefs [clj-http.client/post
                                    (fn []
                                      (throw (ex-info "Bugsnag should not be notified!" {:cause "foo"})))]
                        (bugsnag/report (trigger-exception)
                                        {:api-key "770c0307e1a8949161ba0a8c904ebd6d"
                                         :notify-release-stages ["production"]
                                         :release-stage "staging"}))))
