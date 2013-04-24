(ns nightweb-desktop.window
  (:use [seesaw.core :only [invoke-later frame pack! show! button alert]]
        [nightweb.router :only [stop-router]]
        [nightweb-desktop.utils :only [get-string]]
        [nightweb-desktop.server :only [port]]))

(defn open-in-browser
  [e]
  (let [address (str "http://localhost:" port)]
    (if (java.awt.Desktop/isDesktopSupported)
      (let [uri (java.net.URI. address)]
        (.browse (java.awt.Desktop/getDesktop) uri))
      (alert e address))))

(defn start-window
  "Launches a window."
  []
  (invoke-later
    (-> (frame :title (get-string :app_name)
               :content (button :text (get-string :open_in_browser)
                                :listen [:action #(open-in-browser %)])
               :on-close :exit)
        pack!
        show!)))
