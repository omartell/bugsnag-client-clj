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
                             :release-stage "production"})
      params/wrap-params
      session/wrap-session))
```

### Reporting non-web exceptions

Use `bugsnag/report` to report any exceptions raised outside of web handlers.

```Clojure
(ns app.core
  (:require [bugsnag-client.core :as bugsnag]))

(def config
  {:api-key "Your project API key goes here"
  :notify-release-stages ["staging" "production"]
  :release-stage "production"})

(def metadata
  {:user {:email "foo@bar.com"}})

(bugsnag/report (Exception. "Ooops")
                config
                metadata)
```

## Copyright & License

The MIT License (MIT)

Copyright © 2015 Oliver Martell

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
