package com.arjanvlek.oxygenupdater.about;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.ActivityLauncher;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity;

public class AboutActivity extends SupportActionBarActivity {

	@Override
	public void onCreate(Bundle savedInstanceSate) {
		super.onCreate(savedInstanceSate);
		setContentView(R.layout.activity_about);

		ActivityLauncher activityLauncher = new ActivityLauncher(this);

		// Set the version number of the app in the version number field.
		String versionNumber = BuildConfig.VERSION_NAME;
		TextView versionNumberView = findViewById(R.id.aboutVersionNumberView);
		versionNumberView.setText(String.format(getString(R.string.about_version), versionNumber));

		//Make the links in the background story clickable.
		TextView storyView = findViewById(R.id.aboutBackgroundStoryView);
		storyView.setMovementMethod(LinkMovementMethod.getInstance());

		// Set onClick listener to Google Play rate button.
		Button rateAppButton = findViewById(R.id.aboutRateButton);
		rateAppButton.setOnClickListener(v -> activityLauncher.launchPlayStorePage(this));

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
