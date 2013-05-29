(ns nightweb.zip)

(def ^:const def-pass "hunter2")

(defn zip-dir 
  [path dest-path password]
  (try
    (let [zip-file (net.lingala.zip4j.core.ZipFile. dest-path)
          params (net.lingala.zip4j.model.ZipParameters.)
          comp-method net.lingala.zip4j.util.Zip4jConstants/COMP_DEFLATE
          comp-level net.lingala.zip4j.util.Zip4jConstants/DEFLATE_LEVEL_NORMAL
          enc-method net.lingala.zip4j.util.Zip4jConstants/ENC_METHOD_AES
          enc-strength net.lingala.zip4j.util.Zip4jConstants/AES_STRENGTH_256]
      (.setCompressionMethod params comp-method)
      (.setCompressionLevel params comp-level)
      (.setEncryptFiles params true)
      (.setEncryptionMethod params enc-method)
      (.setAesKeyStrength params enc-strength)
      (.setPassword params (if (zero? (count password)) def-pass password))
      (.addFolder zip-file path params)
      true)
    (catch java.lang.Exception e false)))

(defn unzip-dir
  [path dest-path password]
  (try
    (let [zip-file (net.lingala.zip4j.core.ZipFile. path)]
      (.setPassword zip-file (if (zero? (count password)) def-pass password))
      (.extractAll zip-file dest-path)
      true)
    (catch java.lang.Exception e false)))

(defn get-zip-headers
  [path]
  (try
    (let [zip-file (net.lingala.zip4j.core.ZipFile. path)]
      (for [header (.getFileHeaders zip-file)]
        (.getFileName header)))
    (catch java.lang.Exception e nil)))
