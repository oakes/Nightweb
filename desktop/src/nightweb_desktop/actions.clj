(ns nightweb-desktop.actions
  (:use [nightweb.router :only [create-meta-torrent
                                create-imported-user]]
        [nightweb.io :only [list-dir
                            write-file
                            delete-file
                            write-pic-file
                            write-profile-file
                            write-post-file
                            write-fav-file
                            delete-orphaned-pics]]
        [nightweb.db :only [insert-profile
                            insert-post
                            insert-fav
                            get-single-fav-data]]
        [nightweb.torrents-dht :only [add-user-hash]]
        [nightweb.formats :only [profile-encode
                                 post-encode
                                 fav-encode
                                 b-decode
                                 b-decode-map
                                 base32-decode]]
        [nightweb.zip :only [zip-dir unzip-dir get-zip-headers]]
        [nightweb.constants :only [my-hash-bytes
                                   my-hash-str
                                   base-dir
                                   nw-dir
                                   user-zip-file
                                   slash
                                   get-user-dir]]
        [nightweb.router :only [create-user load-user delete-user]]
        [nightweb-desktop.utils :only [get-string
                                       decode-data-uri]])
  (:require clojure.edn))

(defn save-profile
  [params]
  (let [pic-str (:pic params)
        name-str (:name params)
        body-str (:body params)
        image-barray (decode-data-uri pic-str)
        pic-hash (write-pic-file image-barray)
        profile (profile-encode name-str body-str pic-hash)]
    (insert-profile @my-hash-bytes (b-decode-map (b-decode profile)))
    (delete-orphaned-pics @my-hash-bytes)
    (write-profile-file profile)
    (create-meta-torrent)))

(defn import-user
  [params]
  (let [path (str @base-dir nw-dir slash user-zip-file)
        dest-path (get-user-dir)
        file-str (:file params)
        password (:pass params)]
    (write-file path (decode-data-uri file-str))
    (if (unzip-dir path dest-path password)
      (let [paths (set (get-zip-headers path))
            new-dirs (-> (fn [d] (contains? paths (str d slash)))
                         (filter (list-dir dest-path)))]
        (if (create-imported-user new-dirs)
          ""
          (get-string :import_error)))
      (get-string :unzip_error))))

(defn export-user
  [params]
  (let [path (get-user-dir @my-hash-str)
        dest-path (str @base-dir nw-dir slash user-zip-file)
        password (:pass params)]
    (delete-file dest-path)
    (if (zip-dir path dest-path password)
      dest-path
      "")))

(defn new-post
  [params]
  (let [text (:body params)
        pics (clojure.edn/read-string (:pics params))
        pic-hashes (for [pic-str pics]
                     (write-pic-file (decode-data-uri pic-str)))
        post (post-encode :text text
                          :pic-hashes pic-hashes
                          :status 1
                          :ptrhash (base32-decode (:ptrhash params))
                          :ptrtime (:ptrtime params))
        create-time (.getTime (java.util.Date.))]
    (insert-post @my-hash-bytes
                 create-time
                 (b-decode-map (b-decode post)))
    (delete-orphaned-pics @my-hash-bytes)
    (write-post-file create-time post)
    (create-meta-torrent)))

(defn toggle-fav
  [params]
  (let [ptr-hash (base32-decode (:userhash params))
        ptr-time (clojure.edn/read-string (:time params))
        content (get-single-fav-data {:userhash ptr-hash
                                      :time ptr-time})
        new-status (if (= 1 (:status content)) 0 1)
        fav-time (or (:time content) (.getTime (java.util.Date.)))
        fav (fav-encode ptr-hash ptr-time new-status)]
    (insert-fav @my-hash-bytes
                fav-time
                (b-decode-map (b-decode fav)))
    (write-fav-file fav-time fav)
    (add-user-hash ptr-hash)
    (create-meta-torrent)))

(defn do-action
  [params]
  (case (:type params)
    "save-profile" (save-profile params)
    "import-user" (import-user params)
    "export-user" (export-user params)
    "new-post" (new-post params)
    "switch-user" (load-user (base32-decode (:userhash params)))
    "delete-user" (let [thread (delete-user (base32-decode (:userhash params)))]
                    (deref thread)
                    nil)
    "create-user" (load-user (create-user))
    "toggle-fav" (toggle-fav params)
    nil))
