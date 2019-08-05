package com.arjanvlek.oxygenupdater.settings;


import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.arjanvlek.oxygenupdater.ActivityLauncher;
import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.UpdateMethod;
import com.arjanvlek.oxygenupdater.internal.ThemeUtils;
import com.arjanvlek.oxygenupdater.notifications.Dialogs;
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus;
import com.crashlytics.android.Crashlytics;

import java.util.HashMap;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
public class SettingsFragment extends PreferenceFragmentCompat implements OnPreferenceChangeListener {

	private AppCompatActivity context;
	private ApplicationData application;
	private ActivityLauncher activityLauncher;
	private InAppPurchaseDelegate delegate;

	private SharedPreferences sharedPreferences;

	private ListPreference devicePreference;
	private ListPreference updateMethodPreference;

	void setInAppPurchaseDelegate(InAppPurchaseDelegate delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		context = (AppCompatActivity) getActivity();
		//noinspection ConstantConditions
		application = (ApplicationData) context.getApplication();
		activityLauncher = new ActivityLauncher(context);

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		addPreferencesFromResource(R.xml.preferences);

		setupSupportPreferences();
		setupDevicePreferences();
		setupThemePreference();
		setupAdvancedModePreference();
		setupAboutPreferences();
	}

	/**
	 * Sets up buy ad-free and contribute preferences
	 */
	private void setupSupportPreferences() {
		Preference contribute = findPreference(getString(R.string.key_contributor));

		//noinspection ConstantConditions
		contribute.setOnPreferenceClickListener(preference -> {
			activityLauncher.Contribute();
			return true;
		});
	}

	/**
	 * Sets up device and update method list preferences.
	 * <p>
	 * Entries are retrieved from the server, which calls for dynamically setting <code>entries</code> and <code>entryValues</code>
	 */
	private void setupDevicePreferences() {
		devicePreference = findPreference(getString(R.string.key_device));
		updateMethodPreference = findPreference(getString(R.string.key_update_method));

		devicePreference.setEnabled(false);
		updateMethodPreference.setEnabled(false);

		application.getServerConnector().getDevices(true, this::populateDeviceSettings);
	}

	private void setupThemePreference() {
		String key = getString(R.string.key_theme);

		Preference themePreference = findPreference(key);

		onPreferenceChange(themePreference, sharedPreferences.getString(key, getString(R.string.theme_system)));

		//noinspection ConstantConditions
		themePreference.setOnPreferenceChangeListener((preference, value) -> {
			onPreferenceChange(preference, value);

			sharedPreferences.edit()
					.putString(key, value.toString())
					.apply();

			AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(context));

			return true;
		});
	}

	private void setupAdvancedModePreference() {
		Preference advancedMode = findPreference(SettingsManager.PROPERTY_ADVANCED_MODE);

		//noinspection ConstantConditions
		advancedMode.setOnPreferenceClickListener(preference -> {
			boolean isAdvancedMode = sharedPreferences.getBoolean(SettingsManager.PROPERTY_ADVANCED_MODE, false);
			if (isAdvancedMode) {
				Dialogs.showAdvancedModeExplanation(application, context.getSupportFragmentManager());
			}

			return true;
		});
	}

	/**
	 * Sets up privacy policy, rating, and version preferences in the 'About' category
	 */
	private void setupAboutPreferences() {
		Preference privacyPolicy = findPreference(getString(R.string.key_privacy_policy));
		Preference rateApp = findPreference(getString(R.string.key_rate_app));
		Preference oxygenUpdater = findPreference(getString(R.string.key_oxygen));

		// Use Chrome Custom Tabs to open the privacy policy link
		Uri privacyPolicyUri = Uri.parse("https://oxygenupdater.com/legal");
		CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
				.setToolbarColor(getResources().getColor(R.color.appBarBackground))
				.build();

		//noinspection ConstantConditions
		privacyPolicy.setOnPreferenceClickListener(preference -> {
			customTabsIntent.launchUrl(context, privacyPolicyUri);
			return true;
		});

		// Open the app's Play Store page
		//noinspection ConstantConditions
		rateApp.setOnPreferenceClickListener(preference -> {
			activityLauncher.launchPlayStorePage(context);
			return true;
		});

		//noinspection ConstantConditions
		oxygenUpdater.setSummary(getResources().getString(R.string.summary_oxygen, BuildConfig.VERSION_NAME));

		oxygenUpdater.setOnPreferenceClickListener(preference -> {
			activityLauncher.About();
			return true;
		});
	}

	/**
	 * Populates {@link #devicePreference} and sets up a preference change listener to re-populate {@link #updateMethodPreference}
	 *
	 * @param devices retrieved from the server
	 */
	private void populateDeviceSettings(List<Device> devices) {
		if (devices != null && !devices.isEmpty()) {
			devicePreference.setEnabled(true);

			HashMap<CharSequence, Long> deviceMap = new HashMap<>();
			CharSequence[] deviceNames = new CharSequence[devices.size()];
			for (int i = 0; i < devices.size(); i++) {
				Device device = devices.get(i);

				deviceNames[i] = device.getName();

				deviceMap.put(deviceNames[i], device.getId());
			}

			// Populate device names
			devicePreference.setEntries(deviceNames);
			devicePreference.setEntryValues(deviceNames);

			// update summary
			onPreferenceChange(devicePreference, devicePreference.getValue());

			// Retrieve update methods for the selected device
			//noinspection ConstantConditions
			long longValue = deviceMap.get(devicePreference.getValue());
			application.getServerConnector().getUpdateMethods(longValue, this::populateUpdateMethods);

			// listen for preference change so that we can save the corresponding device ID,
			// and populate update methods
			devicePreference.setOnPreferenceChangeListener((preference, value) -> {
				// update summary
				onPreferenceChange(preference, value);

				//noinspection ConstantConditions
				long deviceId = deviceMap.get(value.toString());

				// Save device to shared preferences
				sharedPreferences.edit()
						.putString(getString(R.string.key_device), value.toString())
						.apply();

				// Save device ID to shared preferences
				sharedPreferences.edit()
						.putLong(getString(R.string.key_device_id), deviceId)
						.apply();

				// disable the update method preference since device has changed
				updateMethodPreference.setEnabled(false);

				// Retrieve update methods for the selected device
				application.getServerConnector().getUpdateMethods(deviceId, this::populateUpdateMethods);

				return true;
			});
		} else {
			PreferenceCategory deviceCategory = findPreference(getString(R.string.key_category_device));
			deviceCategory.setVisible(false);
			// devicePreference.setVisible(false);
			// updateMethodPreference.setVisible(false);
		}
	}

	/**
	 * Populates {@link #updateMethodPreference} and calls {@link Crashlytics#setUserIdentifier(String)}
	 *
	 * @param updateMethods retrieved from the server
	 */
	private void populateUpdateMethods(List<UpdateMethod> updateMethods) {
		if (updateMethods != null && !updateMethods.isEmpty()) {
			updateMethodPreference.setEnabled(true);

			HashMap<CharSequence, Long> updateMethodMap = new HashMap<>();
			CharSequence[] updateMethodNames = new CharSequence[updateMethods.size()];
			for (int i = 0; i < updateMethods.size(); i++) {
				UpdateMethod updateMethod = updateMethods.get(i);

				updateMethodNames[i] = updateMethod.getEnglishName();

				updateMethodMap.put(updateMethodNames[i], updateMethod.getId());
			}

			// Populate device names
			updateMethodPreference.setEntries(updateMethodNames);
			updateMethodPreference.setEntryValues(updateMethodNames);

			// update summary
			onPreferenceChange(updateMethodPreference, updateMethodPreference.getValue());

			updateMethodPreference.setOnPreferenceChangeListener((preference, value) -> {
				// update summary
				onPreferenceChange(preference, value);

				//noinspection ConstantConditions
				long updateMethodId = updateMethodMap.get(value.toString());

				// Save update method to shared preferences
				sharedPreferences.edit()
						.putString(getString(R.string.key_update_method), value.toString())
						.apply();

				// Save update method ID to shared preferences
				sharedPreferences.edit()
						.putLong(getString(R.string.key_update_method_id), updateMethodId)
						.apply();

				Crashlytics.setUserIdentifier("Device: " + devicePreference.getValue() + ", Update Method: " + updateMethodPreference.getValue());

				// Google Play services are not required if the user doesn't use notifications
				if (application.checkPlayServices(context.getParent(), false)) {
					// Subscribe to notifications for the newly selected device and update method
					NotificationTopicSubscriber.subscribe(application);
				} else {
					Toast.makeText(context, getString(R.string.notification_no_notification_support), LENGTH_LONG)
							.show();
				}

				return true;
			});
		} else {
			devicePreference.setVisible(false);
			updateMethodPreference.setVisible(false);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object value) {
		if (value != null) {
			preference.setSummary(value.toString());
		}

		return true;
	}

	/**
	 * Handler which is called when the Buy button is clicked
	 * <p>
	 * Starts the purchase process or initializes a new IabHelper if the current one was disposed early
	 *
	 * @param preference the buy ad free preference
	 */
	private void onBuyAdFreePreferenceClicked(Preference preference) {
		// Disable the Purchase button and set its text to "Processing...".
		preference.setEnabled(false);
		preference.setSummary(getString(R.string.processing));

		delegate.performInAppPurchase();
	}

	/**
	 * @param status purchase status
	 *
	 * @see #setupBuyAdFreePreference(PurchaseStatus, String)
	 */
	void setupBuyAdFreePreference(PurchaseStatus status) {
		setupBuyAdFreePreference(status, null);
	}

	/**
	 * Set summary and enable/disable the buy preference depending on purchased status
	 *
	 * @param status      purchase status
	 * @param adFreePrice price to display if the product can be bought
	 */
	void setupBuyAdFreePreference(PurchaseStatus status, @Nullable String adFreePrice) {
		Preference buyAdFree = findPreference(getString(R.string.key_ad_free));

		switch (status) {
			case UNAVAILABLE:
				//noinspection ConstantConditions
				buyAdFree.setEnabled(false);
				buyAdFree.setSummary(getString(R.string.settings_buy_button_not_possible));
				buyAdFree.setOnPreferenceClickListener(null);
				break;
			case AVAILABLE:
				//noinspection ConstantConditions
				buyAdFree.setEnabled(true);
				buyAdFree.setSummary(getString(R.string.settings_buy_button_buy, adFreePrice));
				buyAdFree.setOnPreferenceClickListener(preference -> {
					onBuyAdFreePreferenceClicked(preference);
					return true;
				});
				break;
			case ALREADY_BOUGHT:
				//noinspection ConstantConditions
				buyAdFree.setEnabled(false);
				buyAdFree.setSummary(getString(R.string.settings_buy_button_bought));
				buyAdFree.setOnPreferenceClickListener(null);
				break;
			default:
				throw new IllegalStateException("ShowBuyAdFreeButton: Invalid PurchaseStatus " + status.toString() + "!");
		}
	}

	public interface InAppPurchaseDelegate {
		void performInAppPurchase();
	}
}
