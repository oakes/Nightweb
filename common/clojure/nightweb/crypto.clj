(ns nightweb.crypto
  (:use [nightweb.constants :only [priv-key-file
                                   pub-key-file]]
        [nightweb.io :only [file-exists
                            file-write
                            file-read
                            b-encode
                            b-decode]]))

(def priv-key nil)
(def pub-key nil)

(defn read-priv-key
  [key-file-path]
  (let [nightkey-map (b-decode (file-read key-file-path))
        signing-key-str (.get nightkey-map "sign-key")]
    (net.i2p.data.SigningPrivateKey. (.getBytes signing-key-str))))

(defn gen-priv-key
  []
  (let [context (net.i2p.I2PAppContext/getGlobalContext)
        key-gen (.keyGenerator context)
        signing-keys (.generateSigningKeypair key-gen)]
    (aget signing-keys 1)))

(defn write-key
  [key-file-path key-obj]
  (file-write key-file-path
              (b-encode {"sign-key" (.getData key-obj)
                         "sign-algo" "DSA-SHA1"})))

(defn create-keys
  [base-dir-path]
  (let [priv-key-path (str base-dir-path priv-key-file)
        pub-key-path (str base-dir-path pub-key-file)]
    (def priv-key (if (file-exists priv-key-path)
                    (read-priv-key priv-key-path)
                    (gen-priv-key)))
    (def pub-key (.toPublic priv-key))
    (if (not (file-exists priv-key-path))
      (write-key pub-key-path pub-key)
      (write-key priv-key-path priv-key))
    pub-key-path))

(defn create-signature
  [message]
  (.getData (.sign (net.i2p.crypto.DSAEngine/getInstance) message priv-key)))

(defn verify-signature
  [sig message]
  (.verifySignature (net.i2p.crypto.DSAEngine/getInstance)
                    (net.i2p.data.Signature. sig)
                    message
                    0
                    (alength message)
                    pub-key))
