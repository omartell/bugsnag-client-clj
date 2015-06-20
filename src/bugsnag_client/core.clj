(ns bugsnag-client.core
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clj-http.client :as http]
            [clj-stacktrace.core :as stacktrace]))

(def hostname
  (.getHostName (java.net.InetAddress/getLocalHost)))

(defn- post-json-to-bugsnag [payload]
  (http/post "https://notify.bugsnag.com"
             {:body (json/generate-string payload)
              :content-type :json
              :accept :json}))

(defn- notifications-enabled? [config]
  (let [notify-release-stages (into #{} (:notify-release-stages config))]
    (notify-release-stages (config :release-stage "production"))))

(defn report [exception config]
  (when (notifications-enabled? config)
    (let [parsed-exception (stacktrace/parse-exception exception)]
      (post-json-to-bugsnag {:apiKey (:api-key config)
                             :notifier {:name "bugsnag-client"
                                        :version "0.0.1"
                                        :url "https://github.com/omartell/bugsnag-client"}
                             :events [{:severity "error"
                                       :app {:releaseStage (:release-stage config)}
                                       :device {:hostname hostname}
                                       :payloadVersion "2"
                                       :exceptions [{"message" (:message parsed-exception)
                                                     "errorClass" (last (string/split (str (:class parsed-exception)) #" "))
                                                     "stacktrace" (map #(-> {"file" (:file %)
                                                                             "lineNumber" (:line %)
                                                                             "method" (if (:java %)
                                                                                        (str (:class %) "/" (:method %))
                                                                                        (str (:ns %) "/" (:fn %))) })
                                                                       (:trace-elems parsed-exception))}]}]}))))

(defn- report-web-exception [exception config request]
  (report exception config))

(defn wrap-bugsnag [handler config]
  "Ring compatible handler that sends exceptions raised by other handlers to Bugsnag."
  (fn [request]
    (assert (string? (:api-key config))
            (str "The api key must be passed as a string under :api-key, but got " (pr-str (:api-key config)) " from (:api-key config)"))
    (try
      (handler request)
      (catch Exception e
        (report-web-exception e config request)
        (throw e)))))
