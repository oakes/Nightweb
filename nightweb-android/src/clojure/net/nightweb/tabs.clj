(ns net.nightweb.tabs
  (:import android.app.ActionBar
           ;net.nightweb.TabBar
           android.content.Context
           android.widget.LinearLayout)
  (:use neko.resource))

(do
  (gen-class
    :name "TabBar"
    :extends android.widget.LinearLayout
    :state "state"
    :init "init"
    :prefix "tab-bar-"
    :constructors {[android.content.Context] [android.content.Context]}
    :exposes-methods {onMeasure superOnMeasure
                      onConfigurationChanged superOnConfigurationChanged
                      setMeasuredDimension superSetMeasuredDimension})
  (defn tab-bar-init
    [context]
    (let [res (.getResources context)
          add-tab-overlap (get-resource :dimen :tab_addoverlap)
          tab-width (get-resource :dimen :tab_width)
          new-tab (get-resource :id :newtab)]
      [[context] (atom
                   {:context context
                    :tab-add-overlap (.getDimension res add-tab-overlap)
                    :tab-width (.getDimension res tab-width)
                    :new-tab (.findViewById context new-tab)})]))
  (defn tab-bar-onConfigurationChanged
    [this new-config]
    (.superOnConfigurationChanged this new-config)
    (let [context (get (.state this) :context)
          tab-width (get-resource :dimen :tab_width)
          res (.getResources context)]
      (swap! (.state this)
             (update-in (.state this)
                        [:tab-width]
                        (.getDimension res tab-width)))
      ))
  (comment
  (defn tab-bar-onMeasure
    [this hspec vspec]
    (.superOnMeasure this hspec vspec)
    (.superSetMeasuredDimension this
                           (- (.getMeasuredWidth this)
                              (get (.state this) :tab-add-overlap))
                           (.getMeasuredHeight this))))
  (comment
  (defn tab-bar-onLayout
    [this changed left top right bottom]
    (let [pl (.getPaddingLeft this)
          pt (.getPaddingTop this)
          sw 0
          w (- right left pl)
          new-tab (get (.state this) :new-tab)
          tab-add-overlap (get (.state this) :tab-add-overlap)
          button-width (- (.getMeasuredWidth new-tab) tab-add-overlap)]
      )))
  )

(defn show-tab-bar
  [context]
  (let [action-bar (.getActionBar context)
        tab-bar (TabBar. context)]
    (.setNavigationMode action-bar ActionBar/NAVIGATION_MODE_STANDARD)
    (.setDisplayOptions action-bar ActionBar/DISPLAY_SHOW_CUSTOM)
    (.setCustomView action-bar tab-bar)))