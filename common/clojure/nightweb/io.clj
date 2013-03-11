(ns nightweb.io
  (:use [clojure.java.io :only [file
                                input-stream
                                output-stream
                                copy]]
        [nightweb.crypto :only [create-hash
                                create-signature]]
        [nightweb.formats :only [b-encode
                                 b-decode
                                 b-decode-map
                                 base32-encode
                                 base32-decode
                                 remove-dupes-and-nils]]
        [nightweb.constants :only [base-dir
                                   my-hash-bytes
                                   my-hash-str
                                   slash
                                   meta-dir
                                   link-ext
                                   profile
                                   priv-node-key-file
                                   pub-node-key-file
                                   get-user-dir
                                   get-meta-dir
                                   get-pic-dir
                                   get-post-dir]]))

; basic file operations

(defn file-exists?
  [path]
  (.exists (file path)))

(defn write-file
  [path data-barray]
  (if-let [parent-dir (.getParentFile (file path))]
    (.mkdirs parent-dir))
  (with-open [bos (output-stream path)]
    (.write bos data-barray 0 (alength data-barray))))

(defn read-file
  [path]
  (when (file-exists? path)
    (let [length (.length (file path))]
      (if (< length 1000000)
        (let [data-barray (byte-array length)]
          (with-open [bis (input-stream path)]
            (.read bis data-barray))
          data-barray)))))

(defn delete-file
  [path]
  (when (file-exists? path)
    (.delete (file path))))

(defn make-dir
  [path]
  (.mkdirs (file path)))

(defn iterate-dir
  [path func]
  (doseq [f (.listFiles (file path))]
    (if (.isDirectory f)
      (func (.getName f)))))

(defn get-files-in-uri
  [uri-str]
  (let [java-uri (java.net.URI/create uri-str)
        files (for [uri-file (file-seq (file java-uri))]
                (if (.isFile uri-file)
                  (.getCanonicalPath uri-file)))]
    (remove-dupes-and-nils files)))

; read/write specific files

(defn write-key-file
  [file-path key-obj]
  (write-file file-path (b-encode {"sign_key" (.getData key-obj)
                                   "sign_algo" "DSA-SHA1"})))

(defn read-key-file
  [file-path]
  (if-let [key-map (b-decode-map (b-decode (read-file file-path)))]
    (if-let [sign-key-str (.get key-map "sign_key")]
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
    (if (file-exists? path)
      (input-stream (file path)))))

(defn write-pub-node-key-file
  [pub-node]
  (write-file (str base-dir pub-node-key-file)
              (.getBytes pub-node)))

(defn read-pub-node-key-file
  []
  (let [path (str base-dir pub-node-key-file)]
    (if (file-exists? path)
      (org.klomp.snark.dht.NodeInfo. (apply str (map char (read-file path)))))))

(defn read-pic-file
  [user-hash-bytes image-hash-bytes]
  (if (and user-hash-bytes image-hash-bytes)
    (let [path (str (get-pic-dir (base32-encode user-hash-bytes))
                    slash
                    (base32-encode image-hash-bytes))]
      (read-file path))))

(defn write-pic-file
  [data-barray]
  (if data-barray
    (let [image-hash (create-hash data-barray)
          file-name (base32-encode image-hash)]
      (write-file (str (get-pic-dir my-hash-str) slash file-name)
                  data-barray)
      image-hash)))

(defn write-post-file
  [text image-barrays]
  (let [image-hashes (for [bitmap image-barrays]
                       (write-pic-file bitmap))
        args {"body" text
              "time" (.getTime (java.util.Date.))
              "pics" (remove-dupes-and-nils image-hashes)}
        data-barray (b-encode args)
        hash-str (base32-encode (create-hash data-barray))]
    (write-file (str (get-post-dir my-hash-str) slash hash-str)
                data-barray)))

(defn write-profile-file
  [name-text body-text image-barray]
  (let [image-hash (write-pic-file image-barray)
        args {"title" name-text
              "body" body-text
              "pics" (if image-hash [image-hash] [])}]
    (write-file (str (get-meta-dir my-hash-str) slash profile)
                (b-encode args))))

(defn write-link-file
  [link-hash]
  (let [args {"user_hash" my-hash-bytes
              "link_hash" link-hash
              "time" (.getTime (java.util.Date.))}
        signed-data (b-encode args)
        signature (create-signature signed-data)]
    (write-file (str (get-meta-dir my-hash-str) link-ext)
                (b-encode {"data" signed-data
                           "sig" signature}))))

(defn read-link-file
  [user-hash-str]
  (let [link-path (str (get-meta-dir user-hash-str) link-ext)]
    (if-let [link-bytes (read-file link-path)]
      (b-decode-map (b-decode link-bytes))
      (doto (java.util.HashMap.)
        (.put "data" (b-encode {"user_hash" (base32-decode user-hash-str)}))))))

(defn read-meta-file
  [user-dir path-leaves]
  (let [end-path (str slash (clojure.string/join slash path-leaves))
        full-path (str (.getAbsolutePath user-dir) meta-dir end-path)
        rev-leaves (reverse path-leaves)]
    {:file-name (nth rev-leaves 0 nil)
     :dir-name (nth rev-leaves 1 nil)
     :contents (b-decode-map (b-decode (read-file full-path)))}))

(defn delete-meta-files
  [user-hash-bytes file-list])
