(ns net.nightweb.actions
  (:use [neko.resource :only [get-resource get-string]]
        [neko.threading :only [on-ui]]
        [neko.find-view :only [find-view]]
        [net.nightweb.clandroid.activity :only [set-state get-state]]
        [nightweb.router :only [create-meta-torrent]]
        [nightweb.io :only [write-post-file
                            write-profile-file]]
        [nightweb.formats :only [base32-encode
                                 url-encode]]))

(defn share-url
  [context]
  (let [intent (android.content.Intent.
                 android.content.Intent/ACTION_SEND)
        url (url-encode (get-state context :share))]
    (.setType intent "text/plain")
    (.putExtra intent android.content.Intent/EXTRA_TEXT url)
    (.startActivity context intent)))

(defn request-image
  [context callback]
  (set-state context :request-image callback)
  (let [intent (android.content.Intent.
                 android.content.Intent/ACTION_GET_CONTENT)]
    (.setType intent "image/*")
    (.startActivityForResult context intent 1)))

(defn receive-result
  [context request-code result-code intent]
  (if intent
    (case request-code
      1 (if-let [callback (get-state context :request-image)]
          (callback (.getData intent))))))

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
                         (if func (func context view)))))]
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

(defn show-basic
  [context content]
  (show-page context "net.nightweb.BasicPage" content))

(defn do-refresh-page
  [context]
  (on-ui (.recreate context)))

(defn do-send-new-post
  [context dialog-view]
  (let [text (.toString (.getText dialog-view))]
    (write-post-file text))
  (future
    (create-meta-torrent)
    (do-refresh-page context)))

(defn do-attach-to-new-post
  [context dialog-view]
  (println "attach"))

(defn do-save-profile
  [context dialog-view]
  (let [linear-layout (.getChildAt dialog-view 0)
        name-field (.getChildAt linear-layout 0)
        body-field (.getChildAt linear-layout 1)
        image-view (.getChildAt linear-layout 2)
        name-text (.toString (.getText name-field))
        body-text (.toString (.getText body-field))
        image-bitmap (if-let [drawable (.getDrawable image-view)]
                       (.getBitmap drawable))]
    (write-profile-file name-text body-text image-bitmap))
  (future
    (create-meta-torrent)
    (do-refresh-page context)))

(defn do-cancel
  [context dialog-view]
  (println "cancel"))

(defn do-menu-action
  [context item]
  (if (= (.getItemId item) (get-resource :id :android/home))
    (show-page context "net.nightweb.MainPage" {})))

(defn do-tile-action
  [context item]
  (if-let [func (case (get item :type)
                  :fav show-favorites
                  :tran show-transfers
                  :custom-func (get item :func)
                  show-basic)]
    (func context item)))
