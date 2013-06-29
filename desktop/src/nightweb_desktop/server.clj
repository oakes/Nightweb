(ns nightweb-desktop.server
  (:require [nightweb.constants :as c]
            [nightweb.formats :as f]
            [nightweb-desktop.actions :as actions]
            [nightweb-desktop.pages :as pages]
            [nightweb-desktop.utils :as utils]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as res]
            [ring.middleware.multipart-params :as multi]))

(def port (atom (or (utils/read-pref :port) 4707)))
(def server (atom nil))

(defn handler
  [request]
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
                             {:root @c/base-dir})
          (res/resource-response (:uri request)))))))

(defn start-server
  []
  (when @server (.stop @server))
  (reset! server (jetty/run-jetty (multi/wrap-multipart-params handler)
                                  {:port @port
                                   :host (if (utils/read-pref :remote)
                                           nil
                                           "127.0.0.1")
                                   :join? false})))

(defn set-port
  [port-str]
  (try
    (let [port-num (Integer/parseInt port-str)]
      (when (and (>= port-num 1024) (not= port-num @port))
        (utils/write-pref :port port-num)
        (reset! port port-num)
        (start-server)))
    (catch Exception e nil)))
