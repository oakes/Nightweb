(ns nightweb.io
  (:use [clojure.java.io :only [file
                                input-stream
                                output-stream]]
        [nightweb.crypto :only [create-hash]]
        [nightweb.formats :only [b-encode
                                 b-decode
                                 b-decode-map
                                 b-decode-byte-list
                                 base32-encode
                                 base32-decode
                                 key-encode
                                 link-encode
                                 remove-dupes-and-nils]]
        [nightweb.db :only [get-pic-data]]
        [nightweb.constants :only [base-dir
                                   my-hash-bytes
                                   my-hash-str
                                   slash
                                   meta-dir
                                   link-ext
                                   profile
                                   priv-node-key-file
                                   pub-node-key-file
                                   get-user-list-file
                                   get-user-dir
                                   get-meta-dir
                                   get-pic-dir
                                   get-post-dir
                                   get-fav-dir]]))

; basic file operations

(defn file-exists?
  [path]
  (.exists (file path)))

(defn write-file
  [path data-barray]
  (when-let [parent-dir (.getParentFile (file path))]
    (.mkdirs parent-dir))
  (with-open [bos (output-stream path)]
    (.write bos data-barray 0 (alength data-barray))))

(defn read-file
  [path]
  (when (file-exists? path)
    (let [length (.length (file path))]
      (when (< length 500000)
        (let [data-barray (byte-array length)]
          (with-open [bis (input-stream path)]
            (.read bis data-barray))
          data-barray)))))

(defn delete-file
  [path]
  (.delete (file path)))

(defn delete-file-recursively
  [path]
  (let [f (file path)]
    (when (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child)))
    (delete-file f)))

(defn make-dir
  [path]
  (.mkdirs (file path)))

(defn iterate-dir
  [path func]
  (doseq [f (.listFiles (file path))]
    (when (.isDirectory f)
      (func (.getName f)))))

(defn list-dir
  [path]
  (for [f (.listFiles (file path))]
    (when (.isDirectory f)
      (.getName f))))

(defn get-files-in-uri
  [uri-str]
  (let [java-uri (java.net.URI/create uri-str)
        files (for [uri-file (file-seq (file java-uri))]
                (when (.isFile uri-file)
                  (.getCanonicalPath uri-file)))]
    (remove-dupes-and-nils files)))

; read/write specific files

(defn write-key-file
  [file-path key-obj]
  (write-file file-path (key-encode key-obj)))

(defn read-key-file
  [file-path]
  (when-let [key-map (b-decode-map (b-decode (read-file file-path)))]
    (when-let [sign-key-str (.get key-map "sign_key")]
      (.getBytes sign-key-str))))

(defn write-priv-node-key-file
  [priv-node]
  (let [priv-node-file (net.i2p.data.PrivateKeyFile.
                         (file (str base-dir priv-node-key-file))
                         priv-node)]
    (.write priv-node-file)))

(defn read-priv-node-key-file
  []
  (let [path (str base-dir priv-node-key-file)]
    (when (file-exists? path)
      (input-stream (file path)))))

(defn write-pub-node-key-file
  [pub-node]
  (write-file (str base-dir pub-node-key-file)
              (.getBytes pub-node)))

(defn read-pub-node-key-file
  []
  (let [path (str base-dir pub-node-key-file)]
    (when (file-exists? path)
      (org.klomp.snark.dht.NodeInfo. (apply str (map char (read-file path)))))))

(defn write-pic-file
  [data-barray]
  (when data-barray
    (let [image-hash (create-hash data-barray)
          file-name (base32-encode image-hash)]
      (write-file (str (get-pic-dir my-hash-str) slash file-name)
                  data-barray)
      image-hash)))

(defn write-post-file
  [create-time encoded-args]
  (write-file (str (get-post-dir my-hash-str) slash create-time) encoded-args))

(defn write-profile-file
  [encoded-args]
  (write-file (str (get-meta-dir my-hash-str) slash profile) encoded-args))

(defn write-fav-file
  [create-time encoded-args]
  (write-file (str (get-fav-dir my-hash-str) slash create-time) encoded-args))

(defn write-link-file
  [link-hash]
  (write-file (str (get-meta-dir my-hash-str) link-ext)
              (link-encode link-hash)))

(defn read-user-list-file
  []
  (-> (get-user-list-file)
      (read-file)
      (b-decode)
      (b-decode-byte-list)))

(defn write-user-list-file
  [user-list]
  (write-file (get-user-list-file) (b-encode user-list)))

(defn read-link-file
  [user-hash-str]
  (let [link-path (str (get-meta-dir user-hash-str) link-ext)]
    (if-let [link-bytes (read-file link-path)]
      (b-decode-map (b-decode link-bytes))
      (doto (java.util.HashMap.)
        (.put "data" (b-encode {"user_hash" (base32-decode user-hash-str)}))))))

(defn read-meta-file
  ([user-dir path-leaves]
   (read-meta-file (->> (clojure.string/join slash path-leaves)
                        (str slash)
                        (str (.getAbsolutePath user-dir) meta-dir))))
  ([path]
   (let [path-parts (reverse (clojure.string/split path (re-pattern slash)))]
     {:file-name (nth path-parts 0 nil)
      :dir-name (nth path-parts 1 nil)
      :contents (b-decode-map (b-decode (read-file path)))})))

(defn delete-orphaned-pics
  [user-hash]
  (when user-hash
    (doseq [pic (file-seq (file (get-pic-dir (base32-encode user-hash))))]
      (when (and (.isFile pic)
                 (-> {:userhash user-hash
                      :pichash (base32-decode (.getName pic))}
                     (get-pic-data)
                     (count)
                     (= 0)))
        (.delete pic)))))

(defn delete-orphaned-files
  [user-hash file-list]
  (when user-hash
    (let [meta-dir (get-meta-dir (base32-encode user-hash))]
      (doseq [meta-file (file-seq (file meta-dir))]
        (when (and (.isFile meta-file)
                   (->> file-list
                        (filter #(= (.getCanonicalPath meta-file)
                                    (.getCanonicalPath
                                      (file meta-dir
                                            (clojure.string/join slash %)))))
                        (count)
                        (= 0)))
          (.delete meta-file))))))
