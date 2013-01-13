(ns nightweb.crypto
  (:use [nightweb.io :only [file-exists
                            file-write
                            file-read
                            b-encode
                            b-decode]]))

(def private-key nil)
(def public-key nil)

(defn read-priv-nkey
  [key-file-path]
  (let [nkey-map (b-decode (file-read key-file-path))
        signing-key-str (.get nkey-map "sign-key")]
    (net.i2p.data.SigningPrivateKey. (.getBytes signing-key-str))))

(defn write-priv-nkey
  [key-file-path]
  (let [context (net.i2p.I2PAppContext/getGlobalContext)
        key-gen (.keyGenerator context)
        signing-keys (.generateSigningKeypair key-gen)
        priv-signing-key (aget signing-keys 1)]
    (file-write key-file-path
                (b-encode {"sign-key" (.getData priv-signing-key)
                           "sign-algo" "DSA-SHA1"}))
    priv-signing-key))

(defn create-priv-nkey
  [key-file-path]
  (def private-key
    (if (file-exists key-file-path)
      (read-priv-nkey key-file-path)
      (write-priv-nkey key-file-path)))
  (def public-key (.toPublic private-key)))

(defn create-sig
  [message]
  (.getData (.sign (net.i2p.crypto.DSAEngine/getInstance) message private-key)))

(defn verify-sig
  [sig message]
  (.verifySignature (net.i2p.crypto.DSAEngine/getInstance)
                    (net.i2p.data.Signature. sig)
                    message
                    0
                    (alength message)
                    public-key))
