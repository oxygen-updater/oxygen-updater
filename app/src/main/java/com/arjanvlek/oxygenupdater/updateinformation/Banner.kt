package com.arjanvlek.oxygenupdater.updateinformation


import android.content.Context

interface Banner {

    fun getBannerText(context: Context): CharSequence

    fun getColor(context: Context): Int
}
