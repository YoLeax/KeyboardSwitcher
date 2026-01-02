package com.kunzisoft.keyboard.switcher;

import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.kunzisoft.keyboard.switcher.utils.Utilities;

/**
 * Activity to show keyboard manager
 */
public class KeyboardManagerActivity extends AppCompatActivity {

	public static final String KEYBOARD_ID = "KEYBOARD_ID";

	private static final long DELAY = 200L;

    private int tryCounter = 5;
	private String keyboardId = null;

	private InputMethodManager imeManager;
    private Runnable openPickerRunnable;
    private View rootView;

    enum DialogState {
        NONE, PICKING, CHOSEN
    }

    private DialogState mState = DialogState.NONE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DynamicColors.applyToActivityIfAvailable(this);

        setContentView(R.layout.empty);
        rootView = findViewById(R.id.root_view);

        findViewById(R.id.cancel_button).setOnClickListener(view -> finish());
        findViewById(R.id.relaunch_button).setOnClickListener(view -> launchKeyboardPicker());

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_INPUT_METHODS)) {
            imeManager = (InputMethodManager) getApplication().getSystemService(INPUT_METHOD_SERVICE);
        }

        initLauncher();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initLauncher();
    }

    private void initLauncher() {
        rootView.removeCallbacks(openPickerRunnable);
        retrieveIntentExtra(getIntent());
        launchKeyboardAutoSwitch();
        openPickerRunnable = this::tryLaunchKeyboardPicker;
    }

    private void retrieveIntentExtra(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra(KEYBOARD_ID))
                keyboardId = intent.getStringExtra(KEYBOARD_ID);
        }
    }

    private void showDialog() {
        rootView.setVisibility(VISIBLE);
    }

    private void launchKeyboardAutoSwitch() {
        if (keyboardId != null) {
            try {
                if (!Utilities.getCurrentDefaultKeyboard(this).equals(keyboardId)) {
                    Settings.Secure.putString(
                            getContentResolver(),
                            Settings.Secure.DEFAULT_INPUT_METHOD,
                            keyboardId
                    );
                    Toast.makeText(
                            this,
                            getString(R.string.auto_switch_message, keyboardId),
                            Toast.LENGTH_LONG
                    ).show();
                }
                finish();
            } catch (Exception e) {
                Log.w(KeyboardManagerActivity.class.getSimpleName(), "Unable to set default keyboard", e);
            }
        }
    }

    private void launchKeyboardPicker() {
        if (imeManager != null) {
            imeManager.showInputMethodPicker();
        } else {
            Toast.makeText(
                    this,
                    getString(R.string.error_unavailable_keyboard_feature),
                    Toast.LENGTH_SHORT
            ).show();
            finish();
        }
    }

    private void tryLaunchKeyboardPicker() {
        if (tryCounter <= 0) {
            showDialog();
        } else if (mState != DialogState.CHOSEN) {
            tryCounter--;
            launchKeyboardPicker();
            mState = DialogState.PICKING;
            // Retry after the delay
            rootView.postDelayed(openPickerRunnable, DELAY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(!hasFocus && mState == DialogState.PICKING) {
            mState = DialogState.CHOSEN;
        } else if(hasFocus && mState == DialogState.CHOSEN) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        rootView.postDelayed(openPickerRunnable, DELAY);
    }

    @Override
    protected void onPause() {
        rootView.removeCallbacks(openPickerRunnable);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Close the back activity
        finish();
    }

    public static Intent getIntent(Context context) {
        return getIntent(context, null);
    }

    public static Intent getIntent(
            Context context,
            @Nullable String keyboardId
    ) {
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        Intent intent = new Intent(context, KeyboardManagerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK
        );
        if (keyboardId != null)
            intent.putExtra(KeyboardManagerActivity.KEYBOARD_ID, keyboardId);
        return intent;
    }

    public static PendingIntent getPendingIntent(Context context) {
        return getPendingIntent(context, null);
    }

    @SuppressLint("UnsafeIntentLaunch")
    public static PendingIntent getPendingIntent(
            Context context,
            @Nullable String keyboardId
    ) {
        return PendingIntent.getActivity(
                context,
                0,
                getIntent(context, keyboardId),
                PendingIntent.FLAG_IMMUTABLE
                        | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    @SuppressLint("UnsafeIntentLaunch")
    public static void launch(Context context) {
        context.startActivity(getIntent(context));
    }
}
