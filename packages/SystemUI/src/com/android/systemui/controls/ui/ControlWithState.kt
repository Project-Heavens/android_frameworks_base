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

import android.service.controls.Control

import com.android.systemui.controls.controller.ControlInfo

/**
 * A container for:
 * <ul>
 *  <li>ControlInfo - Basic cached info about a Control
 *  <li>Control - Actual Control parcelable received directly from
 *  the participating application
 * </ul>
 */
data class ControlWithState(val ci: ControlInfo, val control: Control?)