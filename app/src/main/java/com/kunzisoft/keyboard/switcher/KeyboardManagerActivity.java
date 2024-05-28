package com.kunzisoft.keyboard.switcher;

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
	private Runnable openPickerRunnable;

	private InputMethodManager imeManager;
    private View rootView;
    private View progressView;

    enum DialogState {
        NONE, PICKING, CHOSEN
    }

    private DialogState mState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
		mState = DialogState.NONE;
        setContentView(R.layout.empty);
        rootView = findViewById(R.id.root_view);
        progressView = findViewById(R.id.progress);
		super.onCreate(savedInstanceState);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_INPUT_METHODS)) {
            imeManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        } else {
            Toast.makeText(this, getString(R.string.error_unavailable_keyboard_feature), Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.cancel_button).setOnClickListener(view -> finish());

		openPickerRunnable = () -> {
            if (imeManager != null) {
                imeManager.showInputMethodPicker();
                mState = DialogState.PICKING;
            }
        };

		if (getIntent() != null) {
			delay = getIntent().getLongExtra(DELAY_SHOW_KEY, delay);
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
        rootView.removeCallbacks(openPickerRunnable);
        rootView.postDelayed(openPickerRunnable, delay);
        progressView.setVisibility(View.VISIBLE);
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
}
