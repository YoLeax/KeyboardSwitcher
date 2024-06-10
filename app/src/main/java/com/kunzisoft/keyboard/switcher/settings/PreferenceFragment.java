package com.kunzisoft.keyboard.switcher.settings;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.kunzisoft.androidclearchroma.ChromaPreferenceFragmentCompat;
import com.kunzisoft.keyboard.switcher.KeyboardSwitcherService;
import com.kunzisoft.keyboard.switcher.R;
import com.kunzisoft.keyboard.switcher.dialogs.WarningFloatingButtonDialog;
import com.kunzisoft.keyboard.switcher.utils.Utilities;

public class PreferenceFragment extends ChromaPreferenceFragmentCompat {

    /* https://stackoverflow.com/questions/7569937/unable-to-add-window-android-view-viewrootw44da9bc0-permission-denied-for-t
    code to post/handler request for permission
    */
    private final static int REQUEST_CODE = 6517;

    private TwoStatePreference preferenceNotification;
    private TwoStatePreference preferenceOverlay;

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
                    Utilities.chooseAKeyboard(getContext());
                    return false;
                });

        preferenceNotification = findPreference(getString(R.string.settings_notification_key));
        preferenceNotification.setOnPreferenceClickListener(preference -> {
            if (preferenceNotification.isChecked()) {
                preferenceNotification.setChecked(false);
                startNotificationServiceIfAllowed();
            } else {
                stopNotificationService();
            }
            return false;
        });

        preferenceOverlay = findPreference(getString(R.string.settings_floating_button_key));
        preferenceOverlay.setOnPreferenceClickListener(preference -> {
            if (preferenceOverlay.isChecked()) {
                preferenceOverlay.setChecked(false);
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
                    SwitchPreference switchPreference = (SwitchPreference) preference;
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
    }

    /*
     * ************ *
     * NOTIFICATION
     * ************ *
     */

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
        if (preferenceNotification != null)
            preferenceNotification.setChecked(true);
        KeyboardSwitcherService.startService(requireActivity());
    }

    private void showNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName()));
        }
    }

    private void explainNotificationPermission() {
        if (preferenceNotification != null)
            preferenceNotification.setChecked(false);
        Toast.makeText(
                requireContext(),
                R.string.error_notification_permission,
                Toast.LENGTH_SHORT
        ).show();
    }

    void stopNotificationService() {
        if (preferenceNotification != null)
            preferenceNotification.setChecked(false);
        refreshKeyboardSwitcherService();
    }

    /*
     * ******* *
     * OVERLAY
     * ******* *
     */

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean overlayPermissionAllowed() {
        return Settings.canDrawOverlays(getActivity());
    }

    /** @noinspection deprecation*/
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openOverlaySetting() {
        if (preferenceOverlay != null)
            preferenceOverlay.setChecked(false);
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
        if (preferenceOverlay != null)
            preferenceOverlay.setChecked(false);
        Toast.makeText(
                requireContext(),
                R.string.error_overlay_permission,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void startOverlayService() {
        if (preferenceOverlay != null)
            preferenceOverlay.setChecked(true);
        KeyboardSwitcherService.startService(requireActivity());
    }

    void startOverlayServiceIfAllowed() {
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

    void stopOverlayService() {
        if (preferenceOverlay != null)
            preferenceOverlay.setChecked(false);
        refreshKeyboardSwitcherService();
    }

	@Override
	public void onResume() {
		super.onResume();
        refreshKeyboardSwitcherService();
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
        KeyboardSwitcherService.startService(requireActivity());
	}
}