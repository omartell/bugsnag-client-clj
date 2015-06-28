(ns bugsnag-client.ring-integration-test
  (:use
   (compojure [core :only [defroutes GET POST HEAD DELETE ANY context]]
              [handler :only [site]]))
  (:require
   [environ.core :refer [env]]
   [clojure.test :refer :all]
   [clojure.pprint :refer :all]
   [org.httpkit.server :as http-server]
   [org.httpkit.client :as http-client]
   [ring.middleware.params :as params]
   [ring.middleware.session :as session]
   [clojure.data.json :as json]
   [bugsnag-client.core :as bugsnag]))

(def bugsnag-errors-url
  (env :bugsnag-errors-url))

(defroutes app-routes
  (GET "/boom" []
       (throw
        (ex-info "Kaboom!"
                 {:causes #{:fridge-door-open :dangerously-high-temperature}
                  :current-temperature {:value 25 :unit :celcius}})))
  (POST "/crash" []
        (throw
         (ex-info "Crash!"
                  {:causes #{:fridge-door-open :dangerously-high-temperature}
                   :current-temperature {:value 25 :unit :celcius}}))))

(def bugsnag-config
  {:api-key (env :bugsnag-api-key)
   :notify-release-stages ["staging" "production"]
   :release-stage "production"})

(defn hoover-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e))))

(def app
  (-> app-routes
      (bugsnag/wrap-bugsnag bugsnag-config)
      params/wrap-params
      session/wrap-session
      hoover-exceptions))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (@server)
    (reset! server nil)))

(defn start-server []
  (when (nil? @server)
    (reset! server (http-server/run-server app {:port 4347}))))

(deftest notify-bugsnag-on-exception-on-get-request
  (start-server)
  (let [application-response @(http-client/get "http://localhost:4347/boom")]
    (is (= (:status application-response) 404)))
  (let [errors-in-bugsnag (-> @(http-client/get bugsnag-errors-url)
                              :body
                              (json/read-str :key-fn keyword))]
    (is (some (fn [e] (= (:last_message e) "Kaboom!")) errors-in-bugsnag)))
  (stop-server))

(deftest notify-bugsnag-on-exception-on-post-request
  (start-server)
  (let [application-response @(http-client/post "http://localhost:4347/crash"
                                                {:form-params {:foo 1
                                                               :bar ["async" "client" "server"]}})]
    (is (= (:status application-response) 404)))
  (let [errors-in-bugsnag (-> @(http-client/get bugsnag-errors-url)
                              :body
                              (json/read-str :key-fn keyword))]
    (is (some (fn [e] (= (:last_message e) "Crash!")) errors-in-bugsnag)))
  (stop-server))
