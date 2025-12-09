package com.kunzisoft.keyboard.switcher.settings;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.kunzisoft.keyboard.switcher.R;
import com.kunzisoft.keyboard.switcher.dialogs.AppDialog;
import com.kunzisoft.keyboard.switcher.dialogs.WarningFloatingButtonDialog;

public class PreferenceActivity extends AppCompatActivity implements WarningFloatingButtonDialog.OnFloatingButtonListener{

    private static final String TAG_PREFERENCE_FRAGMENT = "TAG_PREFERENCE_FRAGMENT";
    private View testZoneView;
    private Fragment currentFragment;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preference_activity);

        testZoneView = findViewById(R.id.test_zone_container);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }

        // Manage fragment who contains list of preferences
        currentFragment = getSupportFragmentManager().findFragmentByTag(TAG_PREFERENCE_FRAGMENT);
        if (currentFragment == null)
            currentFragment = new PreferenceFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, currentFragment, TAG_PREFERENCE_FRAGMENT)
                .commit();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(getString(R.string.app_warning_key), true)) {
            AppDialog dialogFragment = new AppDialog();
            dialogFragment.show(getSupportFragmentManager(), "application_dialog");
        }
    }

    public void showTestZone(boolean value) {
        if (value) {
            testZoneView.setVisibility(View.VISIBLE);
        } else {
            testZoneView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.contribution, menu);
        return true;
    }

    private void switchToPreferenceScreen() {
        getSupportFragmentManager().popBackStack();
    }

    private void switchToAboutScreen() {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, new AboutFragment(), TAG_PREFERENCE_FRAGMENT)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_contribute) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_PREFERENCE_FRAGMENT);
            if (fragment instanceof PreferenceFragment) {
                switchToAboutScreen();
            } else {
                switchToPreferenceScreen();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFloatingButtonDialogPositiveButtonClick() {
        if (currentFragment != null && currentFragment instanceof PreferenceFragment)
            ((PreferenceFragment) currentFragment).startOverlayServiceIfAllowed();
    }

    @Override
    public void onFloatingButtonDialogNegativeButtonClick() {
        if (currentFragment != null && currentFragment instanceof PreferenceFragment)
            ((PreferenceFragment) currentFragment).stopOverlayService();
    }

    @Override
    public void onBackPressed() {
        if (currentFragment instanceof AboutFragment) {
            switchToPreferenceScreen();
        } else {
            super.onBackPressed();
        }
    }
}
