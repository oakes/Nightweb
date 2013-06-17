(ns nightweb.actions
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
                                 b-decode-map]]
        [nightweb.zip :only [zip-dir unzip-dir get-zip-headers]]
        [nightweb.constants :only [my-hash-bytes
                                   my-hash-str
                                   base-dir
                                   nw-dir
                                   user-zip-file
                                   slash
                                   get-user-dir]])
  (:require clojure.edn))

(defn save-profile
  [{:keys [pic name-str body-str]}]
  (let [pic-hash (write-pic-file pic)
        profile (profile-encode name-str body-str pic-hash)]
    (insert-profile @my-hash-bytes (b-decode-map (b-decode profile)))
    (delete-orphaned-pics @my-hash-bytes)
    (write-profile-file profile)
    (future (create-meta-torrent))))

(defn import-user
  [{:keys [file pass-str]}]
  (let [path (str @base-dir nw-dir slash user-zip-file)
        dest-path (get-user-dir)]
    (write-file path file)
    (if (unzip-dir path dest-path pass-str)
      (let [paths (set (get-zip-headers path))
            new-dirs (-> (fn [d] (contains? paths (str d slash)))
                         (filter (list-dir dest-path)))]
        (if (create-imported-user new-dirs)
          nil
          :import_error))
      :unzip_error)))

(defn export-user
  [pass-str]
  (let [path (get-user-dir @my-hash-str)
        dest-path (str @base-dir nw-dir slash user-zip-file)]
    (delete-file dest-path)
    (if (zip-dir path dest-path pass-str)
      dest-path
      "")))

(defn new-post
  [{:keys [pics body-str ptr-hash ptr-time]}]
  (let [pic-hashes (for [pic pics] (write-pic-file pic))
        post (post-encode :text body-str
                          :pic-hashes pic-hashes
                          :status 1
                          :ptrhash ptr-hash
                          :ptrtime ptr-time)
        create-time (.getTime (java.util.Date.))]
    (insert-post @my-hash-bytes
                 create-time
                 (b-decode-map (b-decode post)))
    (delete-orphaned-pics @my-hash-bytes)
    (write-post-file create-time post)
    (future (create-meta-torrent))))

(defn toggle-fav
  [{:keys [ptr-hash ptr-time]}]
  (let [content (get-single-fav-data {:userhash ptr-hash
                                      :time ptr-time})
        new-status (if (= 1 (:status content)) 0 1)
        fav-time (or (:time content) (.getTime (java.util.Date.)))
        fav (fav-encode ptr-hash ptr-time new-status)]
    (insert-fav @my-hash-bytes
                fav-time
                (b-decode-map (b-decode fav)))
    (write-fav-file fav-time fav)
    (add-user-hash ptr-hash)
    (future (create-meta-torrent))))
