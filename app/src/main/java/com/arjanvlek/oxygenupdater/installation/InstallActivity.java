package com.arjanvlek.oxygenupdater.installation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.download.DownloadHelper;
import com.arjanvlek.oxygenupdater.installation.automatic.InstallationStatus;
import com.arjanvlek.oxygenupdater.installation.automatic.RootInstall;
import com.arjanvlek.oxygenupdater.installation.automatic.UpdateInstallationException;
import com.arjanvlek.oxygenupdater.installation.automatic.UpdateInstaller;
import com.arjanvlek.oxygenupdater.installation.manual.InstallGuideFragment;
import com.arjanvlek.oxygenupdater.installation.manual.InstallGuidePage;
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.Worker;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.internal.root.RootAccessChecker;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;
import com.ipaulpro.afilechooser.utils.FileUtils;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.UUID;

import static com.arjanvlek.oxygenupdater.ApplicationData.NUMBER_OF_INSTALL_GUIDE_PAGES;

public class InstallActivity extends AppCompatActivity {

    private final SparseArray<InstallGuidePage> installGuideCache = new SparseArray<>();
    private final SparseArray<Bitmap> installGuideImageCache = new SparseArray<>();
    private SettingsManager settingsManager;
    private ServerConnector serverConnector;

    public static final String INTENT_SHOW_DOWNLOAD_PAGE = "show_download_page";
    public static final String INTENT_UPDATE_DATA = "update_data";
    private static final int REQUEST_FILE_PICKER = 1606;
    private static final String TAG = "InstallActivity";
    private static final String EXTENSION_ZIP = ".zip";
    private static final String PACKAGE_ID = "id";
    private static final String SETTINGS_SWITCH = "Switch";

    private boolean showDownloadPage = true;
    private UpdateData updateData;
    private int layoutId;
    private boolean rooted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsManager = new SettingsManager(getApplication());
        serverConnector = ((ApplicationData) getApplication()).getServerConnector();

        showDownloadPage = getIntent() == null || getIntent().getBooleanExtra(INTENT_SHOW_DOWNLOAD_PAGE, true);

        if (getIntent() != null) {
            updateData = getIntent().getParcelableExtra(INTENT_UPDATE_DATA);
        }

        initialize();
    }

    private void initialize() {
        setTitle(getString(R.string.install));
        setContentView(R.layout.fragment_checking_root_access);

        RootAccessChecker.checkRootAccess((isRooted) -> {
            this.rooted = isRooted;

            if (isRooted) {
                ApplicationData applicationData = (ApplicationData) getApplication();
                ServerConnector serverConnector = applicationData.getServerConnector();
                serverConnector.getServerStatus(Utils.checkNetworkConnection(getApplication()), (serverStatus -> {
                    if (serverStatus.isAutomaticInstallationEnabled()) {
                        openMethodSelectionPage();
                    } else {
                        Toast.makeText(getApplication(), getString(R.string.install_guide_automatic_install_disabled), Toast.LENGTH_LONG).show();
                        openInstallGuide();
                    }
                }));
            } else {
                Toast.makeText(getApplication(), getString(R.string.install_guide_no_root), Toast.LENGTH_LONG).show();
                openInstallGuide();
            }
        });
    }

    private void openMethodSelectionPage() {
        switchView(R.layout.fragment_choose_install_method);

        CardView automaticInstallCard = findViewById(R.id.automaticInstallCard);
        automaticInstallCard.setOnClickListener((__) -> openAutomaticInstallOptionsSelection());

        CardView manualInstallCard = findViewById(R.id.manualInstallCard);
        manualInstallCard.setOnClickListener((__) -> openInstallGuide());

    }

    private void openAutomaticInstallOptionsSelection() {
        switchView(R.layout.fragment_install_options);

        initSettingsSwitch(SettingsManager.PROPERTY_BACKUP_DEVICE, true);

        initSettingsSwitch(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false, (buttonView, isChecked) -> {
            View additionalZipFileContainer = findViewById(R.id.additionalZipContainer);
            if (isChecked) {
                additionalZipFileContainer.setVisibility(View.VISIBLE);
            } else {
                additionalZipFileContainer.setVisibility(View.GONE);
            }

            settingsManager.savePreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, isChecked);
        });

        View additionalZipFileContainer = findViewById(R.id.additionalZipContainer);
        if (settingsManager.getPreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false)) {
            additionalZipFileContainer.setVisibility(View.VISIBLE);
        } else {
            additionalZipFileContainer.setVisibility(View.GONE);
        }

        initSettingsSwitch(SettingsManager.PROPERTY_WIPE_CACHE_PARTITION, true);
        initSettingsSwitch(SettingsManager.PROPERTY_REBOOT_AFTER_INSTALL, true);

        ImageButton filePickerButton = findViewById(R.id.additionalZipFilePickButton);
        filePickerButton.setOnClickListener((view) -> {

            // Implicitly allow the user to select a particular kind of data
            final Intent intent = new Intent(getApplicationContext(), com.ipaulpro.afilechooser.FileChooserActivity.class);
            // The MIME data type filter
            intent.setType("application/zip");
            // Only return URIs that can be opened with ContentResolver
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            startActivityForResult(intent, REQUEST_FILE_PICKER);
        });

        displayZipFilePath();

        ImageButton clearFileButton = findViewById(R.id.additionalZipFileClearButton);
        clearFileButton.setOnClickListener((view) -> {
            settingsManager.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null);
            displayZipFilePath();
        });

        findViewById(R.id.startInstallButton).setOnClickListener((view) -> {

            String additionalZipFilePath = settingsManager.getPreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null);

            if (settingsManager.getPreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false) && additionalZipFilePath == null) {
                Toast.makeText(getApplication(), R.string.install_guide_zip_file_missing, Toast.LENGTH_LONG).show();
                return;
            }

            if (additionalZipFilePath != null) {
                File file = new File(additionalZipFilePath);
                if (!file.exists()) {
                    Toast.makeText(getApplication(), R.string.install_guide_zip_file_deleted, Toast.LENGTH_LONG).show();
                    return;
                }
            }

            switchView(R.layout.fragment_installing_update);

            boolean backup = settingsManager.getPreference(SettingsManager.PROPERTY_BACKUP_DEVICE, true);
            boolean wipeCachePartition = settingsManager.getPreference(SettingsManager.PROPERTY_WIPE_CACHE_PARTITION, true);
            boolean rebootDevice = settingsManager.getPreference(SettingsManager.PROPERTY_REBOOT_AFTER_INSTALL, true);

            // Plan install verification on reboot.
            String currentOSVersion = new SystemVersionProperties(false).getOxygenOSOTAVersion();
            String targetOSVersion = updateData.getOtaVersionNumber();

            logInstallationStart(getApplication(), currentOSVersion, targetOSVersion, currentOSVersion, () -> {

                new FunctionalAsyncTask<Void, Void, String>(Worker.NOOP, (args) -> {
                    try {
                        settingsManager.savePreference(SettingsManager.PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, true);
                        settingsManager.savePreference(SettingsManager.PROPERTY_OLD_SYSTEM_VERSION, currentOSVersion);
                        settingsManager.savePreference(SettingsManager.PROPERTY_TARGET_SYSTEM_VERSION, targetOSVersion);
                        UpdateInstaller.installUpdate(getApplication(), DownloadHelper.getFilePath(updateData), additionalZipFilePath, backup, wipeCachePartition, rebootDevice);
                        return null;
                    } catch (UpdateInstallationException e) {
                        return e.getMessage();
                    } catch (InterruptedException e) {
                        Logger.logWarning(TAG, "Error installing update: ", e);
                        return getString(R.string.install_temporary_error);
                    }
                }, (errorMessage) -> {
                    if (errorMessage != null) {
                        // Cancel the verification planned on reboot.
                        settingsManager.savePreference(SettingsManager.PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, false);

                        openAutomaticInstallOptionsSelection();
                        Toast.makeText(getApplication(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                    // Otherwise, the device will reboot via SU.
                }).execute();

            });
        });
    }

    private void openInstallGuide() {
        setTitle(getString(R.string.install_guide_title, 1, showDownloadPage ? NUMBER_OF_INSTALL_GUIDE_PAGES : NUMBER_OF_INSTALL_GUIDE_PAGES - 1));
        switchView(R.layout.activity_install_guide);

        ViewPager viewPager = findViewById(R.id.updateInstallationInstructionsPager);
        viewPager.setVisibility(View.VISIBLE);
        viewPager.setOffscreenPageLimit(4); // Install guide is 5 pages max. So there can be only 4 off-screen.
        viewPager.setAdapter(new InstallGuideSectionsPagerAdapter(getSupportFragmentManager()));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setTitle(getString(R.string.install_guide_title, (position + 1), showDownloadPage ? NUMBER_OF_INSTALL_GUIDE_PAGES : NUMBER_OF_INSTALL_GUIDE_PAGES - 1));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void displayZipFilePath() {

        ImageButton clearButton = findViewById(R.id.additionalZipFileClearButton);
        TextView zipFileField = findViewById(R.id.additionalZipFilePath);

        String text;
        String additionalZipFilePath = settingsManager.getPreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null);

        if (additionalZipFilePath != null) {
            // Remove the path prefix (/storage/emulated/xx). Only keep the local file path.
            text = additionalZipFilePath.replace(Environment.getExternalStoragePublicDirectory(DownloadHelper.DIRECTORY_ROOT).getAbsolutePath() + File.separator, "");
            String extension = text.substring(text.length() - 4, text.length());
            if (!extension.equals(EXTENSION_ZIP)) {
                Toast.makeText(getApplication(), R.string.install_zip_file_wrong_file_type, Toast.LENGTH_LONG).show();
                return;
            }
            zipFileField.setText(text);
            clearButton.setVisibility(View.VISIBLE);
        } else {
            zipFileField.setText(getString(R.string.install_zip_file_placeholder));
            clearButton.setVisibility(View.GONE);
        }

    }

    private void initSettingsSwitch(String settingName, boolean defaultValue, CompoundButton.OnCheckedChangeListener listener) {
        SwitchCompat switchCompat = findViewById(getResources().getIdentifier(settingName + SETTINGS_SWITCH, PACKAGE_ID, getPackageName()));
        switchCompat.setChecked(settingsManager.getPreference(settingName, defaultValue));
        switchCompat.setOnCheckedChangeListener(listener);
    }

    private void initSettingsSwitch(String settingName, boolean defaultValue) {
        initSettingsSwitch(settingName, defaultValue, ((buttonView, isChecked) -> settingsManager.savePreference(settingName, isChecked)));
    }

    private void switchView(int newViewId) {
        this.layoutId = newViewId;

        View newView = getLayoutInflater().inflate(newViewId, null, false);
        newView.startAnimation(AnimationUtils.loadAnimation(getApplication(), android.R.anim.fade_in));

        setContentView(newView);
    }

    private void handleBackAction() {
        // If at the install options screen or in the install guide when rooted, go back to the method selection page.
        if (this.layoutId == R.layout.fragment_install_options || (this.rooted && settingsManager.getPreference(SettingsManager.PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, false) && this.layoutId == R.layout.activity_install_guide)) {
            openMethodSelectionPage();
        } else if (this.layoutId == R.layout.fragment_installing_update) {
            // Once the installation is being started, there is no way out.
            Toast.makeText(getApplication(), R.string.install_going_back_not_possible, Toast.LENGTH_LONG).show();
        } else {
            finish();
        }
    }

    private class InstallGuideSectionsPagerAdapter extends FragmentStatePagerAdapter {

        InstallGuideSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a InstallGuideFragment.
            int startingPage = position + (showDownloadPage ? 1 : 2);
            return InstallGuideFragment.newInstance(startingPage, position == 0);
        }

        @Override
        public int getCount() {
            // Show the predefined amount of total pages.
            return showDownloadPage ? NUMBER_OF_INSTALL_GUIDE_PAGES : NUMBER_OF_INSTALL_GUIDE_PAGES - 1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (position >= getCount()) {
                FragmentManager manager = ((Fragment) object).getFragmentManager();

                if (manager != null) {
                    FragmentTransaction trans = manager.beginTransaction();
                    trans.remove((Fragment) object);
                    trans.commit();
                }
            }
        }
    }

    /**
     * Handle the action from the ZIP file picker
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_FILE_PICKER:
                if (resultCode == RESULT_OK) {

                    try {
                        final Uri uri = data.getData();

                        // Get the zip file path from the Uri
                        settingsManager.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, FileUtils.getPath(this, uri));
                        displayZipFilePath();
                    } catch (Throwable e) {
                        Logger.logError(TAG, "Error handling ZIP selection: ", e);
                        settingsManager.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null);
                        displayZipFilePath();
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        handleBackAction();
    }

    @Override
    public void setContentView(int layoutResID) {
        this.layoutId = layoutResID;
        super.setContentView(layoutResID);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                handleBackAction();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public SparseArray<InstallGuidePage> getInstallGuideCache() {
        return this.installGuideCache;
    }

    public SparseArray<Bitmap> getInstallGuideImageCache() {
        return this.installGuideImageCache;
    }

    private void logInstallationStart(Context context, String startOs, String destinationOs, String currentOs, Worker successFunction) {
        // Create installation ID.
        String installationId = UUID.randomUUID().toString();
        SettingsManager manager = new SettingsManager(context);
        manager.savePreference(SettingsManager.PROPERTY_INSTALLATION_ID, installationId);

        long deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L);
        long updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L);
        String timestamp = LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")).toString();
        RootInstall installation = new RootInstall(deviceId, updateMethodId, InstallationStatus.STARTED, installationId, timestamp, startOs, destinationOs, currentOs, "");

        serverConnector.logRootInstall(installation, (result) -> {
            if (result == null) {
                Logger.init((ApplicationData) getApplication());
                Logger.logError(TAG, "Failed to log update installation action: No response from server");
            } else if (!result.isSuccess()) {
                Logger.init((ApplicationData) getApplication());
                Logger.logError(TAG, "Failed to log update installation action: " + result.getErrorMessage());
            }
            // Always start the installation, as we don't want the user to have to press "install" multiple times if the server failed to respond.
            successFunction.start();
        });
    }
}
