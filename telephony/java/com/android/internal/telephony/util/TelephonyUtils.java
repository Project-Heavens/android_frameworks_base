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
package com.android.internal.telephony.util;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.RemoteException;
import android.os.SystemProperties;

import java.io.PrintWriter;

/**
 * This class provides various util functions
 */
public final class TelephonyUtils {
    public static boolean IS_USER = "user".equals(android.os.Build.TYPE);
    public static boolean IS_DEBUGGABLE = SystemProperties.getInt("ro.debuggable", 0) == 1;

    /**
     * Verify that caller holds {@link android.Manifest.permission#DUMP}.
     *
     * @return true if access should be granted.
     */
    public static boolean checkDumpPermission(Context context, String tag, PrintWriter pw) {
        if (context.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump " + tag + " from from pid="
                    + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid()
                    + " due to missing android.permission.DUMP permission");
            return false;
        } else {
            return true;
        }
    }

    /** Returns an empty string if the input is {@code null}. */
    public static String emptyIfNull(@Nullable String str) {
        return str == null ? "" : str;
    }

    /** Throws a {@link RuntimeException} that wrapps the {@link RemoteException}. */
    public static RuntimeException rethrowAsRuntimeException(RemoteException remoteException) {
        throw new RuntimeException(remoteException);
    }

    /**
     * Returns a {@link ComponentInfo} from the {@link ResolveInfo},
     * or throws an {@link IllegalStateException} if not available.
     */
    public static ComponentInfo getComponentInfo(@NonNull ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo;
        throw new IllegalStateException("Missing ComponentInfo!");
    }
}