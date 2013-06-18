(ns nightweb-desktop.actions
  (:require [clojure.edn :as edn]
            [nightweb.actions :as actions]
            [nightweb.constants :as c]
            [nightweb.formats :as f]
            [nightweb.io :as io]
            [nightweb.users :as users]
            [nightweb-desktop.utils :as utils]))

(defn do-action
  [params]
  (case (:type params)
    "save-profile" (-> (assoc params :pic-hash (-> (:pic-str params)
                                                   utils/decode-data-uri
                                                   io/write-pic-file))
                       actions/save-profile
                       deref
                       (do nil))
    "import-user" (let [path (str @c/base-dir c/nw-dir c/slash c/user-zip-file)
                        file-barray (utils/decode-data-uri (:file-str params))]
                    (io/write-file path file-barray)
                    (-> (assoc params :source-str path)
                        actions/import-user
                        utils/get-string))
    "export-user" (let [path (str @c/base-dir c/nw-dir c/slash c/user-zip-file)]
                    (actions/export-user (assoc params :dest-str path)))
    "new-post" (-> (assoc params
                          :pic-hashes (for [pic (-> (:pics-str params)
                                                    edn/read-string)]
                                        (-> pic
                                            utils/decode-data-uri
                                            io/write-pic-file))
                          :ptr-hash (f/base32-decode (:ptr-hash params))
                          :ptr-time (edn/read-string (:ptr-time params))
                          :status 1)
                   actions/new-post
                   deref
                   (do nil))
    "edit-post" (-> (assoc params
                           :create-time (-> (:create-time params)
                                            edn/read-string)
                           :ptr-hash (f/base32-decode (:ptr-hash params))
                           :ptr-time (edn/read-string (:ptr-time params))
                           :pic-hashes (for [pic (-> (:pic-hashes params)
                                                     edn/read-string)]
                                         (f/base32-decode pic))
                           :status 1)
                    actions/new-post
                    deref
                    (do nil))
    "delete-post" (-> {:create-time (edn/read-string (:create-time params))
                       :status 0}
                    actions/new-post
                    deref
                    (do nil))
    "switch-user" (-> (:userhash params)
                      f/base32-decode
                      users/load-user)
    "delete-user" (-> (:userhash params)
                      f/base32-decode
                      users/delete-user
                      deref
                      (do nil))
    "create-user" (do
                    (users/load-user (users/create-user))
                    (actions/fav-default-user)
                    nil)
    "toggle-fav" (-> (assoc params
                            :ptr-hash (f/base32-decode (:userhash params))
                            :ptr-time (edn/read-string (:time params)))
                     actions/toggle-fav
                     deref
                     (do nil))
    nil))
