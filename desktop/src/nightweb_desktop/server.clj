(ns nightweb-desktop.server
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.util.response :only [response
                                   file-response
                                   resource-response]]
        [ring.middleware.multipart-params :only [wrap-multipart-params]]
        [nightweb.formats :only [url-decode]]
        [nightweb.constants :only [nw-dir]]
        [nightweb-desktop.pages :only [get-main-page
                                       get-category-page
                                       get-basic-page]]
        [nightweb-desktop.actions :only [do-action]]
        [nightweb-desktop.utils :only [decode-values]]))

(def ^:const port 3000)

(defn handler
  [request]
  (if (= :post (:request-method request))
    (-> (slurp (:body request))
        (url-decode false)
        decode-values
        (merge (clojure.walk/keywordize-keys (:multipart-params request)))
        do-action
        response)
    (let [params (url-decode (:query-string request))]
      (case (:uri request)
        "/" (response (get-main-page params))
        "/c" (response (get-category-page params))
        "/b" (response (get-basic-page params))
        (if (> (.indexOf (:uri request) nw-dir) 0)
          (file-response (clojure.string/replace (:uri request) ".webp" "")
                         {:root "."})
          (resource-response (:uri request) {:root "public"}))))))

(defn start-server
  []
  (future (run-jetty (wrap-multipart-params handler) {:port port})))
