(ns nightweb.crypto
  (:use [nightweb.constants :only [priv-key-file
                                   pub-key-file]]
        [nightweb.io :only [file-exists?
                            write-key-file
                            read-key-file]]))

(def priv-key nil)
(def pub-key nil)

(defn gen-priv-key
  []
  (let [context (net.i2p.I2PAppContext/getGlobalContext)
        key-gen (.keyGenerator context)
        signing-keys (.generateSigningKeypair key-gen)]
    (aget signing-keys 1)))

(defn create-user-keys
  [base-dir-path]
  (let [priv-key-path (str base-dir-path priv-key-file)
        pub-key-path (str base-dir-path pub-key-file)]
    (def priv-key (if (file-exists? priv-key-path)
                    (net.i2p.data.SigningPrivateKey.
                      (read-key-file priv-key-path))
                    (gen-priv-key)))
    (def pub-key (.toPublic priv-key))
    (when (not (file-exists? priv-key-path))
      (write-key-file pub-key-path (.getData pub-key))
      (write-key-file priv-key-path (.getData priv-key)))
    pub-key-path))

(defn create-signature
  [message]
  (.getData (.sign (net.i2p.crypto.DSAEngine/getInstance) message priv-key)))

(defn verify-signature
  [sig-bytes pub-key-bytes message-bytes]
  (.verifySignature (net.i2p.crypto.DSAEngine/getInstance)
                    (net.i2p.data.Signature. sig-bytes)
                    message-bytes
                    0
                    (alength message-bytes)
                    (net.i2p.data.SigningPublicKey. pub-key-bytes)))
