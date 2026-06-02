package com.kunzisoft.keyboard.switcher.settings;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;
import androidx.preference.TwoStatePreference;

import com.kunzisoft.androidclearchroma.ChromaPreferenceFragmentCompat;
import com.kunzisoft.keyboard.switcher.KeyboardSwitchController;
import com.kunzisoft.keyboard.switcher.KeyboardVisibilityAccessibilityService;
import com.kunzisoft.keyboard.switcher.KeyboardSwitcherService;
import com.kunzisoft.keyboard.switcher.R;
import com.kunzisoft.keyboard.switcher.SecureSettingsPermissionHelper;
import com.kunzisoft.keyboard.switcher.dialogs.WarningFloatingButtonDialog;
import com.kunzisoft.keyboard.switcher.utils.Utilities;

import java.util.List;

public class PreferenceFragment extends ChromaPreferenceFragmentCompat {

    /* https://stackoverflow.com/questions/7569937/unable-to-add-window-android-view-viewrootw44da9bc0-permission-denied-for-t
    code to post/handler request for permission
    */
    private final static int REQUEST_CODE = 6517;

    private TwoStatePreference preferenceNotification;
    private TwoStatePreference preferenceOverlay;
    private TwoStatePreference preferenceDirectKeyboardSwitch;
    private ListPreference preferenceDirectKeyboardFirst;
    private ListPreference preferenceDirectKeyboardSecond;
    private Preference preferenceDirectKeyboardPermission;
    private Object shizukuPermissionResultListener;

    ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startNotificationService();
                } else {
                    explainNotificationPermission();
                }
            });

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // add listeners for non-default actions
        findPreference(getString(R.string.settings_ime_available_key))
                .setOnPreferenceClickListener(preference -> {
                    Utilities.openAvailableKeyboards(getContext());
                    return false;
                });
        findPreference(getString(R.string.settings_ime_change_key))
                .setOnPreferenceClickListener(preference -> {
                    KeyboardSwitchController.Result result = KeyboardSwitchController.perform(
                            requireContext(),
                            getPreferenceManager().getSharedPreferences(),
                            KeyboardSwitchController.Trigger.SETTINGS
                    );
                    if (result.isSwitched()) {
                        showSwitchMessage(result);
                    } else {
                        Utilities.chooseAKeyboard(getContext());
                        explainDirectSwitchFallback(result);
                    }
                    return false;
                });

        preferenceDirectKeyboardSwitch =
                findPreference(getString(R.string.settings_direct_keyboard_switch_key));
        preferenceDirectKeyboardFirst =
                findPreference(getString(R.string.settings_direct_keyboard_first_key));
        preferenceDirectKeyboardSecond =
                findPreference(getString(R.string.settings_direct_keyboard_second_key));
        preferenceDirectKeyboardPermission =
                findPreference(getString(R.string.settings_direct_keyboard_permission_key));

        preferenceDirectKeyboardSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
            TwoStatePreference switchPreference = (TwoStatePreference) preference;
            switchPreference.setChecked((Boolean) newValue);
            refreshDirectKeyboardPreferences();
            if ((Boolean) newValue) {
                requestSecureSettingsPermissionIfPossible();
            }
            return false;
        });
        preferenceDirectKeyboardFirst.setOnPreferenceChangeListener((preference, newValue) -> {
            preferenceDirectKeyboardFirst.setValue((String) newValue);
            refreshDirectKeyboardPreferences();
            return false;
        });
        preferenceDirectKeyboardSecond.setOnPreferenceChangeListener((preference, newValue) -> {
            preferenceDirectKeyboardSecond.setValue((String) newValue);
            refreshDirectKeyboardPreferences();
            return false;
        });
        preferenceDirectKeyboardPermission.setOnPreferenceClickListener(preference -> {
            requestSecureSettingsPermissionIfPossible();
            return false;
        });

        preferenceNotification = findPreference(getString(R.string.settings_notification_key));
        preferenceNotification.setOnPreferenceClickListener(preference -> {
            if (preferenceNotification.isChecked()) {
                checkNotification(false);
                startNotificationServiceIfAllowed();
            } else {
                stopNotificationService();
            }
            return false;
        });

        preferenceOverlay = findPreference(getString(R.string.settings_floating_button_key));
        preferenceOverlay.setOnPreferenceClickListener(preference -> {
            if (preferenceOverlay.isChecked()) {
                checkOverlay(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    WarningFloatingButtonDialog dialogFragment = new WarningFloatingButtonDialog();
                    dialogFragment.show(getParentFragmentManager(), "warning_floating_button_dialog");
                } else {
                    startOverlayServiceIfAllowed();
                }
            } else {
                stopOverlayService();
            }
            return false;
        });

        findPreference(getString(R.string.settings_floating_button_lock_key))
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    TwoStatePreference switchPreference = (TwoStatePreference) preference;
                    switchPreference.setChecked((Boolean) newValue);
                    startOverlayServiceIfAllowed();
                    return false;
                });
        findPreference(getString(R.string.settings_floating_button_keyboard_visibility_key))
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    TwoStatePreference switchPreference = (TwoStatePreference) preference;
                    switchPreference.setChecked((Boolean) newValue);
                    startOverlayServiceIfAllowed();
                    return false;
                });
        findPreference(getString(R.string.settings_floating_size_key))
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
                    seekBarPreference.setValue((int) newValue);
                    startOverlayServiceIfAllowed();
                    return false;
                });

        addShizukuPermissionResultListener();
        refreshDirectKeyboardPreferences();
    }

    /*
     * ********************** *
     * DIRECT KEYBOARD SWITCH
     * ********************** *
     */

    private void refreshDirectKeyboardPreferences() {
        if (preferenceDirectKeyboardFirst == null || preferenceDirectKeyboardSecond == null) {
            return;
        }

        List<InputMethodInfo> enabledKeyboards = Utilities.getInstalledKeyboards(requireContext(), true);
        CharSequence[] entries = new CharSequence[enabledKeyboards.size()];
        CharSequence[] entryValues = new CharSequence[enabledKeyboards.size()];
        for (int index = 0; index < enabledKeyboards.size(); index++) {
            InputMethodInfo keyboard = enabledKeyboards.get(index);
            entries[index] = keyboard.loadLabel(requireContext().getPackageManager());
            entryValues[index] = keyboard.getId();
        }

        preferenceDirectKeyboardFirst.setEntries(entries);
        preferenceDirectKeyboardFirst.setEntryValues(entryValues);
        preferenceDirectKeyboardSecond.setEntries(entries);
        preferenceDirectKeyboardSecond.setEntryValues(entryValues);

        boolean hasEnabledKeyboards = !enabledKeyboards.isEmpty();
        preferenceDirectKeyboardFirst.setEnabled(hasEnabledKeyboards);
        preferenceDirectKeyboardSecond.setEnabled(hasEnabledKeyboards);
        preferenceDirectKeyboardFirst.setSummary(
                keyboardSummary(enabledKeyboards, preferenceDirectKeyboardFirst.getValue())
        );
        preferenceDirectKeyboardSecond.setSummary(
                keyboardSummary(enabledKeyboards, preferenceDirectKeyboardSecond.getValue())
        );
        refreshDirectKeyboardPermissionPreference();
    }

    private CharSequence keyboardSummary(
            List<InputMethodInfo> enabledKeyboards,
            String keyboardId
    ) {
        if (enabledKeyboards.isEmpty()) {
            return getString(R.string.settings_direct_keyboard_no_enabled_keyboards);
        }
        if (keyboardId == null || keyboardId.isEmpty()) {
            return getString(R.string.settings_direct_keyboard_not_selected);
        }

        InputMethodInfo keyboard = KeyboardSwitchController.findKeyboard(enabledKeyboards, keyboardId);
        if (keyboard == null) {
            return getString(R.string.settings_direct_keyboard_unavailable, keyboardId);
        }
        return keyboard.loadLabel(requireContext().getPackageManager());
    }

    private void refreshDirectKeyboardPermissionPreference() {
        if (preferenceDirectKeyboardPermission == null) {
            return;
        }

        if (SecureSettingsPermissionHelper.hasWriteSecureSettings(requireContext())) {
            preferenceDirectKeyboardPermission.setSummary(
                    R.string.settings_direct_keyboard_permission_granted
            );
            preferenceDirectKeyboardPermission.setEnabled(false);
            return;
        }

        preferenceDirectKeyboardPermission.setEnabled(true);
        String adbCommand = SecureSettingsPermissionHelper.getAdbGrantCommand(requireContext());
        if (SecureSettingsPermissionHelper.hasShizukuPermission()) {
            preferenceDirectKeyboardPermission.setSummary(
                    R.string.settings_direct_keyboard_permission_shizuku_ready
            );
        } else if (SecureSettingsPermissionHelper.isShizukuAvailable()) {
            preferenceDirectKeyboardPermission.setSummary(
                    getString(R.string.settings_direct_keyboard_permission_shizuku, adbCommand)
            );
        } else {
            preferenceDirectKeyboardPermission.setSummary(
                    getString(R.string.settings_direct_keyboard_permission_adb, adbCommand)
            );
        }
    }

    private void requestSecureSettingsPermissionIfPossible() {
        SecureSettingsPermissionHelper.GrantResult result =
                SecureSettingsPermissionHelper.grantWriteSecureSettingsWithShizuku(requireContext());
        if (result == SecureSettingsPermissionHelper.GrantResult.ALREADY_GRANTED
                || result == SecureSettingsPermissionHelper.GrantResult.GRANTED) {
            Toast.makeText(
                    requireContext(),
                    R.string.settings_direct_keyboard_permission_granted_toast,
                    Toast.LENGTH_SHORT
            ).show();
        } else if (result == SecureSettingsPermissionHelper.GrantResult.SHIZUKU_PERMISSION_REQUIRED) {
            SecureSettingsPermissionHelper.requestShizukuPermission();
        } else if (result == SecureSettingsPermissionHelper.GrantResult.FAILED) {
            Toast.makeText(
                    requireContext(),
                    R.string.settings_direct_keyboard_permission_failed_toast,
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(
                    requireContext(),
                    R.string.settings_direct_keyboard_permission_required_toast,
                    Toast.LENGTH_SHORT
            ).show();
        }
        refreshDirectKeyboardPreferences();
    }

    private void onShizukuPermissionResult(int requestCode, int grantResult) {
        if (requestCode != SecureSettingsPermissionHelper.SHIZUKU_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            requestSecureSettingsPermissionIfPossible();
        } else {
            Toast.makeText(
                    requireContext(),
                    R.string.settings_direct_keyboard_permission_required_toast,
                    Toast.LENGTH_SHORT
            ).show();
            refreshDirectKeyboardPreferences();
        }
    }

    private void showSwitchMessage(KeyboardSwitchController.Result result) {
        CharSequence label = result.getTargetKeyboardLabel();
        Toast.makeText(
                requireContext(),
                getString(
                        R.string.auto_switch_message,
                        label != null ? label : result.getTargetKeyboardId()
                ),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void explainDirectSwitchFallback(KeyboardSwitchController.Result result) {
        if (result.getReason() == KeyboardSwitchController.Reason.WRITE_SECURE_SETTINGS_MISSING) {
            Toast.makeText(
                    requireContext(),
                    R.string.settings_direct_keyboard_permission_required_toast,
                    Toast.LENGTH_SHORT
            ).show();
        } else if (result.getReason()
                == KeyboardSwitchController.Reason.SECURE_SETTING_WRITE_FAILED) {
            Toast.makeText(
                    requireContext(),
                    R.string.settings_direct_keyboard_permission_failed_toast,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void addShizukuPermissionResultListener() {
        shizukuPermissionResultListener =
                SecureSettingsPermissionHelper.addPermissionResultListener(
                        this::onShizukuPermissionResult
                );
    }

    private void removeShizukuPermissionResultListener() {
        SecureSettingsPermissionHelper.removePermissionResultListener(shizukuPermissionResultListener);
        shizukuPermissionResultListener = null;
    }

    /*
     * ************ *
     * NOTIFICATION
     * ************ *
     */

    private void checkNotification(boolean value) {
        if (preferenceNotification != null) {
            preferenceNotification.setChecked(value);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    boolean notificationsPermissionAllowed() {
        return ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    void startNotificationServiceIfAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notificationsPermissionAllowed()) {
                startNotificationService();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(), Manifest.permission.POST_NOTIFICATIONS
            )) {
                explainNotificationPermission();
                showNotificationSettings();
            } else {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            startNotificationService();
        }
    }

    private void startNotificationService() {
        checkNotification(true);
        startService();
    }

    private void startService() {
        Activity activity = getActivity();
        if (activity != null) {
            Context context = activity.getApplicationContext();
            if (context != null) {
                KeyboardSwitcherService.startService(context);
            }
        }
    }

    private void showNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName()));
        }
    }

    private void explainNotificationPermission() {
        checkNotification(false);
        Toast.makeText(
                requireContext(),
                R.string.error_notification_permission,
                Toast.LENGTH_SHORT
        ).show();
    }

    void stopNotificationService() {
        checkNotification(false);
        refreshKeyboardSwitcherService();
    }

    /*
     * ******* *
     * OVERLAY
     * ******* *
     */

    private void checkOverlay(boolean value) {
        if (preferenceOverlay != null) {
            preferenceOverlay.setChecked(value);
        }
        // Disable the notification because is necessary in Android > O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (preferenceNotification != null) {
                preferenceNotification.setEnabled(!value);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean overlayPermissionAllowed() {
        return Settings.canDrawOverlays(getActivity());
    }

    /** @noinspection deprecation*/
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openOverlaySetting() {
        if (preferenceOverlay != null)
            checkOverlay(false);
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + requireActivity().getPackageName()));
            /* request permission via start activity for result */
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            explainOverlayPermission();
        }
    }

    private void explainOverlayPermission() {
        checkOverlay(false);
        Toast.makeText(
                requireContext(),
                R.string.error_overlay_permission,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void startOverlayService() {
        checkOverlay(true);
        startService();
    }

    void startOverlayServiceIfAllowed() {
        if (shouldUseAccessibilityFloatingButton()) {
            startAccessibilityFloatingButtonIfAllowed();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (overlayPermissionAllowed()) {
                startOverlayService();
            } else {
                openOverlaySetting();
            }
        } else {
            startOverlayService();
        }
    }

    private boolean shouldUseAccessibilityFloatingButton() {
        return preferenceOverlay != null
                && preferenceOverlay.isChecked()
                && getPreferenceManager().getSharedPreferences().getBoolean(
                        getString(R.string.settings_floating_button_keyboard_visibility_key),
                        false
                );
    }

    private void startAccessibilityFloatingButtonIfAllowed() {
        checkOverlay(true);
        refreshKeyboardSwitcherService();
        if (KeyboardVisibilityAccessibilityService.isEnabled(requireContext())) {
            return;
        }

        Toast.makeText(
                requireContext(),
                R.string.error_accessibility_permission,
                Toast.LENGTH_LONG
        ).show();
        showAccessibilitySettings();
    }

    private void showAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    requireContext(),
                    R.string.error_accessibility_permission,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    void stopOverlayService() {
        checkOverlay(false);
        refreshKeyboardSwitcherService();
    }

	@Override
	public void onResume() {
		super.onResume();

        PreferenceActivity activity = ((PreferenceActivity) requireActivity());
        ActionBar toolbar = activity.getSupportActionBar();
        if (toolbar != null)
            toolbar.setTitle(R.string.app_name);
        activity.showTestZone(true);
        // To upgrade states
        checkNotification(preferenceNotification.isChecked());
        checkOverlay(preferenceOverlay.isChecked());
        refreshDirectKeyboardPreferences();
        refreshKeyboardSwitcherService();
	}

    @Override
    public void onDestroy() {
        removeShizukuPermissionResultListener();
        super.onDestroy();
    }

    @Override
    /*
     * To manage color selection
     */
    public void onPositiveButtonClick(@ColorInt int color) {
        super.onPositiveButtonClick(color);
        startOverlayServiceIfAllowed();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /* check if received result code
         is equal our requested code for draw permission  */
        if (requestCode == REQUEST_CODE) {
            /* if so check once again if we have permission */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // BUG : https://stackoverflow.com/questions/46173460/why-does-settings-candrawoverlays-method-in-android-8-returns-false-when-use
                if (Settings.canDrawOverlays(getActivity())) {
                    startOverlayServiceIfAllowed();
                }
            }
        }
    }

	private void refreshKeyboardSwitcherService() {
        startService();
	}
}
