package com.arjanvlek.oxygenupdater.views;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

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

import org.joda.time.LocalDateTime;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_ADVANCED_MODE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_AD_FREE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_NOTIFICATION_TOPIC;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SETUP_DONE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_CHECKED_DATE;


@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements ActionBar.TabListener {

    private ViewPager mViewPager;
    private SettingsManager settingsManager;
    private ActivityLauncher activityLauncher;
    private Consumer<Boolean> downloadPermissionCallback;

    public static final int PAGE_NEWS = 0;
    public static final int PAGE_UPDATE_INFORMATION = 1;
    public static final int PAGE_DEVICE_INFORMATION = 2;


    // Permissions constants
    public final static String DOWNLOAD_FILE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    public final static String VERIFY_FILE_PERMISSION = "android.permission.READ_EXTERNAL_STORAGE";
    public final static int PERMISSION_REQUEST_CODE = 200;

    public static final String INTENT_START_PAGE = "start_page";
    private InterstitialAd newsAd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity);
        Context context = getApplicationContext();
        settingsManager = new SettingsManager(context);

        // App version 2.4.6: Migrated old setting Show if system is up to date (default: ON) to Advanced mode (default: OFF).
        if (settingsManager.containsPreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE)) {
            settingsManager.savePreference(PROPERTY_ADVANCED_MODE, !settingsManager.getPreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true));
            settingsManager.deletePreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE);
        }

        // Supported device check
        if (!settingsManager.getPreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)) {
            ApplicationData applicationData = ((ApplicationData) getApplication());
            applicationData.getServerConnector().getDevices(result -> {
                if (!Utils.isSupportedDevice(applicationData.getSystemVersionProperties(), result)) {
                    displayUnsupportedDeviceMessage();
                }
            });
        }

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }
        setTitle(getString(R.string.app_name));

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = findViewById(R.id.mainActivityPager);

        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
            mViewPager.setOffscreenPageLimit(2); // Max is 2 when on Device Info or on News.
            mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    if (actionBar != null) {
                        actionBar.setSelectedNavigationItem(position);
                    }
                }
            });
        }


        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Creates a tab with text corresponding to the page title defined by
            // the adapter.
            //noinspection ConstantConditions
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        this.activityLauncher = new ActivityLauncher(this);

        // Set start page to Update Information Screen (middle page).
        try {
            int startPage = PAGE_UPDATE_INFORMATION;

            if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(INTENT_START_PAGE)) {
                startPage = getIntent().getExtras().getInt(INTENT_START_PAGE);
            }

            mViewPager.setCurrentItem(startPage);
        } catch (IndexOutOfBoundsException ignored) {

        }

        if (((ApplicationData) getApplication()).checkPlayServices(this, false)) {
            MobileAds.initialize(this, "ca-app-pub-0760639008316468~7665206420");
        } else {
            Toast.makeText(getApplication(), getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show();
        }

        if (!settingsManager.getPreference(PROPERTY_AD_FREE, false)) {
            this.newsAd = new InterstitialAd(this);
            this.newsAd.setAdUnitId(getString(R.string.news_ad_unit_id));
            this.newsAd.loadAd(((ApplicationData) getApplication()).buildAdRequest());
        }

        // Support functions for Android 8.0 "Oreo" and up.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createPushNotificationChannel();
            createProgressNotificationChannel();
        }

        // Remove "long" download ID in vavor of "int" id
        try {
            //noinspection unused var is needed to cast class
            int downloadId = settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, -1);
        } catch (ClassCastException e) {
            settingsManager.deletePreference(PROPERTY_DOWNLOAD_ID);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Mark the welcome tutorial as finished if the user is moving from older app version. This is checked by either having stored update information for offline viewing, or if the last update checked date is set (if user always had up to date system and never viewed update information before).
        if (!settingsManager.getPreference(PROPERTY_SETUP_DONE, false) && (settingsManager.checkIfOfflineUpdateDataIsAvailable() || settingsManager.containsPreference(PROPERTY_UPDATE_CHECKED_DATE))) {
            settingsManager.savePreference(PROPERTY_SETUP_DONE, true);
        }

        // Show the welcome tutorial if the app needs to be set up.
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handles action bar item clicks.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            activityLauncher.Settings();
            return true;
        }
        if (id == R.id.action_about) {
            activityLauncher.About();
            return true;
        }

        if (id == R.id.action_help) {
            activityLauncher.Help();
            return true;
        }

        if (id == R.id.action_faq) {
            activityLauncher.FAQ();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Action when clicked on a tab.
     * @param tab Tab which is selected
     * @param fragmentTransaction Android Fragment Transaction, unused here.
     */
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public void displayUnsupportedDeviceMessage() {
        View checkBoxView = View.inflate(MainActivity.this, R.layout.message_dialog_checkbox, null);
        final CheckBox checkBox = checkBoxView.findViewById(R.id.unsupported_device_warning_checkbox);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(checkBoxView);
        builder.setTitle(getString(R.string.unsupported_device_warning_title));
        builder.setMessage(getString(R.string.unsupported_device_warning_message));

        builder.setPositiveButton(getString(R.string.download_error_close), (dialog, which) -> {
            settingsManager.savePreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, checkBox.isChecked());
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
            return this.newsAd;
        } else if (mayShowNewsAd()) {
            InterstitialAd interstitialAd = new InterstitialAd(this);
            interstitialAd.setAdUnitId(getString(R.string.news_ad_unit_id));
            interstitialAd.loadAd(((ApplicationData) getApplication()).buildAdRequest());
            this.newsAd = interstitialAd;
            return this.newsAd;
        } else {
            return null;
        }
    }

    public boolean mayShowNewsAd() {
        return !settingsManager.getPreference(PROPERTY_AD_FREE, false) &&
                LocalDateTime.parse(settingsManager.getPreference(PROPERTY_LAST_NEWS_AD_SHOWN, "1970-01-01T00:00:00.000"))
                        .isBefore(LocalDateTime.now().minusMinutes(5));

    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a FragmentBuilder (defined as a static inner class below).
            return FragmentBuilder.newInstance(position);
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
                    return getString(R.string.update_information_header_short) ;
                case PAGE_DEVICE_INFORMATION:
                    return getString(R.string.device_information_header_short);
            }
            return null;
        }
    }

    /**
     * An inner class that constructs the fragments used in this application.
     */
    public static class FragmentBuilder {

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        static Fragment newInstance(int sectionNumber) {
            if (sectionNumber == PAGE_NEWS) {
                return new NewsFragment();
            }
            if (sectionNumber == PAGE_UPDATE_INFORMATION) {
                return new UpdateInformationFragment();
            }
            if (sectionNumber == PAGE_DEVICE_INFORMATION) {
                return new DeviceInformationFragment();
            }
            return null;
        }
    }

    public ActivityLauncher getActivityLauncher() {
        return this.activityLauncher;
    }

    // Android 6.0 Run-time permissions

    public void requestDownloadPermissions(@NonNull Consumer<Boolean> callback) {
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            this.downloadPermissionCallback = callback;
            requestPermissions(new String[]{DOWNLOAD_FILE_PERMISSION, VERIFY_FILE_PERMISSION}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int  permsRequestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (permsRequestCode) {
            case PERMISSION_REQUEST_CODE:
                if (this.downloadPermissionCallback != null && grantResults.length > 0) {
                    this.downloadPermissionCallback.accept(grantResults[0] == PackageManager.PERMISSION_GRANTED);
                }

        }
    }

    public boolean hasDownloadPermissions() {
        return ContextCompat.checkSelfPermission(getApplication(), VERIFY_FILE_PERMISSION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getApplication(), DOWNLOAD_FILE_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    // Android 8.0 Notification Channels

    private void createPushNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            // Unsupported on older Android versions
            return;
        }

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

    private void createProgressNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            // Unsupported on older Android versions
            return;
        }

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
}
