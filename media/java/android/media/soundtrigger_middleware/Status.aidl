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
package android.media.soundtrigger_middleware;

/**
 * {@hide}
 */
@Backing(type="int")
enum Status {
    /** Success. */
    SUCCESS = 0,
    /** Failure due to resource contention. This is typically a temporary condition. */
    RESOURCE_CONTENTION = 1,
    /** Operation is not supported in this implementation. This is a permanent condition. */
    OPERATION_NOT_SUPPORTED = 2,
}