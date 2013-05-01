(ns nightweb-desktop.server
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.util.response :only [file-response
                                   resource-response]]
        [ring.util.codec :only [form-decode-str]]
        [nightweb.formats :only [url-decode]]
        [nightweb.constants :only [nw-dir]]
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
  (if (= :post (:request-method request))
    (make-response (do-action (-> (slurp (:body request))
                                  (form-decode-str)
                                  (url-decode false))))
    (let [params (url-decode (:query-string request))]
      (case (:uri request)
        "/" (make-response (get-main-page params))
        "/c" (make-response (get-category-page params))
        "/b" (make-response (get-basic-page params))
        (if (> (.indexOf (:uri request) nw-dir) 0)
          (file-response (:uri request) {:root "."})
          (resource-response (:uri request) {:root "public"}))))))

(defn start-server
  []
  (future (run-jetty handler {:port port})))
