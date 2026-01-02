package com.kunzisoft.keyboard.switcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.android.material.color.DynamicColors;

/**
 * Activity to create a shortcut to show the Keyboard Switcher.
 */
public class CreateShortcutActivity extends AppCompatActivity {

    private static final String SHORTCUT_ID = "show_keyboard_switcher";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DynamicColors.applyToActivityIfAvailable(this);

        String shortcutLabel = getString(R.string.shortcut_name);

        IconCompat shortcutIcon =
                IconCompat.createWithResource(this, R.drawable.ic_shortcut_24dp);

        ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(this, SHORTCUT_ID)
                .setIcon(shortcutIcon)
                .setShortLabel(shortcutLabel)
                .setIntent(KeyboardManagerActivity.getIntent(this))
                .build();

        Intent shortcutIntentResult =
                ShortcutManagerCompat.createShortcutResultIntent(this, shortcutInfo);

        setResult(Activity.RESULT_OK, shortcutIntentResult);
        finish();
    }
}
