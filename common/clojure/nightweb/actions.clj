(ns nightweb.actions
  (:use [nightweb.router :only [create-meta-torrent
                                create-imported-user]]
        [nightweb.io :only [list-dir
                            write-file
                            delete-file
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
                                   slash
                                   get-user-dir]])
  (:require clojure.edn))

(defn save-profile
  [{:keys [pic-hash name-str body-str]}]
  (let [profile (profile-encode name-str body-str pic-hash)]
    (insert-profile @my-hash-bytes (b-decode-map (b-decode profile)))
    (delete-orphaned-pics @my-hash-bytes)
    (write-profile-file profile)
    (future (create-meta-torrent))))

(defn new-post
  [{:keys [pic-hashes body-str ptr-hash ptr-time status create-time]}]
  (let [post (post-encode :text body-str
                          :pic-hashes pic-hashes
                          :status status
                          :ptrhash ptr-hash
                          :ptrtime ptr-time)
        file-name (or create-time (.getTime (java.util.Date.)))]
    (insert-post @my-hash-bytes file-name (b-decode-map (b-decode post)))
    (delete-orphaned-pics @my-hash-bytes)
    (write-post-file file-name post)
    (future (create-meta-torrent))))

(defn toggle-fav
  [{:keys [ptr-hash ptr-time]}]
  (let [content (get-single-fav-data {:userhash ptr-hash
                                      :time ptr-time})
        new-status (if (= 1 (:status content)) 0 1)
        fav-time (or (:time content) (.getTime (java.util.Date.)))
        fav (fav-encode ptr-hash ptr-time new-status)]
    (insert-fav @my-hash-bytes fav-time (b-decode-map (b-decode fav)))
    (write-fav-file fav-time fav)
    (add-user-hash ptr-hash)
    (future (create-meta-torrent))))

(defn import-user
  [{:keys [source-str pass-str]}]
  (let [dest-str (get-user-dir)]
    (if (unzip-dir source-str dest-str pass-str)
      (let [paths (set (get-zip-headers source-str))
            new-dirs (-> (fn [d] (contains? paths (str d slash)))
                         (filter (list-dir dest-str)))]
        (if (create-imported-user new-dirs)
          nil
          :import_error))
      :unzip_error)))

(defn export-user
  [{:keys [dest-str pass-str]}]
  (let [source-str (get-user-dir @my-hash-str)]
    (delete-file dest-str)
    (when (zip-dir source-str dest-str pass-str)
      dest-str)))
