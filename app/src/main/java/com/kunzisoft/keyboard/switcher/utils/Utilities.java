package com.kunzisoft.keyboard.switcher.utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.kunzisoft.keyboard.switcher.R;

import java.util.ArrayList;
import java.util.List;

public class Utilities {

    public static String getCurrentDefaultKeyboard(@Nullable Context context) {
        if (context != null) {
            InputMethodManager imm = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && imm != null) {
                InputMethodInfo currentInputMethod = imm.getCurrentInputMethodInfo();
                return currentInputMethod != null ? currentInputMethod.getId() : null;
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
