package com.arjanvlek.oxygenupdater.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.i18n.AppLocale
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.UpdateMethod

object CustomDropdown {

    fun initCustomDeviceDropdown(
        currentPosition: Int,
        convertView: View?,
        parent: ViewGroup?, @LayoutRes layoutType: Int,
        devices: List<Device>,
        recommendedPosition: Int,
        context: Context?
    ): View? {
        var convertView = convertView
        if (convertView == null) {
            val inflater = (Utils.getSystemService(context, Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?) ?: return View(context)
            // Create either the selected item or the dropdown menu.
            convertView = inflater.inflate(layoutType, parent, false)
        }

        // Set the text of the item to the device's name
        convertView!!.findViewById<TextView>(android.R.id.text1).apply {
            text = devices[currentPosition].name

            // If this device is the recommended device, change the text color to green
            // Otherwise, set it to the default foreground
            if (recommendedPosition != -1) {
                if (currentPosition == recommendedPosition) {
                    setTextColor(ContextCompat.getColor(context!!, R.color.colorPositive))
                } else {
                    setTextColor(ContextCompat.getColor(context!!, R.color.foreground))
                }
            } else {
                setTextColor(ContextCompat.getColor(context!!, R.color.foreground))
            }
        }

        return convertView
    }

    fun initCustomUpdateMethodDropdown(
        currentPosition: Int,
        convertView: View?,
        parent: ViewGroup?, @LayoutRes layoutType: Int,
        updateMethods: List<UpdateMethod>,
        recommendedPositions: ArrayList<Int>?,
        context: Context?
    ): View? {
        var convertView = convertView
        if (convertView == null) {
            val inflater = (Utils.getSystemService(context, Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?) ?: return View(context)
            convertView = inflater.inflate(layoutType, parent, false)
        }

        convertView!!.findViewById<TextView>(android.R.id.text1).apply {
            // Set the view to the update method's name, according to the app language.
            text = if (AppLocale.get() == AppLocale.NL) updateMethods[currentPosition].dutchName else updateMethods[currentPosition].englishName
            setTextColor(ContextCompat.getColor(context!!, R.color.foreground))

            // If this is a recommended update method, make the text green
            recommendedPositions?.forEach {
                if (currentPosition == it) {
                    setTextColor(ContextCompat.getColor(context, R.color.colorPositive))
                }
            }
        }

        return convertView
    }
}
