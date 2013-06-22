(ns nightweb-desktop.core
  (:gen-class)
  (:require [clojure.java.io :as java.io]
            [nightweb.router :as router]
            [nightweb-desktop.server :as server]
            [nightweb-desktop.utils :as utils]
            [nightweb-desktop.window :as window]))

(defn -main
  []
  (let [dir (-> (jwrapper.jwutils.JWSystem/getAllAppVersionsSharedFolder)
                .getCanonicalPath)]
    (router/start-router dir)
    (server/start-server dir)
    (window/start-window)))
