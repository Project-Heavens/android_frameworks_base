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

package android.os.incremental;

import android.os.incremental.IncrementalDataLoaderParamsParcel;

/** @hide */
interface IIncrementalManager {
    /**
     * A set of flags for the |createMode| parameters when creating a new Incremental storage.
     */
    const int CREATE_MODE_TEMPORARY_BIND = 1;
    const int CREATE_MODE_PERMANENT_BIND = 2;
    const int CREATE_MODE_CREATE = 4;
    const int CREATE_MODE_OPEN_EXISTING = 8;

    /**
     * Opens or creates a storage given a target path and data loader params. Returns the storage ID.
     */
    int openStorage(in @utf8InCpp String path);
    int createStorage(in @utf8InCpp String path, in IncrementalDataLoaderParamsParcel params, int createMode);
    int createLinkedStorage(in @utf8InCpp String path, int otherStorageId, int createMode);

    /**
     * Bind-mounts a path under a storage to a full path. Can be permanent or temporary.
     */
    const int BIND_TEMPORARY = 0;
    const int BIND_PERMANENT = 1;
    int makeBindMount(int storageId, in @utf8InCpp String pathUnderStorage, in @utf8InCpp String targetFullPath, int bindType);

    /**
     * Deletes an existing bind mount on a path under a storage. Returns 0 on success, and -errno on failure.
     */
    int deleteBindMount(int storageId, in @utf8InCpp String targetFullPath);

    /**
     * Creates a directory under a storage. The target directory is specified by its relative path under the storage.
     */
    int makeDirectory(int storageId, in @utf8InCpp String pathUnderStorage);

    /**
     * Recursively creates a directory under a storage. The target directory is specified by its relative path under the storage.
     * All the parent directories of the target directory will be created if they do not exist already.
     */
    int makeDirectories(int storageId, in @utf8InCpp String pathUnderStorage);

    /**
     * Creates a file under a storage, specifying its name, size and metadata.
     */
    int makeFile(int storageId, in @utf8InCpp String pathUnderStorage, long size, in byte[] metadata);

    /**
     * Creates a file under a storage. Content of the file is from a range inside another file.
     * Both files are specified by relative paths under storage.
     */
    int makeFileFromRange(int storageId, in @utf8InCpp String targetPathUnderStorage, in @utf8InCpp String sourcePathUnderStorage, long start, long end);

    /**
     * Creates a hard link between two files in two storage instances.
     * Source and dest specified by parent storage IDs and their relative paths under the storage.
     * The source and dest storage instances should be in the same fs mount.
     * Note: destStorageId can be the same as sourceStorageId.
     */
    int makeLink(int sourceStorageId, in @utf8InCpp String sourcePathUnderStorage, int destStorageId, in @utf8InCpp String destPathUnderStorage);

    /**
     * Deletes a hard link in a storage, specified by the relative path of the link target under storage.
     */
    int unlink(int storageId, in @utf8InCpp String pathUnderStorage);

    /**
     * Checks if a file's certain range is loaded. File is specified by relative file path under storage.
     */
    boolean isFileRangeLoaded(int storageId, in @utf8InCpp String pathUnderStorage, long start, long end);

    /**
     * Reads the metadata of a file. File is specified by relative path under storage.
     */
    byte[] getFileMetadata(int storageId, in @utf8InCpp String pathUnderStorage);

    /**
     * Starts loading data for a storage.
     */
    boolean startLoading(int storageId);

    /**
     * Deletes a storage given its ID. Deletes its bind mounts and unmount it. Stop its data loader.
     */
    void deleteStorage(int storageId);
}