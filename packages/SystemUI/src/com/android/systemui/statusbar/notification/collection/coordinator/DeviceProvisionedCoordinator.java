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

import android.Manifest;
import android.app.AppGlobals;
import android.app.Notification;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;

import com.android.systemui.statusbar.notification.collection.NotifCollection;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.listbuilder.NotifListBuilder;
import com.android.systemui.statusbar.notification.collection.listbuilder.pluggable.NotifFilter;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Filters out most notifications when the device is unprovisioned.
 * Special notifications with extra permissions and tags won't be filtered out even when the
 * device is unprovisioned.
 */
@Singleton
public class DeviceProvisionedCoordinator implements Coordinator {
    private static final String TAG = "DeviceProvisionedCoordinator";

    private final DeviceProvisionedController mDeviceProvisionedController;

    @Inject
    public DeviceProvisionedCoordinator(DeviceProvisionedController deviceProvisionedController) {
        mDeviceProvisionedController = deviceProvisionedController;
    }

    @Override
    public void attach(NotifCollection notifCollection, NotifListBuilder notifListBuilder) {
        mDeviceProvisionedController.addCallback(mDeviceProvisionedListener);

        notifListBuilder.addFilter(mNotifFilter);
    }

    protected final NotifFilter mNotifFilter = new NotifFilter(TAG) {
        @Override
        public boolean shouldFilterOut(NotificationEntry entry, long now) {
            return !mDeviceProvisionedController.isDeviceProvisioned()
                    && !showNotificationEvenIfUnprovisioned(entry.getSbn());
        }
    };

    /**
     * Only notifications coming from packages with permission
     * android.permission.NOTIFICATION_DURING_SETUP that also have special tags
     * marking them as relevant for setup are allowed to show when device is unprovisioned
     */
    private boolean showNotificationEvenIfUnprovisioned(StatusBarNotification sbn) {
        final boolean hasPermission = checkUidPermission(AppGlobals.getPackageManager(),
                Manifest.permission.NOTIFICATION_DURING_SETUP,
                sbn.getUid()) == PackageManager.PERMISSION_GRANTED;
        return hasPermission
                && sbn.getNotification().extras.getBoolean(Notification.EXTRA_ALLOW_DURING_SETUP);
    }

    private static int checkUidPermission(IPackageManager packageManager, String permission,
            int uid) {
        try {
            return packageManager.checkUidPermission(permission, uid);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private final DeviceProvisionedController.DeviceProvisionedListener mDeviceProvisionedListener =
            new DeviceProvisionedController.DeviceProvisionedListener() {
                @Override
                public void onDeviceProvisionedChanged() {
                    mNotifFilter.invalidateList();
                }
            };
}