(ns nightweb.crypto)

(def priv-key (atom nil))
(def pub-key (atom nil))

(defn gen-priv-key
  []
  (let [context (net.i2p.I2PAppContext/getGlobalContext)
        key-gen (.keyGenerator context)
        signing-keys (.generateSigningKeypair key-gen)]
    (aget signing-keys 1)))

(defn load-user-keys
  [priv-key-bytes]
  (reset! priv-key (if priv-key-bytes
                     (net.i2p.data.SigningPrivateKey. priv-key-bytes)
                     (gen-priv-key)))
  (reset! pub-key (.toPublic @priv-key)))

(defn get-hash-algo
  []
  (java.security.MessageDigest/getInstance "SHA1"))

(defn create-hash
  [data-barray]
  (let [algo (get-hash-algo)]
    (.digest algo data-barray)))

(defn create-signature
  ([message-bytes]
   (-> (net.i2p.crypto.DSAEngine/getInstance)
       (.sign message-bytes @priv-key)
       (.getData)))
  ([priv-key-bytes message-bytes]
   (-> (net.i2p.crypto.DSAEngine/getInstance)
       (.sign message-bytes (net.i2p.data.SigningPrivateKey. priv-key-bytes))
       (.getData))))

(defn verify-signature
  [pub-key-bytes sig-bytes message-bytes]
  (when (and pub-key-bytes sig-bytes message-bytes)
    (.verifySignature (net.i2p.crypto.DSAEngine/getInstance)
                      (net.i2p.data.Signature. sig-bytes)
                      message-bytes
                      0
                      (alength message-bytes)
                      (net.i2p.data.SigningPublicKey. pub-key-bytes))))
