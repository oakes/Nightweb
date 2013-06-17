(ns nightweb-desktop.actions
  (:use [nightweb.router :only [create-user load-user delete-user]]
        [nightweb.actions :only [save-profile
                                 new-post
                                 import-user
                                 export-user
                                 toggle-fav]]
        [nightweb.io :only [write-pic-file]]
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
                       println)
    "import-user" (let [path (str @base-dir nw-dir slash user-zip-file)
                        file-barray (decode-data-uri (:file-str params))]
                    (write-file path file-barray)
                    (-> (assoc params :source-str path)
                        import-user
                        get-string))
    "export-user" (let [path (str @base-dir nw-dir slash user-zip-file)]
                    (export-user (:pass-str params)))
    "new-post" (-> (assoc params
                          :pic-hashes (for [pic (-> (:pics-str params)
                                                    clojure.edn/read-string)]
                                        (-> pic
                                            decode-data-uri
                                            write-pic-file)))
                   new-post
                   deref
                   println)
    "switch-user" (-> (:userhash params)
                      base32-decode
                      load-user)
    "delete-user" (-> (:userhash params)
                      base32-decode
                      delete-user
                      deref
                      println)
    "create-user" (load-user (create-user))
    "toggle-fav" (-> (assoc params
                            :ptr-hash (base32-decode (:userhash params))
                            :ptr-time (clojure.edn/read-string (:time params)))
                     toggle-fav
                     deref
                     println)
    nil))
