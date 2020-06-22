package com.arjanvlek.oxygenupdater.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

/**
 * LiveData can sometimes be annoying, especially with Room.
 * This extension ensures events are fired only if value changes,
 * to prevent unnecessary redraws etc.
 */
fun <T> LiveData<T>.getDistinct(): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()
    distinctLiveData.addSource(this, object : Observer<T> {
        private var initialized = false
        private var lastObj: T? = null

        override fun onChanged(obj: T?) {
            if (!initialized) {
                initialized = true
                lastObj = obj
                distinctLiveData.postValue(lastObj)
            } else if ((obj == null && lastObj != null) || obj != lastObj) {
                lastObj = obj
                distinctLiveData.postValue(lastObj)
            }
        }
    })
    return distinctLiveData
}
