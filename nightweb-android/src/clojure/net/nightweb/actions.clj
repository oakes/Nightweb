(ns net.nightweb.actions
  (:use [neko.resource :only [get-resource get-string]]
        [net.nightweb.views :only [get-profile-view]]))

(defn share-url
  [context]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)]
    (.setType intent "text/plain")
    (.putExtra intent android.content.Intent/EXTRA_TEXT "nightweb://asdf")
    (.startActivity context intent)))

(defn show-page
  [context class-name params]
  (let [class-symbol (java.lang.Class/forName class-name)
        intent (android.content.Intent. context class-symbol)]
    (.putExtra intent "params" (pr-str params))
    (.startActivity context intent)))

(defn show-save-dialog
  [context view]
  (let [builder (android.app.AlertDialog$Builder. context)
        do-save (proxy [android.content.DialogInterface$OnClickListener] []
                  (onClick [dialog which]))
        do-cancel (proxy [android.content.DialogInterface$OnClickListener] []
                    (onClick [dialog which]))]
    (.setView builder view)
    (.setPositiveButton builder (get-string :save) do-save)
    (.setNegativeButton builder (get-string :cancel) do-cancel)
    (.create builder)
    (.show builder)))

(defn do-menu-action
  [context item]
  (if (= (.getItemId item) (get-resource :id :android/home))
    (show-page context "net.nightweb.MainPage" {})))

(defn show-profile
  [context content]
  (show-save-dialog context (get-profile-view context content)))

(defn show-favorites
  [context content]
  (show-page context "net.nightweb.FavoritesPage" {}))

(defn show-downloads
  [context content]
  (show-page context "net.nightweb.DownloadsPage" {}))

(defn show-tags
  [context content]
  (show-page context "net.nightweb.GridPage" {:title (get-string :tags)}))
