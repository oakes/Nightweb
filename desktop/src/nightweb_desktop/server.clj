(ns nightweb-desktop.server
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.middleware.params :only [wrap-params]]
        [nightweb-desktop.pages :only [get-main-page]]))

(def port 3000)

(defn get-markup
  [params]
  (if (empty? params)
    (get-main-page)
    "Not found."))

(defn handler
  [request]
  (case (get request :request-method)
    :get {:status 200
          :headers {"Content-Type" "text/html"}
          :body (get-markup (get request :params))}
    nil))

(defn start-server
  []
  (future (run-jetty (wrap-params handler) {:port port})))
