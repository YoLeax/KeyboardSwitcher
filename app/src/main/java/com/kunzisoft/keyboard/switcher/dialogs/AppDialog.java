package com.kunzisoft.keyboard.switcher.dialogs;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.kunzisoft.keyboard.switcher.R;

/**
 * Custom Dialog launch at startup.
 */
public class AppDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        assert getActivity() != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(getString(R.string.app_warning_key), false).apply();

        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        stringBuilder.append(getString(R.string.app_warning));
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {});
        builder.setMessage(stringBuilder);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
