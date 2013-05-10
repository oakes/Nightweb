(ns nightweb-desktop.server
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.util.response :only [response
                                   file-response
                                   resource-response]]
        [nightweb.formats :only [url-decode]]
        [nightweb.constants :only [nw-dir]]
        [nightweb-desktop.pages :only [get-main-page
                                       get-category-page
                                       get-basic-page]]
        [nightweb-desktop.actions :only [do-action]]
        [nightweb-desktop.utils :only [decode-values]]))

(def port 3000)

(defn handler
  [request]
  (if (= :post (:request-method request))
    (response (do-action (-> (slurp (:body request))
                         (url-decode false)
                         (decode-values))))
    (let [params (url-decode (:query-string request))]
      (case (:uri request)
        "/" (response (get-main-page params))
        "/c" (response (get-category-page params))
        "/b" (response (get-basic-page params))
        (if (> (.indexOf (:uri request) nw-dir) 0)
          (file-response (:uri request) {:root "."})
          (resource-response (:uri request) {:root "public"}))))))

(defn start-server
  []
  (future (run-jetty handler {:port port})))
