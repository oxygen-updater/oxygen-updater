package com.arjanvlek.oxygenupdater.views;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;

public class AboutActivity extends AppCompatActivity {

    private static final String GOOGLE_PLAY_BASE_URL = "market://details?id=com.arjanvlek.oxygenupdater";
    private static final String GOOGLE_PLAY_BROWSER__BASE_URL = "https://play.google.com/store/apps/details?id=com.arjanvlek.oxygenupdater";

    @Override
    public void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        String versionNumber = BuildConfig.VERSION_NAME;
        setContentView(R.layout.activity_about);
        TextView versionNumberView = (TextView) findViewById(R.id.aboutVersionNumberView);
        versionNumberView.setText(String.format(getString(R.string.about_version), versionNumber));

        //Make link clickable
        TextView storyView = (TextView) findViewById(R.id.aboutBackgroundStoryView);
        storyView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void rateApp(View view) {
        try {
            final String appPackageName = getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_BASE_URL + appPackageName)));
            } catch (ActivityNotFoundException e) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_BROWSER__BASE_URL + appPackageName)));
                } catch (ActivityNotFoundException e1) {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_unable_to_rate_app), Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
