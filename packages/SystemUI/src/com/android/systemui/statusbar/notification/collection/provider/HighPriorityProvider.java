/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.systemui.statusbar.notification.collection.provider;

import android.app.Notification;
import android.app.NotificationManager;

import com.android.systemui.statusbar.notification.collection.GroupEntry;
import com.android.systemui.statusbar.notification.collection.ListEntry;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.people.PeopleNotificationIdentifier;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Determines whether a notification is considered 'high priority'.
 *
 * Notifications that are high priority are visible on the lock screen/status bar and in the top
 * section in the shade.
 */
@Singleton
public class HighPriorityProvider {
    private final PeopleNotificationIdentifier mPeopleNotificationIdentifier;

    @Inject
    public HighPriorityProvider(PeopleNotificationIdentifier peopleNotificationIdentifier) {
        mPeopleNotificationIdentifier = peopleNotificationIdentifier;
    }

    /**
     * @return true if the ListEntry is high priority, else false
     *
     * A NotificationEntry is considered high priority if it:
     *  - has importance greater than or equal to IMPORTANCE_DEFAULT
     *  OR
     *  - their importance has NOT been set to a low priority option by the user AND the
     *  notification fulfills one of the following:
     *      - has a person associated with it
     *      - has a media session associated with it
     *      - has messaging style
     *
     * A GroupEntry is considered high priority if its representativeEntry (summary) or children are
     * high priority
     */
    public boolean isHighPriority(ListEntry entry) {
        if (entry == null) {
            return false;
        }

        final NotificationEntry notifEntry = entry.getRepresentativeEntry();
        return notifEntry.getRanking().getImportance() >= NotificationManager.IMPORTANCE_DEFAULT
                || hasHighPriorityCharacteristics(notifEntry)
                || hasHighPriorityChild(entry);
    }


    private boolean hasHighPriorityChild(ListEntry entry) {
        if (entry instanceof GroupEntry) {
            for (NotificationEntry child : ((GroupEntry) entry).getChildren()) {
                if (isHighPriority(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasHighPriorityCharacteristics(NotificationEntry entry) {
        return !hasUserSetImportance(entry)
                && (isImportantOngoing(entry)
                || entry.getSbn().getNotification().hasMediaSession()
                || isPeopleNotification(entry)
                || isMessagingStyle(entry));
    }

    private boolean isImportantOngoing(NotificationEntry entry) {
        return entry.getSbn().getNotification().isForegroundService()
                && entry.getRanking().getImportance() >= NotificationManager.IMPORTANCE_LOW;
    }

    private boolean isMessagingStyle(NotificationEntry entry) {
        return Notification.MessagingStyle.class.equals(
                entry.getSbn().getNotification().getNotificationStyle());
    }

    private boolean isPeopleNotification(NotificationEntry entry) {
        return mPeopleNotificationIdentifier.isPeopleNotification(entry.getSbn());
    }

    private boolean hasUserSetImportance(NotificationEntry entry) {
        return entry.getRanking().getChannel() != null
                && entry.getRanking().getChannel().hasUserSetImportance();
    }
}