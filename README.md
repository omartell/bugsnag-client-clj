# bugsnag-client

Report exceptions from your Clojure application to Bugsnag

## Usage

### Ring middleware

Use the `bugnag/wrap-bugsnag` ring middleware to track exceptions
raised by your web handlers. If you want to have access to the request params and
session information in your exception report, then make sure to add
`bugsnag/wrap-bugsnag` after those middlewares.

``` Clojure
(ns app.core
  (:require [bugsnag-client.core :as bugsnag]))

(def app
  (-> app-routes
      (bugsnag/wrap-bugsnag {:api-key "Your project API key goes here"
                             :notify-release-stages ["staging" "production"]
                             :release-stage "production"})))
```

## License

Copyright © 2015 Oliver Martell

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
