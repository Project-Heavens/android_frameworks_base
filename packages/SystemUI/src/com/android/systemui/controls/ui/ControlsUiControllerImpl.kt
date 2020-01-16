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

package com.android.systemui.controls.ui

import android.content.ComponentName
import android.service.controls.Control
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlsUiControllerImpl @Inject constructor() : ControlsUiController {

    override fun onRefreshState(componentName: ComponentName, controls: List<Control>) {
        TODO("not implemented")
    }

    override fun onActionResponse(componentName: ComponentName, controlId: String, response: Int) {
        TODO("not implemented")
    }
}