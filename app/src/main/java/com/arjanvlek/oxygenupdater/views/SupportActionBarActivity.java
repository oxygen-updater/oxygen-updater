package com.arjanvlek.oxygenupdater.views;

import android.view.View;
import android.view.ViewGroup;

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

		setupToolbar();
	}

	@Override
	public void setContentView(View view) {
		super.setContentView(view);

		setupToolbar();
	}

	@Override
	public void setContentView(View view, ViewGroup.LayoutParams params) {
		super.setContentView(view, params);

		setupToolbar();
	}

	private void setupToolbar() {
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
