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

package com.android.server.pm;

import android.annotation.ColorRes;
import android.annotation.DrawableRes;
import android.annotation.NonNull;
import android.annotation.StringRes;
import android.content.pm.UserInfo;
import android.content.pm.UserInfo.UserInfoFlag;
import android.content.res.Resources;
import android.os.UserManager;

import com.android.internal.util.Preconditions;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the details about a multiuser "user type", such as a
 * {@link UserManager#USER_TYPE_PROFILE_MANAGED}.
 *
 * Tests are located in UserManagerServiceUserTypeTest.java.
 * @hide
 */
public final class UserTypeDetails {

    /** Indicates that there is no limit to the number of users allowed. */
    public static final int UNLIMITED_NUMBER_OF_USERS = -1;

    /** Name of the user type, such as {@link UserManager#USER_TYPE_PROFILE_MANAGED}. */
    private final @NonNull String mName;

    // TODO(b/142482943): Currently unused. Hook this up.
    private final boolean mEnabled;

    // TODO(b/142482943): Currently unused and not set. Hook this up.
    private final int mLabel;

    /**
     * Maximum number of this user type allowed on the device.
     * Use {@link #UNLIMITED_NUMBER_OF_USERS} to indicate that there is no hard limit.
     */
    private final int mMaxAllowed;

    /**
     * Maximum number of this user type allowed per parent (for user types, like profiles, that
     * have parents).
     * Use {@link #UNLIMITED_NUMBER_OF_USERS} to indicate that there is no hard limit.
     */
    // TODO(b/142482943): Should this also apply to restricted profiles?
    private final int mMaxAllowedPerParent;

    // TODO(b/143784345): Update doc when we clean up UserInfo.
    /** The {@link UserInfo.UserInfoFlag} representing the base type of this user. */
    private final @UserInfoFlag int mBaseType;

    // TODO(b/143784345): Update doc/name when we clean up UserInfo.
    /** The {@link UserInfo.UserInfoFlag}s that all users of this type will automatically have. */
    private final @UserInfoFlag int mDefaultUserInfoPropertyFlags;

    // TODO(b/142482943): Hook these up to something and set them for each type.
    private final List<String> mDefaultRestrictions;


    // Fields for profiles only, controlling the nature of their badges.
    // All badge information should be set if {@link #hasBadge()} is true.

    /** Resource ID of the badge put on icons. */
    private @DrawableRes final int mIconBadge;
    /** Resource ID of the badge. Should be set if mIconBadge is set. */
    private @DrawableRes final int mBadgePlain;
    /** Resource ID of the badge without a background. Should be set if mIconBadge is set. */
    private @DrawableRes final int mBadgeNoBackground;

    /**
     * Resource ID ({@link StringRes}) of the of the labels to describe badged apps; should be the
     * same format as com.android.internal.R.color.profile_badge_1. These are used for accessibility
     * services.
     *
     * <p>This is an array because, in general, there may be multiple users of the same user type.
     * In this case, the user is indexed according to its {@link UserInfo#profileBadge}.
     *
     * <p>Must be set if mIconBadge is set.
     */
    private final int[] mBadgeLabels;

    /**
     * Resource ID ({@link ColorRes}) of the colors badge put on icons.
     * (The value is a resource ID referring to the color; it is not the color value itself).
     *
     * <p>This is an array because, in general, there may be multiple users of the same user type.
     * In this case, the user is indexed according to its {@link UserInfo#profileBadge}.
     *
     * <p>Must be set if mIconBadge is set.
     */
    private final int[] mBadgeColors;

    private UserTypeDetails(@NonNull String name, boolean enabled, int maxAllowed,
            @UserInfoFlag int baseType, @UserInfoFlag int defaultUserInfoPropertyFlags, int label,
            int maxAllowedPerParent,
            int iconBadge, int badgePlain, int badgeNoBackground,
            int[] badgeLabels, int[] badgeColors,
            ArrayList<String> defaultRestrictions) {
        this.mName = name;
        this.mEnabled = enabled;
        this.mMaxAllowed = maxAllowed;
        this.mMaxAllowedPerParent = maxAllowedPerParent;
        this.mBaseType = baseType;
        this.mDefaultUserInfoPropertyFlags = defaultUserInfoPropertyFlags;
        this.mDefaultRestrictions =
                Collections.unmodifiableList(new ArrayList<>(defaultRestrictions));

        this.mIconBadge = iconBadge;
        this.mBadgePlain = badgePlain;
        this.mBadgeNoBackground = badgeNoBackground;
        this.mLabel = label;
        this.mBadgeLabels = badgeLabels;
        this.mBadgeColors = badgeColors;
    }

    /**
     * Returns the name of the user type, such as {@link UserManager#USER_TYPE_PROFILE_MANAGED}.
     */
    public String getName() {
        return mName;
    }

    // TODO(b/142482943) Hook this up or delete it.
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Returns the maximum number of this user type allowed on the device.
     * <p>Returns {@link #UNLIMITED_NUMBER_OF_USERS} to indicate that there is no hard limit.
     */
    public int getMaxAllowed() {
        return mMaxAllowed;
    }

    /**
     * Returns the maximum number of this user type allowed per parent (for user types, like
     * profiles, that have parents).
     * <p>Returns {@link #UNLIMITED_NUMBER_OF_USERS} to indicate that there is no hard limit.
     */
    public int getMaxAllowedPerParent() {
        return mMaxAllowedPerParent;
    }

    // TODO(b/143784345): Update comment when UserInfo is reorganized.
    /** The {@link UserInfo.UserInfoFlag}s that all users of this type will automatically have. */
    public int getDefaultUserInfoFlags() {
        return mDefaultUserInfoPropertyFlags | mBaseType;
    }

    // TODO(b/142482943) Hook this up; it is currently unused.
    public int getLabel() {
        return mLabel;
    }

    /** Returns whether users of this user type should be badged. */
    public boolean hasBadge() {
        return mIconBadge != Resources.ID_NULL;
    }

    /** Resource ID of the badge put on icons. */
    public @DrawableRes int getIconBadge() {
        return mIconBadge;
    }

    /** Resource ID of the badge. Used for {@link UserManager#getUserBadgeResId(int)}. */
    public @DrawableRes int getBadgePlain() {
        return mBadgePlain;
    }

    /** Resource ID of the badge without a background. */
    public @DrawableRes int getBadgeNoBackground() {
        return mBadgeNoBackground;
    }

    /**
     * Returns the Resource ID of the badgeIndexth badge label, where the badgeIndex is expected
     * to be the {@link UserInfo#profileBadge} of the user.
     * If badgeIndex exceeds the number of labels, returns the label for the highest index.
     */
    public @StringRes int getBadgeLabel(int badgeIndex) {
        if (mBadgeLabels == null || mBadgeLabels.length == 0 || badgeIndex < 0) {
            return Resources.ID_NULL;
        }
        return mBadgeLabels[Math.min(badgeIndex, mBadgeLabels.length - 1)];
    }

    /**
     * Returns the Resource ID of the badgeIndexth badge color, where the badgeIndex is expected
     * to be the {@link UserInfo#profileBadge} of the user.
     * If badgeIndex exceeds the number of colors, returns the color for the highest index.
     */
    public @ColorRes int getBadgeColor(int badgeIndex) {
        if (mBadgeColors == null || mBadgeColors.length == 0 || badgeIndex < 0) {
            return Resources.ID_NULL;
        }
        return mBadgeColors[Math.min(badgeIndex, mBadgeColors.length - 1)];
    }

    public boolean isProfile() {
        return (mBaseType & UserInfo.FLAG_PROFILE) != 0;
    }

    // TODO(b/142482943): Hook this up and don't return the original.
    public List<String> getDefaultRestrictions() {
        return mDefaultRestrictions;
    }

    /** Dumps details of the UserTypeDetails. Do not parse this. */
    public void dump(PrintWriter pw) {
        final String prefix = "        ";
        pw.print(prefix); pw.print("mName: "); pw.println(mName);
        pw.print(prefix); pw.print("mBaseType: "); pw.println(UserInfo.flagsToString(mBaseType));
        pw.print(prefix); pw.print("mEnabled: "); pw.println(mEnabled);
        pw.print(prefix); pw.print("mMaxAllowed: "); pw.println(mMaxAllowed);
        pw.print(prefix); pw.print("mMaxAllowedPerParent: "); pw.println(mMaxAllowedPerParent);
        pw.print(prefix); pw.print("mDefaultUserInfoFlags: ");
        pw.println(UserInfo.flagsToString(mDefaultUserInfoPropertyFlags));
        pw.print(prefix); pw.print("mLabel: "); pw.println(mLabel);
        pw.print(prefix); pw.print("mDefaultRestrictions: "); pw.println(mDefaultRestrictions);
        pw.print(prefix); pw.print("mIconBadge: "); pw.println(mIconBadge);
        pw.print(prefix); pw.print("mBadgePlain: "); pw.println(mBadgePlain);
        pw.print(prefix); pw.print("mBadgeNoBackground: "); pw.println(mBadgeNoBackground);
        pw.print(prefix); pw.print("mBadgeLabels.length: ");
        pw.println(mBadgeLabels != null ? mBadgeLabels.length : "0(null)");
        pw.print(prefix); pw.print("mBadgeColors.length: ");
        pw.println(mBadgeColors != null ? mBadgeColors.length : "0(null)");
    }

    /** Builder for a {@link UserTypeDetails}; see that class for documentation. */
    public static final class Builder {
        // UserTypeDetails properties and their default values.
        private String mName; // This MUST be explicitly set.
        private int mBaseType; // This MUST be explicitly set.
        private int mMaxAllowed = UNLIMITED_NUMBER_OF_USERS;
        private int mMaxAllowedPerParent = UNLIMITED_NUMBER_OF_USERS;
        private int mDefaultUserInfoPropertyFlags = 0;
        private ArrayList<String> mDefaultRestrictions = new ArrayList<>();
        private boolean mEnabled = true;
        private int mLabel = Resources.ID_NULL;
        private int[] mBadgeLabels = null;
        private int[] mBadgeColors = null;
        private int mIconBadge = Resources.ID_NULL;
        private int mBadgePlain = Resources.ID_NULL;
        private int mBadgeNoBackground = Resources.ID_NULL;

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }

        public Builder setMaxAllowed(int maxAllowed) {
            mMaxAllowed = maxAllowed;
            return this;
        }

        public Builder setMaxAllowedPerParent(int maxAllowedPerParent) {
            mMaxAllowedPerParent = maxAllowedPerParent;
            return this;
        }

        public Builder setBaseType(@UserInfoFlag int baseType) {
            mBaseType = baseType;
            return this;
        }

        public Builder setDefaultUserInfoPropertyFlags(@UserInfoFlag int flags) {
            mDefaultUserInfoPropertyFlags = flags;
            return this;
        }

        public Builder setBadgeLabels(int ... badgeLabels) {
            mBadgeLabels = badgeLabels;
            return this;
        }

        public Builder setBadgeColors(int ... badgeColors) {
            mBadgeColors = badgeColors;
            return this;
        }

        public Builder setIconBadge(int badgeIcon) {
            mIconBadge = badgeIcon;
            return this;
        }

        public Builder setBadgePlain(int badgePlain) {
            mBadgePlain = badgePlain;
            return this;
        }

        public Builder setBadgeNoBackground(int badgeNoBackground) {
            mBadgeNoBackground = badgeNoBackground;
            return this;
        }

        public Builder setLabel(int label) {
            mLabel = label;
            return this;
        }

        public Builder setDefaultRestrictions(ArrayList<String> restrictions) {
            mDefaultRestrictions = restrictions;
            return this;
        }

        public UserTypeDetails createUserTypeDetails() {
            Preconditions.checkArgument(mName != null,
                    "Cannot create a UserTypeDetails with no name.");
            Preconditions.checkArgument(hasValidBaseType(),
                    "UserTypeDetails " + mName + " has invalid baseType: " + mBaseType);
            Preconditions.checkArgument(hasValidPropertyFlags(),
                    "UserTypeDetails " + mName + " has invalid flags: "
                            + Integer.toHexString(mDefaultUserInfoPropertyFlags));
            if (hasBadge()) {
                Preconditions.checkArgument(mBadgeLabels != null && mBadgeLabels.length != 0,
                        "UserTypeDetails " + mName + " has badge but no badgeLabels.");
                Preconditions.checkArgument(mBadgeColors != null && mBadgeColors.length != 0,
                        "UserTypeDetails " + mName + " has badge but no badgeColors.");
            }

            return new UserTypeDetails(mName, mEnabled, mMaxAllowed, mBaseType,
                    mDefaultUserInfoPropertyFlags, mLabel, mMaxAllowedPerParent,
                    mIconBadge, mBadgePlain, mBadgeNoBackground, mBadgeLabels, mBadgeColors,
                    mDefaultRestrictions);
        }

        private boolean hasBadge() {
            return mIconBadge != Resources.ID_NULL;
        }

        // TODO(b/143784345): Refactor this when we clean up UserInfo.
        private boolean hasValidBaseType() {
            return mBaseType == UserInfo.FLAG_FULL
                    || mBaseType == UserInfo.FLAG_PROFILE
                    || mBaseType == UserInfo.FLAG_SYSTEM
                    || mBaseType == (UserInfo.FLAG_FULL | UserInfo.FLAG_SYSTEM);
        }

        // TODO(b/143784345): Refactor this when we clean up UserInfo.
        private boolean hasValidPropertyFlags() {
            final int forbiddenMask =
                    UserInfo.FLAG_PRIMARY |
                    UserInfo.FLAG_ADMIN |
                    UserInfo.FLAG_INITIALIZED |
                    UserInfo.FLAG_QUIET_MODE |
                    UserInfo.FLAG_FULL |
                    UserInfo.FLAG_SYSTEM |
                    UserInfo.FLAG_PROFILE;
            return (mDefaultUserInfoPropertyFlags & forbiddenMask) == 0;
        }
    }

    /**
     * Returns whether the user type is a managed profile
     * (i.e. {@link UserManager#USER_TYPE_PROFILE_MANAGED}).
     */
    public boolean isManagedProfile() {
        return UserManager.isUserTypeManagedProfile(mName);
    }
}