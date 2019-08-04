package com.arjanvlek.oxygenupdater.views;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.arjanvlek.oxygenupdater.ActivityLauncher;
import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.deviceinformation.DeviceInformationFragment;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.news.NewsFragment;
import com.arjanvlek.oxygenupdater.notifications.MessageDialog;
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateInformationFragment;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.tabs.TabLayout;

import org.joda.time.LocalDateTime;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_ADVANCED_MODE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_AD_FREE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_CONTRIBUTE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_NOTIFICATION_TOPIC;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SETUP_DONE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_CHECKED_DATE;

public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener {

	public static final int PAGE_NEWS = 0;
	public static final int PAGE_UPDATE_INFORMATION = 1;
	public static final int PAGE_DEVICE_INFORMATION = 2;
	// Permissions constants
	public final static int PERMISSION_REQUEST_CODE = 200;

	public final static String DOWNLOAD_FILE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
	public final static String VERIFY_FILE_PERMISSION = "android.permission.READ_EXTERNAL_STORAGE";
	public static final String INTENT_START_PAGE = "start_page";

	private ViewPager viewPager;
	private SettingsManager settingsManager;
	private ActivityLauncher activityLauncher;
	private InterstitialAd newsAd;

	private Consumer<Boolean> downloadPermissionCallback;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Context context = getApplicationContext();
		settingsManager = new SettingsManager(context);

		// App version 2.4.6: Migrated old setting Show if system is up to date (default: ON) to Advanced mode (default: OFF).
		if (settingsManager.containsPreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE)) {
			settingsManager.savePreference(PROPERTY_ADVANCED_MODE, !settingsManager.getPreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true));
			settingsManager.deletePreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE);
		}

		// Supported device check
		if (!settingsManager.getPreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)) {
			ApplicationData application = ((ApplicationData) getApplication());
			application.getServerConnector().getDevices(result -> {
				if (!Utils.isSupportedDevice(application.getSystemVersionProperties(), result)) {
					displayUnsupportedDeviceMessage();
				}
			});
		}

		Toolbar toolbar = findViewById(R.id.toolbar);
		toolbar.setOnMenuItemClickListener(this);

		setupViewPager();

		activityLauncher = new ActivityLauncher(this);

		// Set start page to Update Information Screen (middle page)
		try {
			int startPage = PAGE_UPDATE_INFORMATION;

			Intent intent = getIntent();

			if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(INTENT_START_PAGE)) {
				startPage = intent.getExtras().getInt(INTENT_START_PAGE);
			}

			viewPager.setCurrentItem(startPage);
		} catch (IndexOutOfBoundsException ignored) {
			// no-op
		}

		setupAds();

		// Support functions for Android 8.0 "Oreo" and up.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createPushNotificationChannel();
			createProgressNotificationChannel();
		}

		// Remove "long" download ID in favor of "int" id
		try {
			settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, -1);
		} catch (ClassCastException e) {
			settingsManager.deletePreference(PROPERTY_DOWNLOAD_ID);
		}

		// Offer contribution to users from app versions below 2.4.0
		if (!settingsManager.containsPreference(PROPERTY_CONTRIBUTE) && settingsManager.containsPreference(PROPERTY_SETUP_DONE)) {
			activityLauncher.Contribute();
		}
	}

	/**
	 * Handles toolbar menu clicks
	 *
	 * @param item the menu item
	 *
	 * @return true if clicked
	 */
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
			case R.id.action_settings:
				activityLauncher.Settings();
				return true;
			case R.id.action_about:
				activityLauncher.About();
				return true;
			case R.id.action_help:
				activityLauncher.Help();
				return true;
			case R.id.action_faq:
				activityLauncher.FAQ();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void setupViewPager() {
		TabLayout tabLayout = findViewById(R.id.tabs);
		viewPager = findViewById(R.id.viewpager);

		viewPager.setOffscreenPageLimit(2);
		viewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));

		tabLayout.setupWithViewPager(viewPager);
	}

	/**
	 * Checks for Play Services and initialises {@link MobileAds} if found
	 */
	private void setupAds() {
		ApplicationData application = (ApplicationData) getApplication();

		if (application.checkPlayServices(this, false)) {
			MobileAds.initialize(this, "ca-app-pub-0760639008316468~7665206420");
		} else {
			Toast.makeText(application, getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show();
		}

		if (!settingsManager.getPreference(PROPERTY_AD_FREE, false)) {
			newsAd = new InterstitialAd(this);
			newsAd.setAdUnitId(getString(R.string.news_ad_unit_id));
			newsAd.loadAd(application.buildAdRequest());
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		// Mark the welcome tutorial as finished if the user is moving from older app version.
		// This is checked by either having stored update information for offline viewing,
		// or if the last update checked date is set (if user always had up to date system and never viewed update information before)
		if (!settingsManager.getPreference(PROPERTY_SETUP_DONE, false)
				&& (settingsManager.checkIfOfflineUpdateDataIsAvailable() || settingsManager.containsPreference(PROPERTY_UPDATE_CHECKED_DATE))) {
			settingsManager.savePreference(PROPERTY_SETUP_DONE, true);
		}

		// Show the welcome tutorial if the app needs to be set up
		if (!settingsManager.getPreference(PROPERTY_SETUP_DONE, false)) {
			if (Utils.checkNetworkConnection(getApplicationContext())) {
				activityLauncher.Tutorial();
			} else {
				showNetworkError();
			}
		} else {
			if (!settingsManager.containsPreference(PROPERTY_NOTIFICATION_TOPIC)) {
				NotificationTopicSubscriber.subscribe((ApplicationData) getApplication());
			}
		}
	}

	public void displayUnsupportedDeviceMessage() {
		View checkBoxView = View.inflate(MainActivity.this, R.layout.message_dialog_checkbox, null);
		CheckBox checkBox = checkBoxView.findViewById(R.id.unsupported_device_warning_checkbox);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(checkBoxView);
		builder.setTitle(getString(R.string.unsupported_device_warning_title));
		builder.setMessage(getString(R.string.unsupported_device_warning_message));

		builder.setPositiveButton(getString(R.string.download_error_close), (dialog, which) -> {
			settingsManager.savePreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, checkBox
					.isChecked());
			dialog.dismiss();
		});
		builder.show();
	}

	private void showNetworkError() {
		if (!isFinishing()) {
			MessageDialog errorDialog = new MessageDialog()
					.setTitle(getString(R.string.error_app_requires_network_connection))
					.setMessage(getString(R.string.error_app_requires_network_connection_message))
					.setNegativeButtonText(getString(R.string.download_error_close))
					.setClosable(false);
			errorDialog.show(getSupportFragmentManager(), "NetworkError");
		}
	}

	public InterstitialAd getNewsAd() {
		if (newsAd != null) {
			return newsAd;
		} else if (mayShowNewsAd()) {
			InterstitialAd interstitialAd = new InterstitialAd(this);
			interstitialAd.setAdUnitId(getString(R.string.news_ad_unit_id));
			interstitialAd.loadAd(((ApplicationData) getApplication()).buildAdRequest());
			newsAd = interstitialAd;
			return newsAd;
		} else {
			return null;
		}
	}

	public boolean mayShowNewsAd() {
		return !settingsManager.getPreference(PROPERTY_AD_FREE, false) &&
				LocalDateTime.parse(settingsManager.getPreference(PROPERTY_LAST_NEWS_AD_SHOWN, "1970-01-01T00:00:00.000"))
						.isBefore(LocalDateTime.now().minusMinutes(5));

	}

	public ActivityLauncher getActivityLauncher() {
		return activityLauncher;
	}

	public void requestDownloadPermissions(@NonNull Consumer<Boolean> callback) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			downloadPermissionCallback = callback;
			requestPermissions(new String[]{DOWNLOAD_FILE_PERMISSION, VERIFY_FILE_PERMISSION}, PERMISSION_REQUEST_CODE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (permsRequestCode == PERMISSION_REQUEST_CODE) {
			if (downloadPermissionCallback != null && grantResults.length > 0) {
				downloadPermissionCallback.accept(grantResults[0] == PackageManager.PERMISSION_GRANTED);
			}
		}
	}

	// Android 6.0 Run-time permissions

	public boolean hasDownloadPermissions() {
		return ContextCompat.checkSelfPermission(getApplication(), VERIFY_FILE_PERMISSION) == PackageManager.PERMISSION_GRANTED
				&& ContextCompat.checkSelfPermission(getApplication(), DOWNLOAD_FILE_PERMISSION) == PackageManager.PERMISSION_GRANTED;
	}

	@TargetApi(26)
	private void createPushNotificationChannel() {
		// The id of the channel.
		String id = ApplicationData.PUSH_NOTIFICATION_CHANNEL_ID;

		// The user-visible name of the channel.
		CharSequence name = getString(R.string.push_notification_channel_name);

		// The user-visible description of the channel.
		String description = getString(R.string.push_notification_channel_description);

		int importance = NotificationManager.IMPORTANCE_HIGH;

		NotificationChannel channel = new NotificationChannel(id, name, importance);

		// Configure the notification channel.
		channel.setDescription(description);
		channel.enableLights(true);
		// Sets the notification light color for notifications posted to this
		// channel, if the device supports this feature.
		channel.setLightColor(Color.RED);
		channel.enableVibration(true);
		channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (notificationManager != null) {
			notificationManager.createNotificationChannel(channel);
		}
	}

	@TargetApi(26)
	private void createProgressNotificationChannel() {
		// The id of the channel.
		String id = ApplicationData.PROGRESS_NOTIFICATION_CHANNEL_ID;

		// The user-visible name of the channel.
		CharSequence name = getString(R.string.progress_notification_channel_name);

		// The user-visible description of the channel.
		String description = getString(R.string.progress_notification_channel_description);

		int importance = NotificationManager.IMPORTANCE_LOW;

		NotificationChannel channel = new NotificationChannel(id, name, importance);

		// Configure the notification channel.
		channel.setDescription(description);
		channel.enableLights(false);
		channel.enableVibration(false);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (notificationManager != null) {
			notificationManager.createNotificationChannel(channel);
		}
	}

	// Android 8.0 Notification Channels

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		SectionsPagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		}

		@NonNull
		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			switch (position) {
				case PAGE_NEWS:
					return new NewsFragment();
				case PAGE_UPDATE_INFORMATION:
					return new UpdateInformationFragment();
				case PAGE_DEVICE_INFORMATION:
					return new DeviceInformationFragment();
				default:
					//noinspection ConstantConditions
					return null;
			}
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case PAGE_NEWS:
					return getString(R.string.news);
				case PAGE_UPDATE_INFORMATION:
					return getString(R.string.update_information_header_short);
				case PAGE_DEVICE_INFORMATION:
					return getString(R.string.device_information_header_short);
			}
			return null;
		}
	}
}
