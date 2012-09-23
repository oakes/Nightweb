(ns nightweb.router
  (:import net.i2p.router.Router
           net.i2p.i2ptunnel.I2PTunnel
           java.net.ProxySelector
           java.net.Proxy
           java.net.InetSocketAddress))

(defn start-http-proxy
  "Launch the http proxy."
  []
  (let [proxy-selector (proxy [ProxySelector] []
                         (select [uri]
                                 [(Proxy. java.net.Proxy$Type/HTTP
                                          (InetSocketAddress. "localhost" 4707))])
                         (connectFailed [uri socket exception]
                                        (println "Connect failed" (.toString uri))))]
    (ProxySelector/setDefault proxy-selector))
  (future (I2PTunnel/main
            (into-array java.lang.String ["-nogui" "-nocli" "-e" "httpclient 4707"]))))

(defn start-router
  "Launch the router."
  []
  (Router/main nil))