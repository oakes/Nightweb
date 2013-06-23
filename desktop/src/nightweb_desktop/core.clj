(ns nightweb-desktop.core
  (:gen-class)
  (:require [nightweb.router :as router]
            [nightweb-desktop.server :as server]
            [nightweb-desktop.window :as window]))

(defn -main
  [& args]
  (let [dir (if (> (count args) 0)
              (-> (jwrapper.jwutils.JWSystem/getAllAppVersionsSharedFolder)
                  .getCanonicalPath)
              "nightweb")]
    (router/start-router dir)
    (server/start-server dir)
    (window/start-window)))
