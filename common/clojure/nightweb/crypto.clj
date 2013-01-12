(ns nightweb.crypto
  (:use [clojure.java.io :only [file writer]]))

(def signing-key nil)

(defn create-key-if-not-exists
  [key-file-path]
  (let [key-file (file key-file-path)]
    (if (.exists key-file)
      (let [file-contents (slurp key-file-path)]
        (def signing-key (net.i2p.data.SigningPrivateKey. file-contents)))
      (let [context (net.i2p.I2PAppContext/getGlobalContext)
            key-gen (.keyGenerator context)
            signing-keys (.generateSigningKeypair key-gen)
            priv-key (aget signing-keys 1)]
        (def signing-key priv-key)
        (with-open [wrtr (writer key-file-path)]
          (.write wrtr (.toBase64 priv-key)))))))
