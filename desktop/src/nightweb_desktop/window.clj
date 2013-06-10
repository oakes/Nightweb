(ns nightweb-desktop.window
  (:use [seesaw.core :only [invoke-later
                            frame
                            native!
                            pack!
                            show!
                            vertical-panel
                            button
                            checkbox
                            label]]
        [nightweb.router :only [stop-router]]
        [nightweb-desktop.utils :only [get-string get-version]]
        [nightweb-desktop.server :only [port]]))

(defn open-in-browser
  "Opens the address in the default web browser."
  [address]
  (when (java.awt.Desktop/isDesktopSupported)
    (.browse (java.awt.Desktop/getDesktop) (java.net.URI. address))))

(defn get-window-content
  "Returns the content to be displayed in the main window."
  []
  (let [address (str "http://localhost:" port)
        open-btn (button :text (get-string :open_in_browser)
                         :listen [:action (fn [e] (open-in-browser address))])
        remote-check (checkbox :text (get-string :enable_remote)
                               :listen [:action (fn [e])]
                               :border 10)
        version-label (label :text (get-version))]
    (vertical-panel :items [open-btn remote-check version-label]
                    :border 10)))

(defn start-window
  "Launches the main window."
  []
  (native!)
  (org.pushingpixels.substance.api.SubstanceLookAndFeel/setSkin
    (org.pushingpixels.substance.api.skin.GraphiteSkin.))
  (invoke-later
    (-> (frame :title (get-string :app_name)
               :content (get-window-content)
               :on-close :exit)
        pack!
        show!)))
