/*
 * Copyright 2020 The Android Open Source Project
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
package com.android.server.blob;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Environment;
import android.util.Slog;

import java.io.File;

class BlobStoreConfig {
    public static final String TAG = "BlobStore";

    @Nullable
    public static File prepareBlobFile(long sessionId) {
        final File blobsDir = prepareBlobsDir();
        return blobsDir == null ? null : getBlobFile(blobsDir, sessionId);
    }

    @NonNull
    public static File getBlobFile(long sessionId) {
        return getBlobFile(getBlobsDir(), sessionId);
    }

    @NonNull
    private static File getBlobFile(File blobsDir, long sessionId) {
        return new File(blobsDir, String.valueOf(sessionId));
    }

    @Nullable
    public static File prepareBlobsDir() {
        final File blobsDir = getBlobsDir(prepareBlobStoreRootDir());
        if (!blobsDir.exists() && !blobsDir.mkdir()) {
            Slog.e(TAG, "Failed to mkdir(): " + blobsDir);
            return null;
        }
        return blobsDir;
    }

    @NonNull
    public static File getBlobsDir() {
        return getBlobsDir(getBlobStoreRootDir());
    }

    @NonNull
    private static File getBlobsDir(File blobsRootDir) {
        return new File(blobsRootDir, "blobs");
    }

    @Nullable
    public static File prepareBlobStoreRootDir() {
        final File blobStoreRootDir = getBlobStoreRootDir();
        if (!blobStoreRootDir.exists() && !blobStoreRootDir.mkdir()) {
            Slog.e(TAG, "Failed to mkdir(): " + blobStoreRootDir);
            return null;
        }
        return blobStoreRootDir;
    }

    @NonNull
    public static File getBlobStoreRootDir() {
        return new File(Environment.getDataSystemDirectory(), "blobstore");
    }
}