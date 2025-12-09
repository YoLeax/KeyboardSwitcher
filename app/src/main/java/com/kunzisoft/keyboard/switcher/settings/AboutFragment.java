package com.kunzisoft.keyboard.switcher.settings;

import static com.kunzisoft.keyboard.switcher.utils.Constants.PACKAGE_DONATION;
import static com.kunzisoft.keyboard.switcher.utils.Constants.URL_CONTRIBUTION;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.kunzisoft.keyboard.switcher.R;
import com.kunzisoft.keyboard.switcher.utils.Constants;

/**
 * Show the about page
 */
public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getContext() != null) {
            View rootView = inflater.inflate(R.layout.about_fragment, container, false);

            TextView versionTextView = rootView.findViewById(R.id.activity_about_version);
            String versionString = getString(R.string.about_version) + " " + Constants.getVersion(getContext());
            versionTextView.setText(versionString);

            String htmlAbout =
                    "<p>" + getString(R.string.html_text_purpose) + "</p>" +

                    "<h2>" + getString(R.string.about_title) + "</h2>" +
                    "<p>" + getString(R.string.html_text_free, getString(R.string.app_name)) + "</p>" +
                    "<p>" + getString(R.string.html_text_contribution) + "</p>";

            rootView.findViewById(R.id.contribution_playstore_button).setOnClickListener(view -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_DONATION)));
                } catch (android.content.ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + PACKAGE_DONATION)));
                }
            });

            rootView.findViewById(R.id.contribution_button).setOnClickListener(view -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_CONTRIBUTION)));
            });

            String htmlContact =
                    "<h2>" + getString(R.string.contact_title) + "</h2>" +
                    "<p>" + getString(R.string.source_code) + " <a href=\"" + Constants.URL_SOURCE_CODE + "\">" + Constants.URL_SOURCE_CODE + "</a></p>" +
                    "<p>" + getString(R.string.powered_by) + " <a href=\"" + Constants.URL_WEB_SITE + "\">" + Constants.ORGANIZATION + "</a></p>";

            TextView aboutTextView = rootView.findViewById(R.id.activity_about_content);
            aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
            aboutTextView.setText(HtmlCompat.fromHtml(htmlAbout, HtmlCompat.FROM_HTML_MODE_LEGACY));
            TextView contactTextView = rootView.findViewById(R.id.activity_contact_content);
            contactTextView.setMovementMethod(LinkMovementMethod.getInstance());
            contactTextView.setText(HtmlCompat.fromHtml(htmlContact, HtmlCompat.FROM_HTML_MODE_LEGACY));
            return rootView;
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferenceActivity activity = ((PreferenceActivity) requireActivity());
        ActionBar toolbar = activity.getSupportActionBar();
        if (toolbar != null)
            toolbar.setTitle(R.string.about_title);
        activity.showTestZone(false);
    }
}
