package com.arjanvlek.oxygenupdater.help;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.arjanvlek.oxygenupdater.R;

public class HelpActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceSate) {
		super.onCreate(savedInstanceSate);
		setContentView(R.layout.activity_help);

		Toolbar toolbar = findViewById(R.id.toolbar);

		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
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
