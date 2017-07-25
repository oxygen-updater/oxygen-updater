package com.arjanvlek.oxygenupdater.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import com.arjanvlek.oxygenupdater.ActivityLauncher;
import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.deviceinformation.DeviceInformationFragment;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.news.NewsFragment;
import com.arjanvlek.oxygenupdater.notifications.MessageDialog;
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateInformationFragment;
import com.google.android.gms.ads.InterstitialAd;

import org.joda.time.LocalDateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_NOTIFICATION_TOPIC;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SETUP_DONE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_CHECKED_DATE;


@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements ActionBar.TabListener {

    private ViewPager mViewPager;
    private SettingsManager settingsManager;
    private ActivityLauncher activityLauncher;
    private Consumer<Integer> callback;

    public static final int PAGE_NEWS = 0;
    public static final int PAGE_UPDATE_INFORMATION = 1;
    public static final int PAGE_DEVICE_INFORMATION = 2;


    // Permissions constants
    public final static String DOWNLOAD_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    public final static int PERMISSION_REQUEST_CODE = 200;

    public static final String INTENT_START_PAGE = "start_page";
    private InterstitialAd newsAd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity);
        Context context = getApplicationContext();
        settingsManager = new SettingsManager(context);

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

        mViewPager = (ViewPager) findViewById(R.id.mainActivityPager);

        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
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

        try {
            int startPage = PAGE_UPDATE_INFORMATION;

            if(getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(INTENT_START_PAGE)) {
                startPage = getIntent().getExtras().getInt(INTENT_START_PAGE);
            }

            mViewPager.setCurrentItem(startPage);
        } catch (IndexOutOfBoundsException ignored) {

        }

        this.newsAd = new InterstitialAd(this);
        this.newsAd.setAdUnitId(getString(R.string.news_ad_unit_id));
        this.newsAd.loadAd(((ApplicationData) getApplication()).buildAdRequest());
    }

    @Override
    public void onStart() {
        super.onStart();

        // Mark the welcome tutorial as finished if the user is moving from older app version. This is checked by either having stored update information for offline viewing, or if the last update checked date is set (if user always had up to date system and never viewed update information before).
        if (!settingsManager.getPreference(PROPERTY_SETUP_DONE, false) && (settingsManager.checkIfOfflineUpdateDataIsAvailable() || settingsManager.containsPreference(PROPERTY_UPDATE_CHECKED_DATE))) {
            settingsManager.savePreference(PROPERTY_SETUP_DONE, true);
        }

        // Show the welcome tutorial if the app needs to be set up.
        if(!settingsManager.getPreference(PROPERTY_SETUP_DONE, false)) {
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
        final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.unsupported_device_warning_checkbox);

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
        } else {
            InterstitialAd interstitialAd = new InterstitialAd(this);
            interstitialAd.setAdUnitId(getString(R.string.news_ad_unit_id));
            interstitialAd.loadAd(((ApplicationData) getApplication()).buildAdRequest());
            this.newsAd = interstitialAd;
            return this.newsAd;
        }
    }

    public boolean mayShowNewsAd() {
        try {
            File file = new File(getFilesDir(), ApplicationData.NEWS_ADS_SHOWN_DATE_FILENAME);
            if (!file.exists()) {
                return true; // If an ad was never shown, it's time to show one now.
            }
            ObjectInputStream stream = new ObjectInputStream(new FileInputStream(file));
            LocalDateTime lastShownDate = (LocalDateTime) stream.readObject();

            // If an ad was never shown, it's time to show one. Else, only show one ad every 5 minutes...
            return lastShownDate == null || lastShownDate.isBefore(LocalDateTime.now().minusMinutes(5));

        } catch (IOException | ClassNotFoundException e) {
            Logger.logError("MainActivity", "Failed to check whether news ads are supported: ", e);
            // If it cannot be checked, assume no ad was shown yet. Time to show an ad...
            return true;
        }
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
            boolean MorHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
            switch (position) {
                case PAGE_NEWS:
                    return getString(R.string.news);
                case PAGE_UPDATE_INFORMATION:
                    return MorHigher ? getString(R.string.update_information_header_short) : getString(R.string.update_information_header);
                case PAGE_DEVICE_INFORMATION:
                    return MorHigher ? getString(R.string.device_information_header_short) : getString(R.string.device_information_header);
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
            if(sectionNumber == PAGE_NEWS) {
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


    // Android 6.0 Run-time permissions methods

    public void requestDownloadPermissions(@NonNull Consumer<Integer> callback) {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            this.callback = callback;
            requestPermissions(new String[]{DOWNLOAD_PERMISSION}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int  permsRequestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (permsRequestCode) {
            case PERMISSION_REQUEST_CODE:
                if (this.callback != null) {
                    this.callback.accept(grantResults[0]);
                }

        }
    }

    public boolean hasDownloadPermissions() {
        //noinspection SimplifiableIfStatement Suggested fix results in code that requires API level of M or higher and is not checked against it.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (checkSelfPermission(DOWNLOAD_PERMISSION) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    public ActivityLauncher getActivityLauncher() {
        return this.activityLauncher;
    }
}
