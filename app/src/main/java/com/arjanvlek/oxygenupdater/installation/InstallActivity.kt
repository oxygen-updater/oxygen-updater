package com.arjanvlek.oxygenupdater.installation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.SparseArray
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.NUMBER_OF_INSTALL_GUIDE_PAGES
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties
import com.arjanvlek.oxygenupdater.download.DownloadService.Companion.DIRECTORY_ROOT
import com.arjanvlek.oxygenupdater.installation.automatic.InstallationStatus
import com.arjanvlek.oxygenupdater.installation.automatic.RootInstall
import com.arjanvlek.oxygenupdater.installation.automatic.SubmitUpdateInstallationException
import com.arjanvlek.oxygenupdater.installation.automatic.UpdateInstallationException
import com.arjanvlek.oxygenupdater.installation.automatic.UpdateInstaller.installUpdate
import com.arjanvlek.oxygenupdater.installation.manual.InstallGuideFragment
import com.arjanvlek.oxygenupdater.installation.manual.InstallGuidePage
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.Worker
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.internal.root.RootAccessChecker
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ipaulpro.afilechooser.utils.FileUtils
import java8.util.function.Consumer
import java8.util.function.Function
import kotlinx.android.synthetic.main.fragment_install_options.*
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import java.io.File
import java.util.*

class InstallActivity : SupportActionBarActivity() {
    val installGuideCache = SparseArray<InstallGuidePage>()
    val installGuideImageCache = SparseArray<Bitmap>()
    private var settingsManager: SettingsManager? = null
    private var serverConnector: ServerConnector? = null
    private var showDownloadPage = true
    private var updateData: UpdateData? = null
    private var layoutId: Int = 0
    private var rooted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsManager = SettingsManager(application)
        serverConnector = (application as ApplicationData).getServerConnector()

        showDownloadPage = intent == null || intent.getBooleanExtra(INTENT_SHOW_DOWNLOAD_PAGE, true)

        if (intent != null) {
            updateData = intent.getParcelableExtra(INTENT_UPDATE_DATA)
        }

        initialize()
    }

    override fun setContentView(layoutResID: Int) {
        layoutId = layoutResID
        super.setContentView(layoutResID)
    }

    private fun initialize() {
        setContentView(R.layout.fragment_checking_root_access)

        RootAccessChecker.checkRootAccess(Consumer { isRooted ->
            rooted = isRooted!!

            if (isRooted) {
                val applicationData = application as ApplicationData
                val serverConnector = applicationData.getServerConnector()
                serverConnector.getServerStatus(Utils.checkNetworkConnection(application),
                        Consumer { serverStatus ->
                            if (serverStatus.isAutomaticInstallationEnabled) {
                                openMethodSelectionPage()
                            } else {
                                Toast.makeText(application,
                                        getString(R.string.install_guide_automatic_install_disabled),
                                        LENGTH_LONG).show()
                                openInstallGuide()
                            }
                        })
            } else {
                Toast.makeText(application, getString(R.string.install_guide_no_root), LENGTH_LONG).show()
                openInstallGuide()
            }
        })

    }

    private fun openMethodSelectionPage() {
        switchView(R.layout.fragment_choose_install_method)

        val automaticInstallCard = findViewById<MaterialCardView>(R.id.automaticInstallCard)
        automaticInstallCard.setOnClickListener { openAutomaticInstallOptionsSelection() }

        val manualInstallCard = findViewById<MaterialCardView>(R.id.manualInstallCard)
        manualInstallCard.setOnClickListener { openInstallGuide() }

    }

    private fun openAutomaticInstallOptionsSelection() {
        switchView(R.layout.fragment_install_options)

        initSettingsSwitch(SettingsManager.PROPERTY_BACKUP_DEVICE, true)

        initSettingsSwitch(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false,
                CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    val additionalZipFileContainer = findViewById<View>(R.id.additionalZipContainer)
                    if (isChecked) {
                        additionalZipFileContainer.visibility = View.VISIBLE
                    } else {
                        additionalZipFileContainer.visibility = View.GONE
                    }

                    settingsManager!!.savePreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, isChecked)
                }
        )

        val additionalZipFileContainer = findViewById<View>(R.id.additionalZipContainer)
        if (settingsManager!!.getPreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false)) {
            additionalZipFileContainer.visibility = View.VISIBLE
        } else {
            additionalZipFileContainer.visibility = View.GONE
        }

        initSettingsSwitch(SettingsManager.PROPERTY_WIPE_CACHE_PARTITION, true)
        initSettingsSwitch(SettingsManager.PROPERTY_REBOOT_AFTER_INSTALL, true)

        val filePickerButton = findViewById<ImageButton>(R.id.additionalZipFilePickButton)
        filePickerButton.setOnClickListener {

            // Implicitly allow the user to select a particular kind of data
            val intent = Intent(applicationContext, com.ipaulpro.afilechooser.FileChooserActivity::class.java)
            // The MIME data type filter
            intent.type = "application/zip"
            // Only return URIs that can be opened with ContentResolver
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            startActivityForResult(intent, REQUEST_FILE_PICKER)
        }

        displayZipFilePath()

        val clearFileButton = findViewById<ImageButton>(R.id.additionalZipFileClearButton)
        clearFileButton.setOnClickListener {
            settingsManager!!.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)
            displayZipFilePath()
        }

        startInstallButton.setOnClickListener {

            val additionalZipFilePath = settingsManager!!.getPreference<String?>(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)

            if (settingsManager!!.getPreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false) && additionalZipFilePath == null) {
                Toast.makeText(application, R.string.install_guide_zip_file_missing, LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (additionalZipFilePath != null) {
                val file = File(additionalZipFilePath)
                if (!file.exists()) {
                    Toast.makeText(application, R.string.install_guide_zip_file_deleted, LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            switchView(R.layout.fragment_installing_update)

            val backup = settingsManager!!.getPreference(SettingsManager.PROPERTY_BACKUP_DEVICE, true)
            val wipeCachePartition = settingsManager!!.getPreference(SettingsManager.PROPERTY_WIPE_CACHE_PARTITION, true)
            val rebootDevice = settingsManager!!.getPreference(SettingsManager.PROPERTY_REBOOT_AFTER_INSTALL, true)

            // Plan install verification on reboot.
            val systemVersionProperties = SystemVersionProperties()
            val currentOSVersion = systemVersionProperties.oxygenOSOTAVersion
            val isAbPartitionLayout = systemVersionProperties.isABPartitionLayout
            val targetOSVersion = updateData!!.otaVersionNumber

            logInstallationStart(application, currentOSVersion, targetOSVersion, currentOSVersion,
                    object : Worker {
                        override fun start() {
                            FunctionalAsyncTask<Void, Void, String>(Worker.NOOP, Function {
                                return@Function try {
                                    settingsManager!!.savePreference(SettingsManager
                                            .PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, true)
                                    settingsManager!!.savePreference(SettingsManager
                                            .PROPERTY_OLD_SYSTEM_VERSION, currentOSVersion)
                                    settingsManager!!.savePreference(SettingsManager
                                            .PROPERTY_TARGET_SYSTEM_VERSION, targetOSVersion)
                                    val downloadedUpdateFilePath =
                                            getExternalStoragePublicDirectory(DIRECTORY_ROOT).path +
                                                    File.separator + updateData!!.filename
                                    installUpdate(application, isAbPartitionLayout,
                                            downloadedUpdateFilePath, additionalZipFilePath,
                                            backup, wipeCachePartition, rebootDevice)
                                    null
                                } catch (e: UpdateInstallationException) {
                                    e.message
                                } catch (e: InterruptedException) {
                                    logWarning(TAG, "Error installing update", e)
                                    getString(R.string.install_temporary_error)
                                }
                            }, Consumer { errorMessage ->
                                if (errorMessage != null) {
                                    // Cancel the verification planned on reboot.
                                    settingsManager!!.savePreference(SettingsManager
                                            .PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, false)

                                    openAutomaticInstallOptionsSelection()
                                    Toast.makeText(application, errorMessage, LENGTH_LONG).show()
                                }
                                // Otherwise, the device will reboot via SU.
                            }).execute()
                        }
                    })
        }
    }

    private fun openInstallGuide() {
        title = getString(R.string.install_guide_title, 1, if (showDownloadPage) NUMBER_OF_INSTALL_GUIDE_PAGES else NUMBER_OF_INSTALL_GUIDE_PAGES - 1)
        switchView(R.layout.activity_install_guide)

        val viewPager = findViewById<ViewPager>(R.id.updateInstallationInstructionsPager)
        viewPager.visibility = View.VISIBLE
        viewPager.offscreenPageLimit = 4 // Install guide is 5 pages max. So there can be only 4 off-screen.
        viewPager.adapter = InstallGuideSectionsPagerAdapter(supportFragmentManager)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                title = getString(R.string.install_guide_title, position + 1, if (showDownloadPage) NUMBER_OF_INSTALL_GUIDE_PAGES else NUMBER_OF_INSTALL_GUIDE_PAGES - 1)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    private fun displayZipFilePath() {

        val clearButton = findViewById<ImageButton>(R.id.additionalZipFileClearButton)
        val zipFileField = findViewById<TextView>(R.id.additionalZipFilePath)

        val text: String
        val additionalZipFilePath = settingsManager?.getPreference(SettingsManager
                .PROPERTY_ADDITIONAL_ZIP_FILE_PATH, "")

        if (additionalZipFilePath != null) {
            // Remove the path prefix (/storage/emulated/xx). Only keep the local file path.
            text = additionalZipFilePath.replace(getExternalStoragePublicDirectory(DIRECTORY_ROOT).absolutePath + File.separator, "")
            val extension = text.substring(text.length - 4)
            if (extension != EXTENSION_ZIP) {
                Toast.makeText(application, R.string.install_zip_file_wrong_file_type,
                        LENGTH_LONG).show()
                return
            }
            zipFileField.text = text
            clearButton.visibility = View.VISIBLE
        } else {
            zipFileField.text = getString(R.string.install_zip_file_placeholder)
            clearButton.visibility = View.GONE
        }

    }

    private fun initSettingsSwitch(settingName: String, defaultValue: Boolean,
                                   listener: CompoundButton.OnCheckedChangeListener) {
        val switchCompat = findViewById<SwitchMaterial>(resources.getIdentifier(settingName +
                SETTINGS_SWITCH, PACKAGE_ID, packageName))
        switchCompat.isChecked = settingsManager!!.getPreference(settingName, defaultValue)
        switchCompat.setOnCheckedChangeListener(listener)
    }

    private fun initSettingsSwitch(settingName: String, defaultValue: Boolean) {
        initSettingsSwitch(settingName, defaultValue,
                CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    settingsManager?.savePreference(settingName, isChecked)
                }
        )
    }

    private fun switchView(newViewId: Int) {
        layoutId = newViewId

        val newView = layoutInflater.inflate(newViewId, null, false)
        newView.startAnimation(AnimationUtils.loadAnimation(application, android.R.anim.fade_in))

        setContentView(newView)
    }

    private fun handleBackAction() {
        // If at the install options screen or in the install guide when rooted, go back to
        // the method selection page.
        if (layoutId == R.layout.fragment_install_options || rooted &&
                settingsManager!!.getPreference(PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED,
                        false) && layoutId == R.layout.activity_install_guide) {
            openMethodSelectionPage()
        } else if (layoutId == R.layout.fragment_installing_update) {
            // Once the installation is being started, there is no way out.
            Toast.makeText(application, R.string.install_going_back_not_possible, LENGTH_LONG).show()
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
                val uri = data!!.data

                // Get the zip file path from the Uri
                settingsManager!!.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, FileUtils.getPath(this, uri!!))
                displayZipFilePath()
            } catch (e: Throwable) {
                logError(TAG, "Error handling root package ZIP selection", e)
                settingsManager!!.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)
                displayZipFilePath()
            }

        }

        super.onActivityResult(resultCode, resultCode, data)
    }

    override fun onBackPressed() {
        handleBackAction()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == android.R.id.home) {
            handleBackAction()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logInstallationStart(context: Context, startOs: String, destinationOs: String?, currentOs: String, successFunction: Worker) {
        // Create installation ID.
        val installationId = UUID.randomUUID().toString()
        val manager = SettingsManager(context)
        manager.savePreference(SettingsManager.PROPERTY_INSTALLATION_ID, installationId)

        val deviceId = settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        val timestamp = LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")).toString()
        val installation = RootInstall(deviceId, updateMethodId, InstallationStatus.STARTED, installationId, timestamp, startOs, destinationOs, currentOs, "")

        serverConnector?.logRootInstall(installation, Consumer { result ->
            if (result == null) {
                logError(TAG, SubmitUpdateInstallationException("Failed to log update installation action: No response from server"))
            } else if (!result.isSuccess) {
                logError(TAG, SubmitUpdateInstallationException("Failed to log update installation action: " + result.errorMessage!!))
            }
            // Always start the installation, as we don't want the user to have to press "install" multiple times if the server failed to respond.
            successFunction.start()
        })
    }

    private inner class InstallGuideSectionsPagerAdapter internal constructor(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a InstallGuideFragment.
            val startingPage = position + if (showDownloadPage) 1 else 2
            return InstallGuideFragment.newInstance(startingPage, position == 0)
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            if (position >= count) {
                val manager = (`object` as Fragment).fragmentManager

                if (manager != null) {
                    val trans = manager.beginTransaction()
                    trans.remove(`object`)
                    trans.commit()
                }
            }
        }

        override fun getCount(): Int {
            // Show the predefined amount of total pages.
            return if (showDownloadPage) NUMBER_OF_INSTALL_GUIDE_PAGES else NUMBER_OF_INSTALL_GUIDE_PAGES - 1
        }
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
}
