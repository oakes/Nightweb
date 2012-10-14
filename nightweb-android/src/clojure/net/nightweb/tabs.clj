(ns net.nightweb.tabs
  (:use neko.resource
        neko.listeners.view
        neko.find-view))

(defn set-state!
  [this mkey mval]
  (swap! (.state this) assoc-in [mkey] mval))

(defn get-state
  [this mkey]
  (get @(.state this) mkey))

(do
  (gen-class
    :name "net.nightweb.tabs.TabLayout"
    :extends android.widget.LinearLayout
    :state "state"
    :init "init"
    :post-init "post-init"
    :prefix "tab-layout-"
    :constructors {[android.content.Context] [android.content.Context]
                   [android.content.Context android.util.AttributeSet] [android.content.Context android.util.AttributeSet]
                   [android.content.Context android.util.AttributeSet int] [android.content.Context android.util.AttributeSet int]}
    :methods []
    :exposes-methods {onMeasure superOnMeasure
                      onLayout superOnLayout
                      setMeasuredDimension superSetMeasuredDimension})
  (defn tab-layout-create-state
    [context]
    (let [res (.getResources context)
          tab-overlap (.getDimension res (get-resource :dimen :tab_overlap))]
      (atom {:tab-overlap tab-overlap})))
  (defn tab-layout-init
    ([context] [[context] (tab-layout-create-state context)])
    ([context attrs] [[context attrs] (tab-layout-create-state context)])
    ([context attrs def-style] [[context attrs def-style] (tab-layout-create-state context)]))
  (defn tab-layout-post-init
    ([this context]
;      (.setChildrenDrawingOrderEnabled this true)
      )
    ([this context attrs] (tab-layout-post-init this context))
    ([this context attrs def-style] (tab-layout-post-init this context)))
  (defn tab-layout-onMeasure
    [this hspec vspec]
    (.superOnMeasure this hspec vspec)
    (let [child-count (- (.getChildCount this) 1)
          max (java.lang.Math/max 0 child-count)
          tab-overlap (get-state this :tab-overlap)]
;      (.superSetMeasuredDimension this
;                                  (- (.getMeasuredWidth this) (* max tab-overlap))
;                                  (.getMeasuredHeight this))
      ))
  (defn tab-layout-onLayout
    [this changed l t r b]
    (.superOnLayout this changed l t r b)
    (if (> (.getChildCount this) 0)
      (for [i (range 1 (.getChildCount this))
            :let [first-child (.getChildAt this 0)
                  tab-overlap (get-state this :tab-overlap)
                  left (- (.getRight first-child) tab-overlap)
                  total-left (* left i)
                  tab (.getChildAt this i)
                  w (- (.getRight tab) (.getLeft tab))]]
        (.layout tab total-left (.getTop tab) (+ total-left w) (.getBottom tab)))))
  (defn tab-layout-getChildDrawingOrder
    [this count i]
    ())
    )

(do
  (gen-class
    :name "net.nightweb.tabs.TabScrollView"
    :extends android.widget.HorizontalScrollView
    :state "state"
    :init "init"
    :post-init "post-init"
    :prefix "tab-scroll-view-"
    :constructors {[android.content.Context] [android.content.Context]
                   [android.content.Context android.util.AttributeSet] [android.content.Context android.util.AttributeSet]
                   [android.content.Context android.util.AttributeSet int] [android.content.Context android.util.AttributeSet int]}
    :methods []
    :exposes-methods {onLayout superOnLayout})
  (defn tab-scroll-view-create-state
    [context]
    (atom {:selected -1 :content-view (net.nightweb.tabs.TabLayout. context)}))
  (defn tab-scroll-view-init
    ([context] [[context] (tab-scroll-view-create-state context)])
    ([context attrs] [[context attrs] (tab-scroll-view-create-state context)])
    ([context attrs def-style] [[context attrs def-style] (tab-scroll-view-create-state context)]))
  (defn tab-scroll-view-post-init
    ([this context]
      (let [res (.getResources context)
            tab-first-padding-left (.getDimension res (get-resource :dimen :tab_first_padding_left))
            content-view (get-state this :content-view)]
        (.setHorizontalScrollBarEnabled this false)
        (.setOverScrollMode this android.view.View/OVER_SCROLL_NEVER)
        (.setOrientation content-view android.widget.LinearLayout/HORIZONTAL)
        (.setLayoutParams content-view
                          (android.view.ViewGroup$LayoutParams.
                            android.view.ViewGroup$LayoutParams/WRAP_CONTENT
                            android.view.ViewGroup$LayoutParams/MATCH_PARENT))
        (.setPadding content-view tab-first-padding-left 0 0 0)
        (.addView this content-view)
        (.scrollTo this (.getScrollX this) (.getScrollY this))))
    ([this context attrs] (tab-scroll-view-post-init this context))
    ([this context attrs def-style] (tab-scroll-view-post-init this context)))
  (defn tab-scroll-view-onLayout
    [this changed l t r b]
    (.superOnLayout this changed l t r b)
    (let []
      ))
  )

(do
  (gen-class
    :name "net.nightweb.tabs.TabView"
    :extends android.widget.LinearLayout
    :state "state"
    :init "init"
    :post-init "post-init"
    :prefix "tab-view-"
    :constructors {[android.content.Context] [android.content.Context]
                   [android.content.Context android.util.AttributeSet] [android.content.Context android.util.AttributeSet]
                   [android.content.Context android.util.AttributeSet int] [android.content.Context android.util.AttributeSet int]}
    :methods [["setTitle" [String] void]]
    :exposes-methods {onLayout superOnLayout})
  (defn tab-view-create-state
    [context]
    (atom {:selected false
           :path (android.graphics.Path.)
           :focus-path (android.graphics.Path.)
           :tab-slice-width (get-resource :dimen :tab_slice)
           :tab-overlap (get-resource :dimen :tab_slice)}))
  (defn tab-view-init
    ([context] [[context] (tab-view-create-state context)])
    ([context attrs] [[context attrs] (tab-view-create-state context)])
    ([context attrs def-style] [[context attrs def-style] (tab-view-create-state context)]))
  (defn tab-view-post-init
    ([this context]
      (.setWillNotDraw this false)
      (.setGravity this android.view.Gravity/CENTER_VERTICAL)
      (.setOrientation this android.widget.LinearLayout/HORIZONTAL)
      (.setPadding this (get-state this :tab-overlap) 0
                   (get-state this :tab-slice-width) 0)
      (let [inflater (android.view.LayoutInflater/from context)
            tab-content (.inflate inflater (get-resource :layout :tab_title) this true)
            title (find-view tab-content (get-resource :id :title))
            icon-view (find-view tab-content (get-resource :id :favicon))
            lock (find-view tab-content (get-resource :id :lock))
            close (find-view tab-content (get-resource :id :close))]
        (set-state! this :title title)))
    ([this context attrs] (tab-view-post-init this context))
    ([this context attrs def-style] (tab-view-post-init this context)))
  (defn tab-view-setActivated
    [this selected]
    (set-state! this :selected selected))
  (defn tab-view-onLayout
    [this changed l t r b]
    (.superOnLayout this changed l t r b)
    (let [path (get-state this :path)
          focus-path (get-state this :focus-path)
          tab-slice-width (get-state this :tab-slice-width)
          local-l 0
          local-t 0
          local-r (- r l)
          local-b (- b t)]
      (.reset path)
      (.moveTo path local-l local-b)
      (.lineTo path local-l local-t)
      (.lineTo path (- local-r tab-slice-width) local-t)
      (.lineTo path local-r local-b)
      (.close path)
      (.reset focus-path)
      (.moveTo focus-path local-l local-b)
      (.lineTo focus-path local-l local-t)
      (.lineTo focus-path (- local-r (get-state this :tab-slice-width)) local-t)
      (.lineTo focus-path local-r local-b)))
  (defn tab-view-setTitle
    [this text]
    (let [title (get-state this :title)]
      (.setText title text)))
  )

(do
  (gen-class
    :name "net.nightweb.tabs.TabBar"
    :extends android.widget.LinearLayout
    :state "state"
    :init "init"
    :post-init "post-init"
    :prefix "tab-bar-"
    :constructors {[android.content.Context] [android.content.Context]
                   [android.content.Context android.util.AttributeSet] [android.content.Context android.util.AttributeSet]
                   [android.content.Context android.util.AttributeSet int] [android.content.Context android.util.AttributeSet int]}
    :methods []
    :exposes-methods {onMeasure superOnMeasure
                      onConfigurationChanged superOnConfigurationChanged
                      setMeasuredDimension superSetMeasuredDimension})
  (defn tab-bar-create-state
    [context]
    (let [res (.getResources context)]
      (atom {:context context
             :tab-add-overlap (.getDimension res (get-resource :dimen :tab_addoverlap))
             :tab-width (.getDimension res (get-resource :dimen :tab_width))
             :tab-map {}})))
  (defn tab-bar-init
    ([context] [[context] (tab-bar-create-state context)])
    ([context attrs] [[context attrs] (tab-bar-create-state context)])
    ([context attrs def-style] [[context attrs def-style] (tab-bar-create-state context)]))
  (defn tab-bar-post-init
    ([this context]
      (.inflate (android.view.LayoutInflater/from context)
                (get-resource :layout :tab_bar) this)
      (let [res (.getResources context)
            tab-padding-top (.getDimension res (get-resource :dimen :tab_padding_top))
            tabs (find-view this (get-resource :id :tabs))
            new-tab (find-view this (get-resource :id :newtab))]
        (.setPadding this 0 tab-padding-top 0 0)
        (.setOnClickListener new-tab
                             (on-click (println "on-click")))
        (set-state! this :tabs tabs)
        (set-state! this :new-tab new-tab)
        ))
    ([this context attrs] (tab-bar-post-init this context))
    ([this context attrs def-style] (tab-bar-post-init this context)))
  (defn tab-bar-onConfigurationChanged
    [this new-config]
    (.superOnConfigurationChanged this new-config)
    (let [context (get-state this :context)
          res (.getResources context)
          tab-width (get-resource :dimen :tab_width)]
      (set-state! this :tab-width (.getDimension res tab-width))
      (.updateLayout (get-state this :tabs))))
;  (defn tab-bar-onMeasure
;    [this hspec vspec]
;    (.superOnMeasure this hspec vspec)
;    (.superSetMeasuredDimension this
;                           (- (.getMeasuredWidth this)
;                              (get-state this :tab-add-overlap))
;                           (.getMeasuredHeight this)))
;  (defn tab-bar-onLayout
;    [this changed left top right bottom]
;    (let [new-tab (get-state this :new-tab)
;          tabs (get-state this :tabs)
;          pl (.getPaddingLeft this)
;          pt (.getPaddingTop this)
;          w (- right left pl)
;          tab-add-overlap (get-state this :tab-add-overlap)
;          button-width (- (.getMeasuredWidth new-tab) tab-add-overlap)
;          sw (- w button-width)]
;      (.layout tabs pl pt (+ pl sw) (- bottom top))
;      (.layout new-tab (- (+ pl sw) tab-add-overlap) pt
;               (- (+ pl sw button-width) tab-add-overlap) (- bottom top))))
  )

(defn show-tab-bar
  [context]
  (let [action-bar (.getActionBar context)
        tab-bar (net.nightweb.tabs.TabBar. context)]
    (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_STANDARD)
    (.setDisplayOptions action-bar android.app.ActionBar/DISPLAY_SHOW_CUSTOM)
    (.setCustomView action-bar tab-bar)))