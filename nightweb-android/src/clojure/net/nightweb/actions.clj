(ns net.nightweb.actions
  (:use [neko.resource :only [get-resource get-string]]
        [net.nightweb.views :only [get-profile-view get-new-post-view]]))

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

(defn show-dialog
  [context view buttons]
  (let [builder (android.app.AlertDialog$Builder. context)
        btn-action (fn [func]
                     (proxy [android.content.DialogInterface$OnClickListener] []
                       (onClick [dialog which]
                         (if func (func dialog)))))]
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
    (.create builder)
    (.show builder)))

(defn do-menu-action
  [context item]
  (if (= (.getItemId item) (get-resource :id :android/home))
    (show-page context "net.nightweb.MainPage" {})))

(defn show-new-post
  [context content]
  (show-dialog context
               (get-new-post-view context
                                  [{:title (get-string :attach_users)}
                                   {:title (get-string :attach_photos)}
                                   {:title (get-string :attach_videos)}
                                   {:title (get-string :attach_audio)}])
               {:positive-name (get-string :send)
                :positive-func (fn [dialog] (println "send"))
                :neutral-name (get-string :attach)
                :neutral-func (fn [dialog] (println "attach"))
                :negative-name (get-string :cancel)
                :negative-func (fn [dialog] (println "cancel"))}))

(defn show-profile
  [context content]
  (show-dialog context
               (get-profile-view context content)
               {:positive-name (get-string :save)
                :positive-func (fn [dialog] (println "save"))
                :negative-name (get-string :cancel)
                :negative-func (fn [dialog] (println "cancel"))}))

(defn show-favorites
  [context content]
  (show-page context "net.nightweb.FavoritesPage" {}))

(defn show-downloads
  [context content]
  (show-page context "net.nightweb.DownloadsPage" {}))

(defn show-tags
  [context content]
  (show-page context "net.nightweb.GridPage" {:title (get-string :tags)}))
