package com.arjanvlek.oxygenupdater.views;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;

/**
 * Sets support action bar and enables home up button on the toolbar
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
public abstract class SupportActionBarActivity extends AppCompatActivity {

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
