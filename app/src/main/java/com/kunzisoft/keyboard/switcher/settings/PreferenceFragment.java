package com.kunzisoft.keyboard.switcher.settings;

import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.FLOATING_BUTTON_START;
import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.FLOATING_BUTTON_STOP;
import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.NOTIFICATION_START;
import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.NOTIFICATION_STOP;

import android.Manifest;
import android.app.Activity;
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
import androidx.appcompat.app.AlertDialog;
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
    private TwoStatePreference preferenceFloatingButton;

    private boolean tryToOpenExternalDialog;

    private ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    activateNotification();
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
                Activity activity = getActivity();
                if (activity != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                activity,
                                Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED) {
                            activateNotification();
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                                activity, Manifest.permission.POST_NOTIFICATIONS
                        )) {
                            explainNotificationPermission();
                        } else {
                            // You can directly ask for the permission.
                            // The registered ActivityResultCallback gets the result of this request.
                            requestNotificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS

                            );
                        }
                    }
                }
            } else {
                stopKeyboardSwitcherService();
            }
            return false;
        });

        preferenceFloatingButton = findPreference(getString(R.string.settings_floating_button_key));
        preferenceFloatingButton.setOnPreferenceClickListener(preference -> {
            if (preferenceFloatingButton.isChecked()) {
                preferenceFloatingButton.setChecked(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    WarningFloatingButtonDialog dialogFragment = new WarningFloatingButtonDialog();
                    dialogFragment.show(getParentFragmentManager(), "warning_floating_button_dialog");
                } else {
                    startFloatingButtonAndCheckButton();
                }
            } else {
                stopFloatingButtonAndUncheckedButton();
            }
            return false;
        });

        findPreference(getString(R.string.settings_floating_button_lock_key))
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    SwitchPreference switchPreference = (SwitchPreference) preference;
                    switchPreference.setChecked((Boolean) newValue);
                    restartFloatingButtonAndCheckedButton();
                    return false;
                });
        findPreference(getString(R.string.settings_floating_size_key))
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
                    seekBarPreference.setValue((int) newValue);
                    restartFloatingButtonAndCheckedButton();
                    return false;
                });
    }

    private void activateNotification() {
        Intent intent = new Intent(requireActivity(), KeyboardSwitcherService.class);
        intent.setAction(NOTIFICATION_START);
        requireActivity().startService(intent);
    }

    private void explainNotificationPermission() {
        preferenceNotification.setChecked(false);
        Toast.makeText(
                requireContext(),
                R.string.error_notification_permission,
                Toast.LENGTH_SHORT
        ).show();
    }

	@Override
	public void onResume() {
		super.onResume();

		tryToOpenExternalDialog = false;
		// To unchecked the preference floating button if not allowed by the system
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.canDrawOverlays(getActivity())) {
				if (preferenceFloatingButton != null)
					preferenceFloatingButton.setChecked(false);
			}
		}
	}

    @Override
    /*
     * To manage color selection
     */
    public void onPositiveButtonClick(@ColorInt int color) {
        super.onPositiveButtonClick(color);
        restartFloatingButtonAndCheckedButton();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean drawOverlayPermissionAllowed() {
    	if (getActivity() != null) {
			/* check if we already  have permission to draw over other apps */
			if (Settings.canDrawOverlays(getActivity())) {
				return true;
			} else {
				try {
					/* if not construct intent to request permission */
					tryToOpenExternalDialog = true;
					Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
							Uri.parse("package:" + getActivity().getPackageName()));
					/* request permission via start activity for result */
					startActivityForResult(intent, REQUEST_CODE);
				} catch (ActivityNotFoundException e) {
					if (getContext() != null)
						new AlertDialog.Builder(getContext())
								.setMessage(R.string.error_overlay_permission_request)
								.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {}).create().show();
				}
			}
		}
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /* check if received result code
         is equal our requested code for draw permission  */
        if (requestCode == REQUEST_CODE) {
            /* if so check once again if we have permission */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(getActivity())) {
                    startFloatingButtonAndCheckButton();
                }
            }
        }
    }

	/**
	 * Method used to not destroy the main activity when an external dialog is requested
	 * @return 'true' if an external dialog is requested
	 */
	public boolean isTryingToOpenExternalDialog() {
    	return tryToOpenExternalDialog;
	}

	private void startFloatingButton() {
		if (getActivity() != null) {
			Intent intent = new Intent(getActivity(), KeyboardSwitcherService.class);
			intent.setAction(FLOATING_BUTTON_START);
			getActivity().startService(intent);
		}
	}

	private void stopKeyboardSwitcherService() {
		if (getActivity() != null) {
			Intent intent = new Intent(getActivity(), KeyboardSwitcherService.class);
			if (!preferenceNotification.isChecked() && !preferenceFloatingButton.isChecked()) {
				getActivity().stopService(intent);
			} else {
                if (!preferenceFloatingButton.isChecked()) {
                    intent.setAction(FLOATING_BUTTON_STOP);
                    getActivity().startService(intent);
                }
                if (!preferenceNotification.isChecked()) {
                    intent.setAction(NOTIFICATION_STOP);
                    getActivity().startService(intent);
                }
            }
		}
	}

    /*
    ------ Floating Button Service ------
    */

    void startFloatingButtonAndCheckButton() {
		stopKeyboardSwitcherService();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (drawOverlayPermissionAllowed()) {
				startFloatingButton();
			} else {
				if (preferenceFloatingButton != null)
					preferenceFloatingButton.setChecked(false);
			}
		} else {
			startFloatingButton();
		}
        if (preferenceFloatingButton != null)
            preferenceFloatingButton.setChecked(true);
    }

    void stopFloatingButtonAndUncheckedButton() {
    	stopKeyboardSwitcherService();
        if (preferenceFloatingButton != null)
            preferenceFloatingButton.setChecked(false);
    }

    private void restartFloatingButtonAndCheckedButton() {
        // Restart service
        if (getActivity() != null) {
            getActivity().stopService(new Intent(getActivity(), KeyboardSwitcherService.class));
        }
		startFloatingButtonAndCheckButton();
    }
}