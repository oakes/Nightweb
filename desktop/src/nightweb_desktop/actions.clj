(ns nightweb-desktop.actions
  (:use [ring.util.codec :only [base64-decode]]
        [nightweb.router :only [create-meta-torrent
                                create-imported-user]]
        [nightweb.io :only [list-dir
                            write-file
                            delete-file
                            write-pic-file
                            write-profile-file
                            delete-orphaned-pics]]
        [nightweb.db :only [insert-profile]]
        [nightweb.formats :only [profile-encode
                                 b-decode
                                 b-decode-map]]
        [nightweb.zip :only [zip-dir unzip-dir get-zip-headers]]
        [nightweb.constants :only [my-hash-bytes
                                   my-hash-str
                                   base-dir
                                   nw-dir
                                   user-zip-file
                                   slash
                                   get-user-dir]]
        [nightweb-desktop.utils :only [get-string]]))

(defn save-profile
  [params]
  (let [pic-str (:pic params)
        name-str (:name params)
        body-str (:body params)
        image-barray (when pic-str
                       (->> (+ 1 (.indexOf pic-str ","))
                            (subs pic-str)
                            (base64-decode)))
        img-hash (write-pic-file image-barray)
        profile (profile-encode name-str body-str img-hash)]
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
    (write-file path (->> (+ 1 (.indexOf file-str ","))
                          (subs file-str)
                          (base64-decode)))
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

(defn do-action
  [params]
  (case (:type params)
    "profile" (save-profile params)
    "import" (import-user params)
    "export" (export-user params)
    nil))
