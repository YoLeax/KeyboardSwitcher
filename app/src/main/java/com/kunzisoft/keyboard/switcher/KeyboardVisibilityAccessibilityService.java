package com.kunzisoft.keyboard.switcher;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;

public class KeyboardVisibilityAccessibilityService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final long[] SHOW_KEYBOARD_RETRY_DELAYS = {450L, 1200L};

    private SharedPreferences preferences;
    private FloatingButtonController floatingButtonController;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable keyboardRequestRunnable = this::requestKeyboardFromFocusedInput;
    private PendingInputTarget pendingInputTarget;
    private boolean inputTappedForKeyboardRequest;
    private int keyboardRequestAttempt;

    private static class PendingInputTarget {
        final int selectionStart;
        final int selectionEnd;

        PendingInputTarget(
                int selectionStart,
                int selectionEnd
        ) {
            this.selectionStart = selectionStart;
            this.selectionEnd = selectionEnd;
        }

        boolean hasSelection() {
            return selectionStart >= 0 && selectionEnd >= 0;
        }
    }

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
        handler.removeCallbacksAndMessages(null);
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

    public void prepareKeyboardRequestAfterDirectSwitch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        AccessibilityNodeInfo node = findFocusedInputNode();
        if (node == null) {
            pendingInputTarget = null;
            inputTappedForKeyboardRequest = false;
            keyboardRequestAttempt = 0;
            return;
        }

        try {
            pendingInputTarget = new PendingInputTarget(
                    node.getTextSelectionStart(),
                    node.getTextSelectionEnd()
            );
            inputTappedForKeyboardRequest = false;
            keyboardRequestAttempt = 0;
        } finally {
            node.recycle();
        }
    }

    public void requestKeyboardAfterDirectSwitch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || pendingInputTarget == null) {
            return;
        }

        handler.removeCallbacks(keyboardRequestRunnable);
        keyboardRequestAttempt = 0;
        for (long delay : SHOW_KEYBOARD_RETRY_DELAYS) {
            handler.postDelayed(keyboardRequestRunnable, delay);
        }
    }

    private void requestKeyboardFromFocusedInput() {
        if (isKeyboardVisible()) {
            restoreSelectionFromFocusedInput();
            handler.removeCallbacks(keyboardRequestRunnable);
            pendingInputTarget = null;
            return;
        }

        keyboardRequestAttempt++;
        AccessibilityNodeInfo node = findFocusedInputNode();
        if (node == null) {
            return;
        }

        try {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            restoreSelection(node);
            if (!inputTappedForKeyboardRequest && isLastKeyboardRequestAttempt()) {
                inputTappedForKeyboardRequest = true;
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                restoreSelection(node);
            }
        } finally {
            node.recycle();
        }
    }

    private AccessibilityNodeInfo findFocusedInputNode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }

        List<AccessibilityWindowInfo> windows = getWindows();
        for (AccessibilityWindowInfo window : windows) {
            if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) {
                continue;
            }

            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) {
                continue;
            }

            AccessibilityNodeInfo node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            root.recycle();
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    private void restoreSelectionFromFocusedInput() {
        AccessibilityNodeInfo node = findFocusedInputNode();
        if (node == null) {
            return;
        }

        try {
            restoreSelection(node);
        } finally {
            node.recycle();
        }
    }

    private void restoreSelection(AccessibilityNodeInfo node) {
        if (pendingInputTarget == null || !pendingInputTarget.hasSelection()) {
            return;
        }

        Bundle arguments = new Bundle();
        arguments.putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                pendingInputTarget.selectionStart
        );
        arguments.putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                pendingInputTarget.selectionEnd
        );
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments);
    }

    private boolean isLastKeyboardRequestAttempt() {
        return keyboardRequestAttempt >= SHOW_KEYBOARD_RETRY_DELAYS.length;
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
