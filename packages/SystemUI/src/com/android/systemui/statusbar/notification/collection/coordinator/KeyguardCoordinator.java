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

package com.android.systemui.statusbar.notification.collection.coordinator;

import static android.app.Notification.VISIBILITY_SECRET;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;

import androidx.annotation.MainThread;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.collection.NotifCollection;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.listbuilder.NotifListBuilder;
import com.android.systemui.statusbar.notification.collection.listbuilder.pluggable.NotifFilter;
import com.android.systemui.statusbar.policy.KeyguardStateController;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Filters low priority and privacy-sensitive notifications from the lockscreen.
 */
@Singleton
public class KeyguardCoordinator implements Coordinator {
    private static final String TAG = "KeyguardNotificationCoordinator";

    private final Context mContext;
    private final Handler mMainHandler;
    private final KeyguardStateController mKeyguardStateController;
    private final NotificationLockscreenUserManager mLockscreenUserManager;
    private final BroadcastDispatcher mBroadcastDispatcher;
    private final StatusBarStateController mStatusBarStateController;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;

    @Inject
    public KeyguardCoordinator(
            Context context,
            @MainThread Handler mainThreadHandler,
            KeyguardStateController keyguardStateController,
            NotificationLockscreenUserManager lockscreenUserManager,
            BroadcastDispatcher broadcastDispatcher,
            StatusBarStateController statusBarStateController,
            KeyguardUpdateMonitor keyguardUpdateMonitor) {
        mContext = context;
        mMainHandler = mainThreadHandler;
        mKeyguardStateController = keyguardStateController;
        mLockscreenUserManager = lockscreenUserManager;

        mBroadcastDispatcher = broadcastDispatcher;
        mStatusBarStateController = statusBarStateController;
        mKeyguardUpdateMonitor = keyguardUpdateMonitor;
    }

    @Override
    public void attach(NotifCollection notifCollection, NotifListBuilder notifListBuilder) {
        setupInvalidateNotifListCallbacks();
        notifListBuilder.addFilter(mNotifFilter);
    }

    protected final NotifFilter mNotifFilter = new NotifFilter(TAG) {
        @Override
        public boolean shouldFilterOut(NotificationEntry entry, long now) {
            final StatusBarNotification sbn = entry.getSbn();

            // FILTER OUT the notification when the notification isn't for the current profile
            if (!mLockscreenUserManager.isCurrentProfile(sbn.getUserId())) {
                return true;
            }

            // FILTER OUT the notification when the keyguard is showing and...
            if (mKeyguardStateController.isShowing()) {
                // ... user settings or the device policy manager doesn't allow lockscreen
                // notifications;
                if (!mLockscreenUserManager.shouldShowLockscreenNotifications()) {
                    return true;
                }

                final int currUserId = mLockscreenUserManager.getCurrentUserId();
                final int notifUserId = (sbn.getUser().getIdentifier() == UserHandle.USER_ALL)
                        ? currUserId : sbn.getUser().getIdentifier();

                // ... user is in lockdown
                if (mKeyguardUpdateMonitor.isUserInLockdown(currUserId)
                        || mKeyguardUpdateMonitor.isUserInLockdown(notifUserId)) {
                    return true;
                }

                // ... device is in public mode and the user's settings doesn't allow
                // notifications to show in public mode
                if (mLockscreenUserManager.isLockscreenPublicMode(currUserId)
                        || mLockscreenUserManager.isLockscreenPublicMode(notifUserId)) {
                    if (entry.getRanking().getVisibilityOverride() == VISIBILITY_SECRET) {
                        return true;
                    }

                    if (!mLockscreenUserManager.userAllowsNotificationsInPublic(currUserId)
                            || !mLockscreenUserManager.userAllowsNotificationsInPublic(
                            notifUserId)) {
                        return true;
                    }
                }

                // ... neither this notification nor its summary have high enough priority
                // to be shown on the lockscreen
                // TODO: grouping hasn't happened yet (b/145134683)
                if (entry.getParent() != null) {
                    final NotificationEntry summary = entry.getParent().getRepresentativeEntry();
                    if (priorityExceedsLockscreenShowingThreshold(summary)) {
                        return false;
                    }
                }
                return !priorityExceedsLockscreenShowingThreshold(entry);
            }
            return false;
        }
    };

    private boolean priorityExceedsLockscreenShowingThreshold(NotificationEntry entry) {
        if (entry == null) {
            return false;
        }
        if (NotificationUtils.useNewInterruptionModel(mContext)
                && hideSilentNotificationsOnLockscreen()) {
            // TODO: make sure in the NewNotifPipeline that entry.isHighPriority() has been
            //  correctly updated before reaching this point (b/145134683)
            return entry.isHighPriority();
        } else {
            return !entry.getRanking().isAmbient();
        }
    }

    private boolean hideSilentNotificationsOnLockscreen() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1) == 0;
    }

    private void setupInvalidateNotifListCallbacks() {
        // register onKeyguardShowing callback
        mKeyguardStateController.addCallback(mKeyguardCallback);
        mKeyguardUpdateMonitor.registerCallback(mKeyguardUpdateCallback);

        // register lockscreen settings changed callbacks:
        final ContentObserver settingsObserver = new ContentObserver(mMainHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (mKeyguardStateController.isShowing()) {
                    invalidateListFromFilter("Settings " + uri + " changed");
                }
            }
        };

        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS),
                false,
                settingsObserver,
                UserHandle.USER_ALL);

        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS),
                true,
                settingsObserver,
                UserHandle.USER_ALL);

        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ZEN_MODE),
                false,
                settingsObserver);

        // register (maybe) public mode changed callbacks:
        mStatusBarStateController.addCallback(mStatusBarStateListener);
        mBroadcastDispatcher.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mKeyguardStateController.isShowing()) {
                    // maybe public mode changed
                    invalidateListFromFilter(intent.getAction());
                }
            }}, new IntentFilter(Intent.ACTION_USER_SWITCHED));
    }

    private void invalidateListFromFilter(String reason) {
        mNotifFilter.invalidateList();
    }

    private final KeyguardStateController.Callback mKeyguardCallback =
            new KeyguardStateController.Callback() {
        @Override
        public void onUnlockedChanged() {
            invalidateListFromFilter("onUnlockedChanged");
        }

        @Override
        public void onKeyguardShowingChanged() {
            invalidateListFromFilter("onKeyguardShowingChanged");
        }
    };

    private final StatusBarStateController.StateListener mStatusBarStateListener =
            new StatusBarStateController.StateListener() {
                @Override
                public void onStateChanged(int newState) {
                    // maybe public mode changed
                    invalidateListFromFilter("onStatusBarStateChanged");
                }
    };

    private final KeyguardUpdateMonitorCallback mKeyguardUpdateCallback =
            new KeyguardUpdateMonitorCallback() {
        @Override
        public void onStrongAuthStateChanged(int userId) {
            // maybe lockdown mode changed
            invalidateListFromFilter("onStrongAuthStateChanged");
        }
    };
}