package com.arjanvlek.oxygenupdater.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.core.app.NavUtils;
import androidx.preference.PreferenceManager;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.SetupUtils;
import com.arjanvlek.oxygenupdater.settings.SettingsFragment.InAppPurchaseDelegate;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseType;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.GooglePlayBillingException;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabHelper;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabResult;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK1;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK2;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.Purchase;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.SkuDetails;
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity;

import org.joda.time.LocalDateTime;

import java.util.Collections;

import java8.util.function.Consumer;

import static android.widget.Toast.LENGTH_LONG;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logInfo;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_AD_FREE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;
import static com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus.ALREADY_BOUGHT;
import static com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus.AVAILABLE;
import static com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus.UNAVAILABLE;

/**
 * @author Adhiraj Singh Chauhan (gjthub.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
public class SettingsActivity extends SupportActionBarActivity implements InAppPurchaseDelegate {
	public static final String SKU_AD_FREE = "oxygen_updater_ad_free";
	private static final int IAB_REQUEST_CODE = 1995;
	private static final String TAG = "SettingsActivity";

	private SettingsManager settingsManager;
	private SettingsFragment settingsFragment;
	private IabHelper iabHelper;

	private String price = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		settingsFragment = new SettingsFragment();
		settingsFragment.setInAppPurchaseDelegate(this);

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.settings_container, settingsFragment, "Settings")
				.commit();

		settingsManager = new SettingsManager(getApplicationContext());

		iabHelper = new IabHelper(this, PK1.A + "/" + PK2.B);
		iabHelper.enableDebugLogging(BuildConfig.DEBUG);
		setupIabHelper(iabHelper);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			if (iabHelper != null) {
				iabHelper.disposeWhenFinished();
			}
			iabHelper = null;
		} catch (Throwable ignored) {

		}
	}

	private void showSettingsWarning() {
		long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L);
		long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);

		if (deviceId == -1L || updateMethodId == -1L) {
			logWarning(TAG, SetupUtils.getAsError("Settings screen", deviceId, updateMethodId));
			Toast.makeText(this, getString(R.string.settings_entered_incorrectly), LENGTH_LONG).show();
		} else {
			Toast.makeText(this, getString(R.string.settings_saving), LENGTH_LONG).show();
		}
	}

	@Override
	public void onBackPressed() {
		if (settingsManager.checkIfSetupScreenHasBeenCompleted()) {
			NavUtils.navigateUpFromSameTask(this);
		} else {
			showSettingsWarning();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Respond to the action bar's Up/Home button
		if (item.getItemId() == android.R.id.home) {
			if (settingsManager.checkIfSetupScreenHasBeenCompleted()) {
				NavUtils.navigateUpFromSameTask(this);
				return true;
			} else {
				showSettingsWarning();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	/* IN APP BILLING (AD FREE PURCHASING) METHODS */

	/**
	 * Initialize the In-App Billing helper (IabHelper), query for purchasable products and set the
	 * state of the Purchase ad free button accordingly to the purchase information
	 *
	 * @param iabHelper In-App Billing helper which has been setup and is ready to accept purchases from the user
	 */
	private void setupIabHelper(IabHelper iabHelper) {
		logDebug(TAG, "IAB: start setup of IAB");

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		// Set up the helper. Once it is done, it will call the embedded listener with its setupResult.
		iabHelper.startSetup(setupResult -> {
			// Setup error? we can't do anything else but stop here. Purchasing ad-free will be unavailable.
			if (!setupResult.isSuccess()) {
				logIABError("Failed to set up in-app billing", setupResult);
				return;
			}

			logDebug(TAG, "IAB: Setup complete");

			// Have we been disposed of in the meantime? If so, quit.
			if (iabHelper == null) {
				return;
			}

			logDebug(TAG, "IAB: Start querying inventory");

			// Query the billing inventory to get details about the in-app-billing item (such as the price in the right currency).
			queryInAppBillingInventory((queryResult, inventory) -> {
				logDebug(TAG, "IAB: Queried inventory");

				// Querying failed? Then the user can't buy anything as we can't determine whether or not the user already bought the item.
				if (queryResult.isFailure()) {
					logIABError("Failed to obtain in-app billing product list", queryResult);
					return;
				}

				SkuDetails productDetails = inventory.getSkuDetails(SKU_AD_FREE);

				// If the product details are not found (unlikely to happen, but possible), stop.
				if (productDetails == null || !productDetails.getSku().equals(SKU_AD_FREE)) {
					logIABError("In-app billing product " + SKU_AD_FREE + " is not available", queryResult);
					return;
				}

				logDebug(TAG, "IAB: Found product. Checking purchased state...");

				// Check if the user has purchased the item. If so, grant ad-free and set the button to "Purchased". If not, remove ad-free and set the button to the right price.
				if (inventory.hasPurchase(SKU_AD_FREE)) {
					logDebug(TAG, "IAB: Product has already been purchased");
					sharedPreferences.edit()
							.putBoolean(PROPERTY_AD_FREE, true)
							.apply();

					settingsFragment.setupBuyAdFreePreference(ALREADY_BOUGHT);
				} else {
					logDebug(TAG, "IAB: Product has not yet been purchased");

					// Save, because we can guarantee that the device is online and that the purchase check has succeeded.
					sharedPreferences.edit()
							.putBoolean(PROPERTY_AD_FREE, false)
							.apply();

					price = productDetails.getPrice();
					settingsFragment.setupBuyAdFreePreference(AVAILABLE, productDetails.getPrice());
				}
			});
		});
	}

	/**
	 * Queries the IAB inventory and retries if an operation is already in progress.
	 *
	 * @param queryInventoryFinishedListener Listener to execute once the query operation has finished.
	 */
	private void queryInAppBillingInventory(IabHelper.QueryInventoryFinishedListener queryInventoryFinishedListener) {
		try {
			iabHelper.queryInventoryAsync(true, Collections.singletonList(SKU_AD_FREE), null, queryInventoryFinishedListener);
		} catch (IabHelper.IabAsyncInProgressException e) {
			new Handler().postDelayed(() -> queryInAppBillingInventory(queryInventoryFinishedListener), 3000);
		}
	}

	private void logIABError(String errorMessage, IabResult result) {
		logError(TAG, new GooglePlayBillingException("IAB Error: {" + errorMessage + "}. IAB State: {" + result.toString() + "}"));
		settingsFragment.setupBuyAdFreePreference(UNAVAILABLE);
	}

	/**
	 * Validate the in app purchase on the app's server
	 *
	 * @param purchase Purchase which must be validated
	 * @param callback Whether or not the purchase was valid. Contains function to handle after purchase validation.
	 */
	private void validateAdFreePurchase(Purchase purchase, Consumer<Boolean> callback) {
		@SuppressLint("HardwareIds")
		String expectedPayload = "OxygenUpdater-AdFree-" + (!Build.SERIAL.equals("unknown") ? Build.SERIAL + "-" : "");

		if (!purchase.getDeveloperPayload().startsWith(expectedPayload)) {
			logError(TAG, new GooglePlayBillingException("Purchase of the ad-free version failed. The returned developer payload was incorrect ("
					+ purchase.getDeveloperPayload() + ")"));
			callback.accept(false);
		}

		getApplicationData().getServerConnector()
				.verifyPurchase(purchase, price, PurchaseType.AD_FREE, validationResult -> {
					if (validationResult == null) {
						// server can't be reached. Keep trying until it can be reached...
						new Handler().postDelayed(() -> validateAdFreePurchase(purchase, callback), 2000);
					} else if (validationResult.isSuccess()) {
						callback.accept(true);
					} else {
						logError(TAG, new GooglePlayBillingException("Purchase of the ad-free version failed. Failed to verify purchase on the server. Error message: "
								+ validationResult.getErrorMessage()));
						callback.accept(false);
					}
				});
	}

	@Override
	public void performInAppPurchase() {
		if (iabHelper != null) {
			doPurchaseAdFree();
		} else {
			logInfo(TAG, "IAB purchase helper was disposed early. Initiating new instance...");
			iabHelper = new IabHelper(this, PK1.A + "/" + PK2.B);
			iabHelper.enableDebugLogging(BuildConfig.DEBUG);
			iabHelper.startSetup(setupResult -> {
				if (!setupResult.isSuccess()) {
					logIABError("Purchase of the ad-free version failed due to an unknown error BEFORE the purchase screen was opened", setupResult);
					Toast.makeText(this, getString(R.string.purchase_error_before_payment), LENGTH_LONG).show();
					return;
				}

				doPurchaseAdFree();
			});
		}
	}

	/**
	 * Start the purchase process.
	 */
	public void doPurchaseAdFree() {
		assert iabHelper != null;

		try {
			logDebug(TAG, "IAB: Start purchase flow");

			@SuppressLint("HardwareIds")
			String developerPayload = "OxygenUpdater-AdFree-"
					+ (!Build.SERIAL.equals("unknown") ? Build.SERIAL + "-" : "")
					+ LocalDateTime.now().toString("yyyy-MM-dd HH:mm:ss");

			// Open the purchase window.
			iabHelper.launchPurchaseFlow(this, SKU_AD_FREE, IAB_REQUEST_CODE, (result, purchase) -> {
				logDebug(TAG, "IAB: Purchase dialog closed. Result: " + result.toString() + (purchase != null ? ", purchase: " + purchase.toString() : ""));

				// If the purchase failed, but the user did not cancel it, notify the user and log an error. Otherwise, do nothing.
				if (result.isFailure()) {
					if (result.getResponse() != IabHelper.IABHELPER_USER_CANCELLED) {
						logIABError("Purchase of the ad-free version failed due to an unknown error DURING the purchase flow", result);
						Toast.makeText(getApplication(), getString(R.string.purchase_error_after_payment), LENGTH_LONG).show();
					} else {
						logDebug(TAG, "Purchase of ad-free version was cancelled by the user.");
						settingsFragment.setupBuyAdFreePreference(PurchaseStatus.AVAILABLE, price);
					}
					return;
				}

				// if the result is successful and contains purchase data, verify the purchase details on the server and grant the ad-free package to the user.
				if (result.isSuccess() && purchase != null) {
					if (purchase.getSku().equals(SKU_AD_FREE)) {
						validateAdFreePurchase(purchase, valid -> {
							if (valid) {
								settingsFragment.setupBuyAdFreePreference(PurchaseStatus.ALREADY_BOUGHT);
								settingsManager.savePreference(PROPERTY_AD_FREE, true);
							} else {
								settingsFragment.setupBuyAdFreePreference(PurchaseStatus.AVAILABLE, price);
							}
						});
					} else {
						logIABError("Another product than expected was bought. (" + purchase.toString() + ")", result);
					}
				}
			}, developerPayload);
		} catch (IabHelper.IabAsyncInProgressException e) {
			// If the purchase window can't be opened because an operation is in progress, try opening it again in a second (repeated until it can be opened).
			new Handler().postDelayed(this::doPurchaseAdFree, 1000);
		}
	}

	/**
	 * Called when the purchase window has been closed.
	 *
	 * @param requestCode Intent request
	 * @param resultCode  Intent result
	 * @param data        Intent data
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// If the activity result can't be processed by IabHelper (because it is for something else or the IabHelper is null), let the parent activity process it.
		if (iabHelper == null || !iabHelper.handleActivityResult(requestCode, resultCode, data)) {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
}
