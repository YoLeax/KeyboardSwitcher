package com.kunzisoft.keyboard.switcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.kunzisoft.keyboard.switcher.utils.Utilities;

import java.util.List;

public class KeyboardSwitchController {

    public enum Trigger {
        ACTIVITY,
        FLOATING_BUTTON,
        SETTINGS,
        NOTIFICATION,
        TILE,
        WIDGET,
        SHORTCUT
    }

    public enum Status {
        SWITCHED,
        FALLBACK_PICKER,
        FAILED
    }

    public enum Reason {
        DIRECT_SWITCH_DISABLED,
        INCOMPLETE_CONFIG,
        SAME_KEYBOARD,
        WRITE_SECURE_SETTINGS_MISSING,
        KEYBOARD_UNAVAILABLE,
        INPUT_METHODS_UNAVAILABLE,
        SECURE_SETTING_WRITE_FAILED
    }

    public enum PositionScope {
        DEFAULT,
        FIRST,
        SECOND
    }

    public static class Result {
        private final Status status;
        private final Reason reason;
        private final String targetKeyboardId;
        private final CharSequence targetKeyboardLabel;

        private Result(
                Status status,
                @Nullable Reason reason,
                @Nullable String targetKeyboardId,
                @Nullable CharSequence targetKeyboardLabel
        ) {
            this.status = status;
            this.reason = reason;
            this.targetKeyboardId = targetKeyboardId;
            this.targetKeyboardLabel = targetKeyboardLabel;
        }

        public Status getStatus() {
            return status;
        }

        public Reason getReason() {
            return reason;
        }

        public String getTargetKeyboardId() {
            return targetKeyboardId;
        }

        public CharSequence getTargetKeyboardLabel() {
            return targetKeyboardLabel;
        }

        public boolean isSwitched() {
            return status == Status.SWITCHED;
        }

        public boolean shouldFallbackToPicker() {
            return status == Status.FALLBACK_PICKER;
        }
    }

    private KeyboardSwitchController() {}

    public static Result perform(
            Context context,
            SharedPreferences preferences,
            Trigger trigger
    ) {
        if (!isDirectSwitchEnabled(context, preferences)) {
            return fallback(Reason.DIRECT_SWITCH_DISABLED);
        }

        String firstKeyboard = getFirstKeyboard(context, preferences);
        String secondKeyboard = getSecondKeyboard(context, preferences);
        if (TextUtils.isEmpty(firstKeyboard) || TextUtils.isEmpty(secondKeyboard)) {
            return fallback(Reason.INCOMPLETE_CONFIG);
        }
        if (firstKeyboard.equals(secondKeyboard)) {
            return fallback(Reason.SAME_KEYBOARD);
        }
        if (!SecureSettingsPermissionHelper.hasWriteSecureSettings(context)) {
            return fallback(Reason.WRITE_SECURE_SETTINGS_MISSING);
        }

        List<Utilities.KeyboardInfo> enabledKeyboards =
                Utilities.getEnabledKeyboardChoices(context);
        if (enabledKeyboards.isEmpty()) {
            return fallback(Reason.INPUT_METHODS_UNAVAILABLE);
        }

        Utilities.KeyboardInfo firstKeyboardInfo = Utilities.findKeyboard(
                enabledKeyboards,
                firstKeyboard
        );
        Utilities.KeyboardInfo secondKeyboardInfo = Utilities.findKeyboard(
                enabledKeyboards,
                secondKeyboard
        );
        if (firstKeyboardInfo == null || secondKeyboardInfo == null) {
            return fallback(Reason.KEYBOARD_UNAVAILABLE);
        }

        String currentKeyboard = Utilities.getCurrentDefaultKeyboard(context);
        Utilities.KeyboardInfo targetKeyboardInfo = firstKeyboardInfo;
        if (Utilities.sameInputMethod(firstKeyboard, currentKeyboard)) {
            targetKeyboardInfo = secondKeyboardInfo;
        }

        try {
            boolean switched = Settings.Secure.putString(
                    context.getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD,
                    targetKeyboardInfo.getId()
            );
            if (!switched) {
                return failed(Reason.SECURE_SETTING_WRITE_FAILED);
            }
            return new Result(
                    Status.SWITCHED,
                    null,
                    targetKeyboardInfo.getId(),
                    targetKeyboardInfo.getLabel()
            );
        } catch (Exception ignored) {
            return failed(Reason.SECURE_SETTING_WRITE_FAILED);
        }
    }

    public static boolean isDirectSwitchEnabled(
            Context context,
            SharedPreferences preferences
    ) {
        return preferences.getBoolean(
                context.getString(R.string.settings_direct_keyboard_switch_key),
                false
        );
    }

    public static PositionScope getCurrentPositionScope(
            Context context,
            SharedPreferences preferences
    ) {
        return getPositionScope(
                context,
                preferences,
                Utilities.getCurrentDefaultKeyboard(context)
        );
    }

    public static PositionScope getPositionScope(
            Context context,
            SharedPreferences preferences,
            @Nullable String keyboardId
    ) {
        if (TextUtils.isEmpty(keyboardId) || !isDirectSwitchEnabled(context, preferences)) {
            return PositionScope.DEFAULT;
        }

        if (keyboardId.equals(getFirstKeyboard(context, preferences))) {
            return PositionScope.FIRST;
        }
        if (keyboardId.equals(getSecondKeyboard(context, preferences))) {
            return PositionScope.SECOND;
        }
        return PositionScope.DEFAULT;
    }

    public static String getFirstKeyboard(
            Context context,
            SharedPreferences preferences
    ) {
        return preferences.getString(
                context.getString(R.string.settings_direct_keyboard_first_key),
                ""
        );
    }

    public static String getSecondKeyboard(
            Context context,
            SharedPreferences preferences
    ) {
        return preferences.getString(
                context.getString(R.string.settings_direct_keyboard_second_key),
                ""
        );
    }

    private static Result fallback(Reason reason) {
        return new Result(Status.FALLBACK_PICKER, reason, null, null);
    }

    private static Result failed(Reason reason) {
        return new Result(Status.FAILED, reason, null, null);
    }
}
