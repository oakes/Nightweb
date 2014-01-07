(ns nightweb.crypto
  (:import [java.security MessageDigest]
           [net.i2p I2PAppContext]
           [net.i2p.crypto DSAEngine]
           [net.i2p.data Signature SigningPrivateKey SigningPublicKey]))

(def priv-key (atom nil))
(def pub-key (atom nil))

(defn gen-priv-key
  []
  (let [context (I2PAppContext/getGlobalContext)
        key-gen (.keyGenerator context)
        signing-keys (.generateSigningKeypair key-gen)]
    (aget signing-keys 1)))

(defn load-user-keys!
  [priv-key-bytes]
  (reset! priv-key (if priv-key-bytes
                     (SigningPrivateKey. ^bytes priv-key-bytes)
                     (gen-priv-key)))
  (reset! pub-key (.toPublic ^SigningPrivateKey @priv-key)))

(defn get-hash-algo
  []
  (MessageDigest/getInstance "SHA1"))

(defn create-hash
  [data-barray]
  (let [^MessageDigest algo (get-hash-algo)]
    (.digest algo data-barray)))

(defn create-signature
  ([^bytes message-bytes]
   (-> (DSAEngine/getInstance)
       (.sign message-bytes ^SigningPrivateKey @priv-key)
       (.getData)))
  ([^bytes priv-key-bytes ^bytes message-bytes]
   (-> (DSAEngine/getInstance)
       (.sign message-bytes
              ^SigningPrivateKey (SigningPrivateKey. priv-key-bytes))
       (.getData))))

(defn verify-signature
  [^bytes pub-key-bytes ^bytes sig-bytes ^bytes message-bytes]
  (when (and pub-key-bytes sig-bytes message-bytes)
    (.verifySignature (DSAEngine/getInstance)
                      (Signature. sig-bytes)
                      message-bytes
                      0
                      (alength message-bytes)
                      (SigningPublicKey. pub-key-bytes))))
