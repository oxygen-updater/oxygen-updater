package com.arjanvlek.oxygenupdater.views;

import android.annotation.SuppressLint;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;

/**
 * Sets
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
@SuppressLint("Registered")
public class SupportActionBarActivity extends AppCompatActivity {

	private ApplicationData applicationData;

	@Override
	public void setContentView(@LayoutRes int layoutResId) {
		super.setContentView(layoutResId);

		Toolbar toolbar = findViewById(R.id.toolbar);

		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	protected ApplicationData getApplicationData() {
		if (applicationData == null) {
			applicationData = (ApplicationData) getApplication();
		}
		return applicationData;
	}
}
