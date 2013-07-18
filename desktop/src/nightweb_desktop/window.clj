(ns nightweb-desktop.window
  (:require [seesaw.core :as s]
            [nightweb.router :as router]
            [nightweb-desktop.server :as server]
            [nightweb-desktop.utils :as utils]))

(def external-ip (delay (try (-> (slurp "http://checkip.amazonaws.com/")
                                 (clojure.string/replace "\n" ""))
                          (catch Exception e nil))))

(declare update-window-content)

(defn open-in-browser
  "Opens the address in the default web browser."
  [address]
  (when (java.awt.Desktop/isDesktopSupported)
    (.browse (java.awt.Desktop/getDesktop) (java.net.URI. address))))

(defn get-external-address
  "Returns the address you'll use for remote access."
  []
  (if @external-ip
    (str "http://" @external-ip ":" @server/port)
    (str "http://EXTERNAL_IP:" @server/port)))

(defn get-main-items
  "Returns items to always display in the window."
  [ui-root]
  [(s/button :text (utils/get-string :open_in_browser)
             :listen [:action (fn [e]
                                (-> (str "http://localhost:" @server/port)
                                    open-in-browser))])
   (-> (s/combobox :model (keys utils/lang-files)
                   :listen [:action (fn [e]
                                      (when (not= (s/selection e) @utils/lang)
                                        (utils/change-lang (s/selection e))
                                        (update-window-content ui-root)))])
       (s/selection! @utils/lang))
   (s/checkbox :text (utils/get-string :check_for_updates)
               :selected? @utils/update?
               :listen [:action (fn [e]
                                  (->> (not @utils/update?)
                                       (reset! utils/update?)
                                       (utils/write-pref :update)))])
   (s/checkbox :text (utils/get-string :enable_remote)
               :selected? @utils/remote?
               :listen [:action (fn [e]
                                  (->> (not @utils/remote?)
                                       (reset! utils/remote?)
                                       (utils/write-pref :remote))
                                  (server/start-server)
                                  (update-window-content ui-root))])])

(defn get-remote-items
  "Returns items to only display when remote access is enabled."
  [ui-root]
  [(utils/get-string :forward_port)
   (s/text :text @server/port
           :listen [:key-released (fn [e]
                                    (when (server/set-port (s/text e))
                                      (s/text! (s/select ui-root [:#address])
                                               (get-external-address))))])
   (utils/get-string :open_address)
   (s/text :id :address
           :text (get-external-address)
           :editable? false)])

(defn update-window-content
  "Updates the items in the window."
  [ui-root]
  (let [main-items (get-main-items ui-root)
        items (if @utils/remote?
                (concat main-items (get-remote-items ui-root))
                main-items)]
    (s/config! (s/select ui-root [:#grid])
               :rows (count items)
               :columns 1
               :items items)
    (s/pack! ui-root)))

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
                 :content (s/grid-panel :id :grid :vgap 5 :border 20)
                 :on-close :exit)
        update-window-content
        s/show!)))
