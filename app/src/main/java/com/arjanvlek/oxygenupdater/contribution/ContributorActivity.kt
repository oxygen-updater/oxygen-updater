package com.arjanvlek.oxygenupdater.contribution

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.util.Consumer
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.MainActivity.Companion.PERMISSION_REQUEST_CODE
import com.arjanvlek.oxygenupdater.views.MainActivity.Companion.VERIFY_FILE_PERMISSION
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity
import kotlinx.android.synthetic.main.activity_contributor.*
import java.util.concurrent.atomic.AtomicBoolean

class ContributorActivity : SupportActionBarActivity() {

    private val localContributeSetting = AtomicBoolean(false)
    private val saveOptionsHidden = AtomicBoolean(false)
    private var permissionCallback: Consumer<Boolean>? = null

    public override fun onCreate(savedInstanceSate: Bundle?) {
        super.onCreate(savedInstanceSate)
        setContentView(R.layout.activity_contributor)

        if (intent.getBooleanExtra(INTENT_HIDE_ENROLLMENT, false)) {
            contributeCheckbox.visibility = GONE
            contributeSaveButton.visibility = GONE
            saveOptionsHidden.compareAndSet(false, true)
        }
    }

    public override fun onStart() {
        super.onStart()
        setInitialCheckboxState()
    }

    public override fun onResume() {
        super.onResume()
        setCheckboxClickListener()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && permissionCallback != null && grantResults.isNotEmpty()) {
            permissionCallback!!.accept(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onBackPressed() {
        // Respond to the device's back button
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Back arrow button
        if (item.itemId == android.R.id.home) {
            if (!saveOptionsHidden.get()) {
                onSaveButtonClick(null)
            } else {
                finish()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setInitialCheckboxState() {
        val settingsManager = SettingsManager(applicationContext)
        val isContributing = settingsManager.getPreference(SettingsManager.PROPERTY_CONTRIBUTE, false)
        localContributeSetting.set(isContributing)

        contributeCheckbox.isChecked = isContributing
    }

    private fun setCheckboxClickListener() {
        contributeCheckbox.setOnCheckedChangeListener { _, isChecked -> localContributeSetting.set(isChecked) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSaveButtonClick(checkbox: View?) {
        val contributorUtils = ContributorUtils(application)
        val contributor = localContributeSetting.get()

        if (contributor) {
            requestContributorStoragePermissions(Consumer { granted ->
                if (granted!!) {
                    contributorUtils.flushSettings(true)
                    finish()
                } else {
                    Toast.makeText(this@ContributorActivity, R.string.contribute_allow_storage, LENGTH_LONG).show()
                }
            })
        } else {
            contributorUtils.flushSettings(false)
            finish()
        }
    }

    private fun requestContributorStoragePermissions(permissionCallback: Consumer<Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.permissionCallback = permissionCallback
            requestPermissions(arrayOf(VERIFY_FILE_PERMISSION), PERMISSION_REQUEST_CODE)
        } else {
            permissionCallback.accept(true)
        }
    }

    companion object {
        const val INTENT_HIDE_ENROLLMENT = "hide_enrollment"
    }
}
