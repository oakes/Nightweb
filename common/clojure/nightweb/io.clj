(ns nightweb.io
  (:use [clojure.java.io :only [file
                                input-stream
                                output-stream]]
        [nightweb.constants :only [slash
                                   post-ext
                                   link-ext
                                   get-user-dir
                                   get-meta-dir
                                   get-posts-dir
                                   profile-file]]))

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
  (let [length (.length (file path))
        data-barray (byte-array length)]
    (with-open [bis (input-stream path)]
      (.read bis data-barray))
    data-barray))

(defn make-dir
  [path]
  (.mkdirs (file path)))

(defn iterate-dir
  [path func]
  (doseq [f (.listFiles (file path))]
    (if (.isDirectory f)
      (func (.getName f)))))

; encodings/decodings

(defn b-encode
  [data-map]
  (org.klomp.snark.bencode.BEncoder/bencode data-map))

(defn b-decode
  [data-barray]
  (try
    (.getMap (org.klomp.snark.bencode.BDecoder/bdecode
               (java.io.ByteArrayInputStream. data-barray)))
    (catch java.lang.Exception e nil)))

(defn base32-encode
  [data-barray]
  (net.i2p.data.Base32/encode data-barray))

(defn base32-decode
  [data-str]
  (net.i2p.data.Base32/decode data-str))

; read/write specific files

(defn write-key-file
  [file-path key-data]
  (write-file file-path (b-encode {"sign-key" key-data
                                   "sign-algo" "DSA-SHA1"})))

(defn read-key-file
  [file-path]
  (let [priv-key-map (b-decode (read-file file-path))
        sign-key-str (.get priv-key-map "sign-key")]
    (.getBytes sign-key-str)))

(defn write-post-file
  [dir-path user-hash text]
  (write-file (str dir-path
                   (get-posts-dir user-hash)
                   slash (.getTime (java.util.Date.)) post-ext)
              (b-encode {"text" text})))

(defn write-profile-file
  [dir-path user-hash name-text about-text]
  (write-file (str dir-path (get-meta-dir user-hash) profile-file)
              (b-encode {"name" name-text
                         "about" about-text})))

(defn write-link-file
  [file-path meta-hash sign]
  (let [signed-data (b-encode {"hash" meta-hash
                               "time" (.getTime (java.util.Date.))})
        signature (sign signed-data)]
    (write-file (str file-path link-ext)
                (b-encode {"data" signed-data
                           "sig" signature}))))
