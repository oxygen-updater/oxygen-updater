package com.oxygenupdater.activities

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.oxygenupdater.R
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.utils.ContributorUtils
import kotlinx.android.synthetic.main.activity_contributor.*
import java.util.concurrent.atomic.AtomicBoolean

class ContributorActivity : SupportActionBarActivity() {

    private val localContributeSetting = AtomicBoolean(false)
    private val saveOptionsHidden = AtomicBoolean(false)
    private var permissionCallback: KotlinCallback<Boolean>? = null

    override fun onCreate(
        savedInstanceSate: Bundle?
    ) = super.onCreate(savedInstanceSate).also {
        setContentView(R.layout.activity_contributor)

        if (intent.getBooleanExtra(INTENT_HIDE_ENROLLMENT, false)) {
            contributeCheckbox.isVisible = false
            contributeSaveButton.isVisible = false

            saveOptionsHidden.compareAndSet(false, true)
        }

        setInitialCheckboxState()
        setCheckboxClickListener()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) = super.onRequestPermissionsResult(
        requestCode,
        permissions,
        grantResults
    ).also {
        if (requestCode == MainActivity.PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            permissionCallback?.invoke(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onBackPressed() = if (!saveOptionsHidden.get()) {
        // Save settings when user leaves the activity
        onSaveButtonClick(null)
    } else {
        finish()
    }

    /**
     * Respond to the action bar's Up/Home button.
     * Delegate to [onBackPressed] if [android.R.id.home] is clicked, otherwise call `super`
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> onBackPressed().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setInitialCheckboxState() {
        val isContributing = SettingsManager.getPreference(SettingsManager.PROPERTY_CONTRIBUTE, false)

        localContributeSetting.set(isContributing)
        contributeCheckbox.isChecked = isContributing
    }

    private fun setCheckboxClickListener() {
        contributeCheckbox.setOnCheckedChangeListener { _, isChecked -> localContributeSetting.set(isChecked) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSaveButtonClick(checkbox: View?) {
        val contributor = localContributeSetting.get()

        if (contributor) {
            requestContributorStoragePermissions { granted ->
                if (granted) {
                    ContributorUtils.flushSettings(true)
                    finish()
                } else {
                    Toast.makeText(this, R.string.contribute_allow_storage, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            ContributorUtils.flushSettings(false)
            finish()
        }
    }

    private fun requestContributorStoragePermissions(permissionCallback: KotlinCallback<Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.permissionCallback = permissionCallback
            requestPermissions(arrayOf(MainActivity.VERIFY_FILE_PERMISSION), MainActivity.PERMISSION_REQUEST_CODE)
        } else {
            permissionCallback.invoke(true)
        }
    }

    companion object {
        const val INTENT_HIDE_ENROLLMENT = "hide_enrollment"
    }
}
