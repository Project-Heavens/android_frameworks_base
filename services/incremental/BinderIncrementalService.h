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

#pragma once

#include <binder/BinderService.h>
#include <binder/IServiceManager.h>

#include "IncrementalService.h"
#include "android/os/incremental/BnIncrementalManagerNative.h"
#include "incremental_service.h"

namespace android::os::incremental {

class BinderIncrementalService : public BnIncrementalManagerNative,
                                 public BinderService<BinderIncrementalService> {
public:
    BinderIncrementalService(const sp<IServiceManager> &sm);

    static BinderIncrementalService *start();
    static const char16_t *getServiceName() { return u"incremental_service"; }
    status_t dump(int fd, const Vector<String16> &args) final;

    void onSystemReady();
    void onInvalidStorage(int mountId);

    binder::Status openStorage(const std::string &path, int32_t *_aidl_return) final;
    binder::Status createStorage(
            const std::string &path,
            const ::android::content::pm::DataLoaderParamsParcel &params,
            int32_t createMode, int32_t *_aidl_return) final;
    binder::Status createLinkedStorage(const std::string &path, int32_t otherStorageId,
                                       int32_t createMode, int32_t *_aidl_return) final;
    binder::Status makeBindMount(int32_t storageId, const std::string &pathUnderStorage,
                                 const std::string &targetFullPath, int32_t bindType,
                                 int32_t *_aidl_return) final;
    binder::Status deleteBindMount(int32_t storageId, const std::string &targetFullPath,
                                   int32_t *_aidl_return) final;
    binder::Status deleteStorage(int32_t storageId) final;
    binder::Status makeDirectory(int32_t storageId, const std::string &pathUnderStorage,
                                 int32_t *_aidl_return) final;
    binder::Status makeDirectories(int32_t storageId, const std::string &pathUnderStorage,
                                   int32_t *_aidl_return) final;
    binder::Status makeFile(int32_t storageId, const std::string &pathUnderStorage, int64_t size,
                            const std::vector<uint8_t> &metadata, int32_t *_aidl_return) final;
    binder::Status makeFileFromRange(int32_t storageId, const std::string &pathUnderStorage,
                                     const std::string &sourcePathUnderStorage, int64_t start,
                                     int64_t end, int32_t *_aidl_return);
    binder::Status makeLink(int32_t sourceStorageId, const std::string &relativeSourcePath,
                            int32_t destStorageId, const std::string &relativeDestPath,
                            int32_t *_aidl_return) final;
    binder::Status unlink(int32_t storageId, const std::string &pathUnderStorage,
                          int32_t *_aidl_return) final;
    binder::Status isFileRangeLoaded(int32_t storageId, const std::string &relativePath,
                                     int64_t start, int64_t end, bool *_aidl_return) final;
    binder::Status getFileMetadata(int32_t storageId, const std::string &relativePath,
                                   std::vector<uint8_t> *_aidl_return) final;
    binder::Status startLoading(int32_t storageId, bool *_aidl_return) final;

private:
    android::incremental::IncrementalService mImpl;
};

} // namespace android::os::incremental