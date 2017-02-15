package com.arjanvlek.oxygenupdater.views;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.Banner;
import com.arjanvlek.oxygenupdater.Model.DownloadProgressData;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.ServerMessage;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.Support.Callback;
import com.arjanvlek.oxygenupdater.Support.DateTimeFormatter;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.UpdateDescriptionParser;
import com.arjanvlek.oxygenupdater.Support.UpdateDownloadListener;
import com.arjanvlek.oxygenupdater.Support.UpdateDownloader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.app.DownloadManager.ERROR_CANNOT_RESUME;
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
import static com.arjanvlek.oxygenupdater.ApplicationContext.LOCALE_DUTCH;
import static com.arjanvlek.oxygenupdater.ApplicationContext.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.Model.ServerStatus.Status.NORMAL;
import static com.arjanvlek.oxygenupdater.Model.ServerStatus.Status.UNREACHABLE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.*;
import static com.arjanvlek.oxygenupdater.Support.UpdateDownloader.NOT_SET;
import static com.arjanvlek.oxygenupdater.views.InstallGuideActivity.INTENT_SHOW_DOWNLOAD_PAGE;

public class UpdateInformationFragment extends AbstractUpdateInformationFragment {


    private SwipeRefreshLayout updateInformationRefreshLayout;
    private SwipeRefreshLayout systemIsUpToDateRefreshLayout;
    private RelativeLayout rootView;
    private AdView adView;

    private Context context;
    private UpdateDownloader updateDownloader;

    private DateTime refreshedDate;
    private boolean isFetched;
    private boolean adsAreSupported;

    public static final int NOTIFICATION_ID = 1;
    public static final int DOWNLOAD_COMPLETE_NOTIFICATION_ID = 1000000000;

    // In app message bar collections and identifiers.
    private static final String KEY_APP_UPDATE_BARS = "app_update_bars";
    private static final String KEY_SERVER_ERROR_BARS = "server_error_bars";
    private static final String KEY_NO_NETWORK_CONNECTION_BARS = "no_network_connection_bars";
    private static final String KEY_SERVER_MESSAGE_BARS = "server_message_bars";
    private static final String KEY_HAS_DOWNLOAD_ERROR = "has_download_error";
    private static final String KEY_DOWNLOAD_ERROR_TITLE = "download_error_title";
    private static final String KEY_DOWNLOAD_ERROR_MESSAGE = "download_error_message";
    private Map<String, List<Object>> inAppMessageBarData = new HashMap<>();
    private List<ServerMessageBar> inAppMessageBars = new ArrayList<>();

    /*
      -------------- ANDROID ACTIVITY LIFECYCLE METHODS -------------------
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {
            context = getActivity().getApplicationContext();
            // If the activity is started with a download error (when clicked on a "download failed" notification), show it to the user.
            if(getActivity().getIntent() != null && getActivity().getIntent().getBooleanExtra(KEY_HAS_DOWNLOAD_ERROR, false)) {
                Intent i = getActivity().getIntent();
                showDownloadError(i.getStringExtra(KEY_DOWNLOAD_ERROR_TITLE), i.getStringExtra(KEY_DOWNLOAD_ERROR_MESSAGE));
            }
        }

        adsAreSupported = getApplicationContext().checkPlayServices(getActivity(), false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_updateinformation, container, false);
        return this.rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(isAdded()) {
            initLayout();
            initData();
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
        if (refreshedDate != null && isFetched && settingsManager.checkIfSetupScreenHasBeenCompleted() && isAdded()) {
            if (refreshedDate.plusMinutes(5).isBefore(DateTime.now())) {
                if (networkConnectionManager.checkNetworkConnection()) {
                    getServerData();
                    refreshedDate = DateTime.now();
                } else if (settingsManager.checkIfCacheIsAvailable()) {
                    getServerData();
                    displayUpdateInformation(buildOfflineOxygenOTAUpdate(), false, false);
                    refreshedDate = DateTime.now();
                } else {
                    showNetworkError();
                }
            }
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
        if (updateInformationRefreshLayout == null && rootView != null && isAdded()) {
            updateInformationRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.updateInformationRefreshLayout);
            systemIsUpToDateRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.updateInformationSystemIsUpToDateRefreshLayout);
            if(updateInformationRefreshLayout != null) {
                updateInformationRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        if (networkConnectionManager.checkNetworkConnection()) {
                            getServerData();
                        } else if (settingsManager.checkIfCacheIsAvailable()) {
                            getServerData();
                            displayUpdateInformation(buildOfflineOxygenOTAUpdate(), false, false);
                        } else {
                            showNetworkError();
                        }
                    }
                });
                Button installGuideButton = (Button) updateInformationRefreshLayout.findViewById(R.id.updateInstallationInstructionsButton);
                installGuideButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).getActivityLauncher().UpdateInstructions(checkIfUpdateIsDownloaded(oxygenOTAUpdate));
                        hideDownloadCompleteNotification();
                    }
                });
                updateInformationRefreshLayout.setColorSchemeResources(R.color.oneplus_red, R.color.holo_orange_light, R.color.holo_red_light);
            }
            if(systemIsUpToDateRefreshLayout != null) {
                systemIsUpToDateRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        if (networkConnectionManager.checkNetworkConnection()) {
                            getServerData();
                        } else if (settingsManager.checkIfCacheIsAvailable()) {
                            getServerData();
                            displayUpdateInformation(buildOfflineOxygenOTAUpdate(), false, false);
                        } else {
                            showNetworkError();
                        }
                    }
                });
                systemIsUpToDateRefreshLayout.setColorSchemeResources(R.color.oneplus_red, R.color.holo_orange_light, R.color.holo_red_light);
            }
        }
    }

    /**
     * Checks if there is a network connection. If that's the case, start the connections to the backend
     * If not, build an offline {@link OxygenOTAUpdate} and display it without trying to reach the server at all
     * If an offline {@link OxygenOTAUpdate} is not available, display a "No network connection error message".
     */
    private void initData() {
        if (!isFetched && settingsManager.checkIfSetupScreenHasBeenCompleted()) {
            if (networkConnectionManager.checkNetworkConnection()) {
                getServerData();
                showAds();
                refreshedDate = DateTime.now();
                isFetched = true;
            } else if (settingsManager.checkIfCacheIsAvailable()) {
                getServerData();
                oxygenOTAUpdate = buildOfflineOxygenOTAUpdate();
                displayUpdateInformation(oxygenOTAUpdate, false, false);
                initDownloadManager();
                hideAds();
                refreshedDate = DateTime.now();
                isFetched = true;
            } else {
                hideAds();
                showNetworkError();
            }
        }
    }

    /**
     * Fetches all server data. This includes update information, server messages and server status checks
     */
    private void getServerData() {
        this.inAppMessageBarData = new HashMap<>();
        new GetServerData().execute(new Callback() {
            @Override
            public void onActionPerformed(Object... result) {
                System.err.println("hey");
            }
        });
    }


    /*
      -------------- METHODS FOR DISPLAYING DATA ON THE FRAGMENT -------------------
     */


    private int addMessageBar(ServerMessageBar view, int numberOfBars) {
        // Add the message to the update information screen if it is not null.
        if(this.rootView != null) {
            // Set the layout params based on the view count.
            // First view should go below the app update message bar (if visible)
            // Consecutive views should go below their parent / previous view.
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.topMargin = numberOfBars * diPToPixels(20);
            view.setId((numberOfBars * 20000000 + 1));
            this.rootView.addView(view, params);
            numberOfBars = numberOfBars + 1;
            this.inAppMessageBars.add(view);
        }
        return numberOfBars;
    }

    private void deleteAllInAppMessageBars(List<ServerMessageBar> inAppMessageBars) {
        for(ServerMessageBar view : inAppMessageBars) {
            if(view != null && isAdded()) {
                this.rootView.removeView(view);
            }
        }
        this.inAppMessageBars = new ArrayList<>();
    }

    protected void displayInAppMessageBars(List<Banner> banners) {

        deleteAllInAppMessageBars(this.inAppMessageBars);

        int numberOfBars = 0;

        List<ServerMessageBar> createdServerMessageBars = new ArrayList<>();

        for(Banner banner : banners) {
            ServerMessageBar bar = new ServerMessageBar(getApplicationContext(), null);
            View backgroundBar = bar.getBackgroundBar();
            TextView textView = bar.getTextView();

            backgroundBar.setBackgroundColor(banner.getColor(context));
            textView.setText(banner.getBannerText(context));

            if(banner.getBannerText(context) instanceof Spanned) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }

            numberOfBars = addMessageBar(bar, numberOfBars);
            createdServerMessageBars.add(bar);
        }

        // Set the margins of the app ui to be below the last added server message bar.
        if(!createdServerMessageBars.isEmpty() && isAdded()) {
            View lastServerMessageView = createdServerMessageBars.get(createdServerMessageBars.size() - 1);
            if (lastServerMessageView != null) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                params.addRule(BELOW, lastServerMessageView.getId());
                if(adView != null) {
                    params.addRule(ABOVE, adView.getId());
                }

                if(systemIsUpToDateRefreshLayout != null) {
                    systemIsUpToDateRefreshLayout.setLayoutParams(params);
                }
                if(updateInformationRefreshLayout != null) {
                    updateInformationRefreshLayout.setLayoutParams(params);
                }
            }
        }
        this.inAppMessageBars = createdServerMessageBars;
    }

    /**
     * Displays the update information from a {@link OxygenOTAUpdate} with update information.
     * @param oxygenOTAUpdate Update information to display
     * @param online Whether or not the device has an active network connection
     * @param displayInfoWhenUpToDate Flag set to show update information anyway, even if the system is up to date.
     */
    @Override
    public void displayUpdateInformation(final OxygenOTAUpdate oxygenOTAUpdate, final boolean online, boolean displayInfoWhenUpToDate) {
        // Abort if no update data is found or if the fragment is not attached to its activity to prevent crashes.
        if(!isAdded() || rootView == null) {
            return;
        }

        View loadingScreen = rootView.findViewById(R.id.updateInformationLoadingScreen);
        if(loadingScreen != null) {
            loadingScreen.setVisibility(GONE);
        }

        if(oxygenOTAUpdate == null) {
            return;
        }

        if(!oxygenOTAUpdate.isSystemIsUpToDateCheck()) {
            oxygenOTAUpdate.setSystemIsUpToDate(isSystemUpToDateStringCheck(oxygenOTAUpdate));
        }

        if(((oxygenOTAUpdate.isSystemIsUpToDate(settingsManager)) && !displayInfoWhenUpToDate) || !oxygenOTAUpdate.isUpdateInformationAvailable()) {
            displayUpdateInformationWhenUpToDate(oxygenOTAUpdate, online);
        } else {
            displayUpdateInformationWhenNotUpToDate(oxygenOTAUpdate, online, displayInfoWhenUpToDate);
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
            updateInformationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    displayUpdateInformation(oxygenOTAUpdate, true, true);
                }
            });
        }

        // Save last time checked if online.
        if(online) {
            settingsManager.savePreference(PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString());
        }

        // Show last time checked.
        TextView dateCheckedView = (TextView) rootView.findViewById(R.id.updateInformationSystemIsUpToDateDateTextView);
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatter(context, this);
        dateCheckedView.setText(String.format(getString(R.string.update_information_last_checked_on), dateTimeFormatter.formatDateTime((String) settingsManager.getPreference(PROPERTY_UPDATE_CHECKED_DATE))));

    }

    private void displayUpdateInformationWhenNotUpToDate(final OxygenOTAUpdate oxygenOTAUpdate, boolean online, boolean displayInfoWhenUpToDate) {
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

        final Button downloadButton = (Button) rootView.findViewById(R.id.updateInformationDownloadButton);

        // Activate download button, or make it gray when the device is offline or if the update is not downloadable.
        if (online && oxygenOTAUpdate.getDownloadUrl() != null) {
            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDownloadButtonClick(downloadButton);
                }
            });
            downloadButton.setEnabled(true);
            downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));
        } else {
            downloadButton.setEnabled(false);
            downloadButton.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
        }

        // Format top title based on system version installed.
        TextView headerLabel = (TextView) rootView.findViewById(R.id.headerLabel);
        Button updateInstallationGuideButton = (Button) rootView.findViewById(R.id.updateInstallationInstructionsButton);
        View downloadSizeTable = rootView.findViewById(R.id.buttonTable);
        View downloadSizeImage = rootView.findViewById(R.id.downloadSizeImage);


        if(displayInfoWhenUpToDate) {
            headerLabel.setText(getString(R.string.update_information_installed_update));
            downloadButton.setVisibility(GONE);
            updateInstallationGuideButton.setVisibility(GONE);
            fileNameView.setVisibility(GONE);
            downloadSizeTable.setVisibility(GONE);
            downloadSizeImage.setVisibility(GONE);
            downloadSizeView.setVisibility(GONE);
        } else {
            if(oxygenOTAUpdate.isSystemIsUpToDateCheck()) {
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

    /**
     * Builds a {@link OxygenOTAUpdate} class based on the data that was stored when the device was online.
     * @return OxygenOTAUpdate with data from the latest succesful fetch.
     */
    @Override
    protected OxygenOTAUpdate buildOfflineOxygenOTAUpdate() {
        OxygenOTAUpdate oxygenOTAUpdate = new OxygenOTAUpdate();
        oxygenOTAUpdate.setVersionNumber((String) settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_NAME));
        oxygenOTAUpdate.setDownloadSize((int) settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE));
        oxygenOTAUpdate.setDescription((String) settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DESCRIPTION));
        oxygenOTAUpdate.setUpdateInformationAvailable((boolean) settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE));
        oxygenOTAUpdate.setFilename((String) settingsManager.getPreference(PROPERTY_OFFLINE_FILE_NAME));
        return oxygenOTAUpdate;
    }

    /**
     * Additional check if system is up to date by comparing version Strings.
     * This is needed to show the "System is up to date" message for full updates as incremental (parent) versions are not checked there.
     * @param oxygenOTAUpdate OxygenOTAUpdate that needs to be checked against the current version.
     * @return True if the system is up to date, false if not.
     */
    private boolean isSystemUpToDateStringCheck(OxygenOTAUpdate oxygenOTAUpdate) {
        if(settingsManager.getPreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true)) {
            // This grabs the Oxygen OS version from build.prop. As there is no direct SDK way to do this, it has to be done in this way.
            SystemVersionProperties systemVersionProperties = ((ApplicationContext)getActivity().getApplication()).getSystemVersionProperties();

            String oxygenOSVersion = systemVersionProperties.getOxygenOSVersion();
            String newOxygenOSVersion = oxygenOTAUpdate.getVersionNumber();

            if(newOxygenOSVersion == null || newOxygenOSVersion.isEmpty()) {
                return false;
            }

            if (oxygenOSVersion == null || oxygenOSVersion.isEmpty() || oxygenOSVersion.equals(NO_OXYGEN_OS)) {
                return false;
            } else {
                if (newOxygenOSVersion.equals(oxygenOSVersion)) {
                    return true;
                } else {
                    // remove incremental version naming.
                    newOxygenOSVersion = newOxygenOSVersion.replace(" Incremental", "");
                    if (newOxygenOSVersion.equals(oxygenOSVersion)) {
                        return true;
                    } else {
                        newOxygenOSVersion = newOxygenOSVersion.replace("-", " ");
                        oxygenOSVersion = oxygenOSVersion.replace("-", " ");
                        return newOxygenOSVersion.contains(oxygenOSVersion);
                    }
                }
            }
        }
        else {
            return false; // Always show update info if user does not want to see if system is up to date.
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
    @Override
    protected void initDownloadManager() {
        if(isAdded() && updateDownloader == null) {
            updateDownloader = new UpdateDownloader(getActivity())
                    .setUpdateDownloadListener(new UpdateDownloadListener() {
                        @Override
                        public void onDownloadManagerInit() {
                            getDownloadCancelButton().setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    updateDownloader.cancelDownload();
                                }
                            });
                        }

                        @Override
                        public void onDownloadStarted(long downloadID) {
                            if(isAdded()) {
                                getDownloadButton().setText(getString(R.string.downloading));
                                getDownloadButton().setClickable(false);

                                showDownloadProgressBar();
                                getDownloadProgressBar().setIndeterminate(false);
                            }
                        }

                        @Override
                        public void onDownloadPending() {
                            if(isAdded()) {
                                showDownloadProgressBar();
                                getDownloadButton().setText(getString(R.string.downloading));
                                getDownloadButton().setClickable(false);
                                TextView downloadStatusText = getDownloadStatusText();
                                downloadStatusText.setText(getString(R.string.download_pending));
                            }
                        }

                        @Override
                        public void onDownloadProgressUpdate(DownloadProgressData downloadProgressData) {
                            if(isAdded()) {
                                showDownloadProgressBar();
                                getDownloadButton().setText(getString(R.string.downloading));
                                getDownloadButton().setClickable(false);
                                getDownloadProgressBar().setIndeterminate(false);
                                getDownloadProgressBar().setProgress(downloadProgressData.getProgress());

                                if(downloadProgressData.getDownloadSpeed() == NOT_SET || downloadProgressData.getTimeRemaining() == null) {
                                    getDownloadStatusText().setText(getString(R.string.download_progress_text_unknown_time_remaining, downloadProgressData.getProgress()));
                                } else {
                                    DownloadProgressData.TimeRemaining timeRemaining = downloadProgressData.getTimeRemaining();

                                    if(timeRemaining.getHoursRemaining() > 1) {
                                        getDownloadStatusText().setText(getString(R.string.download_progress_text_hours_remaining, downloadProgressData.getProgress(), timeRemaining.getHoursRemaining()));
                                    } else if(timeRemaining.getHoursRemaining() == 1) {
                                        getDownloadStatusText().setText(getString(R.string.download_progress_text_one_hour_remaining, downloadProgressData.getProgress()));
                                    } else if(timeRemaining.getHoursRemaining() == 0 && timeRemaining.getMinutesRemaining() > 1) {
                                        getDownloadStatusText().setText(getString(R.string.download_progress_text_minutes_remaining, downloadProgressData.getProgress(), timeRemaining.getMinutesRemaining()));
                                    } else if(timeRemaining.getHoursRemaining() == 0 && timeRemaining.getMinutesRemaining() == 1) {
                                        getDownloadStatusText().setText(getString(R.string.download_progress_text_one_minute_remaining, downloadProgressData.getProgress()));
                                    } else if(timeRemaining.getHoursRemaining() == 0 && timeRemaining.getMinutesRemaining() == 0 && timeRemaining.getSecondsRemaining() > 10) {
                                        getDownloadStatusText().setText(getString(R.string.download_progress_text_less_than_a_minute_remaining, downloadProgressData.getProgress()));
                                    } else if(timeRemaining.getHoursRemaining() == 0 && timeRemaining.getMinutesRemaining() == 0 && timeRemaining.getSecondsRemaining() <= 10) {
                                        getDownloadStatusText().setText(getString(R.string.download_progress_text_seconds_remaining, downloadProgressData.getProgress()));
                                    }
                                }
                            }
                        }

                        @Override
                        public void onDownloadPaused(int statusCode) {
                            if(isAdded()) {
                                showDownloadProgressBar();
                                getDownloadButton().setText(getString(R.string.downloading));
                                getDownloadButton().setClickable(false);

                                TextView downloadStatusText = getDownloadStatusText();
                                switch (statusCode) {
                                    case PAUSED_QUEUED_FOR_WIFI:
                                        downloadStatusText.setText(getString(R.string.download_waiting_for_wifi));
                                        break;
                                    case PAUSED_WAITING_FOR_NETWORK:
                                        downloadStatusText.setText(getString(R.string.download_waiting_for_network));
                                        break;
                                    case PAUSED_WAITING_TO_RETRY:
                                        downloadStatusText.setText(getString(R.string.download_will_retry_soon));
                                        break;
                                    case PAUSED_UNKNOWN:
                                        downloadStatusText.setText(getString(R.string.download_paused_unknown));
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
                                getDownloadButton().setClickable(true);
                                getDownloadButton().setText(getString(R.string.download));
                                hideDownloadProgressBar();
                            }
                        }

                        @Override
                        public void onDownloadError(int statusCode) {
                            // Treat any HTTP status code exception (lower than 1000) as a network error.
                            // Handle any other errors according to the error message.
                            if(isAdded()) {
                                if (statusCode < 1000) {
                                    showDownloadError(getString(R.string.download_error), getString(R.string.download_error_network));
                                    showDownloadFailedNotification(getString(R.string.download_error), getString(R.string.download_error_network), getString(R.string.download_notification_error_network));
                                } else {
                                    switch (statusCode) {
                                        case ERROR_UNHANDLED_HTTP_CODE:
                                        case ERROR_HTTP_DATA_ERROR:
                                        case ERROR_TOO_MANY_REDIRECTS:
                                            showDownloadError(getString(R.string.download_error), getString(R.string.download_error_network));
                                            showDownloadFailedNotification(getString(R.string.download_error), getString(R.string.download_error_network), getString(R.string.download_notification_error_network));
                                            break;
                                        case ERROR_FILE_ERROR:
                                            updateDownloader.makeDownloadDirectory();
                                            showDownloadError(getString(R.string.download_error), getString(R.string.download_error_directory));
                                            showDownloadFailedNotification(getString(R.string.download_error), getString(R.string.download_error_directory), getString(R.string.download_notification_error_storage_not_found));
                                            break;
                                        case ERROR_INSUFFICIENT_SPACE:
                                            showDownloadError(getString(R.string.download_error), getString(R.string.download_error_storage));
                                            showDownloadFailedNotification(getString(R.string.download_error), getString(R.string.download_error_storage), getString(R.string.download_notification_error_storage_full));
                                            break;
                                        case ERROR_DEVICE_NOT_FOUND:
                                            showDownloadError(getString(R.string.download_error), getString(R.string.download_error_sd_card));
                                            showDownloadFailedNotification(getString(R.string.download_error), getString(R.string.download_error_sd_card), getString(R.string.download_notification_error_sd_card_missing));
                                            break;
                                        case ERROR_CANNOT_RESUME:
                                            updateDownloader.cancelDownload();
                                            if (networkConnectionManager.checkNetworkConnection() && oxygenOTAUpdate != null && oxygenOTAUpdate.getDownloadUrl() != null) {
                                                updateDownloader.downloadUpdate(oxygenOTAUpdate);
                                            }
                                            break;
                                        case ERROR_FILE_ALREADY_EXISTS:
                                            Toast.makeText(getApplicationContext(), getString(R.string.download_already_downloaded), Toast.LENGTH_LONG).show();
                                            onUpdateDownloaded(true, false);
                                    }
                                }

                                // Make sure the failed download file gets deleted before the user tries to download it again.
                                updateDownloader.cancelDownload();
                                hideDownloadProgressBar();
                                getDownloadButton().setText(getString(R.string.download));
                                onUpdateDownloaded(false, true);
                            }
                        }

                        @Override
                        public void onVerifyStarted() {
                            if(isAdded()) {
                                showDownloadProgressBar();
                                getDownloadProgressBar().setIndeterminate(true);
                                showVerifyingNotification(false);
                                getDownloadButton().setText(getString(R.string.download_verifying));
                                getDownloadStatusText().setText(getString(R.string.download_progress_text_verifying));
                            }
                        }

                        @Override
                        public void onVerifyError() {
                            if(isAdded()) {
                                showDownloadError(getString(R.string.download_error), getString(R.string.download_error_corrupt));
                                showDownloadFailedNotification(getString(R.string.download_error), getString(R.string.download_error_corrupt), getString(R.string.download_notification_error_corrupt));
                                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + oxygenOTAUpdate.getFilename());
                                try {
                                    //noinspection ResultOfMethodCallIgnored
                                    file.delete();
                                } catch (Exception ignored) {

                                }
                                showVerifyingNotification(true);
                            }
                        }

                        @Override
                        public void onVerifyComplete() {
                            if(isAdded()) {
                                hideDownloadProgressBar();
                                hideVerifyingNotification();
                                onUpdateDownloaded(true, false);
                                Toast.makeText(getApplicationContext(), getString(R.string.download_complete), Toast.LENGTH_LONG).show();
                                showDownloadCompleteNotification();
                            }
                        }
                    });
            updateDownloader.checkDownloadProgress(oxygenOTAUpdate);
        }
    }

    /**
     * Shows an {@link MessageDialog} with the occured download error.
     * @param title Title of the error message
     * @param message Contents of the error message
     */
    private void showDownloadError(String title, String message) {
        MessageDialog errorDialog = new MessageDialog()
                .setTitle(title)
                .setMessage(message)
                .setPositiveButtonText(getString(R.string.download_error_close))
                .setNegativeButtonText(getString(R.string.download_error_retry))
                .setClosable(true)
                .setDialogListener(new MessageDialog.DialogListener() {
                    @Override
                    public void onDialogPositiveButtonClick(DialogFragment dialogFragment) {
                        hideDownloadCompleteNotification();
                    }

                    @Override
                    public void onDialogNegativeButtonClick(DialogFragment dialogFragment) {
                        hideDownloadCompleteNotification();
                        updateDownloader.cancelDownload();
                        updateDownloader.downloadUpdate(oxygenOTAUpdate);
                    }
                });
        errorDialog.setTargetFragment(this, 0);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(errorDialog, "DownloadError");
        transaction.commitAllowingStateLoss();
    }

    private void showDownloadFailedNotification(String title, String message, String notificationMessage) {
        // If the download complete notification is clicked, hide the first page of the install guide.
        Intent resultIntent = new Intent(getActivity(), MainActivity.class);
        resultIntent.putExtra(KEY_HAS_DOWNLOAD_ERROR, true);
        resultIntent.putExtra(KEY_DOWNLOAD_ERROR_TITLE, title);
        resultIntent.putExtra(KEY_DOWNLOAD_ERROR_MESSAGE, message);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());
        // Adds the back stack
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(false)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setContentTitle(getString(R.string.download_failed))
                .setContentText(notificationMessage);

        if (Build.VERSION.SDK_INT >= 21) {
            builder.setCategory(Notification.CATEGORY_SYSTEM);
        }

        NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(DOWNLOAD_COMPLETE_NOTIFICATION_ID, builder.build());
    }

    /**
     * Common actions for changing download button parameters and deleting incomplete download files.
     * @param updateIsDownloaded Whether or not the update is successfully downloaded.
     * @param fileMayBeDeleted Whether or not the update file may be deleted.
     */
    private void onUpdateDownloaded(boolean updateIsDownloaded, boolean fileMayBeDeleted) {
        final Button downloadButton = getDownloadButton();

        if(updateIsDownloaded && isAdded()) {
            downloadButton.setEnabled(true);
            downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));
            downloadButton.setClickable(true);
            downloadButton.setText(getString(R.string.downloaded));
            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onDownloadedButtonClick();
                }
            });
        } else {
            if (networkConnectionManager != null && networkConnectionManager.checkNetworkConnection() && oxygenOTAUpdate != null && oxygenOTAUpdate.getDownloadUrl() != null && isAdded()) {
                if(updateDownloader != null) {
                    updateDownloader.checkDownloadProgress(oxygenOTAUpdate);
                } else {
                    initDownloadManager();
                }
                downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDownloadButtonClick(downloadButton);
                    }
                });
                downloadButton.setEnabled(true);
                downloadButton.setTextColor(ContextCompat.getColor(context, R.color.oneplus_red));

                if(fileMayBeDeleted) {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + oxygenOTAUpdate.getFilename());
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            } else {
                if(isAdded()) {
                    downloadButton.setEnabled(false);
                    downloadButton.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                }
            }
        }
    }

    /**
     * Download button click listener. Performs these actions when the button is clicked.
     * @param downloadButton Button to perform actions on.
     */
    private void onDownloadButtonClick(Button downloadButton) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity != null) {
            if(mainActivity.hasDownloadPermissions()) {
                if(updateDownloader != null) {
                    updateDownloader.downloadUpdate(oxygenOTAUpdate);
                    downloadButton.setText(getString(R.string.downloading));
                    downloadButton.setClickable(false);
                }
            } else {
                Callback callback = new Callback() {
                    @Override
                    public void onActionPerformed(Object... result) {
                        if((int)result[0] == PackageManager.PERMISSION_GRANTED && updateDownloader != null && oxygenOTAUpdate != null) {
                            updateDownloader.downloadUpdate(oxygenOTAUpdate);
                        }
                    }
                };
                mainActivity.requestDownloadPermissions(callback);
            }
        }
    }

    /**
     * Allows an already downloaded update file to be deleted to save storage space.
     */
    private void onDownloadedButtonClick() {
        MessageDialog dialog = new MessageDialog()
                .setTitle(getString(R.string.delete_message_title))
                .setMessage(getString(R.string.delete_message_contents))
                .setClosable(true)
                .setPositiveButtonText(getString(R.string.download_error_close))
                .setNegativeButtonText(getString(R.string.delete_message_delete_button))
                .setDialogListener(new MessageDialog.DialogListener() {
                    @Override
                    public void onDialogPositiveButtonClick(DialogFragment dialogFragment) {

                    }

                    @Override
                    public void onDialogNegativeButtonClick(DialogFragment dialogFragment) {
                        if(oxygenOTAUpdate != null) {
                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + oxygenOTAUpdate.getFilename());
                            if(file.exists()) {
                                if(file.delete()) {
                                    getDownloadButton().setText(getString(R.string.download));
                                    checkIfUpdateIsAlreadyDownloaded(oxygenOTAUpdate);
                                }
                            }
                        }
                    }
                });
        dialog.setTargetFragment(this, 0);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(dialog, "DeleteDownload");
        transaction.commitAllowingStateLoss();
    }

    /**
     * Checks if an update file is already downloaded.
     * @param oxygenOTAUpdate Oxygen OTA Update data containing the file name of the update.
     */
    @Override
    public void checkIfUpdateIsAlreadyDownloaded(OxygenOTAUpdate oxygenOTAUpdate) {
        if(oxygenOTAUpdate != null) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + oxygenOTAUpdate.getFilename());
            onUpdateDownloaded(file.exists() && !settingsManager.containsPreference(PROPERTY_DOWNLOAD_ID), false);
        }
    }

    public boolean checkIfUpdateIsDownloaded(OxygenOTAUpdate oxygenOTAUpdate) {
        if (oxygenOTAUpdate == null) return false;
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + oxygenOTAUpdate.getFilename());
        return (file.exists() && !settingsManager.containsPreference(PROPERTY_DOWNLOAD_ID));
    }


    /*
      -------------- UPDATE VERIFICATION METHODS -------------------
     */


    /**
     * Shows a notification that the downloaded update file is being verified on MD5 sums.
     * @param error If an error occurred during verification, display an error text in the notification.
     */
    private void showVerifyingNotification(boolean error) {
        NotificationCompat.Builder builder;
        try {
            builder = new NotificationCompat.Builder(getActivity())
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .setProgress(100, 50, true);

            if(error) {
                builder.setContentTitle(getString(R.string.download_verifying_error));
            } else {
                builder.setContentTitle(getString(R.string.download_verifying));
            }

            if (Build.VERSION.SDK_INT >= 21) {
                builder.setCategory(Notification.CATEGORY_PROGRESS);
            }
            NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(NOTIFICATION_ID, builder.build());
        } catch(Exception e) {
            try {
                NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancel(NOTIFICATION_ID);
            } catch(Exception e1) {
                try {
                    // If cancelling the notification fails fails (and yes, it happens!), then I assume either that the user's device has a (corrupt) custom firmware or that something is REALLY going wrong.
                    // We try it once more but now with the Application Context instead of the Activity.
                    NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(NOTIFICATION_ID);
                } catch (Exception ignored) {
                    // If the last attempt has also failed, well then there's no hope.
                    // We leave everything as is, but the user will likely be stuck with a verifying notification that stays until a reboot.
                }
            }
        }
    }

    /**
     * Shows a notification that the downloaded update file is downloaded successfully.
\     */
    private void showDownloadCompleteNotification() {
        try {
            // If the download complete notification is clicked, hide the first page of the install guide.
            Intent resultIntent = new Intent(getActivity(), InstallGuideActivity.class);
            resultIntent.putExtra(INTENT_SHOW_DOWNLOAD_PAGE, false);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());
            // Adds the back stack
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            // Gets a PendingIntent containing the entire back stack
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.download_complete_notification));

            if (Build.VERSION.SDK_INT >= 21) {
                builder.setCategory(Notification.CATEGORY_SYSTEM);
            }

            NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(DOWNLOAD_COMPLETE_NOTIFICATION_ID, builder.build());
        } catch(Exception ignored) {

        }
    }

    /**
     * Hides the verifying notification. Used when verification has succeeded.
     */
    private void hideVerifyingNotification() {
        NotificationManager manager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);
    }

    /**
     * Hides the download complete notification. Used when the install guide is manually clicked from within the app.
     */
    private void hideDownloadCompleteNotification() {
        NotificationManager manager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(DOWNLOAD_COMPLETE_NOTIFICATION_ID);
    }

    /**
     * Converts DiP units to pixels
     */
    private int diPToPixels(int numberOfPixels) {
        if(getActivity() != null && getActivity().getResources() != null) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    numberOfPixels,
                    getActivity().getResources().getDisplayMetrics()
            );
        } else {
            return 0;
        }
    }
}