package com.oxygenupdater.viewmodels

import android.util.SparseArray
import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.models.InstallGuidePage
import com.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * For [com.oxygenupdater.activities.InstallActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class InstallViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

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

    private val _toolbarImage = MutableLiveData<Pair<Int, Boolean>>()
    val toolbarImage: LiveData<Pair<Int, Boolean>>
        get() = _toolbarImage

    fun fetchInstallGuidePage(
        deviceId: Long,
        updateMethodId: Long,
        pageNumber: Int,
        callback: KotlinCallback<InstallGuidePage?>
    ) = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchInstallGuidePage(deviceId, updateMethodId, pageNumber).let {
            withContext(Dispatchers.Main) {
                callback.invoke(it)
            }
        }
    }

    fun updateToolbarImage(
        @DrawableRes resId: Int,
        applyTint: Boolean = false
    ) = _toolbarImage.postValue(Pair(resId, applyTint))

    fun markFirstInstallGuidePageLoaded() = _firstInstallGuidePageLoaded.postValue(true)
}
