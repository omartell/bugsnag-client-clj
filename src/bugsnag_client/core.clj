(ns bugsnag-client.core
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clj-http.client :as http]
            [ring.util.request :as util]
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

(defn- request-metadata [request]
  (let [metadata (select-keys request
                              [:server-port
                               :server-name
                               :remote-addr
                               :headers
                               :content-type])
        with-session (if (:session request)
                       (assoc metadata :session (:session request))
                       metadata)
        with-params (if (:params request)
                      (assoc metadata :params (:params request))
                      metadata)]
    (merge with-params
           {:url (util/request-url request)
            :request-method (-> request :request-method name string/upper-case)})))

(defn bugsnag-payload
  [exception config metadata]
  (let [parsed-exception (stacktrace/parse-exception exception)]
    {:apiKey (:api-key config)
     :notifier {:name "bugsnag-client"
                :version "0.0.1"
                :url "https://github.com/omartell/bugsnag-client"}
     :events [{:severity "error"
               :app {:releaseStage (:release-stage config)}
               :payloadVersion "2"
               :metaData (let [exception-metadata {:host {:hostname (.getHostName (java.net.InetAddress/getLocalHost))}}]
                           (if (:request metadata)
                             (merge exception-metadata
                                    {:request (request-metadata (:request metadata))})
                             exception-metadata))
               :exceptions [{:message (:message parsed-exception)
                             :errorClass (last (string/split (str (:class parsed-exception)) #" "))
                             :stacktrace (generate-bugsnag-stacktrace parsed-exception config)}]}]}))

(defn report
  ([exception config metadata]
   (when (report-to-bugsnag? config)
     (post-json-to-bugsnag (bugsnag-payload exception config metadata) config)))
  ([exception config]
   (report exception config {})))

(defn wrap-bugsnag [handler config]
  "Ring compatible handler that sends exceptions raised by other handlers to Bugsnag."
  (fn [request]
    (assert (string? (:api-key config))
            (str "The api key must be passed as a string under :api-key, but got " (pr-str (:api-key config)) " from (:api-key config)"))
    (try
      (handler request)
      (catch Throwable t
        (report t config {:request request})
        (throw t)))))
