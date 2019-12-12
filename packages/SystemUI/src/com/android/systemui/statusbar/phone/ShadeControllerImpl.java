/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.android.systemui.assist.AssistManager;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.StatusBarState;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;

/** An implementation of {@link com.android.systemui.statusbar.phone.ShadeController}. */
@Singleton
public class ShadeControllerImpl implements ShadeController {

    private static final String TAG = "ShadeControllerImpl";
    private static final boolean SPEW = false;

    private final CommandQueue mCommandQueue;
    private final StatusBarStateController mStatusBarStateController;
    protected final StatusBarWindowController mStatusBarWindowController;
    private final StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private final int mDisplayId;
    protected final Lazy<StatusBar> mStatusBarLazy;
    private final Lazy<AssistManager> mAssistManagerLazy;
    private final Lazy<BubbleController> mBubbleControllerLazy;

    private final ArrayList<Runnable> mPostCollapseRunnables = new ArrayList<>();

    @Inject
    public ShadeControllerImpl(
            CommandQueue commandQueue,
            StatusBarStateController statusBarStateController,
            StatusBarWindowController statusBarWindowController,
            StatusBarKeyguardViewManager statusBarKeyguardViewManager,
            WindowManager windowManager,
            Lazy<StatusBar> statusBarLazy,
            Lazy<AssistManager> assistManagerLazy,
            Lazy<BubbleController> bubbleControllerLazy
    ) {
        mCommandQueue = commandQueue;
        mStatusBarStateController = statusBarStateController;
        mStatusBarWindowController = statusBarWindowController;
        mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
        mDisplayId = windowManager.getDefaultDisplay().getDisplayId();
        // TODO: Remove circular reference to StatusBar when possible.
        mStatusBarLazy = statusBarLazy;
        mAssistManagerLazy = assistManagerLazy;
        mBubbleControllerLazy = bubbleControllerLazy;
    }

    @Override
    public void instantExpandNotificationsPanel() {
        // Make our window larger and the panel expanded.
        getStatusBar().makeExpandedVisible(true /* force */);
        getNotificationPanelView().expand(false /* animate */);
        mCommandQueue.recomputeDisableFlags(mDisplayId, false /* animate */);
    }

    @Override
    public void animateCollapsePanels() {
        animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
    }

    @Override
    public void animateCollapsePanels(int flags) {
        animateCollapsePanels(flags, false /* force */, false /* delayed */,
                1.0f /* speedUpFactor */);
    }

    @Override
    public void animateCollapsePanels(int flags, boolean force) {
        animateCollapsePanels(flags, force, false /* delayed */, 1.0f /* speedUpFactor */);
    }

    @Override
    public void animateCollapsePanels(int flags, boolean force, boolean delayed) {
        animateCollapsePanels(flags, force, delayed, 1.0f /* speedUpFactor */);
    }

    @Override
    public void animateCollapsePanels(int flags, boolean force, boolean delayed,
            float speedUpFactor) {
        if (!force && mStatusBarStateController.getState() != StatusBarState.SHADE) {
            runPostCollapseRunnables();
            return;
        }
        if (SPEW) {
            Log.d(TAG, "animateCollapse():"
                    + " mExpandedVisible=" + getStatusBar().isExpandedVisible()
                    + " flags=" + flags);
        }

        if ((flags & CommandQueue.FLAG_EXCLUDE_RECENTS_PANEL) == 0) {
            getStatusBar().postHideRecentApps();
        }

        // TODO(b/62444020): remove when this bug is fixed
        Log.v(TAG, "mStatusBarWindow: " + getStatusBarWindowView() + " canPanelBeCollapsed(): "
                + getNotificationPanelView().canPanelBeCollapsed());
        if (getStatusBarWindowView() != null && getNotificationPanelView().canPanelBeCollapsed()) {
            // release focus immediately to kick off focus change transition
            mStatusBarWindowController.setStatusBarFocusable(false);

            getStatusBar().getStatusBarWindowViewController().cancelExpandHelper();
            getStatusBarView().collapsePanel(true /* animate */, delayed, speedUpFactor);
        } else {
            mBubbleControllerLazy.get().collapseStack();
        }
    }


    @Override
    public boolean closeShadeIfOpen() {
        if (!getNotificationPanelView().isFullyCollapsed()) {
            mCommandQueue.animateCollapsePanels(
                    CommandQueue.FLAG_EXCLUDE_RECENTS_PANEL, true /* force */);
            getStatusBar().visibilityChanged(false);
            mAssistManagerLazy.get().hideAssist();
        }
        return false;
    }

    @Override
    public void postOnShadeExpanded(Runnable executable) {
        getNotificationPanelView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (getStatusBar().getStatusBarWindow().getHeight()
                                != getStatusBar().getStatusBarHeight()) {
                            getNotificationPanelView().getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                            getNotificationPanelView().post(executable);
                        }
                    }
                });
    }

    @Override
    public void addPostCollapseAction(Runnable action) {
        mPostCollapseRunnables.add(action);
    }

    @Override
    public void runPostCollapseRunnables() {
        ArrayList<Runnable> clonedList = new ArrayList<>(mPostCollapseRunnables);
        mPostCollapseRunnables.clear();
        int size = clonedList.size();
        for (int i = 0; i < size; i++) {
            clonedList.get(i).run();
        }
        mStatusBarKeyguardViewManager.readyForKeyguardDone();
    }

    @Override
    public void goToLockedShade(View startingChild) {
        // TODO: Move this code out of StatusBar into ShadeController.
        getStatusBar().goToLockedShade(startingChild);
    }

    @Override
    public boolean collapsePanel() {
        if (!getNotificationPanelView().isFullyCollapsed()) {
            // close the shade if it was open
            animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_RECENTS_PANEL,
                    true /* force */, true /* delayed */);
            getStatusBar().visibilityChanged(false);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void collapsePanel(boolean animate) {
        if (animate) {
            boolean willCollapse = collapsePanel();
            if (!willCollapse) {
                runPostCollapseRunnables();
            }
        } else if (!getPresenter().isPresenterFullyCollapsed()) {
            getStatusBar().instantCollapseNotificationPanel();
            getStatusBar().visibilityChanged(false);
        } else {
            runPostCollapseRunnables();
        }
    }

    private StatusBar getStatusBar() {
        return mStatusBarLazy.get();
    }

    private NotificationPresenter getPresenter() {
        return getStatusBar().getPresenter();
    }

    protected StatusBarWindowView getStatusBarWindowView() {
        return getStatusBar().getStatusBarWindow();
    }

    protected PhoneStatusBarView getStatusBarView() {
        return (PhoneStatusBarView) getStatusBar().getStatusBarView();
    }

    private NotificationPanelView getNotificationPanelView() {
        return getStatusBar().getPanel();
    }
}