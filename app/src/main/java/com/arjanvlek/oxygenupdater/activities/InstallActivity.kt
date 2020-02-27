package com.arjanvlek.oxygenupdater.activities

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.SparseArray
import android.view.MenuItem
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.arjanvlek.oxygenupdater.OxygenUpdater.Companion.NUMBER_OF_INSTALL_GUIDE_PAGES
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.exceptions.SubmitUpdateInstallationException
import com.arjanvlek.oxygenupdater.exceptions.UpdateInstallationException
import com.arjanvlek.oxygenupdater.fragments.InstallGuideFragment.Companion.newInstance
import com.arjanvlek.oxygenupdater.internal.AutomaticUpdateInstaller.installUpdate
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.InstallGuidePage
import com.arjanvlek.oxygenupdater.models.InstallationStatus
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.models.SystemVersionProperties
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.services.DownloadService
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import com.arjanvlek.oxygenupdater.utils.RootAccessChecker
import com.arjanvlek.oxygenupdater.utils.Utils.checkNetworkConnection
import com.arjanvlek.oxygenupdater.viewmodels.InstallViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ipaulpro.afilechooser.FileChooserActivity
import com.ipaulpro.afilechooser.utils.FileUtils
import kotlinx.android.synthetic.main.activity_install_guide.*
import kotlinx.android.synthetic.main.fragment_choose_install_method.*
import kotlinx.android.synthetic.main.fragment_install_options.*
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.util.*

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class InstallActivity : SupportActionBarActivity() {

    var installGuideCache = SparseArray<InstallGuidePage>()
        private set

    private var showDownloadPage = true
    private var updateData: UpdateData? = null
    private var layoutId = 0
    private var rooted = false

    private val settingsManager by inject<SettingsManager>()
    private val installViewModel by viewModel<InstallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showDownloadPage = intent == null || intent.getBooleanExtra(INTENT_SHOW_DOWNLOAD_PAGE, true)

        if (intent != null) {
            updateData = intent.getParcelableExtra(INTENT_UPDATE_DATA)
        }

        initialize()
    }

    override fun setContentView(layoutResId: Int) {
        layoutId = layoutResId
        super.setContentView(layoutResId)
    }

    private fun initialize() {
        setContentView(R.layout.fragment_checking_root_access)

        RootAccessChecker.checkRootAccess { isRooted ->
            rooted = isRooted

            if (isRooted) {
                installViewModel.fetchServerStatus(checkNetworkConnection(this)).observe(this, Observer { serverStatus ->
                    if (serverStatus.automaticInstallationEnabled) {
                        openMethodSelectionPage()
                    } else {
                        Toast.makeText(this, getString(R.string.install_guide_automatic_install_disabled), LENGTH_LONG).show()
                        openInstallGuide()
                    }
                })
            } else {
                Toast.makeText(this, getString(R.string.install_guide_no_root), LENGTH_LONG).show()
                openInstallGuide()
            }
        }
    }

    private fun openMethodSelectionPage() {
        switchView(R.layout.fragment_choose_install_method)

        automaticInstallCard.setOnClickListener { openAutomaticInstallOptionsSelection() }
        manualInstallCard.setOnClickListener { openInstallGuide() }
    }


    private fun openAutomaticInstallOptionsSelection() {
        switchView(R.layout.fragment_install_options)

        initSettingsSwitch(SettingsManager.PROPERTY_BACKUP_DEVICE, true)

        initSettingsSwitch(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false, CompoundButton.OnCheckedChangeListener { _, isChecked ->
            additionalZipContainer.isVisible = isChecked

            settingsManager.savePreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, isChecked)
        })

        additionalZipContainer.isVisible = settingsManager.getPreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false)

        initSettingsSwitch(SettingsManager.PROPERTY_WIPE_CACHE_PARTITION, true)
        initSettingsSwitch(SettingsManager.PROPERTY_REBOOT_AFTER_INSTALL, true)

        additionalZipFilePickButton.setOnClickListener {
            // Implicitly allow the user to select a particular kind of data
            val intent = Intent(applicationContext, FileChooserActivity::class.java)
                // Only return URIs that can be opened with ContentResolver
                .addCategory(Intent.CATEGORY_OPENABLE)
                // The MIME data type filter
                .setType("application/zip")

            startActivityForResult(intent, REQUEST_FILE_PICKER)
        }

        displayZipFilePath()

        additionalZipFileClearButton.setOnClickListener {
            settingsManager.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)
            displayZipFilePath()
        }

        startInstallButton.setOnClickListener {
            val additionalZipFilePath = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)

            if (settingsManager.getPreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false) && additionalZipFilePath == null) {
                Toast.makeText(this, R.string.install_guide_zip_file_missing, LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (additionalZipFilePath != null) {
                val file = File(additionalZipFilePath)

                if (!file.exists()) {
                    Toast.makeText(this, R.string.install_guide_zip_file_deleted, LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            switchView(R.layout.fragment_installing_update)

            val backup = settingsManager.getPreference(SettingsManager.PROPERTY_BACKUP_DEVICE, true)
            val wipeCachePartition = settingsManager.getPreference(SettingsManager.PROPERTY_WIPE_CACHE_PARTITION, true)
            val rebootDevice = settingsManager.getPreference(SettingsManager.PROPERTY_REBOOT_AFTER_INSTALL, true)

            // Plan install verification on reboot.
            val systemVersionProperties = SystemVersionProperties()
            val currentOSVersion = systemVersionProperties.oxygenOSOTAVersion
            val isAbPartitionLayout = systemVersionProperties.isABPartitionLayout
            val targetOSVersion = updateData!!.otaVersionNumber!!

            logInstallationStart(currentOSVersion, targetOSVersion, currentOSVersion) {
                FunctionalAsyncTask<Void?, Void, String?>({}, {
                    try {
                        settingsManager.savePreference(SettingsManager.PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, true)
                        settingsManager.savePreference(SettingsManager.PROPERTY_OLD_SYSTEM_VERSION, currentOSVersion)
                        settingsManager.savePreference(SettingsManager.PROPERTY_TARGET_SYSTEM_VERSION, targetOSVersion)

                        val downloadedUpdateFilePath =
                            Environment.getExternalStoragePublicDirectory(DownloadService.DIRECTORY_ROOT).path + File.separator + updateData!!.filename

                        installUpdate(this, isAbPartitionLayout, downloadedUpdateFilePath, additionalZipFilePath, backup, wipeCachePartition, rebootDevice)
                        null
                    } catch (e: UpdateInstallationException) {
                        e.message
                    } catch (e: InterruptedException) {
                        logWarning(TAG, "Error installing update", e)
                        getString(R.string.install_temporary_error)
                    }
                }, { errorMessage: String? ->
                    if (errorMessage != null) {
                        // Cancel the verification planned on reboot.
                        settingsManager.savePreference(SettingsManager.PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, false)

                        openAutomaticInstallOptionsSelection()
                        Toast.makeText(this, errorMessage, LENGTH_LONG).show()
                    }
                }).execute()
            }
        }
    }

    private fun openInstallGuide() {
        title = getString(
            R.string.install_guide_title,
            1,
            if (showDownloadPage) NUMBER_OF_INSTALL_GUIDE_PAGES else NUMBER_OF_INSTALL_GUIDE_PAGES - 1
        )

        switchView(R.layout.activity_install_guide)

        updateInstallationInstructionsPager.apply {
            isVisible = true
            offscreenPageLimit = 4 // Install guide is 5 pages max. So there can be only 4 off-screen.
            adapter = InstallGuideSectionsPagerAdapter(supportFragmentManager)

            addOnPageChangeListener(object : OnPageChangeListener {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    title = getString(
                        R.string.install_guide_title,
                        position + 1,
                        if (showDownloadPage) NUMBER_OF_INSTALL_GUIDE_PAGES else NUMBER_OF_INSTALL_GUIDE_PAGES - 1
                    )
                }

                override fun onPageScrollStateChanged(state: Int) {}
            })
        }
    }

    private fun displayZipFilePath() {
        val zipFilePath = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)

        if (zipFilePath != null) {
            // Remove the path prefix (/storage/emulated/xx). Only keep the local file path.
            val text = zipFilePath.replace(
                Environment.getExternalStoragePublicDirectory(DownloadService.DIRECTORY_ROOT).absolutePath + File.separator,
                ""
            )

            val extension = text.substring(text.length - 4)
            if (extension != EXTENSION_ZIP) {
                Toast.makeText(this, R.string.install_zip_file_wrong_file_type, LENGTH_LONG).show()
                return
            }

            additionalZipFilePath.text = text
            additionalZipFileClearButton.isVisible = true
        } else {
            additionalZipFilePath.setText(R.string.install_zip_file_placeholder)
            additionalZipFileClearButton.isVisible = false
        }
    }

    private fun initSettingsSwitch(settingName: String, defaultValue: Boolean, listener: CompoundButton.OnCheckedChangeListener) {
        val switchCompat = findViewById<SwitchMaterial>(
            resources.getIdentifier(
                settingName + SETTINGS_SWITCH,
                PACKAGE_ID,
                packageName
            )
        )

        switchCompat.isChecked = settingsManager.getPreference(settingName, defaultValue)
        switchCompat.setOnCheckedChangeListener(listener)
    }

    @Suppress("SameParameterValue")
    private fun initSettingsSwitch(settingName: String, defaultValue: Boolean) {
        initSettingsSwitch(
            settingName,
            defaultValue,
            CompoundButton.OnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean -> settingsManager.savePreference(settingName, isChecked) }
        )
    }


    private fun switchView(newViewId: Int) {
        layoutId = newViewId

        layoutInflater.inflate(newViewId, null, false).let {
            it.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
            setContentView(it)
        }
    }

    private fun handleBackAction() {
        // If at the install options screen or in the install guide when rooted, go back to the method selection page.
        if (layoutId == R.layout.fragment_install_options
            || rooted
            && settingsManager.getPreference(SettingsManager.PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, false)
            && layoutId == R.layout.activity_install_guide
        ) {
            openMethodSelectionPage()
        } else if (layoutId == R.layout.fragment_installing_update) {
            // Once the installation is being started, there is no way out.
            Toast.makeText(this, R.string.install_going_back_not_possible, LENGTH_LONG).show()
        } else {
            finish()
        }
    }

    /**
     * Handle the action from the ZIP file picker
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_FILE_PICKER && resultCode == Activity.RESULT_OK) {
            try {
                val uri = data?.data

                // Get the zip file path from the Uri
                settingsManager.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, FileUtils.getPath(this, uri))
                displayZipFilePath()
            } catch (e: Throwable) {
                logError(TAG, "Error handling root package ZIP selection", e)

                settingsManager.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)
                displayZipFilePath()
            }
        }

        super.onActivityResult(resultCode, resultCode, data)
    }

    override fun onBackPressed() = handleBackAction()

    /**
     * Respond to the action bar's Up/Home button
     */
    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
        handleBackAction()
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    private fun logInstallationStart(
        startOs: String,
        destinationOs: String,
        currentOs: String,
        successFunction: () -> AsyncTask<Void?, Void, String?>
    ) {
        // Create installation ID.
        val installationId = UUID.randomUUID().toString()

        settingsManager.savePreference(SettingsManager.PROPERTY_INSTALLATION_ID, installationId)

        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        val timestamp = LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")).toString()
        val installation = RootInstall(
            deviceId,
            updateMethodId,
            InstallationStatus.STARTED,
            installationId,
            timestamp,
            startOs,
            destinationOs,
            currentOs,
            ""
        )

        installViewModel.logRootInstall(installation).observe(this, Observer { result: ServerPostResult? ->
            if (result == null) {
                logError(
                    TAG,
                    SubmitUpdateInstallationException("Failed to log update installation action: No response from server")
                )
            } else if (!result.success) {
                logError(
                    TAG,
                    SubmitUpdateInstallationException("Failed to log update installation action: ${result.errorMessage}")
                )
            }

            // Always start the installation, as we don't want the user to have to press "install" multiple times if the server failed to respond.
            successFunction.invoke()
        })
    }

    companion object {
        const val INTENT_SHOW_DOWNLOAD_PAGE = "show_download_page"
        const val INTENT_UPDATE_DATA = "update_data"

        private const val REQUEST_FILE_PICKER = 1606
        private const val TAG = "InstallActivity"
        private const val EXTENSION_ZIP = ".zip"
        private const val PACKAGE_ID = "id"
        private const val SETTINGS_SWITCH = "Switch"
    }

    private inner class InstallGuideSectionsPagerAdapter internal constructor(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a InstallGuideFragment.
            val startingPage = position + if (showDownloadPage) 1 else 2
            return newInstance(startingPage, position == 0)
        }

        override fun destroyItem(container: ViewGroup, position: Int, fragment: Any) {
            if (position >= count) {
                val manager = (fragment as Fragment?)?.parentFragmentManager

                if (manager != null) {
                    val transaction = manager.beginTransaction()

                    transaction.remove(fragment)
                    transaction.commit()
                }
            }
        }

        override fun getCount(): Int {
            // Show the predefined amount of total pages.
            return if (showDownloadPage) NUMBER_OF_INSTALL_GUIDE_PAGES else NUMBER_OF_INSTALL_GUIDE_PAGES - 1
        }
    }
}
