(ns net.nightweb.actions
  (:use [neko.resource :only [get-resource get-string]]
        [nightweb.router :only [base-dir get-user-hash create-meta-torrent]]
        [nightweb.io :only [base32-encode
                            write-post-file
                            write-profile-file]]))

(defn share-url
  [context]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)
        content (get @(.state context) :share-content)
        type-param (get content :type)
        hash-param (get content :hash)
        params (concat
                 (if type-param [(str "type=" (name type-param))] nil)
                 (if hash-param [(str "hash=" (base32-encode hash-param))] nil))
        url (str "http://nightweb.net#" (clojure.string/join "&" params))]
    (.setType intent "text/plain")
    (.putExtra intent android.content.Intent/EXTRA_TEXT url)
    (.startActivity context intent)))

(defn show-page
  [context class-name params]
  (let [class-symbol (java.lang.Class/forName class-name)
        intent (android.content.Intent. context class-symbol)]
    (.putExtra intent "params" params)
    (.startActivity context intent)))

(defn show-dialog
  [context view buttons]
  (let [builder (android.app.AlertDialog$Builder. context)
        btn-action (fn [func]
                     (proxy [android.content.DialogInterface$OnClickListener] []
                       (onClick [dialog which]
                         (if func (func view)))))]
    (if-let [positive-name (get buttons :positive-name)]
      (.setPositiveButton builder
                          positive-name
                          (btn-action (get buttons :positive-func))))
    (if-let [neutral-name (get buttons :neutral-name)]
      (.setNeutralButton builder
                         neutral-name
                         (btn-action (get buttons :neutral-func))))
    (if-let [negative-name (get buttons :negative-name)]
      (.setNegativeButton builder
                          negative-name
                          (btn-action (get buttons :negative-func))))
    (.setView builder view)
    (let [dialog (.create builder)]
      (.setCanceledOnTouchOutside dialog false)
      (.show dialog))))

(defn show-favorites
  [context content]
  (show-page context "net.nightweb.FavoritesPage" (get content :content)))

(defn show-transfers
  [context content]
  (show-page context "net.nightweb.TransfersPage" (get content :content)))

(defn show-grid
  [context content]
  (show-page context "net.nightweb.GridPage" content))

(defn do-send-new-post
  [dialog-view]
  (let [text (.toString (.getText dialog-view))]
    (write-post-file base-dir (get-user-hash false) text)
    (create-meta-torrent)))

(defn do-attach-to-new-post
  [dialog-view]
  (println "attach"))

(defn do-save-profile
  [dialog-view]
  (let [linear-layout (.getChildAt dialog-view 0)
        name-field (.getChildAt linear-layout 0)
        about-field (.getChildAt linear-layout 1)
        name-text (.toString (.getText name-field))
        about-text (.toString (.getText about-field))]
    (write-profile-file base-dir (get-user-hash false) name-text about-text))
    (create-meta-torrent))

(defn do-cancel
  [dialog-view]
  (println "cancel"))

(defn do-menu-action
  [context item]
  (if (= (.getItemId item) (get-resource :id :android/home))
    (show-page context "net.nightweb.MainPage" {})))

(defn do-tile-action
  [context item]
  (if-let [func (case (get item :type)
                  :tags show-grid
                  :users show-grid
                  :favorites show-favorites
                  :transfers show-transfers
                  :custom-func (get item :func)
                  nil)]
    (func context item)))
