(ns nightweb-desktop.server
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.util.response :only [file-response]]
        [nightweb.formats :only [url-decode]]
        [nightweb-desktop.pages :only [get-main-page
                                       get-category-page
                                       get-basic-page]]
        [nightweb-desktop.actions :only [do-action]]))

(def port 3000)

(defn make-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn handler
  [request]
  (let [params (url-decode (str "?" (get request :query-string)) "?")]
    (if (= :post (get request :request-method))
      (make-response (do-action params))
      (case (get request :uri)
        "/" (make-response (get-main-page params))
        "/c" (make-response (get-category-page params))
        "/b" (make-response (get-basic-page params))
        (file-response (get request :uri) {:root "resources/public"})))))

(defn start-server
  []
  (future (run-jetty handler {:port port})))
