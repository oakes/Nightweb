(ns nightweb-desktop.actions
  (:require [clojure.edn :as edn]
            [clojure.java.io :as java.io]
            [nightweb.actions :as actions]
            [nightweb.constants :as c]
            [nightweb.formats :as f]
            [nightweb.io :as io]
            [nightweb.router :as router]
            [nightweb.users :as users]
            [nightweb-desktop.utils :as utils]))

(defn save-profile
  [params]
  (-> (assoc params :pic-hash (-> (:pic-str params)
                                  utils/decode-data-uri
                                  io/write-pic-file))
      actions/save-profile)
  nil)

(defn import-user
  [params]
  (let [path (-> (java.io/file @c/base-dir c/nw-dir c/user-zip-file)
                 .getCanonicalPath)
        file-barray (utils/decode-data-uri (:file-str params))]
    (io/write-file path file-barray)
    (-> (assoc params :source-str path)
        actions/import-user
        utils/get-string)))

(defn export-user
  [params]
  (let [path (-> (java.io/file @c/base-dir c/nw-dir c/user-zip-file)
                 .getCanonicalPath)]
    (when-let [dest-str (actions/export-user (assoc params :dest-str path))]
      (utils/get-relative-path @c/base-dir dest-str))))

(defn new-post
  [params]
  (-> (assoc params
             :pic-hashes (for [pic (edn/read-string (:pics-str params))]
                           (-> (utils/decode-data-uri pic)
                               io/write-pic-file))
             :ptr-hash (f/base32-decode (:ptr-hash params))
             :ptr-time (edn/read-string (:ptr-time params))
             :status 1)
      actions/new-post)
  nil)

(defn edit-post
  [params]
  (-> (assoc params
             :create-time (edn/read-string (:create-time params))
             :ptr-hash (f/base32-decode (:ptr-hash params))
             :ptr-time (edn/read-string (:ptr-time params))
             :pic-hashes (for [pic (edn/read-string (:pic-hashes params))]
                           (f/base32-decode pic))
             :status 1)
      actions/new-post)
  nil)

(defn delete-post
  [params]
  (-> {:create-time (edn/read-string (:create-time params))
       :status 0}
      actions/new-post)
  nil)

(defn switch-user
  [params]
  (-> (:userhash params)
      f/base32-decode
      users/load-user)
  nil)

(defn delete-user
  [params]
  (-> (:userhash params)
      f/base32-decode
      users/delete-user)
  nil)

(defn create-user
  [params]
  (users/load-user (users/create-user))
  (actions/fav-default-user)
  nil)

(defn toggle-fav
  [params]
  (-> (assoc params
             :ptr-hash (f/base32-decode (:userhash params))
             :ptr-time (edn/read-string (:time params)))
      actions/toggle-fav)
  nil)

(defn check-user-exists
  [params]
  (when-not (users/user-exists? (f/base32-decode (:userhash params)))
    (utils/get-string :found_user)))

(defn check-user-has-content
  [params]
  (when-not (users/user-has-content? (f/base32-decode (:userhash params)))
    (utils/get-string :pending_user)))

(defn shut-down
  [params]
  (router/stop-router)
  (System/exit 0))

(defn do-action
  [params]
  (when-let [action (case (:type params)
                      "save-profile" save-profile
                      "import-user" import-user
                      "export-user" export-user
                      "new-post" new-post
                      "edit-post" edit-post
                      "delete-post" delete-post
                      "switch-user" switch-user
                      "delete-user" delete-user
                      "create-user" create-user
                      "toggle-fav" toggle-fav
                      "check-user-exists" check-user-exists
                      "check-user-has-content" check-user-has-content
                      "shut-down" shut-down
                      nil)]
    (action params)))
