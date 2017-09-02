package com.arjanvlek.oxygenupdater.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.domain.UpdateMethod;
import com.arjanvlek.oxygenupdater.internal.ThreeTuple;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseType;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabHelper;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabResult;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK1;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK2;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.Purchase;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.SkuDetails;
import com.arjanvlek.oxygenupdater.views.AbstractActivity;
import com.arjanvlek.oxygenupdater.views.CustomDropdown;

import org.joda.time.LocalDateTime;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import java8.util.function.Consumer;
import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_AD_FREE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_NEWS_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPLOAD_LOGS;

@SuppressLint("HardwareIds")
public class SettingsActivity extends AbstractActivity {
    private ProgressBar progressBar;
    private ProgressBar deviceProgressBar;
    private ProgressBar updateMethodsProgressBar;
    private SettingsManager settingsManager;

    private IabHelper iabHelper;
    public static final String SKU_AD_FREE = "oxygen_updater_ad_free";
    private static final int IAB_REQUEST_CODE = 1995;
    private static final String TAG = "SettingsActivity";
    private String price = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsManager = new SettingsManager(getApplicationContext());
        progressBar = (ProgressBar) findViewById(R.id.settingsProgressBar);
        deviceProgressBar = (ProgressBar) findViewById(R.id.settingsDeviceProgressBar);
        updateMethodsProgressBar = (ProgressBar) findViewById(R.id.settingsUpdateMethodProgressBar);

        progressBar.setVisibility(View.VISIBLE);
        deviceProgressBar.setVisibility(View.VISIBLE);

        getApplicationData().getServerConnector().getDevices(true, this::fillDeviceSettings);

        initSwitches();

        iabHelper = new IabHelper(this, PK1.A + "/" + PK2.B);
        iabHelper.enableDebugLogging(BuildConfig.DEBUG);
        setupIabHelper(iabHelper);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (iabHelper != null) iabHelper.disposeWhenFinished();
        iabHelper = null;
    }

    private void initSwitches() {
        List<ThreeTuple<Integer, String, Boolean>> switchesAndSettingsItemsAndDefaultValues = Arrays.asList(
                ThreeTuple.create(R.id.settingsAppUpdatesSwitch, PROPERTY_SHOW_APP_UPDATE_MESSAGES, true),
                ThreeTuple.create(R.id.settingsAppMessagesSwitch, PROPERTY_SHOW_NEWS_MESSAGES, true),
                ThreeTuple.create(R.id.settingsImportantPushNotificationsSwitch, PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS, true),
                ThreeTuple.create(R.id.settingsNewVersionPushNotificationsSwitch, PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS, true),
                ThreeTuple.create(R.id.settingsNewDevicePushNotificationsSwitch, PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS, true),
                ThreeTuple.create(R.id.settingsNewsPushNotificationsSwitch, PROPERTY_RECEIVE_NEWS_NOTIFICATIONS, true),
                ThreeTuple.create(R.id.settingsSystemIsUpToDateSwitch, PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true),
                ThreeTuple.create(R.id.settingsUploadLogsSwitch, PROPERTY_UPLOAD_LOGS, true)
        );

        StreamSupport.stream(switchesAndSettingsItemsAndDefaultValues).forEach(tuple -> {
            SwitchCompat switchView = (SwitchCompat) findViewById(tuple.getFirst());
            switchView.setOnCheckedChangeListener(((buttonView, isChecked) -> settingsManager.savePreference(tuple.getSecond(), isChecked)));
            switchView.setChecked(settingsManager.getPreference(tuple.getSecond(), tuple.getThird()));
        });
    }

    private void fillDeviceSettings(final List<Device> devices) {
        if (devices != null && !devices.isEmpty()) {
            SystemVersionProperties systemVersionProperties = ((ApplicationData) getApplication()).getSystemVersionProperties();

            Spinner spinner = (Spinner) findViewById(R.id.settingsDeviceSpinner);

            if (spinner == null) return;

            // Set the spinner to the previously selected device.
            final int recommendedPosition = StreamSupport.stream(devices)
                    .filter(d -> d.getProductName() != null && d.getProductName().equals(systemVersionProperties.getOxygenDeviceName()))
                    .mapToInt(devices::indexOf).findAny().orElse(-1);

            final int selectedPosition = StreamSupport.stream(devices)
                    .filter(d -> d.getId() == settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L))
                    .mapToInt(devices::indexOf).findAny().orElse(-1);


            ArrayAdapter<Device> adapter = new ArrayAdapter<Device>(this, android.R.layout.simple_spinner_item, devices) {

                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, devices, recommendedPosition, this.getContext());
                }

                @Override
                public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                    return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, devices, recommendedPosition, this.getContext());
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinner.setAdapter(adapter);
            if (selectedPosition != -1) {
                spinner.setSelection(selectedPosition);
            }

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Device device = (Device) adapterView.getItemAtPosition(i);
                    settingsManager.savePreference(SettingsManager.PROPERTY_DEVICE, device.getName());
                    settingsManager.savePreference(SettingsManager.PROPERTY_DEVICE_ID, device.getId());

                    updateMethodsProgressBar.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);

                    getApplicationData().getServerConnector().getUpdateMethods(device.getId(), SettingsActivity.this::fillUpdateMethodSettings);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });


            deviceProgressBar.setVisibility(View.GONE);

        } else {
            hideDeviceAndUpdateMethodSettings();
            progressBar.setVisibility(View.GONE);

        }
    }

    private void fillUpdateMethodSettings(final List<UpdateMethod> updateMethods) {
        if (updateMethods != null && !updateMethods.isEmpty()) {
            Spinner spinner = (Spinner) findViewById(R.id.settingsUpdateMethodSpinner);
            if (spinner == null) return;

            long currentUpdateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L);

            final int[] recommendedPositions = StreamSupport.stream(updateMethods).filter(UpdateMethod::isRecommended).mapToInt(updateMethods::indexOf).toArray();
            final int selectedPosition = StreamSupport.stream(updateMethods).filter(um -> um.getId() == currentUpdateMethodId).mapToInt(updateMethods::indexOf).findAny().orElse(-1);

            ArrayAdapter<UpdateMethod> adapter = new ArrayAdapter<UpdateMethod>(this, android.R.layout.simple_spinner_item, updateMethods) {

                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, updateMethods, recommendedPositions, this.getContext());
                }

                @Override
                public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, updateMethods, recommendedPositions, this.getContext());
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinner.setAdapter(adapter);

            if (selectedPosition != -1) {
                spinner.setSelection(selectedPosition);
            }

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    UpdateMethod updateMethod = (UpdateMethod) adapterView.getItemAtPosition(i);

                    settingsManager.savePreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, updateMethod.getId());
                    settingsManager.savePreference(SettingsManager.PROPERTY_UPDATE_METHOD, updateMethod.getEnglishName());

                    // Google Play services are not required if the user doesn't notifications
                    if (getApplicationData().checkPlayServices(getParent(), false)) {
                        // Subscribe to notifications for the newly selected device and update method
                        NotificationTopicSubscriber.subscribe(getApplicationData());
                    } else {
                        Toast.makeText(getApplication().getApplicationContext(), getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show();
                    }

                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            updateMethodsProgressBar.setVisibility(View.GONE);
        } else {
            hideUpdateMethodSettings();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void hideDeviceAndUpdateMethodSettings() {
        findViewById(R.id.settingsDeviceSpinner).setVisibility(View.GONE);
        findViewById(R.id.settingsDeviceProgressBar).setVisibility(View.GONE);
        findViewById(R.id.settingsDeviceView).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodProgressBar).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodSpinner).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodView).setVisibility(View.GONE);
        findViewById(R.id.settingsDescriptionView).setVisibility(View.GONE);
        findViewById(R.id.settingsUpperDivisor).setVisibility(View.GONE);
    }

    private void hideUpdateMethodSettings() {
        findViewById(R.id.settingsUpdateMethodProgressBar).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodSpinner).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodView).setVisibility(View.GONE);
        findViewById(R.id.settingsDescriptionView).setVisibility(View.GONE);
        findViewById(R.id.settingsUpperDivisor).setVisibility(View.GONE);
    }

    private void showSettingsWarning() {
        Long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L);
        Long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);

        if (deviceId == -1L || updateMethodId == -1L) {
            Logger.logWarning(TAG, "Settings screen did *NOT* save settings correctly. Selected device id: " + deviceId + ", selected update method id: " + updateMethodId);
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show();
        } else {
            Logger.logWarning(false, TAG, "Settings screen did *NOT* save settings correctly. Selected device id: " + deviceId + ", selected update method id: " + updateMethodId);
            Toast.makeText(this, getString(R.string.settings_saving), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onBackPressed() {
        if (settingsManager.checkIfSetupScreenHasBeenCompleted() && !progressBar.isShown()) {
            NavUtils.navigateUpFromSameTask(this);
        } else {
            showSettingsWarning();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (settingsManager.checkIfSetupScreenHasBeenCompleted() && !progressBar.isShown()) {
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
     * Initialize the In-App Billing helper (IabHelper), query for pruchasable products and set the state of the Purchase ad free button accordingly to the purchase information.
     *
     * @param iabHelper In-App Billing helper which has been setup and is ready to accept purchases from the user.
     */
    private void setupIabHelper(IabHelper iabHelper) {
        Logger.logDebug(TAG, "IAB: start setup of IAB");
        // Set up the helper. Once it is done, it will call the embedded listener with its setupResult.
        iabHelper.startSetup(setupResult -> {
            // Setup error? we can't do anything else but stop here. Purchasing ad-free will be unavailable.
            if (!setupResult.isSuccess()) {
                logIABError("Failed to set up in-app billing", setupResult);
                return;
            }

            Logger.logDebug(TAG, "IAB: Setup complete");

            // Have we been disposed of in the meantime? If so, quit.
            if (iabHelper == null) return;

            Logger.logDebug(TAG, "IAB: Start querying inventory");

            // Query the billing inventory to get details about the in-app-billing item (such as the price in the right currency).
            queryInAppBillingInventory((queryResult, inventory) -> {
                Logger.logDebug(TAG, "IAB: Queried inventory");

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

                Logger.logDebug(TAG, "IAB: Found product. Checking purchased state...");

                // Check if the user has purchased the item. If so, grant ad-free and set the button to "Purchased". If not, remove ad-free and set the button to the right price.
                if (inventory.hasPurchase(SKU_AD_FREE)) {
                    Logger.logDebug(TAG, "IAB: Product has already been purchased");
                    settingsManager.savePreference(PROPERTY_AD_FREE, true);
                    setupBuyAdFreeButton(PurchaseStatus.ALREADY_BOUGHT);
                } else {
                    Logger.logDebug(TAG, "IAB: Product has not yet been purchased");
                    settingsManager.savePreference(PROPERTY_AD_FREE, false); // Safe, because we can guarantee that the device is online and that the purchase check has succeeded.
                    this.price = productDetails.getPrice();
                    setupBuyAdFreeButton(PurchaseStatus.AVAILABLE, productDetails.getPrice());
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

    /**
     * Handler which is called when the Buy button is clicked. Starts the purchase process or initializes a new IabHelper if the current one was disposed early.
     *
     * @param v Button which was clicked.
     */
    private void onPurchaseAdFreeButtonClick(View v) {
        // Disable the Purchase button and set its text to "Processing...".
        v.setEnabled(false);
        if (v instanceof Button) {
            ((Button) v).setText(getString(R.string.processing));
        }

        if (iabHelper != null) {
            doPurchaseAdFree();
        } else {
            Logger.logInfo(TAG, "IAB purchase helper was disposed early. Initiating new instance...");
            iabHelper = new IabHelper(this, PK1.A + "/" + PK2.B);
            iabHelper.enableDebugLogging(BuildConfig.DEBUG);
            iabHelper.startSetup(setupResult -> {
                if (!setupResult.isSuccess()) {
                    logIABError("Purchase of the ad-free version failed due to an unknown error BEFORE the purchase screen was opened", setupResult);
                    Toast.makeText(getApplication(), getString(R.string.purchase_error_before_payment), Toast.LENGTH_LONG).show();
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
            Logger.logDebug(TAG, "IAB: Start purchase flow");

            String developerPayload = "OxygenUpdater-AdFree-" + (!Build.SERIAL.equals("unknown") ? Build.SERIAL + "-" : "") + LocalDateTime.now().toString("yyyy-MM-dd HH:mm:ss");

            // Open the purchase window.
            iabHelper.launchPurchaseFlow(this, SKU_AD_FREE, IAB_REQUEST_CODE, (result, purchase) -> {
                Logger.logDebug(TAG, "IAB: Purchase dialog closed. Result: " + result.toString() + (purchase != null ? ", purchase: " + purchase.toString() : ""));

                // If the purchase failed, but the user did not cancel it, notify the user and log an error. Otherwise, do nothing.
                if (result.isFailure()) {
                    if (result.getResponse() != IabHelper.IABHELPER_USER_CANCELLED) {
                        logIABError("Purchase of the ad-free version failed due to an unknown error DURING the purchase flow", result);
                        Toast.makeText(getApplication(), getString(R.string.purchase_error_after_payment), Toast.LENGTH_LONG).show();
                    } else {
                        Logger.logDebug(TAG, "Purchase of ad-free version was cancelled by the user.");
                        setupBuyAdFreeButton(PurchaseStatus.AVAILABLE, this.price);
                    }
                    return;
                }

                // if the result is successful and contains purchase data, verify the purchase details on the server and grant the ad-free package to the user.
                if (result.isSuccess() && purchase != null) {
                    if (purchase.getSku().equals(SKU_AD_FREE)) {
                        validateAdFreePurchase(purchase, valid -> {
                            if (valid) {
                                setupBuyAdFreeButton(PurchaseStatus.ALREADY_BOUGHT);
                                settingsManager.savePreference(PROPERTY_AD_FREE, true);
                            } else {
                                setupBuyAdFreeButton(PurchaseStatus.AVAILABLE, this.price);
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

    private void logIABError(String errorMessage, IabResult result) {
        Logger.logError(TAG, errorMessage + ". IAB State: {" + result.toString() + "}");
        setupBuyAdFreeButton(PurchaseStatus.UNAVAILABLE);
    }

    /**
     * See setupBuyAdFreebutton(status, price)
     *
     * @param status Purchased status
     */
    private void setupBuyAdFreeButton(PurchaseStatus status) {
        setupBuyAdFreeButton(status, null);
    }

    /**
     * Set the text and enable / disable the Buy button depending on the purchased status
     *
     * @param status      Purchased status
     * @param adFreePrice Price to display on the button if the product can be bought.
     */
    private void setupBuyAdFreeButton(PurchaseStatus status, @Nullable String adFreePrice) {
        Button buyAdFreeButton = (Button) findViewById(R.id.settingsBuyAdFreeButton);

        switch (status) {
            case UNAVAILABLE:
                buyAdFreeButton.setEnabled(false);
                buyAdFreeButton.setText(getString(R.string.settings_buy_button_not_possible));
                buyAdFreeButton.setOnClickListener((v) -> {
                });
                break;
            case AVAILABLE:
                buyAdFreeButton.setEnabled(true);
                buyAdFreeButton.setText(getString(R.string.settings_buy_button_buy, adFreePrice));
                buyAdFreeButton.setOnClickListener(this::onPurchaseAdFreeButtonClick);
                break;
            case ALREADY_BOUGHT:
                buyAdFreeButton.setEnabled(false);
                buyAdFreeButton.setText(getString(R.string.settings_buy_button_bought));
                buyAdFreeButton.setOnClickListener((v) -> {
                });
                break;
            default:
                throw new IllegalStateException("ShowBuyAdFreeButton: Invalid PurchaseStatus " + status.toString() + "!");
        }
    }

    /**
     * Validate the in app purchase on the app's server.
     *
     * @param purchase Purchase which must be validated
     * @param callback Whether or not the purchase was valid. Contains function to handle after purchase validation.
     */
    private void validateAdFreePurchase(Purchase purchase, Consumer<Boolean> callback) {
        String expectedPayload = "OxygenUpdater-AdFree-" + (!Build.SERIAL.equals("unknown") ? Build.SERIAL + "-" : "");

        if (!purchase.getDeveloperPayload().startsWith(expectedPayload)) {
            Logger.logError(TAG, "Purchase of the ad-free version failed. The returned developer payload was incorrect (" + purchase.getDeveloperPayload() + ")");
            callback.accept(false);
        }

        getApplicationData().getServerConnector().verifyPurchase(purchase, PurchaseType.AD_FREE, (validationResult) -> {
            if (validationResult == null) {
                // server can't be reached. Keep trying until it can be reached...
                new Handler().postDelayed(() -> validateAdFreePurchase(purchase, callback), 2000);
            } else if (validationResult.isSuccess()) {
                callback.accept(true);
            } else {
                Logger.logError(TAG, "Purchase of the ad-free version failed. Failed to verify purchase on the server. Error message: " + validationResult.getErrorMessage());
                callback.accept(false);
            }
        });
    }

}
