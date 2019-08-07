package com.arjanvlek.oxygenupdater.contribution;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.widget.Toast.LENGTH_LONG;
import static com.arjanvlek.oxygenupdater.views.MainActivity.PERMISSION_REQUEST_CODE;
import static com.arjanvlek.oxygenupdater.views.MainActivity.VERIFY_FILE_PERMISSION;

@SuppressWarnings("Convert2Lambda")
public class ContributorActivity extends SupportActionBarActivity {

	public static final String INTENT_HIDE_ENROLLMENT = "hide_enrollment";

	private final AtomicBoolean localContributeSetting = new AtomicBoolean(false);
	private final AtomicBoolean saveOptionsHidden = new AtomicBoolean(false);
	private Consumer<Boolean> permissionCallback;

	@Override
	public void onCreate(Bundle savedInstanceSate) {
		super.onCreate(savedInstanceSate);
		setContentView(R.layout.activity_contributor);

		if (getIntent().getBooleanExtra(INTENT_HIDE_ENROLLMENT, false)) {
			findViewById(R.id.contributeCheckbox).setVisibility(View.GONE);
			findViewById(R.id.contributeAgreeText).setVisibility(View.GONE);
			findViewById(R.id.contributeSaveButton).setVisibility(View.GONE);
			saveOptionsHidden.compareAndSet(false, true);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		setInitialCheckboxState();
	}

	@Override
	public void onResume() {
		super.onResume();
		setCheckboxClickListener();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_CODE && permissionCallback != null && grantResults.length > 0) {
			permissionCallback.accept(grantResults[0] == PackageManager.PERMISSION_GRANTED);
		}
	}

	@Override
	public void onBackPressed() {
		// Respond to the device's back button
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Respond to the action bar's Back arrow button
		if (item.getItemId() == android.R.id.home) {
			if (!saveOptionsHidden.get()) {
				onSaveButtonClick(null);
			} else {
				finish();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setInitialCheckboxState() {
		SettingsManager settingsManager = new SettingsManager(getApplicationContext());
		boolean isContributing = settingsManager.getPreference(SettingsManager.PROPERTY_CONTRIBUTE, false);
		localContributeSetting.set(isContributing);

		CheckBox checkbox = findViewById(R.id.contributeCheckbox);
		checkbox.setChecked(isContributing);
	}

	private void setCheckboxClickListener() {
		CheckBox checkbox = findViewById(R.id.contributeCheckbox);
		checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				localContributeSetting.set(isChecked);
			}
		});
	}

	public void onSaveButtonClick(View checkbox) {
		ContributorUtils contributorUtils = new ContributorUtils(getApplication());
		boolean contributor = localContributeSetting.get();

		if (contributor) {
			requestContributorStoragePermissions(new Consumer<Boolean>() {
				@Override
				public void accept(Boolean granted) {
					if (granted) {
						contributorUtils.flushSettings(true);
						finish();
					} else {
						Toast.makeText(ContributorActivity.this, R.string.contribute_allow_storage, LENGTH_LONG).show();
					}

				}
			});
		} else {
			contributorUtils.flushSettings(false);
			finish();
		}
	}

	private void requestContributorStoragePermissions(Consumer<Boolean> permissionCallback) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			this.permissionCallback = permissionCallback;
			requestPermissions(new String[]{VERIFY_FILE_PERMISSION}, PERMISSION_REQUEST_CODE);
		} else {
			permissionCallback.accept(true);
		}
	}
}
