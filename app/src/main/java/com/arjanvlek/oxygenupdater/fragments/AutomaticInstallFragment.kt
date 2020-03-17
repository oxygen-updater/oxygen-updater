package com.arjanvlek.oxygenupdater.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.InstallActivity
import com.arjanvlek.oxygenupdater.exceptions.SubmitUpdateInstallationException
import com.arjanvlek.oxygenupdater.exceptions.UpdateInstallationException
import com.arjanvlek.oxygenupdater.extensions.syncWithSharedPreferences
import com.arjanvlek.oxygenupdater.internal.AutomaticUpdateInstaller
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.InstallationStatus
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.models.SystemVersionProperties
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import com.arjanvlek.oxygenupdater.utils.Utils.SERVER_TIME_ZONE
import com.arjanvlek.oxygenupdater.viewmodels.InstallViewModel
import com.arjanvlek.oxygenupdater.workers.DIRECTORY_ROOT
import com.ipaulpro.afilechooser.FileChooserActivity
import com.ipaulpro.afilechooser.utils.FileUtils
import kotlinx.android.synthetic.main.fragment_automatic_install.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.threeten.bp.LocalDateTime
import java.io.File
import java.util.*

class AutomaticInstallFragment : Fragment(R.layout.fragment_automatic_install) {

    private lateinit var updateData: UpdateData

    private val systemVersionProperties by inject<SystemVersionProperties>()
    private val settingsManager by inject<SettingsManager>()
    private val installViewModel by sharedViewModel<InstallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        updateData = requireArguments().getParcelable(InstallActivity.INTENT_UPDATE_DATA)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        installViewModel.updateToolbarTitle(R.string.install_method_chooser_automatic_title)
        installViewModel.updateToolbarSubtitle(R.string.install_options_subtitle)
        installViewModel.updateToolbarImage(R.drawable.auto)

        (activity as InstallActivity?)?.resetAppBarForMethodChooserFragment()

        init()
    }

    private fun init() {
        installViewModel.canGoBack = true

        // show install options layout by default
        preparingInstallationProgressLayout.isVisible = false
        installOptionsLayout.isVisible = true

        additionalZipContainer.isVisible = settingsManager.getPreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false)

        displayZipFilePath()

        val backup = settingsManager.getPreference(SettingsManager.PROPERTY_BACKUP_DEVICE, true)
        val wipeCachePartition = settingsManager.getPreference(SettingsManager.PROPERTY_WIPE_CACHE_PARTITION, true)
        val rebootDevice = settingsManager.getPreference(SettingsManager.PROPERTY_REBOOT_AFTER_INSTALL, true)

        // Plan install verification on reboot.
        val currentOSVersion = systemVersionProperties.oxygenOSOTAVersion
        val isAbPartitionLayout = systemVersionProperties.isABPartitionLayout
        val targetOSVersion = updateData.otaVersionNumber!!

        setupLogRootInstallObserver(
            backup,
            wipeCachePartition,
            rebootDevice,
            currentOSVersion,
            isAbPartitionLayout,
            targetOSVersion
        )

        setupOnClickListeners(currentOSVersion, targetOSVersion)

        setupSwitches()
    }

    private fun setupLogRootInstallObserver(
        backup: Boolean,
        wipeCachePartition: Boolean,
        rebootDevice: Boolean,
        currentOSVersion: String,
        isAbPartitionLayout: Boolean,
        targetOSVersion: String
    ) {
        val additionalZipFilePath = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)

        if (!installViewModel.logRootInstallResult.hasActiveObservers()) {
            installViewModel.logRootInstallResult.observe(viewLifecycleOwner, Observer { result: ServerPostResult? ->
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
                FunctionalAsyncTask<Void?, Void, String?>({}, {
                    try {
                        settingsManager.savePreference(SettingsManager.PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, true)
                        settingsManager.savePreference(SettingsManager.PROPERTY_OLD_SYSTEM_VERSION, currentOSVersion)
                        settingsManager.savePreference(SettingsManager.PROPERTY_TARGET_SYSTEM_VERSION, targetOSVersion)

                        val downloadedUpdateFilePath =
                            Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).path + File.separator + updateData.filename

                        AutomaticUpdateInstaller.installUpdate(
                            requireContext(),
                            isAbPartitionLayout,
                            downloadedUpdateFilePath,
                            additionalZipFilePath,
                            backup,
                            wipeCachePartition,
                            rebootDevice
                        )
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

                        if (isAdded) {
                            init()
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }).execute()
            })
        }
    }

    private fun setupOnClickListeners(
        currentOSVersion: String,
        targetOSVersion: String
    ) {
        additionalZipFilePickButton.setOnClickListener {
            // Implicitly allow the user to select a particular kind of data
            val intent = Intent(context, FileChooserActivity::class.java)
                // Only return URIs that can be opened with ContentResolver
                .addCategory(Intent.CATEGORY_OPENABLE)
                // The MIME data type filter
                .setType("application/zip")

            startActivityForResult(intent, REQUEST_FILE_PICKER)
        }

        additionalZipFileClearButton.setOnClickListener {
            settingsManager.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)
            displayZipFilePath()
        }

        startInstallButton.setOnClickListener {
            val additionalZipFilePath = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)

            if (settingsManager.getPreference(SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED, false) && additionalZipFilePath == null) {
                Toast.makeText(context, R.string.install_guide_zip_file_missing, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (additionalZipFilePath != null) {
                val file = File(additionalZipFilePath)

                if (!file.exists()) {
                    Toast.makeText(context, R.string.install_guide_zip_file_deleted, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            // show progress layout
            preparingInstallationProgressLayout.isVisible = true
            installOptionsLayout.isVisible = false

            installViewModel.canGoBack = false

            logInstallationStart(currentOSVersion, targetOSVersion, currentOSVersion)
        }
    }

    private fun setupSwitches() {
        backupDeviceSwitch.syncWithSharedPreferences(
            SettingsManager.PROPERTY_BACKUP_DEVICE,
            true
        )

        keepDeviceRootedSwitch.syncWithSharedPreferences(
            SettingsManager.PROPERTY_KEEP_DEVICE_ROOTED,
            false
        ) { isChecked -> additionalZipContainer.isVisible = isChecked }

        wipeCachePartitionSwitch.syncWithSharedPreferences(
            SettingsManager.PROPERTY_WIPE_CACHE_PARTITION,
            true
        )

        rebootAfterInstallSwitch.syncWithSharedPreferences(
            SettingsManager.PROPERTY_REBOOT_AFTER_INSTALL,
            true
        )
    }

    private fun logInstallationStart(
        startOs: String,
        destinationOs: String,
        currentOs: String
    ) {
        // Create installation ID.
        val installationId = UUID.randomUUID().toString()

        settingsManager.savePreference(SettingsManager.PROPERTY_INSTALLATION_ID, installationId)

        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        val timestamp = LocalDateTime.now(SERVER_TIME_ZONE).toString()

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

        installViewModel.logRootInstall(installation)
    }

    private fun displayZipFilePath() {
        val zipFilePath = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)

        if (zipFilePath != null) {
            // Remove the path prefix (/storage/emulated/xx). Only keep the local file path.
            val text = zipFilePath.replace(
                Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).absolutePath + File.separator,
                ""
            )

            val extension = text.substring(text.length - 4)
            if (extension != EXTENSION_ZIP) {
                Toast.makeText(context, R.string.install_zip_file_wrong_file_type, Toast.LENGTH_LONG).show()
                return
            }

            additionalZipFilePath.text = text
            additionalZipFileClearButton.isVisible = true
        } else {
            additionalZipFilePath.setText(R.string.install_zip_file_placeholder)
            additionalZipFileClearButton.isVisible = false
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
                settingsManager.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, FileUtils.getPath(context, uri))
                displayZipFilePath()
            } catch (e: Throwable) {
                logError(TAG, "Error handling root package ZIP selection", e)

                settingsManager.savePreference(SettingsManager.PROPERTY_ADDITIONAL_ZIP_FILE_PATH, null)
                displayZipFilePath()
            }
        }

        super.onActivityResult(resultCode, resultCode, data)
    }

    companion object {
        private const val TAG = "AutomaticInstallFragment"
        private const val EXTENSION_ZIP = ".zip"

        private const val REQUEST_FILE_PICKER = 1606
    }
}
