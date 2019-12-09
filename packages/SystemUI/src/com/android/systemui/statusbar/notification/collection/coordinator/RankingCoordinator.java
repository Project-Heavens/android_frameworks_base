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

import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.notification.collection.NotifCollection;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.listbuilder.NotifListBuilder;
import com.android.systemui.statusbar.notification.collection.listbuilder.pluggable.NotifFilter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Filters out NotificationEntries based on its Ranking.
 */
@Singleton
public class RankingCoordinator implements Coordinator {
    private static final String TAG = "RankingNotificationCoordinator";

    private final StatusBarStateController mStatusBarStateController;

    @Inject
    public RankingCoordinator(StatusBarStateController statusBarStateController) {
        mStatusBarStateController = statusBarStateController;
    }

    @Override
    public void attach(NotifCollection notifCollection, NotifListBuilder notifListBuilder) {
        mStatusBarStateController.addCallback(mStatusBarStateCallback);

        notifListBuilder.addFilter(mNotifFilter);
    }

    /**
     * Checks whether to filter out the given notification based the notification's Ranking object.
     * NotifListBuilder invalidates the notification list each time the ranking is updated,
     * so we don't need to explicitly invalidate this filter on ranking update.
     */
    protected final NotifFilter mNotifFilter = new NotifFilter(TAG) {
        @Override
        public boolean shouldFilterOut(NotificationEntry entry, long now) {
            // App suspended from Ranking
            if (entry.getRanking().isSuspended()) {
                return true;
            }

            // Dozing + DND Settings from Ranking object
            if (mStatusBarStateController.isDozing() && entry.shouldSuppressAmbient()) {
                return true;
            }

            if (!mStatusBarStateController.isDozing() && entry.shouldSuppressNotificationList()) {
                return true;
            }

            return false;
        }
    };


    private final StatusBarStateController.StateListener mStatusBarStateCallback =
            new StatusBarStateController.StateListener() {
                @Override
                public void onDozingChanged(boolean isDozing) {
                    mNotifFilter.invalidateList();
                }
            };
}