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

package android.service.controls;

import android.annotation.NonNull;
import android.os.Parcel;

import com.android.internal.util.Preconditions;

/**
 * A template for a {@link Control} with two discrete inputs.
 *
 * The two inputs represent a <i>Negative</i> input and a <i>Positive</i> input.
 * <p>
 * When one of the buttons is actioned, a {@link BooleanAction} will be sent.
 * {@link BooleanAction#getNewState} will be {@code false} if the button was
 * {@link DiscreteToggleTemplate#getNegativeButton} and {@code true} if the button was
 * {@link DiscreteToggleTemplate#getPositiveButton}.
 * @hide
 */
public class DiscreteToggleTemplate extends ControlTemplate {

    private final @NonNull ControlButton mNegativeButton;
    private final @NonNull ControlButton mPositiveButton;

    /**
     * @param templateId the identifier for this template object
     * @param negativeButton a {@ControlButton} for the <i>Negative</i> input
     * @param positiveButton a {@ControlButton} for the <i>Positive</i> input
     */
    public DiscreteToggleTemplate(@NonNull String templateId,
            @NonNull ControlButton negativeButton,
            @NonNull ControlButton positiveButton) {
        super(templateId);
        Preconditions.checkNotNull(negativeButton);
        Preconditions.checkNotNull(positiveButton);
        mNegativeButton = negativeButton;
        mPositiveButton = positiveButton;
    }

    DiscreteToggleTemplate(Parcel in) {
        super(in);
        this.mNegativeButton = ControlButton.CREATOR.createFromParcel(in);
        this.mPositiveButton = ControlButton.CREATOR.createFromParcel(in);
    }

    /**
     * The {@link ControlButton} associated with the <i>Negative</i> action.
     */
    @NonNull
    public ControlButton getNegativeButton() {
        return mNegativeButton;
    }

    /**
     * The {@link ControlButton} associated with the <i>Positive</i> action.
     */
    @NonNull
    public ControlButton getPositiveButton() {
        return mPositiveButton;
    }

    /**
     * @return {@link ControlTemplate#TYPE_DISCRETE_TOGGLE}
     */
    @Override
    public int getTemplateType() {
        return TYPE_DISCRETE_TOGGLE;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        mNegativeButton.writeToParcel(dest, flags);
        mPositiveButton.writeToParcel(dest, flags);
    }

    public static final Creator<DiscreteToggleTemplate> CREATOR =
            new Creator<DiscreteToggleTemplate>() {
                @Override
                public DiscreteToggleTemplate createFromParcel(Parcel source) {
                    return new DiscreteToggleTemplate(source);
                }

                @Override
                public DiscreteToggleTemplate[] newArray(int size) {
                    return new DiscreteToggleTemplate[size];
                }
            };
}