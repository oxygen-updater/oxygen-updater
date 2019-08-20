package com.arjanvlek.oxygenupdater.settings;


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import com.arjanvlek.oxygenupdater.ActivityLauncher;
import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.domain.UpdateMethod;
import com.arjanvlek.oxygenupdater.internal.ThemeUtils;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.arjanvlek.oxygenupdater.notifications.Dialogs;
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;
import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK;
import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT;

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
public class SettingsFragment extends PreferenceFragmentCompat implements OnPreferenceChangeListener {

	private Context context;
	private AppCompatActivity activity;
	private ApplicationData application;
	private ActivityLauncher activityLauncher;
	private InAppPurchaseDelegate delegate;

	private SettingsManager settingsManager;

	private BottomSheetPreference devicePreference;
	private BottomSheetPreference updateMethodPreference;

	void setInAppPurchaseDelegate(InAppPurchaseDelegate delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		init();

		addPreferencesFromResource(R.xml.preferences);

		setupSupportPreferences();
		setupDevicePreferences();
		setupThemePreference();
		setupAdvancedModePreference();
		setupAboutPreferences();
	}

	/**
	 * Initialises context, activity, application, and their relevant references
	 */
	private void init() {
		context = getContext();
		activity = (AppCompatActivity) getActivity();
		//noinspection ConstantConditions
		application = (ApplicationData) activity.getApplication();
		activityLauncher = new ActivityLauncher(activity);

		settingsManager = new SettingsManager(context);
	}

	@Override
	public void onResume() {
		super.onResume();

		init();
	}

	/**
	 * Sets up buy ad-free and contribute preferences
	 */
	private void setupSupportPreferences() {
		Preference contribute = findPreference(context.getString(R.string.key_contributor));

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
		devicePreference = findPreference(context.getString(R.string.key_device));
		updateMethodPreference = findPreference(context.getString(R.string.key_update_method));

		devicePreference.setEnabled(false);
		updateMethodPreference.setEnabled(false);

		application.getServerConnector().getDevices(true, this::populateDeviceSettings);
	}

	private void setupThemePreference() {
		//noinspection ConstantConditions
		findPreference(context.getString(R.string.key_theme)).setOnPreferenceChangeListener((preference, value) -> {
			AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(context));

			return true;
		});
	}

	private void setupAdvancedModePreference() {
		Preference advancedMode = findPreference(SettingsManager.PROPERTY_ADVANCED_MODE);

		//noinspection ConstantConditions
		advancedMode.setOnPreferenceClickListener(preference -> {
			boolean isAdvancedMode = settingsManager.getPreference(SettingsManager.PROPERTY_ADVANCED_MODE, false);
			if (isAdvancedMode) {
				Dialogs.showAdvancedModeExplanation(context, activity.getSupportFragmentManager());
			}

			return true;
		});
	}

	/**
	 * Sets up privacy policy, rating, and version preferences in the 'About' category
	 */
	private void setupAboutPreferences() {
		Preference privacyPolicy = findPreference(context.getString(R.string.key_privacy_policy));
		Preference rateApp = findPreference(context.getString(R.string.key_rate_app));
		Preference oxygenUpdater = findPreference(context.getString(R.string.key_oxygen));

		// Use Chrome Custom Tabs to open the privacy policy link
		Uri privacyPolicyUri = Uri.parse("https://oxygenupdater.com/legal");
		CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
				.setColorScheme(ThemeUtils.isNightModeActive(context) ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT)
				.setToolbarColor(ContextCompat.getColor(context, R.color.appBarBackground))
				.setNavigationBarColor(ContextCompat.getColor(context, R.color.background))
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
			SystemVersionProperties systemVersionProperties = application.getSystemVersionProperties();

			// Set the spinner to the previously selected device.
			int recommendedPosition = -1;

			int selectedPosition = -1;

			Long deviceId = settingsManager.getPreference(context.getString(R.string.key_device_id), -1L);

			List<BottomSheetItem> itemList = new ArrayList<>();
			HashMap<CharSequence, Long> deviceMap = new HashMap<>();
			for (int i = 0; i < devices.size(); i++) {
				Device device = devices.get(i);

				deviceMap.put(device.getName(), device.getId());

				List<String> productNames = device.getProductNames();

				if (productNames != null && productNames.contains(systemVersionProperties.getOxygenDeviceName())) {
					recommendedPosition = i;
				}

				if (device.getId() == deviceId) {
					selectedPosition = i;
				}

				itemList.add(BottomSheetItem.builder()
						.title(device.getName())
						.value(device.getName())
						.secondaryValue(device.getId())
						.build()
				);
			}

			devicePreference.setItemList(itemList);

			// If there's there no device saved in preferences, auto select the recommended device
			if (selectedPosition == -1 && recommendedPosition != -1) {
				devicePreference.setValueIndex(recommendedPosition);
			}

			// Retrieve update methods for the selected device
			application.getServerConnector().getUpdateMethods(deviceId, this::populateUpdateMethods);

			// listen for preference change so that we can save the corresponding device ID,
			// and populate update methods
			devicePreference.setOnPreferenceChangeListener((preference, value) -> {
				// disable the update method preference since device has changed
				updateMethodPreference.setEnabled(false);

				// Retrieve update methods for the selected device
				//noinspection ConstantConditions
				application.getServerConnector().getUpdateMethods(deviceMap.get(value.toString()), this::populateUpdateMethods);

				return true;
			});
		} else {
			PreferenceCategory deviceCategory = findPreference(context.getString(R.string.key_category_device));
			//noinspection ConstantConditions
			deviceCategory.setVisible(false);
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

			long currentUpdateMethodId = settingsManager.getPreference(context.getString(R.string.key_update_method_id), -1L);

			List<Integer> recommendedPositions = new ArrayList<>();
			int selectedPosition = -1;

			List<BottomSheetItem> itemList = new ArrayList<>();
			for (int i = 0; i < updateMethods.size(); i++) {
				UpdateMethod updateMethod = updateMethods.get(i);

				if (updateMethod.isRecommended()) {
					recommendedPositions.add(i);
				}

				if (updateMethod.getId() == currentUpdateMethodId) {
					selectedPosition = i;
				}

				String updateMethodName = Locale.getLocale() == Locale.NL
						? updateMethod.getDutchName()
						: updateMethod.getEnglishName();

				itemList.add(BottomSheetItem.builder()
						.title(updateMethodName)
						.value(updateMethodName)
						.secondaryValue(updateMethod.getId())
						.build()
				);
			}

			updateMethodPreference.setCaption(context.getString(R.string.settings_explanation_incremental_full_update));
			updateMethodPreference.setItemList(itemList);

			// If there's there no update method saved in preferences, auto select the last recommended method
			if (selectedPosition == -1) {
				if (!recommendedPositions.isEmpty()) {
					updateMethodPreference.setValueIndex(recommendedPositions.get(recommendedPositions.size() - 1));
				} else {
					updateMethodPreference.setValueIndex(updateMethods.size() - 1);
				}
			}

			updateMethodPreference.setOnPreferenceChangeListener((preference, value) -> {
				Crashlytics.setUserIdentifier("Device: " + settingsManager.getPreference(context.getString(R.string.key_device), "<UNKNOWN>")
						+ ", Update Method: " + settingsManager.getPreference(context.getString(R.string.key_update_method), "<UNKNOWN>"));

				// Google Play services are not required if the user doesn't use notifications
				if (application.checkPlayServices(activity.getParent(), false)) {
					// Subscribe to notifications for the newly selected device and update method
					NotificationTopicSubscriber.subscribe(application);
				} else {
					Toast.makeText(context, getString(R.string.notification_no_notification_support), LENGTH_LONG).show();
				}

				return true;
			});
		} else {
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
		preference.setSummary(context.getString(R.string.processing));

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
		Preference buyAdFree = findPreference(context.getString(R.string.key_ad_free));

		switch (status) {
			case UNAVAILABLE:
				//noinspection ConstantConditions
				buyAdFree.setEnabled(false);
				buyAdFree.setSummary(context.getString(R.string.settings_buy_button_not_possible));
				buyAdFree.setOnPreferenceClickListener(null);
				break;
			case AVAILABLE:
				//noinspection ConstantConditions
				buyAdFree.setEnabled(true);
				buyAdFree.setSummary(context.getString(R.string.settings_buy_button_buy, adFreePrice));
				buyAdFree.setOnPreferenceClickListener(preference -> {
					onBuyAdFreePreferenceClicked(preference);
					return true;
				});
				break;
			case ALREADY_BOUGHT:
				//noinspection ConstantConditions
				buyAdFree.setEnabled(false);
				buyAdFree.setSummary(context.getString(R.string.settings_buy_button_bought));
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
