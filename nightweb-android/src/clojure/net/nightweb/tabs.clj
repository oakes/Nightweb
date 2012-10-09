(ns net.nightweb.tabs
  (:import android.app.ActionBar
           net.nightweb.TabBar))

(defn show-tab-bar
  [context]
  (let [action-bar (.getActionBar context)
        tab-bar (TabBar. context)]
    (.setNavigationMode action-bar ActionBar/NAVIGATION_MODE_STANDARD)
    (.setDisplayOptions action-bar ActionBar/DISPLAY_SHOW_CUSTOM)
    (.setCustomView action-bar tab-bar)))