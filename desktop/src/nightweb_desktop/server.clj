(ns nightweb-desktop.server
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.middleware.params :only [wrap-params]]
        [ring.util.response :only [file-response]]
        [nightweb-desktop.pages :only [get-page]]))

(def port 3000)

(defn handler
  [request]
  (case (get request :uri)
    "/" (case (get request :request-method)
          :get {:status 200
                :headers {"Content-Type" "text/html"}
                :body (get-page (get request :params))}
          nil)
    (file-response (get request :uri) {:root "resources/public"})))

(defn start-server
  []
  (future (run-jetty (wrap-params handler) {:port port})))
