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

package com.android.settingslib.schedulesprovider;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

/**
 * This is a schedule data item. It contains the schedule title text, the summary text which
 * displays on the summary of the Settings preference and an {@link Intent}. Intent is able to
 * launch the editing page of the schedule data when user clicks this item (preference).
 */
public class ScheduleInfo implements Parcelable {
    private static final String TAG = "ScheduleInfo";
    private final String mTitle;
    private final String mSummary;
    private final Intent mIntent;

    public ScheduleInfo(Builder builder) {
        mTitle = builder.mTitle;
        mSummary = builder.mSummary;
        mIntent = builder.mIntent;
    }

    protected ScheduleInfo(Parcel in) {
        mTitle = in.readString();
        mSummary = in.readString();
        mIntent = in.readParcelable(Intent.class.getClassLoader());
    }

    /**
     * Returns the title text.
     *
     * @return The title.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the summary text.
     *
     * @return The summary.
     */
    public String getSummary() {
        return mSummary;
    }

    /**
     * Returns an {@link Intent}.
     */
    public Intent getIntent() {
        return mIntent;
    }

    /**
     * Verify the member variables are valid.
     *
     * @return {@code true} if all member variables are valid.
     */
    public boolean isValid() {
        return !TextUtils.isEmpty(mTitle) && !TextUtils.isEmpty(mSummary) && (mIntent != null);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mSummary);
        dest.writeParcelable(mIntent, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ScheduleInfo> CREATOR = new Creator<ScheduleInfo>() {
        @Override
        public ScheduleInfo createFromParcel(Parcel in) {
            return new ScheduleInfo(in);
        }

        @Override
        public ScheduleInfo[] newArray(int size) {
            return new ScheduleInfo[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "title : " + mTitle + " summary : " + mSummary + (mIntent == null
                ? " and intent is null." : ".");
    }

    /**
     * A simple builder for {@link ScheduleInfo}.
     */
    public static class Builder {
        @NonNull
        private String mTitle;
        @NonNull
        private String mSummary;
        @NonNull
        private Intent mIntent;

        /**
         * Sets the title.
         *
         * @param title The title of the preference item.
         * @return This instance.
         */
        public Builder setTitle(@NonNull String title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the summary.
         *
         * @param summary The summary of the preference summary.
         * @return This instance.
         */
        public Builder setSummary(@NonNull String summary) {
            mSummary = summary;
            return this;
        }

        /**
         * Sets the {@link Intent}.
         *
         * @param intent The action when user clicks the preference.
         * @return This instance.
         */
        public Builder setIntent(@NonNull Intent intent) {
            mIntent = intent;
            return this;
        }

        /**
         * Creates an instance of {@link ScheduleInfo}.
         *
         * @return The instance of {@link ScheduleInfo}.
         */
        public ScheduleInfo build() {
            return new ScheduleInfo(this);
        }
    }
}