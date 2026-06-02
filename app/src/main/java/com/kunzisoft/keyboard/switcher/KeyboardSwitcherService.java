package com.kunzisoft.keyboard.switcher;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

public class KeyboardSwitcherService extends Service {

    public static final String CHANNEL_ID_KEYBOARD = "com.kunzisoft.keyboard.notification.channel";
    public static final String CHANNEL_NAME_KEYBOARD = "Keyboard switcher notification";

    public static final int NOTIFICATION_ID = 45;

    private static final String NOTIFICATION_START = "NOTIFICATION_START";
    private static final String NOTIFICATION_STOP = "NOTIFICATION_STOP";
    private static final String FLOATING_BUTTON_START = "FLOATING_BUTTON_START";
    private static final String FLOATING_BUTTON_STOP = "FLOATING_BUTTON_STOP";

    private SharedPreferences preferences;
    private FloatingButtonController floatingButtonController;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        floatingButtonController = new FloatingButtonController(
                this,
                windowManager,
                preferences,
                this::overlayWindowType
        );

        // To keep the notification active
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_KEYBOARD,
                    CHANNEL_NAME_KEYBOARD,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setImportance(NotificationManager.IMPORTANCE_LOW);
            NotificationManagerCompat.from(this).createNotificationChannel(channel);
        }
    }

    private NotificationCompat.Builder notificationBuilder() {
        return new NotificationCompat.Builder(this, CHANNEL_ID_KEYBOARD)
                .setSmallIcon(R.drawable.ic_notification_white_24dp)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimaryLight))
                .setContentTitle(this.getString(R.string.notification_keyboard_title))
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentText(this.getString(R.string.notification_keyboard_content_text))
                .setContentIntent(KeyboardManagerActivity.getPendingIntent(this, null, 600L)
                );
    }

    private void removeNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int serviceType = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
        }
        // Start the service as foreground service
        ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notificationBuilder().build(),
                serviceType
        );

        if (intent != null) {
            String action = intent.getAction();
            if (action == null)
                action = "";
            // Manage notification and floating service state
            if (action.contains(NOTIFICATION_START)) {
                startNotification();
            }
            if (action.contains(FLOATING_BUTTON_START)) {
                startFloatingButton();
            }
            if (action.contains(NOTIFICATION_STOP)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    removeNotification();
                }
            }
            if (action.contains(FLOATING_BUTTON_STOP)) {
                stopFloatingButton();
                // Stop the service if all services are stopped
                if (action.contains(NOTIFICATION_STOP)) {
                    stopSelf();
                }
            }
            // Stop the service if no action
            if (action.isEmpty()) {
                stopSelf();
            }
        } else {
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("MissingPermission")
    private void startNotification() {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this)
                    .notify(NOTIFICATION_ID, notificationBuilder().build());
        }
    }

    private int overlayWindowType() {
        int typeFilter = LayoutParams.TYPE_SYSTEM_ALERT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            typeFilter = LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return typeFilter;
    }

    private boolean shouldShowOnlyWhenKeyboardOpen() {
        return preferences.getBoolean(
                getString(R.string.settings_floating_button_keyboard_visibility_key),
                false
        );
    }

    private void startFloatingButton() {
        if (shouldShowOnlyWhenKeyboardOpen()) {
            floatingButtonController.destroy();
            return;
        }
        floatingButtonController.create();
        floatingButtonController.setVisible(true);
    }

    private void stopFloatingButton() {
        floatingButtonController.destroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (preferences.getBoolean(getString(R.string.settings_floating_button_key), false)) {
            startFloatingButton();
        }
    }

    @Override
    public void onDestroy() {
        if (floatingButtonController != null) {
            floatingButtonController.destroy();
        }
        removeNotification();
        super.onDestroy();
    }

    private static String addAction(String action, String add) {
        if(!action.contains(add)) {
            return action+add;
        }
        return action;
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, KeyboardSwitcherService.class);
        String action = "";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean(context.getString(R.string.settings_notification_key), false)) {
            action = addAction(action, NOTIFICATION_START);
        } else {
            action = addAction(action, NOTIFICATION_STOP);
        }
        boolean floatingButtonEnabled =
                preferences.getBoolean(context.getString(R.string.settings_floating_button_key), false);
        boolean keyboardVisibilityMode = preferences.getBoolean(
                context.getString(R.string.settings_floating_button_keyboard_visibility_key),
                false
        );
        if (floatingButtonEnabled && !keyboardVisibilityMode) {
            action = addAction(action, FLOATING_BUTTON_START);
        } else {
            action = addAction(action, FLOATING_BUTTON_STOP);
        }
        intent.setAction(action);
        ContextCompat.startForegroundService(context, intent);
    }
}
