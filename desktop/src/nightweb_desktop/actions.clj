(ns nightweb-desktop.actions
  (:use [nightweb.router :only [create-user load-user delete-user]]
        [nightweb.actions :only [save-profile
                                 new-post
                                 import-user
                                 export-user
                                 toggle-fav]]
        [nightweb.io :only [write-file
                            write-pic-file]]
        [nightweb.formats :only [base32-decode]]
        [nightweb.constants :only [base-dir
                                   nw-dir
                                   slash
                                   user-zip-file]]
        [nightweb-desktop.utils :only [get-string
                                       decode-data-uri]])
  (:require clojure.edn))

(defn do-action
  [params]
  (case (:type params)
    "save-profile" (-> (assoc params :pic-hash (-> (:pic-str params)
                                                   decode-data-uri
                                                   write-pic-file))
                       save-profile
                       deref
                       (do nil))
    "import-user" (let [path (str @base-dir nw-dir slash user-zip-file)
                        file-barray (decode-data-uri (:file-str params))]
                    (write-file path file-barray)
                    (-> (assoc params :source-str path)
                        import-user
                        get-string))
    "export-user" (let [path (str @base-dir nw-dir slash user-zip-file)]
                    (export-user (assoc params :dest-str path)))
    "new-post" (-> (assoc params
                          :pic-hashes (for [pic (-> (:pics-str params)
                                                    clojure.edn/read-string)]
                                        (-> pic
                                            decode-data-uri
                                            write-pic-file))
                          :ptr-hash (base32-decode (:ptr-hash params))
                          :ptr-time (clojure.edn/read-string (:ptr-time params))
                          :status 1)
                   new-post
                   deref
                   (do nil))
    "edit-post" (-> (assoc params
                           :create-time (-> (:create-time params)
                                            clojure.edn/read-string)
                           :ptr-hash (base32-decode (:ptr-hash params))
                           :ptr-time (-> (:ptr-time params)
                                         clojure.edn/read-string)
                           :pic-hashes (for [pic (-> (:pic-hashes params)
                                                   clojure.edn/read-string)]
                                         (base32-decode pic))
                           :status 1)
                    new-post
                    deref
                    (do nil))
    "delete-post" (-> (assoc params
                             :create-time (-> (:create-time params)
                                              clojure.edn/read-string)
                             :body-str nil
                             :ptr-hash nil
                             :ptr-time nil
                             :pic-hashes nil
                             :status 0)
                    new-post
                    deref
                    (do nil))
    "switch-user" (-> (:userhash params)
                      base32-decode
                      load-user)
    "delete-user" (-> (:userhash params)
                      base32-decode
                      delete-user
                      deref
                      (do nil))
    "create-user" (load-user (create-user))
    "toggle-fav" (-> (assoc params
                            :ptr-hash (base32-decode (:userhash params))
                            :ptr-time (clojure.edn/read-string (:time params)))
                     toggle-fav
                     deref
                     (do nil))
    nil))
