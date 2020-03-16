package com.arjanvlek.oxygenupdater.viewmodels

import android.util.SparseArray
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjanvlek.oxygenupdater.models.InstallGuidePage
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.arjanvlek.oxygenupdater.activities.InstallActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class InstallViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    /**
     * Used in [com.arjanvlek.oxygenupdater.activities.InstallActivity] to check
     * if the user can navigate backwards in hierarchy or not.
     *
     * This flag is set to `false` only in [com.arjanvlek.oxygenupdater.fragments.AutomaticInstallFragment],
     * after the user starts the installation process, since that step executes SU commands defined in
     * [com.arjanvlek.oxygenupdater.internal.AutomaticUpdateInstaller], which obviously can't be cancelled
     */
    var canGoBack = true

    val installGuideCache = SparseArray<InstallGuidePage>()

    private val _firstInstallGuidePageLoaded = MutableLiveData<Boolean>()
    val firstInstallGuidePageLoaded: LiveData<Boolean>
        get() = _firstInstallGuidePageLoaded

    private val _toolbarTitle = MutableLiveData<Int>()
    val toolbarTitle: LiveData<Int>
        get() = _toolbarTitle

    private val _toolbarSubtitle = MutableLiveData<Int?>()
    val toolbarSubtitle: LiveData<Int?>
        get() = _toolbarSubtitle

    private val _toolbarImage = MutableLiveData<Int>()
    val toolbarImage: LiveData<Int>
        get() = _toolbarImage

    private val _serverStatus = MutableLiveData<ServerStatus>()
    private val _installGuidePage = MutableLiveData<InstallGuidePage>()

    private val _logRootInstallResult = MutableLiveData<ServerPostResult>()
    val logRootInstallResult: LiveData<ServerPostResult>
        get() = _logRootInstallResult

    fun fetchServerStatus(): LiveData<ServerStatus> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchServerStatus().let {
            _serverStatus.postValue(it)
        }
    }.let { _serverStatus }

    fun fetchInstallGuidePage(
        deviceId: Long,
        updateMethodId: Long,
        pageNumber: Int
    ): LiveData<InstallGuidePage> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchInstallGuidePage(deviceId, updateMethodId, pageNumber)?.let {
            _installGuidePage.postValue(it)
        }
    }.let { _installGuidePage }

    fun logRootInstall(rootInstall: RootInstall) = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.logRootInstall(rootInstall)?.let {
            _logRootInstallResult.postValue(it)
        }
    }

    fun updateToolbarTitle(@StringRes resId: Int) = _toolbarTitle.postValue(resId)

    fun updateToolbarSubtitle(@StringRes resId: Int?) = _toolbarSubtitle.postValue(resId)

    fun updateToolbarImage(@DrawableRes resId: Int) = _toolbarImage.postValue(resId)

    fun markFirstInstallGuidePageLoaded() = _firstInstallGuidePageLoaded.postValue(true)
}
