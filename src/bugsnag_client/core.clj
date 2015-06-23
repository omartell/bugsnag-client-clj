(ns bugsnag-client.core
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clj-http.client :as http]
            [clj-stacktrace.core :as stacktrace]))

(defn- post-json-to-bugsnag [payload config]
  (http/post (if (:use-ssl config true)
               "https://notify.bugsnag.com"
               "http://notify.bugsnag.com")
             {:body (json/write-str payload)
              :content-type :json
              :accept :json}))

(defn- report-to-bugsnag? [config]
  (let [notify-release-stages (into #{} (:notify-release-stages config))]
    (notify-release-stages (config :release-stage "production"))))

(defn- in-project? [namespace config]
  (let [default-application-package (second (edn/read-string (slurp "project.clj")))
        application-packages (:application-packages config [default-application-package])]
    (boolean (some #(.startsWith namespace (str %))
                   application-packages))))

(defn- generate-bugsnag-stacktrace [parsed-exception config]
  (map #(-> {:file (:file %)
             :inProject (if (:java %)
                          (in-project? (:class %) config)
                          (in-project? (:ns %) config))
             :lineNumber (:line %)
             :method (if (:java %)
                       (str (:class %) "/" (:method %))
                       (str (:ns %) "/" (:fn %))) })
       (:trace-elems parsed-exception)))

(defn report [exception config]
  (when (report-to-bugsnag? config)
    (let [parsed-exception (stacktrace/parse-exception exception)]
      (post-json-to-bugsnag {:apiKey (:api-key config)
                             :notifier {:name "bugsnag-client"
                                        :version "0.0.1"
                                        :url "https://github.com/omartell/bugsnag-client"}
                             :events [{:severity "error"
                                       :app {:releaseStage (:release-stage config)}
                                       :device {:hostname (.getHostName (java.net.InetAddress/getLocalHost))}
                                       :payloadVersion "2"
                                       :exceptions [{:message (:message parsed-exception)
                                                     :errorClass (last (string/split (str (:class parsed-exception)) #" "))
                                                     :stacktrace (generate-bugsnag-stacktrace parsed-exception config)}]}]}
                            config))))

(defn- report-web-exception [exception config request]
  (report exception config))

(defn wrap-bugsnag [handler config]
  "Ring compatible handler that sends exceptions raised by other handlers to Bugsnag."
  (fn [request]
    (assert (string? (:api-key config))
            (str "The api key must be passed as a string under :api-key, but got " (pr-str (:api-key config)) " from (:api-key config)"))
    (try
      (handler request)
      (catch Throwable t
        (report-web-exception t config request)
        (throw t)))))
