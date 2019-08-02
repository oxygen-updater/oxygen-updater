package com.arjanvlek.oxygenupdater.setupwizard;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.arjanvlek.oxygenupdater.ActivityLauncher;
import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.contribution.ContributorUtils;
import com.arjanvlek.oxygenupdater.internal.SetupUtils;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_CONTRIBUTE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;
import static com.arjanvlek.oxygenupdater.views.MainActivity.PERMISSION_REQUEST_CODE;
import static com.arjanvlek.oxygenupdater.views.MainActivity.VERIFY_FILE_PERMISSION;


@SuppressWarnings("Convert2Lambda")
public class SetupActivity extends AppCompatActivity {

	private static final String TAG = "SetupActivity";
	private Fragment step3Fragment;
	private Fragment step4Fragment;
	private SettingsManager settingsManager;
	private Consumer<Boolean> permissionCallback;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup);

		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		}

		this.settingsManager = new SettingsManager(getApplicationContext());

		if (!settingsManager.getPreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)) {
			ApplicationData applicationData = ((ApplicationData) getApplication());
			applicationData.getServerConnector().getDevices(result -> {
				if (!Utils.isSupportedDevice(applicationData.getSystemVersionProperties(), result)) {
					displayUnsupportedDeviceMessage();
				}
			});
		}

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		ViewPager mViewPager = findViewById(R.id.tutorialActivityPager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				if (position == 2) {
					if (step3Fragment != null) {
						SetupStep3Fragment setupStep3Fragment = (SetupStep3Fragment) step3Fragment;
						setupStep3Fragment.fetchDevices();
					}
				}
				if (position == 3) {
					if (step4Fragment != null) {
						SetupStep4Fragment setupStep4Fragment = (SetupStep4Fragment) step4Fragment;
						setupStep4Fragment.fetchUpdateMethods();
					}
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

	}

	public void displayUnsupportedDeviceMessage() {
		// Do not show dialog if app was already exited upon receiving of devices from the server.
		if (isFinishing()) {
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
		builder.setTitle(getString(R.string.unsupported_device_warning_title));
		builder.setMessage(getString(R.string.unsupported_device_warning_message));

		builder.setPositiveButton(getString(R.string.download_error_close), (dialog, which) -> dialog
				.dismiss());
		builder.show();
	}

	public Fragment newInstance(int sectionNumber) {
		if (sectionNumber == 3) {
			step3Fragment = new SetupStep3Fragment();
			return step3Fragment;
		}
		if (sectionNumber == 4) {
			step4Fragment = new SetupStep4Fragment();
			return step4Fragment;
		}
		Bundle args = new Bundle();
		args.putInt("section_number", sectionNumber);
		SimpleTutorialFragment simpleTutorialFragment = new SimpleTutorialFragment();
		simpleTutorialFragment.setArguments(args);
		return simpleTutorialFragment;
	}

	public void closeInitialTutorial(View view) {
		if (settingsManager.checkIfSetupScreenIsFilledIn()) {
			CheckBox contributorCheckbox = findViewById(R.id.introduction_step_5_contribute_checkbox);

			if (contributorCheckbox.isChecked()) {
				requestContributorStoragePermissions(new Consumer<Boolean>() {
					@Override
					public void accept(Boolean granted) {
						if (granted) {
							ContributorUtils contributorUtils = new ContributorUtils(getApplicationContext());
							contributorUtils.flushSettings(true); // 1st time, will save setting to true.
							settingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true);
							NavUtils.navigateUpFromSameTask(SetupActivity.this);
						} else {
							Toast.makeText(getApplication(), R.string.contribute_allow_storage, Toast.LENGTH_LONG)
									.show();
						}
					}
				});
			} else {
				settingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true);
				settingsManager.savePreference(PROPERTY_CONTRIBUTE, false); // not signed up, saving this setting will prevent contribute popups which belong to app updates.
				NavUtils.navigateUpFromSameTask(this);
			}
		} else {
			Long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L);
			Long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);
			Logger.logWarning(TAG, SetupUtils.getAsError("Setup wizard", deviceId, updateMethodId));
			Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG)
					.show();
		}
	}

	public void onContributeMoreInfoClick(View textView) {
		ActivityLauncher launcher = new ActivityLauncher(this);
		launcher.Contribute_noenroll();
		launcher.dispose();
	}

	private void requestContributorStoragePermissions(Consumer<Boolean> permissionCallback) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			this.permissionCallback = permissionCallback;
			requestPermissions(new String[]{VERIFY_FILE_PERMISSION}, PERMISSION_REQUEST_CODE);
		} else {
			permissionCallback.accept(true);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case PERMISSION_REQUEST_CODE:
				if (this.permissionCallback != null && grantResults.length > 0) {
					this.permissionCallback.accept(grantResults[0] == PackageManager.PERMISSION_GRANTED);
				}

		}
	}

	/**
	 * Contains the basic / non interactive tutorial fragments.
	 */
	@SuppressLint("ValidFragment")
	public static class SimpleTutorialFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";


		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
		                         Bundle savedInstanceState) {
			Bundle args = getArguments();
			int sectionNumber = args.getInt(ARG_SECTION_NUMBER, 0);
			if (sectionNumber == 1) {
				return inflater.inflate(R.layout.fragment_setup_1, container, false);
			} else if (sectionNumber == 2) {
				return inflater.inflate(R.layout.fragment_setup_2, container, false);
			} else if (sectionNumber == 5) {
				return inflater.inflate(R.layout.fragment_setup_5, container, false);

			}
			return null;
		}
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the
	 * sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a SimpleTutorialFragment (defined as a static inner class below).
			return newInstance(position + 1);
		}

		@Override
		public int getCount() {
			// Show 5 total pages.
			return 5;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return null;
		}
	}
}
