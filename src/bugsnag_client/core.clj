(ns bugsnag-client.core
  (:require [org.httpkit.client :as http-client]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clj-stacktrace.core :as stacktrace]))

(def hostname
  (.getHostName (java.net.InetAddress/getLocalHost)))

(defn post-json-to-bugsnag [json]
  (http-client/post "https://notify.bugsnag.com"
                    {:body (json/generate-string json)
                     :Headers {"Content-Type" "application/json"}}))

(defn notify-bugsnag [request config exception]
  (let [parsed-exception (stacktrace/parse-exception exception)]
    (post-json-to-bugsnag {:apiKey (:api-key config)
                           :notifier {:name "bugsnag-client"
                                      :version "0.0.1"
                                      :url "https://github.com/omartell/bugsnag-client"}
                           :events [{:payloadVersion "2"
                                     :exceptions [{"message" (:message parsed-exception)
                                                   "errorClass" (last (string/split (str (:class parsed-exception)) #" "))
                                                   "stracktrace" (map #(-> {"file" (:file %)
                                                                            "lineNumber" (:line %)
                                                                            "method" (:method %)})
                                                                      (:trace-elems parsed-exception))}]}]
                           :severity "error"
                           :app {:releaseStage (:release-stage config)}
                           :device {:hostname hostname}})))

(defn wrap-bugsnag [handler config]
  "Ring compatible handler that caches any exceptions raised by other handlers and sends those exceptions to Bugsnag."
  (fn [request]
    (assert (string? (:api-key config))
            (str "The api key must be passed as a string under :api-key, but got " (pr-str (:api-key config)) " from (:api-key config)"))
    (try
      (handler request)
      (catch Exception e
        (notify-bugsnag request config e)
        (throw e)))))
