package com.arjanvlek.oxygenupdater.help;

import android.os.Bundle;
import android.view.MenuItem;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity;

public class HelpActivity extends SupportActionBarActivity {

	@Override
	public void onCreate(Bundle savedInstanceSate) {
		super.onCreate(savedInstanceSate);
		setContentView(R.layout.activity_help);
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Respond to the action bar's Up/Home button
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

}
