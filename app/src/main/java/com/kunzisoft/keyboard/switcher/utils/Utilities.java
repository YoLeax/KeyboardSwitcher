package com.kunzisoft.keyboard.switcher.utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.kunzisoft.keyboard.switcher.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Utilities {

    private static final String[] COMPATIBILITY_KEYBOARD_IDS = {
            "com.tencent.qqpinyin/.QQPYInputMethodService"
    };

    public static final class KeyboardInfo {
        private final String id;
        private final CharSequence label;

        public KeyboardInfo(String id, CharSequence label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public CharSequence getLabel() {
            return label;
        }
    }

    public static String getCurrentDefaultKeyboard(@Nullable Context context) {
        if (context != null) {
            InputMethodManager imm = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && imm != null) {
                InputMethodInfo currentInputMethod = imm.getCurrentInputMethodInfo();
                if (currentInputMethod != null) {
                    return currentInputMethod.getId();
                }
            }
            try {
                return Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.DEFAULT_INPUT_METHOD
                );
            } catch (SecurityException ignored) {
                return null;
            }
        } else
            return null;
    }

    public static List<InputMethodInfo> getInstalledKeyboards(@Nullable Context context, Boolean active) {
        if (context != null && context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_INPUT_METHODS)
        ) {
            InputMethodManager imm = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (active)
                return imm.getEnabledInputMethodList();
            else
                return imm.getInputMethodList();
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Returns every enabled input method visible to the app. Some Android 16 OEM builds omit
     * older/non-exported IME services (for example QQ Input 8.7.15 on MagicOS 10) from
     * InputMethodManager#getEnabledInputMethodList even though the system has them enabled.
     * Android 14+ blocks targetSdk 34+ apps from reading ENABLED_INPUT_METHODS, so known affected
     * keyboards are supplemented by querying their installed service instead.
     */
    public static List<KeyboardInfo> getEnabledKeyboardChoices(@Nullable Context context) {
        Map<String, KeyboardInfo> keyboards = new LinkedHashMap<>();
        if (context == null) {
            return new ArrayList<>();
        }

        for (InputMethodInfo keyboard : getInstalledKeyboards(context, true)) {
            addKeyboardChoice(
                    keyboards,
                    keyboard.getId(),
                    keyboard.loadLabel(context.getPackageManager())
            );
        }

        for (String keyboardId : COMPATIBILITY_KEYBOARD_IDS) {
            addCompatibilityKeyboardIfInstalled(context, keyboards, keyboardId);
        }

        return new ArrayList<>(keyboards.values());
    }

    @Nullable
    public static KeyboardInfo findKeyboard(List<KeyboardInfo> keyboards, String keyboardId) {
        if (keyboards == null || TextUtils.isEmpty(keyboardId)) {
            return null;
        }
        for (KeyboardInfo keyboard : keyboards) {
            if (sameInputMethod(keyboard.getId(), keyboardId)) {
                return keyboard;
            }
        }
        return null;
    }

    public static boolean sameInputMethod(@Nullable String firstId, @Nullable String secondId) {
        if (TextUtils.equals(firstId, secondId)) {
            return true;
        }
        if (TextUtils.isEmpty(firstId) || TextUtils.isEmpty(secondId)) {
            return false;
        }

        ComponentName first = ComponentName.unflattenFromString(firstId);
        ComponentName second = ComponentName.unflattenFromString(secondId);
        return first != null && first.equals(second);
    }

    private static void addKeyboardChoice(
            Map<String, KeyboardInfo> keyboards,
            String keyboardId,
            CharSequence label
    ) {
        if (TextUtils.isEmpty(keyboardId) || findKeyboard(keyboards, keyboardId) != null) {
            return;
        }
        keyboards.put(
                keyboardId,
                new KeyboardInfo(
                        keyboardId,
                        TextUtils.isEmpty(label) ? keyboardId : label
                )
        );
    }

    @Nullable
    private static KeyboardInfo findKeyboard(
            Map<String, KeyboardInfo> keyboards,
            String keyboardId
    ) {
        for (KeyboardInfo keyboard : keyboards.values()) {
            if (sameInputMethod(keyboard.getId(), keyboardId)) {
                return keyboard;
            }
        }
        return null;
    }

    private static void addCompatibilityKeyboardIfInstalled(
            Context context,
            Map<String, KeyboardInfo> keyboards,
            String keyboardId
    ) {
        if (findKeyboard(keyboards, keyboardId) != null) {
            return;
        }
        ComponentName componentName = ComponentName.unflattenFromString(keyboardId);
        if (componentName == null) {
            return;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            ServiceInfo serviceInfo = packageManager.getServiceInfo(componentName, 0);
            if (!serviceInfo.enabled || !serviceInfo.applicationInfo.enabled) {
                return;
            }
            CharSequence label = serviceInfo.loadLabel(packageManager);
            if (TextUtils.isEmpty(label)) {
                label = serviceInfo.applicationInfo.loadLabel(packageManager);
            }
            addKeyboardChoice(keyboards, keyboardId, label);
        } catch (PackageManager.NameNotFoundException | SecurityException ignored) {}
    }

    public static void openAvailableKeyboards(@Nullable Context context) {
        if (context != null) {
            try {
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                new AlertDialog.Builder(context)
                        .setMessage(R.string.error_unavailable_keyboard_feature)
                        .setPositiveButton(
                                android.R.string.ok,
                                (dialogInterface, i) -> {}
                        ).create().show();
            }
        }
    }

    public static void chooseAKeyboard(@Nullable Context context) {
        if (context != null && context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_INPUT_METHODS)
        ) {
            InputMethodManager imeManager = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imeManager != null) {
                imeManager.showInputMethodPicker();
            }
        }
    }
}
