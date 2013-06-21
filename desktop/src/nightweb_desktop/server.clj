(ns nightweb-desktop.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as res]
            [ring.middleware.multipart-params :as multi]
            [nightweb.constants :as c]
            [nightweb.formats :as f]
            [nightweb-desktop.actions :as actions]
            [nightweb-desktop.pages :as pages]
            [nightweb-desktop.utils :as utils]))

(def ^:const port 3000)

(defn handler
  [request dir]
  (if (= :post (:request-method request))
    (-> (slurp (:body request))
        (f/url-decode false)
        utils/decode-values
        (merge (clojure.walk/keywordize-keys (:multipart-params request)))
        actions/do-action
        res/response)
    (let [params (f/url-decode (:query-string request))]
      (case (:uri request)
        "/" (res/response (pages/get-main-page params))
        "/c" (res/response (pages/get-category-page params))
        "/b" (res/response (pages/get-basic-page params))
        (if (>= (.indexOf (:uri request) c/nw-dir) 0)
          (res/file-response (clojure.string/replace (:uri request) ".webp" "")
                             {:root dir})
          (res/resource-response (:uri request) {:root "."}))))))

(defn start-server
  [dir]
  (future (jetty/run-jetty (multi/wrap-multipart-params #(handler % dir))
                           {:port port :host "127.0.0.1"})))
