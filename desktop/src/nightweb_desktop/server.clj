(ns nightweb-desktop.server
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.middleware.params :only [wrap-params]]
        [ring.util.response :only [file-response]]
        [nightweb-desktop.views :only [get-main-view]]))

(def port 3000)

(defn get-view
  [params]
  (if (empty? params)
    (get-main-view)
    "Not found."))

(defn handler
  [request]
  (case (get request :uri)
    "/" (case (get request :request-method)
          :get {:status 200
                :headers {"Content-Type" "text/html"}
                :body (get-view (get request :params))}
          nil)
    (file-response (get request :uri) {:root "resources/public"})))

(defn start-server
  []
  (future (run-jetty (wrap-params handler) {:port port})))
