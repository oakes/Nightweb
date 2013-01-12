(ns nightweb.crypto
  (:use [clojure.java.io :only [file writer]]))

(def private-key nil)
(def public-key nil)

(defn create-key-if-not-exists
  [key-file-path]
  (if (.exists (file key-file-path))
    (let [file-contents (slurp key-file-path)]
      (def private-key (net.i2p.data.SigningPrivateKey. file-contents))
      (def public-key (.toPublic private-key)))
    (let [context (net.i2p.I2PAppContext/getGlobalContext)
          key-gen (.keyGenerator context)
          signing-keys (.generateSigningKeypair key-gen)]
      (def private-key (aget signing-keys 1))
      (def public-key (aget signing-keys 0))
      (with-open [wrtr (writer key-file-path)]
        (.write wrtr (.toBase64 private-key))))))

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
