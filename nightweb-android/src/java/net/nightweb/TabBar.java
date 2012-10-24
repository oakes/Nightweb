/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nightweb;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tabbed title bar for xlarge screen browser
 */
public class TabBar extends LinearLayout implements OnClickListener {

    private Activity mActivity;

    private int mTabWidth;

    private TabScrollView mTabs;

    private ImageButton mNewTab;
    private int mButtonWidth;

    private int mTabOverlap;
    private int mAddTabOverlap;
    private int mTabSliceWidth;

    public TabBar(Activity activity) {
        super(activity);
        mActivity = activity;
        Resources res = activity.getResources();
        mTabWidth = (int) res.getDimension(R.dimen.tab_width);

        LayoutInflater factory = LayoutInflater.from(activity);
        factory.inflate(R.layout.tab_bar, this);
        setPadding(0, (int) res.getDimension(R.dimen.tab_padding_top), 0, 0);
        mTabs = (TabScrollView) findViewById(R.id.tabs);
        mNewTab = (ImageButton) findViewById(R.id.newtab);
        mNewTab.setOnClickListener(this);

        mButtonWidth = -1;
        // tab dimensions
        mTabOverlap = (int) res.getDimension(R.dimen.tab_overlap);
        mAddTabOverlap = (int) res.getDimension(R.dimen.tab_addoverlap);
        mTabSliceWidth = (int) res.getDimension(R.dimen.tab_slice);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Resources res = mActivity.getResources();
        // force update of tab bar
        mTabs.updateLayout();
    }

    @Override
    protected void onMeasure(int hspec, int vspec) {
        super.onMeasure(hspec, vspec);
        int w = getMeasuredWidth();
        // adjust for new tab overlap
        w -= mAddTabOverlap;
        setMeasuredDimension(w, getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // use paddingLeft and paddingTop
        int pl = getPaddingLeft();
        int pt = getPaddingTop();
        int sw = mTabs.getMeasuredWidth();
        int w = right - left - pl;
		mButtonWidth = mNewTab.getMeasuredWidth() - mAddTabOverlap;
		if (w-sw < mButtonWidth) {
			sw = w - mButtonWidth;
		}
        mTabs.layout(pl, pt, pl + sw, bottom - top);
        // adjust for overlap
		mNewTab.layout(pl + sw - mAddTabOverlap, pt,
				pl + sw + mButtonWidth - mAddTabOverlap, bottom - top);
    }

    public void onClick(View view) {
		if (mNewTab == view) {
			// create new tab
			Tab tab = new Tab();
			TabView tv = buildTabView(tab);
					mTabs.addTab(tv);
		} else if (mTabs.getSelectedTab() == view) {
			// do nothing
		} else if (view instanceof TabView) {
			// switch to existing tab
			final Tab tab = ((TabView) view).mTab;
				int ix = mTabs.getChildIndex(view);
				if (ix >= 0) {
					mTabs.setSelectedTab(ix);
				}
		}
    }

    private TabView buildTabView(Tab tab) {
        TabView tabview = new TabView(mActivity, tab, mTabs);
        tabview.setOnClickListener(this);
        return tabview;
    }

    /**
     * View used in the tab bar
     */
    class TabView extends LinearLayout implements OnClickListener {

        Tab mTab;
		TabScrollView mTabs;
        View mTabContent;
        TextView mTitle;
        ImageView mIconView;
        ImageView mLock;
        ImageView mClose;
        boolean mSelected;
        Path mPath;
        Path mFocusPath;
        int[] mWindowPos;

        /**
         * @param context
         */
        public TabView(Context context, Tab tab, TabScrollView tabs) {
            super(context);
            setWillNotDraw(false);
            mPath = new Path();
            mFocusPath = new Path();
            mWindowPos = new int[2];
            mTab = tab;
	    	mTabs = tabs;
            setGravity(Gravity.CENTER_VERTICAL);
            setOrientation(LinearLayout.HORIZONTAL);
            setPadding(mTabOverlap, 0, mTabSliceWidth, 0);
            LayoutInflater inflater = LayoutInflater.from(getContext());
            mTabContent = inflater.inflate(R.layout.tab_title, this, true);
            mTitle = (TextView) mTabContent.findViewById(R.id.title);
            mIconView = (ImageView) mTabContent.findViewById(R.id.favicon);
            mLock = (ImageView) mTabContent.findViewById(R.id.lock);
            mClose = (ImageView) mTabContent.findViewById(R.id.close);
            mClose.setOnClickListener(this);
            mSelected = false;
            // update the status
            updateFromTab();
        }

        @Override
        public void onClick(View v) {
            if (v == mClose) {
                closeTab();
            }
        }

        private void updateFromTab() {
            String displayTitle = mTab.getTitle();
            setDisplayTitle(displayTitle);
        }

        @Override
        public void setActivated(boolean selected) {
            mSelected = selected;
            mClose.setVisibility(mSelected ? View.VISIBLE : View.GONE);
            mIconView.setVisibility(mSelected ? View.GONE : View.VISIBLE);
            mTitle.setTextAppearance(mActivity, mSelected ?
                    R.style.TabTitleSelected : R.style.TabTitleUnselected);
            setHorizontalFadingEdgeEnabled(!mSelected);
            super.setActivated(selected);
            updateLayoutParams();
            setFocusable(!selected);
            postInvalidate();
        }

        public void updateLayoutParams() {
            LayoutParams lp = (LinearLayout.LayoutParams) getLayoutParams();
            lp.width = mTabWidth;
            lp.height =  LayoutParams.MATCH_PARENT;
            setLayoutParams(lp);
        }

        void setDisplayTitle(String title) {
            mTitle.setText(title);
        }

        private void closeTab() {
			mTabs.removeTab(this);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            setTabPath(mPath, 0, 0, r - l, b - t);
            setFocusPath(mFocusPath, 0, 0, r - l, b - t);
        }

        private void setTabPath(Path path, int l, int t, int r, int b) {
            path.reset();
            path.moveTo(l, b);
            path.lineTo(l, t);
            path.lineTo(r - mTabSliceWidth, t);
            path.lineTo(r, b);
            path.close();
        }

        private void setFocusPath(Path path, int l, int t, int r, int b) {
            path.reset();
            path.moveTo(l, b);
            path.lineTo(l, t);
            path.lineTo(r - mTabSliceWidth, t);
            path.lineTo(r, b);
        }

    }
}
