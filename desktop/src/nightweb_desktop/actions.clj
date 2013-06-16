(ns nightweb-desktop.actions
  (:use [nightweb.router :only [create-user load-user delete-user]]
        [nightweb.actions :only [save-profile
                                 new-post
                                 import-user
                                 export-user
                                 toggle-fav]]
        [nightweb.formats :only [base32-decode]]
        [nightweb-desktop.utils :only [get-string
                                       decode-data-uri]])
  (:require clojure.edn))

(defn do-action
  [params]
  (case (:type params)
    "save-profile" (-> (save-profile :pic (decode-data-uri (:pic params))
                                     :name-str (:name params)
                                     :body-str (:body params))
                       deref
                       println)
    "import-user" (-> (import-user :file-barray (decode-data-uri (:file params))
                                   :pass-str (:pass params))
                      get-string)
    "export-user" (export-user (:pass params))
    "new-post" (-> (new-post :pics (for [pic-str (-> (:pics params)
                                                   clojure.edn/read-string)]
                                     (decode-data-uri pic-str))
                             :body-str (:body params)
                             :ptr-hash (:ptrhash params)
                             :ptr-time (:ptrtime params))
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
    "toggle-fav" (-> (toggle-fav :ptr-hash (base32-decode (:userhash params))
                                 :ptr-time (-> (:time params)
                                               clojure.edn/read-string))
                     deref
                     println)
    nil))
