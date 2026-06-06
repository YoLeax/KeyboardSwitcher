package com.kunzisoft.keyboard.switcher;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;

import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import rikka.shizuku.Shizuku;

public class SecureSettingsPermissionHelper {

    public static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;

    public interface PermissionResultCallback {
        void onRequestPermissionResult(int requestCode, int grantResult);
    }

    public interface GrantCallback {
        void onGrantResult(GrantResult result);
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

    public static void grantWriteSecureSettingsWithShizuku(Context context, GrantCallback callback) {
        Context appContext = context != null ? context.getApplicationContext() : null;
        if (hasWriteSecureSettings(appContext)) {
            dispatchGrantResult(callback, GrantResult.ALREADY_GRANTED);
            return;
        }
        if (!isShizukuAvailable()) {
            dispatchGrantResult(callback, GrantResult.SHIZUKU_UNAVAILABLE);
            return;
        }
        if (!hasShizukuPermission()) {
            dispatchGrantResult(callback, GrantResult.SHIZUKU_PERMISSION_REQUIRED);
            return;
        }

        Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                new ComponentName(BuildConfig.APPLICATION_ID, SecureSettingsGrantService.class.getName())
        ).daemon(false)
                .processNameSuffix("secure_settings")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)
                .tag("secure_settings_grant");
        AtomicBoolean completed = new AtomicBoolean(false);

        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                GrantResult result = GrantResult.FAILED;
                try {
                    ISecureSettingsGrantService grantService =
                            ISecureSettingsGrantService.Stub.asInterface(service);
                    if (grantService.grantWriteSecureSettings(appContext.getPackageName(), getUserId())
                            && hasWriteSecureSettings(appContext)) {
                        result = GrantResult.GRANTED;
                    }
                } catch (Throwable ignored) {
                    result = GrantResult.FAILED;
                } finally {
                    unbindUserService(args, this);
                    if (completed.compareAndSet(false, true)) {
                        dispatchGrantResult(callback, result);
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                if (completed.compareAndSet(false, true)) {
                    dispatchGrantResult(callback, GrantResult.FAILED);
                }
            }
        };

        try {
            Shizuku.bindUserService(args, connection);
        } catch (Throwable ignored) {
            if (completed.compareAndSet(false, true)) {
                dispatchGrantResult(callback, GrantResult.FAILED);
            }
        }
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

    private static void dispatchGrantResult(GrantCallback callback, GrantResult result) {
        if (callback == null) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> callback.onGrantResult(result));
    }

    private static void unbindUserService(Shizuku.UserServiceArgs args, ServiceConnection connection) {
        try {
            Shizuku.unbindUserService(args, connection, true);
        } catch (Throwable ignored) {}
    }
}
