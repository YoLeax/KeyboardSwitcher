package com.kunzisoft.keyboard.switcher;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;

public class KeyboardVisibilityAccessibilityService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences preferences;
    private FloatingButtonController floatingButtonController;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        floatingButtonController = new FloatingButtonController(
                this,
                windowManager,
                preferences,
                this::accessibilityWindowType
        );
        updateFloatingButton();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        updateFloatingButton();
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (isFloatingButtonPreference(key)) {
            recreateFloatingButton();
        }
    }

    @Override
    public void onDestroy() {
        if (preferences != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        if (floatingButtonController != null) {
            floatingButtonController.destroy();
        }
        super.onDestroy();
    }

    private int accessibilityWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        }
        return LayoutParams.TYPE_SYSTEM_ALERT;
    }

    private void recreateFloatingButton() {
        if (floatingButtonController != null && floatingButtonController.isCreated()) {
            floatingButtonController.destroy();
        }
        updateFloatingButton();
    }

    private void updateFloatingButton() {
        if (floatingButtonController == null) {
            return;
        }

        if (!isAccessibilityFloatingButtonEnabled()) {
            floatingButtonController.destroy();
            return;
        }

        boolean keyboardVisible = isKeyboardVisible();
        if (keyboardVisible && !floatingButtonController.isCreated()) {
            floatingButtonController.create();
        }
        floatingButtonController.setVisible(keyboardVisible);
    }

    private boolean isAccessibilityFloatingButtonEnabled() {
        return preferences != null
                && preferences.getBoolean(getString(R.string.settings_floating_button_key), false)
                && preferences.getBoolean(
                        getString(R.string.settings_floating_button_keyboard_visibility_key),
                        false
                );
    }

    private boolean isKeyboardVisible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }

        List<AccessibilityWindowInfo> windows = getWindows();
        for (AccessibilityWindowInfo window : windows) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                return true;
            }
        }
        return false;
    }

    private boolean isFloatingButtonPreference(String key) {
        return getString(R.string.settings_floating_button_key).equals(key)
                || getString(R.string.settings_floating_button_keyboard_visibility_key).equals(key)
                || getString(R.string.settings_direct_keyboard_switch_key).equals(key)
                || getString(R.string.settings_direct_keyboard_first_key).equals(key)
                || getString(R.string.settings_direct_keyboard_second_key).equals(key)
                || getString(R.string.settings_floating_button_lock_key).equals(key)
                || getString(R.string.settings_colors_key).equals(key)
                || getString(R.string.settings_floating_background_color_key).equals(key)
                || getString(R.string.settings_floating_size_key).equals(key);
    }

    public static boolean isEnabled(Context context) {
        if (context == null) {
            return false;
        }

        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabled == null) {
            return false;
        }

        ComponentName service = new ComponentName(
                context,
                KeyboardVisibilityAccessibilityService.class
        );
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            if (service.flattenToString().equalsIgnoreCase(splitter.next())) {
                return true;
            }
        }
        return false;
    }
}
