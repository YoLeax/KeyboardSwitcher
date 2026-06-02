package com.kunzisoft.keyboard.switcher;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.UserHandle;

import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;

import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.Shizuku;
import rikka.shizuku.SystemServiceHelper;

public class SecureSettingsPermissionHelper {

    public static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;

    public interface PermissionResultCallback {
        void onRequestPermissionResult(int requestCode, int grantResult);
    }

    public enum GrantResult {
        ALREADY_GRANTED,
        GRANTED,
        SHIZUKU_UNAVAILABLE,
        SHIZUKU_PERMISSION_REQUIRED,
        FAILED
    }

    private SecureSettingsPermissionHelper() {}

    public static boolean hasWriteSecureSettings(Context context) {
        return context != null
                && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_SECURE_SETTINGS
                ) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isShizukuAvailable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }

        try {
            return Shizuku.pingBinder();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean hasShizukuPermission() {
        if (!isShizukuAvailable()) {
            return false;
        }

        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void requestShizukuPermission() {
        if (!isShizukuAvailable()) {
            return;
        }

        try {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
        } catch (Throwable ignored) {}
    }

    public static Object addPermissionResultListener(PermissionResultCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || callback == null) {
            return null;
        }

        try {
            Shizuku.OnRequestPermissionResultListener listener =
                    callback::onRequestPermissionResult;
            Shizuku.addRequestPermissionResultListener(listener);
            return listener;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void removePermissionResultListener(Object listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                || !(listener instanceof Shizuku.OnRequestPermissionResultListener)) {
            return;
        }

        try {
            Shizuku.removeRequestPermissionResultListener(
                    (Shizuku.OnRequestPermissionResultListener) listener
            );
        } catch (Throwable ignored) {}
    }

    public static GrantResult grantWriteSecureSettingsWithShizuku(Context context) {
        if (hasWriteSecureSettings(context)) {
            return GrantResult.ALREADY_GRANTED;
        }
        if (!isShizukuAvailable()) {
            return GrantResult.SHIZUKU_UNAVAILABLE;
        }
        if (!hasShizukuPermission()) {
            return GrantResult.SHIZUKU_PERMISSION_REQUIRED;
        }

        try {
            IBinder binder = SystemServiceHelper.getSystemService("package");
            Object packageManager = Class
                    .forName("android.content.pm.IPackageManager$Stub")
                    .getMethod("asInterface", IBinder.class)
                    .invoke(null, new ShizukuBinderWrapper(binder));
            packageManager
                    .getClass()
                    .getMethod(
                            "grantRuntimePermission",
                            String.class,
                            String.class,
                            int.class
                    )
                    .invoke(
                            packageManager,
                            context.getPackageName(),
                            Manifest.permission.WRITE_SECURE_SETTINGS,
                            getUserId()
                    );
            if (hasWriteSecureSettings(context)) {
                return GrantResult.GRANTED;
            }
        } catch (Throwable ignored) {
            return GrantResult.FAILED;
        }
        return GrantResult.FAILED;
    }

    public static String getAdbGrantCommand(Context context) {
        return "adb shell pm grant " + context.getPackageName() + " "
                + Manifest.permission.WRITE_SECURE_SETTINGS;
    }

    private static int getUserId() {
        try {
            Method method = UserHandle.class.getDeclaredMethod("myUserId");
            method.setAccessible(true);
            return (int) method.invoke(null);
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
