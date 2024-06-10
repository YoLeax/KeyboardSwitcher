package com.kunzisoft.keyboard.switcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity to show keyboard manager
 */
public class KeyboardManagerActivity extends AppCompatActivity {

	public static final String DELAY_SHOW_KEY = "DELAY_SHOW_KEY";

	private long delay = 400L;

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

        setContentView(R.layout.empty);
        rootView = findViewById(R.id.root_view);

        findViewById(R.id.cancel_button).setOnClickListener(view -> finish());
        findViewById(R.id.relaunch_button).setOnClickListener(view -> launchKeyboard());

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_INPUT_METHODS)) {
            imeManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        }

        openPickerRunnable = this::launchKeyboard;

        if (getIntent() != null) {
			delay = getIntent().getLongExtra(DELAY_SHOW_KEY, delay);
		}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        delay = intent.getLongExtra(DELAY_SHOW_KEY, delay);
    }

    private void launchKeyboard() {
        if (imeManager != null) {
            imeManager.showInputMethodPicker();
            mState = DialogState.PICKING;
        } else {
            Toast.makeText(this, getString(R.string.error_unavailable_keyboard_feature), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(mState == DialogState.PICKING) {
            mState = DialogState.CHOSEN;
        }
        else if(mState == DialogState.CHOSEN) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        rootView.postDelayed(openPickerRunnable, delay);
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

    public static Intent getIntent(Context context, @Nullable Long delay) {
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
        if (delay != null)
            intent.putExtra(KeyboardManagerActivity.DELAY_SHOW_KEY, delay);
        return intent;
    }

    public static Intent getIntent(Context context) {
        return getIntent(context, null);
    }

    public static PendingIntent getPendingIntent(Context context) {
        return getPendingIntent(context, null);
    }

    @SuppressLint("UnsafeIntentLaunch")
    public static PendingIntent getPendingIntent(Context context, @Nullable Long delay) {
        return PendingIntent.getActivity(
                context,
                0,
                getIntent(context, delay),
                PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    @SuppressLint("UnsafeIntentLaunch")
    public static void launch(Context context) {
        context.startActivity(getIntent(context));
    }
}
