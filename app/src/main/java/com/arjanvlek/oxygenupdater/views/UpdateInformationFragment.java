package com.arjanvlek.oxygenupdater.views;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
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
import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.Banner;
import com.arjanvlek.oxygenupdater.Model.DownloadProgressData;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.ServerResult;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.Callback;
import com.arjanvlek.oxygenupdater.Support.DateTimeFormatter;
import com.arjanvlek.oxygenupdater.Support.Dialogs;
import com.arjanvlek.oxygenupdater.Support.NetworkConnectionManager;
import com.arjanvlek.oxygenupdater.Support.Notifications;
import com.arjanvlek.oxygenupdater.Support.ServerResultCallbackData;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;
import com.arjanvlek.oxygenupdater.Support.UpdateDescriptionParser;
import com.arjanvlek.oxygenupdater.Support.UpdateDownloadListener;
import com.arjanvlek.oxygenupdater.Support.UpdateDownloader;
import com.arjanvlek.oxygenupdater.Support.Utils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import static android.app.DownloadManager.ERROR_DEVICE_NOT_FOUND;
import static android.app.DownloadManager.ERROR_FILE_ALREADY_EXISTS;
import static android.app.DownloadManager.ERROR_FILE_ERROR;
import static android.app.DownloadManager.ERROR_HTTP_DATA_ERROR;
import static android.app.DownloadManager.ERROR_INSUFFICIENT_SPACE;
import static android.app.DownloadManager.ERROR_TOO_MANY_REDIRECTS;
import static android.app.DownloadManager.ERROR_UNHANDLED_HTTP_CODE;
import static android.app.DownloadManager.PAUSED_QUEUED_FOR_WIFI;
import static android.app.DownloadManager.PAUSED_UNKNOWN;
import static android.app.DownloadManager.PAUSED_WAITING_FOR_NETWORK;
import static android.app.DownloadManager.PAUSED_WAITING_TO_RETRY;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.RelativeLayout.ABOVE;
import static android.widget.RelativeLayout.BELOW;
import static com.arjanvlek.oxygenupdater.ApplicationContext.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.Model.ServerStatus.Status.UNREACHABLE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DEVICE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_FILE_NAME;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_UPDATE_CHECKED_DATE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_UPDATE_METHOD_ID;
import static java8.util.stream.StreamSupport.stream;

public class UpdateInformationFragment extends AbstractFragment {


    private SwipeRefreshLayout updateInformationRefreshLayout;
    private SwipeRefreshLayout systemIsUpToDateRefreshLayout;
    private RelativeLayout rootView;
    private AdView adView;

    private Context context;
    private SettingsManager settingsManager;
    private NetworkConnectionManager networkConnectionManager;
    private UpdateDownloader updateDownloader;

    private OxygenOTAUpdate oxygenOTAUpdate;

    private DateTime refreshedDate;
    private boolean isLoadedOnce;
    private boolean adsAreSupported;



    // In app message bar collections and identifiers.
    private static final String KEY_HAS_DOWNLOAD_ERROR = "has_download_error";
    private static final String KEY_DOWNLOAD_ERROR_TITLE = "download_error_title";
    private static final String KEY_DOWNLOAD_ERROR_MESSAGE = "download_error_message";
    private static final String UNABLE_TO_FIND_A_MORE_RECENT_BUILD = "unable to find a more recent build";
    private static final boolean DOWNLOADED = true;
    private List<ServerMessageBar> inAppMessageBars = new ArrayList<>();

    /*
      -------------- ANDROID ACTIVITY LIFECYCLE METHODS -------------------
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        settingsManager = new SettingsManager(getActivity().getApplicationContext());
        networkConnectionManager = new NetworkConnectionManager(getActivity().getApplicationContext());
        adsAreSupported = getApplicationContext().checkPlayServices(getActivity(), false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_updateinformation, container, false);
        if (adsAreSupported) adView = (AdView) rootView.findViewById(R.id.updateInformationAdView);
        return this.rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(isAdded()) {
            initLayout();

            if(settingsManager.checkIfSetupScreenHasBeenCompleted()) {
                load();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adView != null && adsAreSupported) {
            adView.pause();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (adView != null && adsAreSupported) {
            adView.resume();
        }
        if (refreshedDate != null && refreshedDate.plusMinutes(5).isBefore(DateTime.now()) &&  settingsManager.checkIfSetupScreenHasBeenCompleted() && isAdded()) {
            load();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adView != null && adsAreSupported) {
            adView.destroy();
        }
    }

    /*
      -------------- INITIALIZATION / DATA FETCHING METHODS -------------------
     */

    /**
     * Initializes the layout. Sets refresh listeners for pull-down to refresh and applies the right colors for pull-down to refresh screens.
     */
    private void initLayout() {
        updateInformationRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.updateInformationRefreshLayout);
        systemIsUpToDateRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.updateInformationSystemIsUpToDateRefreshLayout);

        updateInformationRefreshLayout.setOnRefreshListener(this::load);
        updateInformationRefreshLayout.setColorSchemeResources(R.color.oneplus_red, R.color.holo_orange_light, R.color.holo_red_light);

        systemIsUpToDateRefreshLayout.setOnRefreshListener(this::load);
        systemIsUpToDateRefreshLayout.setColorSchemeResources(R.color.oneplus_red, R.color.holo_orange_light, R.color.holo_red_light);


        Button installGuideButton = (Button) updateInformationRefreshLayout.findViewById(R.id.updateInstallationInstructionsButton);
        installGuideButton.setOnClickListener(v -> {
            ((MainActivity) getActivity()).getActivityLauncher().UpdateInstructions(updateDownloader.checkIfUpdateIsDownloaded(oxygenOTAUpdate));
            Notifications.hideDownloadCompleteNotification(getActivity());
        });
    }

    /**
     * Fetches all server data. This includes update information, server messages and server status checks
     */
    private void load() {
        final Fragment instance = this;

        new GetServerData().execute(result -> {
            ServerResultCallbackData data = (ServerResultCallbackData) result[0];

            displayUpdateInformation(data.getOxygenOTAUpdate(), data.isOnline(), false);
            displayInAppMessageBars(data.getInAppBars());

            if (data.isOnline()) showAds();
            else hideAds();

            oxygenOTAUpdate = data.getOxygenOTAUpdate();
            if (!isLoadedOnce) updateDownloader = initDownloadManager(oxygenOTAUpdate);

            // If the activity is started with a download error (when clicked on a "download failed" notification), show it to the user.
            if (!isLoadedOnce && getActivity().getIntent() != null && getActivity().getIntent().getBooleanExtra(KEY_HAS_DOWNLOAD_ERROR, false)) {
                Intent i = getActivity().getIntent();
                Dialogs.showDownloadError(instance, updateDownloader, oxygenOTAUpdate, i.getStringExtra(KEY_DOWNLOAD_ERROR_TITLE), i.getStringExtra(KEY_DOWNLOAD_ERROR_MESSAGE));
            }

            isLoadedOnce = true;
            refreshedDate = DateTime.now();
        });
    }


    /*
      -------------- METHODS FOR DISPLAYING DATA ON THE FRAGMENT -------------------
     */


    private void addMessageBar(ServerMessageBar view) {
        // Add the message to the update information screen.
        // Set the layout params based on the view count.
        // First view should go below the app update message bar (if visible)
        // Consecutive views should go below their parent / previous view.
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);

        int numberOfBars = this.inAppMessageBars.size();

        params.topMargin = numberOfBars * Utils.diPToPixels(getActivity(), 20);
        view.setId((numberOfBars * 20000 + 1));
        this.rootView.addView(view, params);
        this.inAppMessageBars.add(view);
    }

    private void deleteAllInAppMessageBars() {
        stream(this.inAppMessageBars).filter(v -> v != null).forEach(v -> this.rootView.removeView(v));

        this.inAppMessageBars = new ArrayList<>();
    }

    private void displayInAppMessageBars(List<Banner> banners) {

        deleteAllInAppMessageBars();

        List<ServerMessageBar> createdServerMessageBars = new ArrayList<>();

        for(Banner banner : banners) {
            ServerMessageBar bar = new ServerMessageBar(getApplicationContext());
            View backgroundBar = bar.getBackgroundBar();
            TextView textView = bar.getTextView();

            backgroundBar.setBackgroundColor(banner.getColor(context));
            textView.setText(banner.getBannerText(context));

            if(banner.getBannerText(context) instanceof Spanned) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }

            addMessageBar(bar);
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
        this.inAppMessageBars = createdServerMessageBars;
    }

    /**
     * Displays the update information from a {@link OxygenOTAUpdate} with update information.
     * @param oxygenOTAUpdate Update information to display
     * @param online Whether or not the device has an active network connection
     * @param displayInfoWhenUpToDate Flag set to show update information anyway, even if the system is up to date.
     */
    private void displayUpdateInformation(final OxygenOTAUpdate oxygenOTAUpdate, final boolean online, boolean displayInfoWhenUpToDate) {
        // Abort if no update data is found or if the fragment is not attached to its activity to prevent crashes.
        if (!isAdded()) {
            return;
        }

        // Hide the loading screen
        rootView.findViewById(R.id.updateInformationLoadingScreen).setVisibility(GONE);

        if (oxygenOTAUpdate.getId() == null) {
            return;
        }

        if (((oxygenOTAUpdate.isSystemIsUpToDate()) && !displayInfoWhenUpToDate) || !oxygenOTAUpdate.isUpdateInformationAvailable()) {
            displayUpdateInformationWhenUpToDate(oxygenOTAUpdate, online);
        } else {
            displayUpdateInformationWhenNotUpToDate(oxygenOTAUpdate, displayInfoWhenUpToDate);
        }

        if(online) {
            // Save update data for offline viewing
            settingsManager.savePreference(PROPERTY_OFFLINE_UPDATE_NAME, oxygenOTAUpdate.getVersionNumber());
            settingsManager.savePreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, oxygenOTAUpdate.getDownloadSize());
            settingsManager.savePreference(PROPERTY_OFFLINE_UPDATE_DESCRIPTION, oxygenOTAUpdate.getDescription());
            settingsManager.savePreference(PROPERTY_OFFLINE_FILE_NAME, oxygenOTAUpdate.getFilename());
            settingsManager.savePreference(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, oxygenOTAUpdate.isUpdateInformationAvailable());
            settingsManager.savePreference(PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString());
        }

        // Hide the refreshing icon if it is present.
        hideRefreshIcons();
    }

    private void displayUpdateInformationWhenUpToDate(final OxygenOTAUpdate oxygenOTAUpdate, boolean online) {
        // Show "System is up to date" view.
        rootView.findViewById(R.id.updateInformationRefreshLayout).setVisibility(GONE);
        rootView.findViewById(R.id.updateInformationSystemIsUpToDateRefreshLayout).setVisibility(VISIBLE);

        // Set the current Oxygen OS version if available.
        String oxygenOSVersion = ((ApplicationContext)getActivity().getApplication()).getSystemVersionProperties().getOxygenOSVersion();
        TextView versionNumberView = (TextView) rootView.findViewById(R.id.updateInformationSystemIsUpToDateVersionTextView);
        if(!oxygenOSVersion.equals(NO_OXYGEN_OS)) {
            versionNumberView.setVisibility(VISIBLE);
            versionNumberView.setText(String.format(getString(R.string.update_information_oxygen_os_version), oxygenOSVersion));
        } else {
            versionNumberView.setVisibility(GONE);
        }

        // Set "No Update Information Is Available" button if needed.
        Button updateInformationButton = (Button) rootView.findViewById(R.id.updateInformationSystemIsUpToDateStatisticsButton);
        if(!oxygenOTAUpdate.isUpdateInformationAvailable()) {
            updateInformationButton.setText(getString(R.string.update_information_no_update_data_available));
            updateInformationButton.setClickable(false);
        } else {
            updateInformationButton.setText(getString(R.string.update_information_view_update_information));
            updateInformationButton.setClickable(true);
            updateInformationButton.setOnClickListener(v -> displayUpdateInformation(oxygenOTAUpdate, true, true));
        }

        // Save last time checked if online.
        if(online) {
            settingsManager.savePreference(PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString());
        }

        // Show last time checked.
        TextView dateCheckedView = (TextView) rootView.findViewById(R.id.updateInformationSystemIsUpToDateDateTextView);
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatter(context, this);
        dateCheckedView.setText(String.format(getString(R.string.update_information_last_checked_on), dateTimeFormatter.formatDateTime(settingsManager.getPreference(PROPERTY_UPDATE_CHECKED_DATE))));

    }

    private void displayUpdateInformationWhenNotUpToDate(final OxygenOTAUpdate oxygenOTAUpdate, boolean displayInfoWhenUpToDate) {
        // Show "System update available" view.
        rootView.findViewById(R.id.updateInformationRefreshLayout).setVisibility(VISIBLE);
        rootView.findViewById(R.id.updateInformationSystemIsUpToDateRefreshLayout).setVisibility(GONE);

        // Display available update version number.
        TextView buildNumberView = (TextView) rootView.findViewById(R.id.updateInformationBuildNumberView);
        if (oxygenOTAUpdate.getVersionNumber() != null && !oxygenOTAUpdate.getVersionNumber().equals("null")) {
            buildNumberView.setText(oxygenOTAUpdate.getVersionNumber());
        } else {
            buildNumberView.setText(String.format(getString(R.string.update_information_unknown_update_name), settingsManager.getPreference(PROPERTY_DEVICE)));
        }

        // Display download size.
        TextView downloadSizeView = (TextView) rootView.findViewById(R.id.updateInformationDownloadSizeView);
        downloadSizeView.setText(String.format(getString(R.string.download_size_megabyte), oxygenOTAUpdate.getDownloadSize()));

        // Display update description.
        String description = oxygenOTAUpdate.getDescription();
        TextView descriptionView = (TextView) rootView.findViewById(R.id.updateDescriptionView);
        descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
        descriptionView.setText(description != null && !description.isEmpty() && !description.equals("null") ? UpdateDescriptionParser.parse(description) : getString(R.string.update_information_description_not_available));

        // Display update file name.
        TextView fileNameView = (TextView) rootView.findViewById(R.id.updateFileNameView);
        fileNameView.setText(String.format(getString(R.string.update_information_file_name), oxygenOTAUpdate.getFilename()));


        // Format top title based on system version installed.
        TextView headerLabel = (TextView) rootView.findViewById(R.id.headerLabel);
        Button updateInstallationGuideButton = (Button) rootView.findViewById(R.id.updateInstallationInstructionsButton);
        View downloadSizeTable = rootView.findViewById(R.id.buttonTable);
        View downloadSizeImage = rootView.findViewById(R.id.downloadSizeImage);

        final Button downloadButton = getDownloadButton();
        initUpdateDownloadButton(updateDownloader.checkIfUpdateIsDownloaded(oxygenOTAUpdate) ? DownloadStatus.DOWNLOADED : updateDownloader.checkIfAnUpdateIsBeingVerified() ? DownloadStatus.VERIFYING : DownloadStatus.NOT_DOWNLOADING);

        if(displayInfoWhenUpToDate) {
            headerLabel.setText(getString(R.string.update_information_installed_update));
            downloadButton.setVisibility(GONE);
            updateInstallationGuideButton.setVisibility(GONE);
            fileNameView.setVisibility(GONE);
            downloadSizeTable.setVisibility(GONE);
            downloadSizeImage.setVisibility(GONE);
            downloadSizeView.setVisibility(GONE);
        } else {
            if (oxygenOTAUpdate.isSystemIsUpToDateCheck(settingsManager)) {
                headerLabel.setText(getString(R.string.update_information_installed_update));
            } else {
                headerLabel.setText(getString(R.string.update_information_latest_available_update));
            }
            downloadButton.setVisibility(VISIBLE);
            updateInstallationGuideButton.setVisibility(VISIBLE);
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
        if(rootView != null) {
            adView = (AdView) rootView.findViewById(R.id.updateInformationAdView);
        }
        if (adView != null && adsAreSupported) {
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(ADS_TEST_DEVICE_ID_OWN_DEVICE)
                    .addTestDevice(ADS_TEST_DEVICE_ID_TEST_DEVICE)
                    .addTestDevice(ADS_TEST_DEVICE_ID_EMULATOR_1)
                    .addTestDevice(ADS_TEST_DEVICE_ID_EMULATOR_2)
                    .addTestDevice(ADS_TEST_DEVICE_ID_EMULATOR_3)
                    .build();

            adView.loadAd(adRequest);
        }
    }

    private void hideAds() {
        if (adView != null && adsAreSupported) {
            adView.destroy();
        }
    }

    /*
      -------------- UPDATE DOWNLOAD METHODS -------------------
     */

    /**
     * Creates a {@link UpdateDownloader} and applies an {@link UpdateDownloadListener} to it to allow displaying update download progress and error messages.
     */
    private UpdateDownloader initDownloadManager(final OxygenOTAUpdate oxygenOTAUpdate) {
        return new UpdateDownloader(getActivity())
                    .setUpdateDownloadListenerAndStartPolling(new UpdateDownloadListener() {
                        @Override
                        public void onDownloadManagerInit(final UpdateDownloader caller) {
                            getDownloadCancelButton().setOnClickListener(v -> caller.cancelDownload(oxygenOTAUpdate));
                        }

                        @Override
                        public void onDownloadStarted(long downloadID) {
                            if(isAdded()) {
                                initUpdateDownloadButton(DownloadStatus.DOWNLOADING);

                                showDownloadProgressBar();
                                getDownloadProgressBar().setIndeterminate(true);

                                getDownloadStatusText().setText(getString(R.string.download_pending));
                            }
                        }

                        @Override
                        public void onDownloadPending() {
                            if(isAdded()) {
                                initUpdateDownloadButton(DownloadStatus.DOWNLOADING);

                                showDownloadProgressBar();
                                getDownloadProgressBar().setIndeterminate(true);

                                getDownloadStatusText().setText(getString(R.string.download_pending));
                            }
                        }

                        @Override
                        public void onDownloadProgressUpdate(DownloadProgressData downloadProgressData) {
                            if(isAdded()) {
                                getDownloadButton().setText(getString(R.string.downloading));
                                getDownloadButton().setClickable(false);

                                showDownloadProgressBar();
                                getDownloadProgressBar().setIndeterminate(false);
                                getDownloadProgressBar().setProgress(downloadProgressData.getProgress());

                                if(downloadProgressData.getTimeRemaining() == null) {
                                    getDownloadStatusText().setText(getString(R.string.download_progress_text_unknown_time_remaining, downloadProgressData.getProgress()));
                                } else {
                                    getDownloadStatusText().setText(downloadProgressData.getTimeRemaining().toString(getActivity()));
                                }
                            }
                        }

                        @Override
                        public void onDownloadPaused(int statusCode) {
                            if(isAdded()) {
                                initUpdateDownloadButton(DownloadStatus.DOWNLOADING);

                                showDownloadProgressBar();

                                switch (statusCode) {
                                    case PAUSED_QUEUED_FOR_WIFI:
                                        getDownloadStatusText().setText(getString(R.string.download_waiting_for_wifi));
                                        break;
                                    case PAUSED_WAITING_FOR_NETWORK:
                                        getDownloadStatusText().setText(getString(R.string.download_waiting_for_network));
                                        break;
                                    case PAUSED_WAITING_TO_RETRY:
                                        getDownloadStatusText().setText(getString(R.string.download_will_retry_soon));
                                        break;
                                    case PAUSED_UNKNOWN:
                                        getDownloadStatusText().setText(getString(R.string.download_paused_unknown));
                                        break;
                                }
                            }
                        }

                        @Override
                        public void onDownloadComplete() {
                            if(isAdded()) {
                                Toast.makeText(getApplicationContext(), getString(R.string.download_verifying_start), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onDownloadCancelled() {
                            if(isAdded()) {
                                initUpdateDownloadButton(DownloadStatus.NOT_DOWNLOADING);

                                hideDownloadProgressBar();
                            }
                        }

                        @Override
                        public void onDownloadError(UpdateDownloader caller, int statusCode) {
                            initUpdateDownloadButton(DownloadStatus.NOT_DOWNLOADING);

                            hideDownloadProgressBar();


                            // Treat any HTTP status code exception (lower than 1000) as a network error.
                            // Handle any other errors according to the error message.
                            if(isAdded()) {
                                if (statusCode < 1000) {
                                   showDownloadError(caller, R.string.download_error_network,R.string.download_notification_error_network);
                                } else {
                                    switch (statusCode) {
                                        case ERROR_UNHANDLED_HTTP_CODE:
                                        case ERROR_HTTP_DATA_ERROR:
                                        case ERROR_TOO_MANY_REDIRECTS:
                                            showDownloadError(caller, R.string.download_error_network, R.string.download_notification_error_network);
                                            break;
                                        case ERROR_FILE_ERROR:
                                            showDownloadError(caller, R.string.download_error_directory, R.string.download_notification_error_storage_not_found);
                                            break;
                                        case ERROR_INSUFFICIENT_SPACE:
                                            showDownloadError(caller, R.string.download_error_storage, R.string.download_notification_error_storage_full);
                                            break;
                                        case ERROR_DEVICE_NOT_FOUND:
                                            showDownloadError(caller, R.string.download_error_sd_card, R.string.download_notification_error_sd_card_missing);
                                            break;
                                        case ERROR_FILE_ALREADY_EXISTS:
                                            initUpdateDownloadButton(DownloadStatus.DOWNLOADED);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onVerifyStarted() {
                            if(isAdded()) {
                                initUpdateDownloadButton(DownloadStatus.VERIFYING);

                                showDownloadProgressBar();
                                getDownloadProgressBar().setIndeterminate(true);
                                getDownloadStatusText().setText(getString(R.string.download_progress_text_verifying));
                            }
                        }

                        @Override
                        public void onVerifyError(UpdateDownloader caller) {
                            if(isAdded()) {
                                initUpdateDownloadButton(DownloadStatus.NOT_DOWNLOADING);

                                hideDownloadProgressBar();

                                showDownloadError(caller, R.string.download_error, R.string.download_notification_error_corrupt);
                            }
                        }

                        @Override
                        public void onVerifyComplete() {
                            if(isAdded()) {
                                initUpdateDownloadButton(DownloadStatus.DOWNLOADED);

                                hideDownloadProgressBar();

                                Toast.makeText(getApplicationContext(), getString(R.string.download_complete), Toast.LENGTH_LONG).show();
                            }
                        }
                    }, oxygenOTAUpdate);
        }

    private void showDownloadError(UpdateDownloader updateDownloader, @StringRes  int title, @StringRes int message) {
        Dialogs.showDownloadError(this, updateDownloader, oxygenOTAUpdate, title, message);
    }


    private enum DownloadStatus {
        NOT_DOWNLOADING, DOWNLOADING, DOWNLOADED, VERIFYING
    }

    private void initUpdateDownloadButton(DownloadStatus downloadStatus) {
        final Button downloadButton = getDownloadButton();

        switch (downloadStatus) {
            case NOT_DOWNLOADING:
                downloadButton.setText(getString(R.string.download));

                if (networkConnectionManager.checkNetworkConnection() && oxygenOTAUpdate.getDownloadUrl() != null && oxygenOTAUpdate.getDownloadUrl().contains("http")) {
                    downloadButton.setEnabled(true);
                    downloadButton.setClickable(true);
                    downloadButton.setOnClickListener(new DownloadButtonOnClickListener());
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
            case DOWNLOADED:
                downloadButton.setText(getString(R.string.downloaded));
                downloadButton.setEnabled(true);
                downloadButton.setClickable(true);
                downloadButton.setOnClickListener(new AlreadyDownloadedOnClickListener(this));
                downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));
                break;
            case VERIFYING:
                downloadButton.setText(getString(R.string.download_verifying));
                downloadButton.setEnabled(true);
                downloadButton.setClickable(false);
                downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));
        }
    }

    /**
     * Download button click listener. Performs these actions when the button is clicked.
     */
    private class DownloadButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                if (mainActivity.hasDownloadPermissions()) {
                    updateDownloader.downloadUpdate(oxygenOTAUpdate);
                } else {
                    Callback callback = result -> {
                        if ((int) result[0] == PackageManager.PERMISSION_GRANTED) {
                            updateDownloader.downloadUpdate(oxygenOTAUpdate);
                        }
                    };
                    mainActivity.requestDownloadPermissions(callback);
                }
            }
        }
    }
    /**
     * Allows an already downloaded update file to be deleted to save storage space.
     */
    private class AlreadyDownloadedOnClickListener implements View.OnClickListener {

        private final Fragment targetFragment;
        private static final String TAG = "DeleteDownload";

        public AlreadyDownloadedOnClickListener(Fragment targetFragment) {
            this.targetFragment = targetFragment;
        }

        @Override
        public void onClick(View v) {
            MessageDialog dialog = new MessageDialog()
                    .setTitle(getString(R.string.delete_message_title))
                    .setMessage(getString(R.string.delete_message_contents))
                    .setClosable(true)
                    .setPositiveButtonText(getString(R.string.install_guide))
                    .setNegativeButtonText(getString(R.string.delete_message_delete_button))
                    .setDialogListener(new MessageDialog.DialogListener() {
                        @Override
                        public void onDialogPositiveButtonClick(DialogFragment dialogFragment) {
                            ActivityLauncher activityLauncher = new ActivityLauncher(getActivity());
                            activityLauncher.UpdateInstructions(DOWNLOADED);
                        }

                        @Override
                        public void onDialogNegativeButtonClick(DialogFragment dialogFragment) {
                            if (oxygenOTAUpdate != null) {
                                updateDownloader.deleteDownload(oxygenOTAUpdate);
                                initUpdateDownloadButton(DownloadStatus.NOT_DOWNLOADING);
                            }
                        }
                    });
            dialog.setTargetFragment(targetFragment, 0);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.add(dialog, TAG);
            transaction.commitAllowingStateLoss();

        }

    }

    private class GetServerData extends AsyncTask<Callback, Void, ServerResult> {

        @Override
        protected ServerResult doInBackground(Callback... callbacks) {
            ServerResult serverResult = new ServerResult();
            serverResult.setCallback(callbacks[0]);
            serverResult.setServerStatus(getApplicationContext().getServerConnector().getServerStatus());

            Long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID);
            Long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID);

            serverResult.setServerMessages(getApplicationContext().getServerConnector().getServerMessages(deviceId, updateMethodId));


            SystemVersionProperties systemVersionProperties = getApplicationContext().getSystemVersionProperties();
            OxygenOTAUpdate oxygenOTAUpdate = getApplicationContext().getServerConnector().getOxygenOTAUpdate(deviceId, updateMethodId, systemVersionProperties.getOxygenOSOTAVersion());
            if (oxygenOTAUpdate != null && oxygenOTAUpdate.getInformation() != null && oxygenOTAUpdate.getInformation().equals(UNABLE_TO_FIND_A_MORE_RECENT_BUILD) && oxygenOTAUpdate.isUpdateInformationAvailable() && oxygenOTAUpdate.isSystemIsUpToDate()) {
                oxygenOTAUpdate = getApplicationContext().getServerConnector().getMostRecentOxygenOTAUpdate(deviceId, updateMethodId);
            } else {
                if (settingsManager.checkIfCacheIsAvailable()) {
                    oxygenOTAUpdate = new OxygenOTAUpdate();
                    oxygenOTAUpdate.setVersionNumber(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_NAME));
                    oxygenOTAUpdate.setDownloadSize(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE));
                    oxygenOTAUpdate.setDescription(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DESCRIPTION));
                    oxygenOTAUpdate.setUpdateInformationAvailable(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE));
                    oxygenOTAUpdate.setFilename(settingsManager.getPreference(PROPERTY_OFFLINE_FILE_NAME));
                } else {
                    Dialogs.showNoNetworkConnectionError(getParentFragment());
                    oxygenOTAUpdate = new OxygenOTAUpdate();
                }
            }
            serverResult.setUpdateData(oxygenOTAUpdate);
            return serverResult;
        }

        @Override
        protected void onPostExecute(final ServerResult serverResult) {
            List<Banner> inAppBars = new ArrayList<>();

            // Add the "No connection" bar depending on the network status of the device.
            boolean online = networkConnectionManager.checkNetworkConnection();
            if (!online) {
                inAppBars.add(new Banner() {
                    @Override
                    public String getBannerText(Context context) {
                        return context.getString(R.string.error_no_internet_connection);
                    }

                    @Override
                    public int getColor(Context context) {
                        return ContextCompat.getColor(context, R.color.holo_red_light);
                    }
                });
            }

            if (serverResult.getServerMessages() != null && settingsManager.getPreference(PROPERTY_SHOW_NEWS_MESSAGES, true)) {
                inAppBars.addAll(serverResult.getServerMessages());
            }

            if (serverResult.getServerStatus() == null) {
                ServerStatus serverStatus = new ServerStatus();
                serverStatus.setStatus(UNREACHABLE);
                serverStatus.setLatestAppVersion(BuildConfig.VERSION_NAME);
                serverResult.setServerStatus(serverStatus);
            }

            ServerStatus.Status status = serverResult.getServerStatus().getStatus();

            if (status.isUserRecoverableError()) {
                inAppBars.add(serverResult.getServerStatus());
            }

            if (status.isNonRecoverableError()) {
                switch (status) {
                    case MAINTENANCE:
                        Dialogs.showServerMaintenanceError(getParentFragment());
                        break;
                    case OUTDATED:
                        Dialogs.showAppOutdatedError(getParentFragment());
                        break;
                }
            }

            if (settingsManager.getPreference(PROPERTY_SHOW_APP_UPDATE_MESSAGES, true) && !serverResult.getServerStatus().checkIfAppIsUpToDate()) {
                inAppBars.add(new Banner() {

                    @Override
                    public CharSequence getBannerText(Context context) {
                        //noinspection deprecation Suggested fix requires API level 24, which is too new for this app, or an ugly if-else statement.
                        return Html.fromHtml(String.format(getString(R.string.new_app_version), serverResult.getServerStatus().getLatestAppVersion()));
                    }

                    @Override
                    public int getColor(Context context) {
                        return ContextCompat.getColor(context, R.color.holo_green_light);
                    }
                });
            }

            serverResult.getCallback().onActionPerformed(new ServerResultCallbackData(serverResult.getUpdateData(), online, inAppBars));
        }
    }
}