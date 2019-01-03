package com.arjanvlek.oxygenupdater.updateinformation;


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.ActivityLauncher;
import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.download.DownloadHelper;
import com.arjanvlek.oxygenupdater.download.DownloadProgressData;
import com.arjanvlek.oxygenupdater.download.DownloadReceiver;
import com.arjanvlek.oxygenupdater.download.DownloadService;
import com.arjanvlek.oxygenupdater.download.UpdateDownloadListener;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.notifications.Dialogs;
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabHelper;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK1;
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK2;
import com.arjanvlek.oxygenupdater.versionformatter.UpdateDataVersionFormatter;
import com.arjanvlek.oxygenupdater.views.AbstractFragment;
import com.arjanvlek.oxygenupdater.views.MainActivity;
import com.google.android.gms.ads.AdView;

import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java8.util.Objects;
import java8.util.function.Consumer;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.RelativeLayout.ABOVE;
import static android.widget.RelativeLayout.BELOW;
import static com.arjanvlek.oxygenupdater.ApplicationData.APP_OUTDATED_ERROR;
import static com.arjanvlek.oxygenupdater.ApplicationData.NETWORK_CONNECTION_ERROR;
import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.ApplicationData.SERVER_MAINTENANCE_ERROR;
import static com.arjanvlek.oxygenupdater.settings.SettingsActivity.SKU_AD_FREE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_AD_FREE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_DOWNLOAD_URL;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_FILE_NAME;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_IS_UP_TO_DATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_CHECKED_DATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;
import static java8.util.stream.StreamSupport.stream;

public class UpdateInformationFragment extends AbstractFragment {


    private SwipeRefreshLayout updateInformationRefreshLayout;
    private SwipeRefreshLayout systemIsUpToDateRefreshLayout;
    private RelativeLayout rootView;
    private AdView adView;

    private Context context;
    private SettingsManager settingsManager;
    private UpdateDownloadListener downloadListener;

    private boolean isLoadedOnce;
    private boolean adsAreSupported = false;

    // In app message bar collections and identifiers.
    public static final String KEY_HAS_DOWNLOAD_ERROR = "has_download_error";
    public static final String KEY_DOWNLOAD_ERROR_TITLE = "download_error_title";
    public static final String KEY_DOWNLOAD_ERROR_MESSAGE = "download_error_message";
    public static final String KEY_DOWNLOAD_ERROR_RESUMABLE = "download_error_resumable";
    private List<ServerMessageBar> serverMessageBars = new ArrayList<>();
    private DownloadReceiver downloadReceiver;

    /*
      -------------- ANDROID ACTIVITY LIFECYCLE METHODS -------------------
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.context = getApplicationData();
        this.settingsManager = new SettingsManager(getApplicationData());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_updateinformation, container, false);
        this.adView = rootView.findViewById(R.id.updateInformationAdView);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(isAdded() && settingsManager.checkIfSetupScreenHasBeenCompleted()) {
            updateInformationRefreshLayout = rootView.findViewById(R.id.updateInformationRefreshLayout);
            systemIsUpToDateRefreshLayout = rootView.findViewById(R.id.updateInformationSystemIsUpToDateRefreshLayout);

            updateInformationRefreshLayout.setOnRefreshListener(() -> load(this.adsAreSupported));
            updateInformationRefreshLayout.setColorSchemeResources(R.color.oneplus_red, R.color.holo_orange_light, R.color.holo_red_light);

            systemIsUpToDateRefreshLayout.setOnRefreshListener(() -> load(this.adsAreSupported));
            systemIsUpToDateRefreshLayout.setColorSchemeResources(R.color.oneplus_red, R.color.holo_orange_light, R.color.holo_red_light);

            checkAdSupportStatus((adsAreSupported) -> {
                this.adsAreSupported = adsAreSupported;

                load(adsAreSupported);

                if (adsAreSupported) {
                    showAds();
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (downloadReceiver != null) unregisterDownloadReceiver();
        if (adView != null) adView.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLoadedOnce) registerDownloadReceiver(this.downloadListener);
        if (adView != null) adView.resume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (downloadReceiver != null) unregisterDownloadReceiver();
        if (getActivity() != null && isDownloadServiceRunning()) getActivity().stopService(new Intent(getContext(), DownloadService.class));
        if (adView != null) adView.destroy();
    }

    /*
      -------------- INITIALIZATION / DATA FETCHING METHODS -------------------
     */

    private void checkAdSupportStatus(Consumer<Boolean> callback) {
        IabHelper helper = new IabHelper(getActivity(), PK1.A + "/" + PK2.B);

        helper.startSetup(setupResult -> {
            if (!setupResult.isSuccess()) {
                // Failed to setup IAB, so we might be offline or the device does not support IAB. Return the last stored value of the ad-free status.
                callback.accept(!settingsManager.getPreference(PROPERTY_AD_FREE, false));
                return;
            }

            try {
                helper.queryInventoryAsync(true, Collections.singletonList(SKU_AD_FREE), null, (queryResult, inventory) -> {
                    if (!queryResult.isSuccess()) {
                        // Failed to check inventory, so we might be offline. Return the last stored value of the ad-free status.
                        callback.accept(!settingsManager.getPreference(PROPERTY_AD_FREE, false));
                        return;
                    }

                    if (queryResult.isSuccess()) {
                        if (inventory.hasPurchase(SKU_AD_FREE)) {
                            // User has bought the upgrade. Save this to the app's settings and return that ads may not be shown.
                            settingsManager.savePreference(PROPERTY_AD_FREE, true);
                            callback.accept(false);
                        } else {
                            // User has not bought the item and we're online, so ads are definitely supported
                            callback.accept(true);
                        }
                    }
                });
            } catch (IabHelper.IabAsyncInProgressException e) {
                // A check is already in progress, so wait 3 secs and try to check again.
                new Handler().postDelayed(() -> checkAdSupportStatus(callback), 3000);
            }
        });
    }

    /**
     * Fetches all server data. This includes update information, server messages and server status checks
     */
    private void load(boolean adsAreSupported) {
        final AbstractFragment instance = this;

        long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L);
        long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);

        boolean online = Utils.checkNetworkConnection(getApplicationData());

        ServerConnector serverConnector = getApplicationData().getServerConnector();
        SystemVersionProperties systemVersionProperties = getApplicationData().getSystemVersionProperties();

        serverConnector.getUpdateData(online, deviceId, updateMethodId, systemVersionProperties.getOxygenOSOTAVersion(), (updateData) -> {
            if (!isLoadedOnce) {
                downloadListener = buildDownloadListener(updateData);
                registerDownloadReceiver(downloadListener);
                DownloadService.performOperation(getActivity(), DownloadService.ACTION_GET_INITIAL_STATUS, updateData);
            }

            // If the activity is started with a download error (when clicked on a "download failed" notification), show it to the user.
            if (!isLoadedOnce && getActivity() != null && getActivity().getIntent() != null && getActivity().getIntent().getBooleanExtra(KEY_HAS_DOWNLOAD_ERROR, false)) {
                Intent i = getActivity().getIntent();
                Dialogs.showDownloadError(instance, updateData, i.getBooleanExtra(KEY_DOWNLOAD_ERROR_RESUMABLE, false), i.getStringExtra(KEY_DOWNLOAD_ERROR_TITLE), i.getStringExtra(KEY_DOWNLOAD_ERROR_MESSAGE));
            }

            displayUpdateInformation(updateData, online, false);

            isLoadedOnce = true;

        }, (error) -> {
            if(error.equals(NETWORK_CONNECTION_ERROR)) {
                Dialogs.showNoNetworkConnectionError(instance);
            }
        });

        serverConnector.getInAppMessages(online, (banners -> this.displayServerMessageBars(banners, adsAreSupported)), (error) -> {
            switch (error) {
                case SERVER_MAINTENANCE_ERROR:
                    Dialogs.showServerMaintenanceError(instance);
                    break;
                case APP_OUTDATED_ERROR:
                    Dialogs.showAppOutdatedError(instance);
                    break;
            }
        });
    }


    /*
      -------------- METHODS FOR DISPLAYING DATA ON THE FRAGMENT -------------------
     */


    private void addServerMessageBar(ServerMessageBar view) {
        // Add the message to the update information screen.
        // Set the layout params based on the view count.
        // First view should go below the app update message bar (if visible)
        // Consecutive views should go below their parent / previous view.
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);

        int numberOfBars = this.serverMessageBars.size();

        params.topMargin = numberOfBars * Utils.diPToPixels(getActivity(), 20);
        view.setId((numberOfBars * 20000 + 1));
        this.rootView.addView(view, params);
        this.serverMessageBars.add(view);
    }

    private void deleteAllServerMessageBars() {
        stream(this.serverMessageBars).filter(Objects::nonNull).forEach(v -> this.rootView.removeView(v));

        this.serverMessageBars = new ArrayList<>();
    }

    private void displayServerMessageBars(List<Banner> banners, boolean adsAreSupported) {

        if(!isAdded()) return;
        deleteAllServerMessageBars();

        List<ServerMessageBar> createdServerMessageBars = new ArrayList<>();

        for(Banner banner : banners) {
            ServerMessageBar bar = new ServerMessageBar(getActivity());
            View backgroundBar = bar.getBackgroundBar();
            TextView textView = bar.getTextView();

            backgroundBar.setBackgroundColor(banner.getColor(context));
            textView.setText(banner.getBannerText(context));

            if(banner.getBannerText(context) instanceof Spanned) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }

            addServerMessageBar(bar);
            createdServerMessageBars.add(bar);
        }

        // Set the margins of the app ui to be below the last added server message bar.
        if (!createdServerMessageBars.isEmpty()) {
            View lastServerMessageView = createdServerMessageBars.get(createdServerMessageBars.size() - 1);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.addRule(BELOW, lastServerMessageView.getId());
            if (adsAreSupported) params.addRule(ABOVE, adView.getId());

            systemIsUpToDateRefreshLayout.setLayoutParams(params);
            updateInformationRefreshLayout.setLayoutParams(params);

        }
        this.serverMessageBars = createdServerMessageBars;
    }

    /**
     * Displays the update information from a {@link UpdateData} with update information.
     * @param updateData Update information to display
     * @param online Whether or not the device has an active network connection
     * @param displayInfoWhenUpToDate Flag set to show update information anyway, even if the system is up to date.
     */
    private void displayUpdateInformation(final UpdateData updateData, final boolean online, boolean displayInfoWhenUpToDate) {
        // Abort if no update data is found or if the fragment is not attached to its activity to prevent crashes.
        if (!isAdded() || updateData == null) {
            return;
        }

        // Hide the loading screen
        rootView.findViewById(R.id.updateInformationLoadingScreen).setVisibility(GONE);

        if (updateData.getId() == null) {
            displayUpdateInformationWhenUpToDate(updateData, online);
        }

        if (((updateData.isSystemIsUpToDateCheck(settingsManager)) && !displayInfoWhenUpToDate) || !updateData.isUpdateInformationAvailable()) {
            displayUpdateInformationWhenUpToDate(updateData, online);
        } else {
            displayUpdateInformationWhenNotUpToDate(updateData, displayInfoWhenUpToDate);
        }

        if(online) {
            // Save update data for offline viewing
            settingsManager.savePreference(PROPERTY_OFFLINE_ID, updateData.getId());
            settingsManager.savePreference(PROPERTY_OFFLINE_UPDATE_NAME, updateData.getVersionNumber());
            settingsManager.savePreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, updateData.getDownloadSizeInMegabytes());
            settingsManager.savePreference(PROPERTY_OFFLINE_UPDATE_DESCRIPTION, updateData.getDescription());
            settingsManager.savePreference(PROPERTY_OFFLINE_FILE_NAME, updateData.getFilename());
            settingsManager.savePreference(PROPERTY_OFFLINE_DOWNLOAD_URL, updateData.getDownloadUrl());
            settingsManager.savePreference(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, updateData.isUpdateInformationAvailable());
            settingsManager.savePreference(PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString());
            settingsManager.savePreference(PROPERTY_OFFLINE_IS_UP_TO_DATE, updateData.isSystemIsUpToDate());
        }

        // Hide the refreshing icon if it is present.
        hideRefreshIcons();
    }

    private void displayUpdateInformationWhenUpToDate(final UpdateData updateData, boolean online) {
        if (getActivity() == null || getActivity().getApplication() == null) {
            return;
        }

        // Show "System is up to date" view.
        rootView.findViewById(R.id.updateInformationRefreshLayout).setVisibility(GONE);
        rootView.findViewById(R.id.updateInformationSystemIsUpToDateRefreshLayout).setVisibility(VISIBLE);

        // Set the current Oxygen OS version if available.
        String oxygenOSVersion = ((ApplicationData) getActivity().getApplication()).getSystemVersionProperties().getOxygenOSVersion();
        TextView versionNumberView = rootView.findViewById(R.id.updateInformationSystemIsUpToDateVersionTextView);
        if(!oxygenOSVersion.equals(NO_OXYGEN_OS)) {
            versionNumberView.setVisibility(VISIBLE);
            versionNumberView.setText(String.format(getString(R.string.update_information_oxygen_os_version), oxygenOSVersion));
        } else {
            versionNumberView.setVisibility(GONE);
        }

        // Set "No Update Information Is Available" button if needed.
        Button updateInformationButton = rootView.findViewById(R.id.updateInformationSystemIsUpToDateStatisticsButton);
        if (!updateData.isUpdateInformationAvailable()) {
            updateInformationButton.setText(getString(R.string.update_information_no_update_data_available));
            updateInformationButton.setClickable(false);
        } else {
            updateInformationButton.setText(getString(R.string.update_information_view_update_information));
            updateInformationButton.setClickable(true);
            updateInformationButton.setOnClickListener(v -> displayUpdateInformation(updateData, online, true));
        }

        // Save last time checked if online.
        if(online) {
            settingsManager.savePreference(PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString());
        }

        // Show last time checked.
        TextView dateCheckedView = rootView.findViewById(R.id.updateInformationSystemIsUpToDateDateTextView);
        dateCheckedView.setText(String.format(getString(R.string.update_information_last_checked_on), Utils.formatDateTime(context, settingsManager.getPreference(PROPERTY_UPDATE_CHECKED_DATE, null))));

    }

    private void displayUpdateInformationWhenNotUpToDate(final UpdateData updateData, boolean displayInfoWhenUpToDate) {
        // Show "System update available" view.
        rootView.findViewById(R.id.updateInformationRefreshLayout).setVisibility(VISIBLE);
        rootView.findViewById(R.id.updateInformationSystemIsUpToDateRefreshLayout).setVisibility(GONE);

        // Display available update version number.
        TextView buildNumberView = rootView.findViewById(R.id.updateInformationBuildNumberView);
        if (updateData.getVersionNumber() != null && !updateData.getVersionNumber().equals("null")) {
            if(UpdateDataVersionFormatter.canVersionInfoBeFormatted(updateData)) {
                buildNumberView.setText(UpdateDataVersionFormatter.getFormattedVersionNumber(updateData));
            } else {
                buildNumberView.setText(updateData.getVersionNumber());
            }
        } else {
            buildNumberView.setText(String.format(getString(R.string.update_information_unknown_update_name), settingsManager.getPreference(PROPERTY_DEVICE, context.getString(R.string.device_information_unknown))));
        }

        // Display download size.
        TextView downloadSizeView = rootView.findViewById(R.id.updateInformationDownloadSizeView);
        downloadSizeView.setText(String.format(getString(R.string.download_size_megabyte), updateData.getDownloadSizeInMegabytes()));

        // Display update description.
        String description = updateData.getDescription();
        TextView descriptionView = rootView.findViewById(R.id.updateDescriptionView);
        descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
        descriptionView.setText(description != null && !description.isEmpty() && !description.equals("null") ? UpdateDescriptionParser.parse(description) : getString(R.string.update_information_description_not_available));

        // Display update file name.
        TextView fileNameView = rootView.findViewById(R.id.updateFileNameView);
        fileNameView.setText(String.format(getString(R.string.update_information_file_name), updateData.getFilename()));


        // Format top title based on system version installed.
        TextView headerLabel = rootView.findViewById(R.id.headerLabel);
        Button updateInstallationGuideButton = rootView.findViewById(R.id.updateInstallationInstructionsButton);
        View downloadSizeTable = rootView.findViewById(R.id.buttonTable);
        View downloadSizeImage = rootView.findViewById(R.id.downloadSizeImage);

        final Button downloadButton = getDownloadButton();
        DownloadStatus status = getDownloadStatus(updateData);
        initUpdateDownloadButton(updateData, status);
        initInstallButton(updateData, status);

        if(displayInfoWhenUpToDate) {
            headerLabel.setText(getString(R.string.update_information_installed_update));
            downloadButton.setVisibility(GONE);
            updateInstallationGuideButton.setVisibility(GONE);
            fileNameView.setVisibility(GONE);
            downloadSizeTable.setVisibility(GONE);
            downloadSizeImage.setVisibility(GONE);
            downloadSizeView.setVisibility(GONE);
        } else {
            if (updateData.isSystemIsUpToDate()) {
                headerLabel.setText(getString(R.string.update_information_installed_update));
            } else {
                headerLabel.setText(getString(R.string.update_information_latest_available_update));
            }
            downloadButton.setVisibility(VISIBLE);
            fileNameView.setVisibility(VISIBLE);
            downloadSizeTable.setVisibility(VISIBLE);
            downloadSizeImage.setVisibility(VISIBLE);
            downloadSizeView.setVisibility(VISIBLE);
        }
    }
    /*
      -------------- USER INTERFACE ELEMENT METHODS -------------------
     */

    private Button getDownloadButton() {
        return (Button) rootView.findViewById(R.id.updateInformationDownloadButton);
    }

    private ImageButton getDownloadCancelButton() {
        return (ImageButton) rootView.findViewById(R.id.updateInformationDownloadCancelButton);
    }

    private ImageButton getDownloadPauseButton() {
        return rootView.findViewById(R.id.updateInformationDownloadPauseButton);
    }


    private TextView getDownloadStatusText() {
        return (TextView) rootView.findViewById(R.id.updateInformationDownloadDetailsView);
    }


    private ProgressBar getDownloadProgressBar() {
        return (ProgressBar) rootView.findViewById(R.id.updateInformationDownloadProgressBar);
    }

    private void showDownloadProgressBar() {
        View downloadProgressBar = rootView.findViewById(R.id.downloadProgressTable);
        if(downloadProgressBar != null) {
            downloadProgressBar.setVisibility(VISIBLE);
        }
    }

    private void hideDownloadProgressBar() {
        rootView.findViewById(R.id.downloadProgressTable).setVisibility(GONE);

    }

    private void hideRefreshIcons() {
        if (updateInformationRefreshLayout != null) {
            if (updateInformationRefreshLayout.isRefreshing()) {
                updateInformationRefreshLayout.setRefreshing(false);
            }
        }
        if (systemIsUpToDateRefreshLayout != null) {
            if (systemIsUpToDateRefreshLayout.isRefreshing()) {
                systemIsUpToDateRefreshLayout.setRefreshing(false);
            }
        }
    }


    /*
      -------------- GOOGLE ADS METHODS -------------------
     */


    private void showAds() {
        if (getApplicationData() != null) {
            adView.loadAd(getApplicationData().buildAdRequest());
        }
    }

    /*
      -------------- UPDATE DOWNLOAD METHODS -------------------
     */

    /**
     * Creates an {@link UpdateDownloadListener}
     */
    private UpdateDownloadListener buildDownloadListener(final UpdateData updateData) {
        return new UpdateDownloadListener() {
            public void onInitialStatusUpdate() {
                if (isAdded()) {
                    getDownloadCancelButton().setOnClickListener(v -> {
                        DownloadService.performOperation(getActivity(), DownloadService.ACTION_CANCEL_DOWNLOAD, updateData);
                        initUpdateDownloadButton(updateData, DownloadStatus.NOT_DOWNLOADING);
                        initInstallButton(updateData, DownloadStatus.NOT_DOWNLOADING);
                    });

                    getDownloadPauseButton().setOnClickListener(v -> {
                        getDownloadPauseButton().setImageDrawable(getResources().getDrawable(R.drawable.resume_download, null));
                        DownloadService.performOperation(getActivity(), DownloadService.ACTION_PAUSE_DOWNLOAD, updateData);
                        initUpdateDownloadButton(updateData, DownloadStatus.PAUSED);
                        initInstallButton(updateData, DownloadStatus.PAUSED);
                    });
                }
            }

            @Override
            public void onDownloadStarted() {
                if (isAdded()) {
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOADING);

                    showDownloadProgressBar();

                    getDownloadPauseButton().setOnClickListener(v -> {
                        getDownloadPauseButton().setImageDrawable(getResources().getDrawable(R.drawable.resume_download, null));
                        DownloadService.performOperation(getActivity(), DownloadService.ACTION_PAUSE_DOWNLOAD, updateData);
                        initUpdateDownloadButton(updateData, DownloadStatus.PAUSED);
                        initInstallButton(updateData, DownloadStatus.PAUSED);
                    });
                }
            }

            @Override
            public void onDownloadProgressUpdate(DownloadProgressData downloadProgressData) {
                if (isAdded()) {
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOADING);

                    showDownloadProgressBar();
                    getDownloadProgressBar().setIndeterminate(false);
                    getDownloadProgressBar().setProgress(downloadProgressData.getProgress());

                    if (downloadProgressData.isWaitingForConnection()) {
                        getDownloadPauseButton().setVisibility(GONE);
                        getDownloadStatusText().setText(getString(R.string.download_waiting_for_network, downloadProgressData.getProgress()));
                        return;
                    }

                    getDownloadPauseButton().setVisibility(VISIBLE);

                    if (downloadProgressData.getTimeRemaining() == null) {
                        getDownloadStatusText().setText(getString(R.string.download_progress_text_unknown_time_remaining, downloadProgressData.getProgress()));
                    } else {
                        getDownloadStatusText().setText(downloadProgressData.getTimeRemaining().toString(getApplicationData()));
                    }
                }
            }

            @Override
            public void onDownloadPaused(boolean pausedByUser, DownloadProgressData progressData) {
                if (isAdded()) {
                    initUpdateDownloadButton(updateData, DownloadStatus.PAUSED);

                    showDownloadProgressBar();

                    if (progressData.isWaitingForConnection()) {
                        onDownloadProgressUpdate(progressData);
                        return;
                    }

                    if (pausedByUser) {
                        getDownloadProgressBar().setProgress(progressData.getProgress());
                        getDownloadStatusText().setText(getString(R.string.download_progress_text_paused, progressData.getProgress()));
                        getDownloadPauseButton().setImageDrawable(getResources().getDrawable(R.drawable.resume_download, null));
                        getDownloadPauseButton().setOnClickListener(v -> {
                            getDownloadPauseButton().setImageDrawable(getResources().getDrawable(R.drawable.pause_download, null));
                            DownloadService.performOperation(getActivity(), DownloadService.ACTION_RESUME_DOWNLOAD, updateData);
                            initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOADING);
                            initInstallButton(updateData, DownloadStatus.DOWNLOADING);
                        });
                    } else {
                        getDownloadStatusText().setText(getString(R.string.download_pending));
                    }
                }
            }

            @Override
            public void onDownloadComplete() {
                if (isAdded()) {
                    Toast.makeText(getApplicationData(), getString(R.string.download_verifying_start), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onDownloadCancelled() {
                if (isAdded()) {
                    initUpdateDownloadButton(updateData, DownloadStatus.NOT_DOWNLOADING);

                    hideDownloadProgressBar();
                }
            }

            @Override
            public void onDownloadError(boolean isInternalError, boolean isStorageSpaceError, boolean isServerError) {
                if (isAdded()) {
                    initUpdateDownloadButton(updateData, DownloadStatus.NOT_DOWNLOADING);

                    hideDownloadProgressBar();

                    if (isServerError) {
                        showDownloadError(updateData, true, R.string.download_error_network);
                    } else if (isStorageSpaceError) {
                        showDownloadError(updateData, false, R.string.download_error_storage);
                    } else {
                        showDownloadError(updateData, false, R.string.download_error_internal);
                    }
                }
            }

            @Override
            public void onVerifyStarted() {
                if (isAdded()) {
                    initUpdateDownloadButton(updateData, DownloadStatus.VERIFYING);

                    showDownloadProgressBar();
                    getDownloadProgressBar().setIndeterminate(true);
                    getDownloadStatusText().setText(getString(R.string.download_progress_text_verifying));
                }
            }

            @Override
            public void onVerifyError() {
                if (isAdded()) {
                    initUpdateDownloadButton(updateData, DownloadStatus.NOT_DOWNLOADING);

                    hideDownloadProgressBar();

                    showDownloadError(updateData, false, R.string.download_error_corrupt);
                }
            }

            @Override
            public void onVerifyComplete() {
                if (isAdded()) {
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOADED);
                    initInstallButton(updateData, DownloadStatus.DOWNLOADED);

                    hideDownloadProgressBar();

                    Toast.makeText(getApplicationData(), getString(R.string.download_complete), Toast.LENGTH_LONG).show();

                    ActivityLauncher launcher = new ActivityLauncher(getActivity());
                    launcher.UpdateInstallation(true, updateData);
                }
            }
        };
    }

    private void showDownloadError(UpdateData updateData, boolean isResumeable, @StringRes int message) {
        Dialogs.showDownloadError(this, updateData, isResumeable, R.string.download_error, message);
    }


    private enum DownloadStatus {
        NOT_DOWNLOADING, DOWNLOADING, DOWNLOADED, VERIFYING, PAUSED
    }

    private DownloadStatus getDownloadStatus(UpdateData updateData) {
        if (new DownloadHelper(getContext()).checkIfUpdateIsDownloaded(updateData)) {
            return DownloadStatus.DOWNLOADED;
        } else if(DownloadService.isVerifying.get()) {
            return DownloadStatus.VERIFYING;
        }

        return DownloadStatus.NOT_DOWNLOADING;
    }

    private void initUpdateDownloadButton(UpdateData updateData, DownloadStatus downloadStatus) {
        final Button downloadButton = getDownloadButton();

        switch (downloadStatus) {
            case NOT_DOWNLOADING:
                downloadButton.setText(getString(R.string.download));

                if (Utils.checkNetworkConnection(getApplicationData()) && updateData.getDownloadUrl() != null && updateData.getDownloadUrl().contains("http")) {
                    downloadButton.setEnabled(true);
                    downloadButton.setClickable(true);
                    downloadButton.setOnClickListener(new DownloadButtonOnClickListener(updateData));
                    downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));
                } else {
                    downloadButton.setEnabled(false);
                    downloadButton.setClickable(false);
                    downloadButton.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                }
                break;
            case DOWNLOADING:
                downloadButton.setText(getString(R.string.downloading));
                downloadButton.setEnabled(true);
                downloadButton.setClickable(false);
                downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));
                break;
            case PAUSED:
                downloadButton.setText(getString(R.string.paused));
                downloadButton.setEnabled(true);
                downloadButton.setClickable(false);
                downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));
                break;
            case DOWNLOADED:
                downloadButton.setText(getString(R.string.downloaded));
                downloadButton.setEnabled(true);
                downloadButton.setClickable(true);
                downloadButton.setOnClickListener(new AlreadyDownloadedOnClickListener(this, updateData));
                downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));
                break;
            case VERIFYING:
                downloadButton.setText(getString(R.string.download_verifying));
                downloadButton.setEnabled(true);
                downloadButton.setClickable(false);
                downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));
        }
    }

    private void initInstallButton(UpdateData updateData, DownloadStatus downloadStatus) {
        Button installButton = rootView.findViewById(R.id.updateInstallationInstructionsButton);
        if (downloadStatus != DownloadStatus.DOWNLOADED) {
            installButton.setVisibility(GONE);
        } else {
            if (getActivity() == null) {
                return;
            }

            installButton.setVisibility(VISIBLE);
            installButton.setOnClickListener(v -> {
                ((MainActivity) getActivity()).getActivityLauncher().UpdateInstallation(true, updateData);
                LocalNotifications.hideDownloadCompleteNotification(getActivity());
            });
        }
    }

    /**
     * Download button click listener. Performs these actions when the button is clicked.
     */
    private class DownloadButtonOnClickListener implements View.OnClickListener {

        private final UpdateData updateData;

        DownloadButtonOnClickListener(UpdateData updateData) {
            this.updateData = updateData;
        }

        @Override
        public void onClick(View v) {
            if (isAdded()) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity == null) {
                    return;
                }

                if (mainActivity.hasDownloadPermissions()) {
                    DownloadService.performOperation(getActivity(), DownloadService.ACTION_DOWNLOAD_UPDATE, updateData);
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOADING);
                    showDownloadProgressBar();
                    getDownloadProgressBar().setIndeterminate(true);
                    getDownloadStatusText().setText(getString(R.string.download_pending));
                } else {
                    mainActivity.requestDownloadPermissions(granted -> {
                        if (granted) {
                            DownloadService.performOperation(getActivity(), DownloadService.ACTION_DOWNLOAD_UPDATE, updateData);
                        }
                    });
                }
            }
        }
    }
    /**
     * Allows an already downloaded update file to be deleted to save storage space.
     */
    private class AlreadyDownloadedOnClickListener implements View.OnClickListener {

        private final Fragment targetFragment;
        private final UpdateData updateData;

        AlreadyDownloadedOnClickListener(Fragment targetFragment, UpdateData updateData) {
            this.targetFragment = targetFragment;
            this.updateData = updateData;
        }

        @Override
        public void onClick(View v) {
            Dialogs.showUpdateAlreadyDownloadedMessage(updateData, targetFragment, (ignored) -> {
                if (updateData != null) {
                    DownloadService.performOperation(getActivity(), DownloadService.ACTION_DELETE_DOWNLOADED_UPDATE, updateData);
                    initUpdateDownloadButton(updateData, UpdateInformationFragment.DownloadStatus.NOT_DOWNLOADING);
                    initInstallButton(updateData, DownloadStatus.NOT_DOWNLOADING);
                }
            });
        }
    }

    private void registerDownloadReceiver(UpdateDownloadListener downloadListener) {
        IntentFilter filter = new IntentFilter(DownloadReceiver.ACTION_DOWNLOAD_EVENT);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        this.downloadReceiver = new DownloadReceiver(downloadListener);

        if (getActivity() !=  null) {
            getActivity().registerReceiver(this.downloadReceiver, filter);
        }
    }

    private void unregisterDownloadReceiver() {
        if (getActivity() != null) {
            getActivity().unregisterReceiver(this.downloadReceiver);
            this.downloadReceiver = null;
        }
    }

    private boolean isDownloadServiceRunning() {
        if (getActivity() == null) {
            return false;
        }

        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DownloadService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}