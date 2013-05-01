(ns nightweb-desktop.actions
  (:use [ring.util.codec :only [base64-decode]]
        [nightweb.router :only [create-meta-torrent]]
        [nightweb.io :only [write-pic-file
                            write-profile-file
                            delete-orphaned-pics]]
        [nightweb.db :only [insert-profile]]
        [nightweb.formats :only [profile-encode
                                 b-decode
                                 b-decode-map]]
        [nightweb.constants :only [my-hash-bytes]]))

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
    (insert-profile my-hash-bytes (b-decode-map (b-decode profile)))
    (delete-orphaned-pics my-hash-bytes)
    (write-profile-file profile)
    (create-meta-torrent)))

(defn do-action
  [params]
  (case (:type params)
    "profile" (save-profile params)
    nil))
