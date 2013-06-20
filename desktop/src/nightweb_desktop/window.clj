(ns nightweb-desktop.window
  (:require [seesaw.core :as s]
            [nightweb.router :as router]
            [nightweb-desktop.server :as server]
            [nightweb-desktop.utils :as utils]))

(defn open-in-browser
  "Opens the address in the default web browser."
  [address]
  (when (java.awt.Desktop/isDesktopSupported)
    (.browse (java.awt.Desktop/getDesktop) (java.net.URI. address))))

(defn get-window-content
  "Returns the content to be displayed in the main window."
  []
  (let [address (str "http://localhost:" server/port)
        open-btn (s/button :text (utils/get-string :open_in_browser)
                           :listen [:action (fn [e] (open-in-browser address))])
        remote-check (s/checkbox :text (utils/get-string :enable_remote)
                                 :listen [:action (fn [e])]
                                 :border 10)]
    (s/vertical-panel :items [open-btn]
                      :border 40)))

(defn start-window
  "Launches the main window."
  []
  (s/native!)
  (org.pushingpixels.substance.api.SubstanceLookAndFeel/setSkin
    (org.pushingpixels.substance.api.skin.GraphiteSkin.))
  (s/invoke-later
    (-> (s/frame :title (str (utils/get-string :app_name)
                             " "
                             (utils/get-version))
                 :content (get-window-content)
                 :on-close :exit)
        s/pack!
        s/show!)))
