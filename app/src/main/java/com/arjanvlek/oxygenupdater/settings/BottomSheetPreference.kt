package com.arjanvlek.oxygenupdater.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import com.arjanvlek.oxygenupdater.R
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.lang.Math.min
import java.util.*

/**
 * Overridden class to add custom functionality (setting title/message, custom on click listeners for dismissing).
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class BottomSheetPreference : Preference {

    private var contextRef: Context? = null
    private var mOnChangeListener: OnPreferenceChangeListener? = null

    private var dialogLayout: LinearLayout? = null
    private var itemListContainer: LinearLayout? = null
    private var dialog: BottomSheetDialog? = null

    private var secondaryKey: String? = null
    private var title: String? = null
    private var caption: String? = null
    private var itemList: MutableList<BottomSheetItem> = ArrayList()
    private var valueSet: Boolean = false
    var value: String? = null
        private set
    var secondaryValue: Any? = null
        private set

    private var settingsManager: SettingsManager? = null

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {

        init(context, attrs, defStyleAttr, defStyleRes)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        init(context, attrs, defStyleAttr, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        init(context, attrs, 0, 0)
    }

    constructor(context: Context) : super(context) {

        init(context, null, 0, 0)
    }

    /**
     * Initialise the preference
     *
     * @param context      the contextRef
     * @param attrs        the attributes
     * @param defStyleAttr default style attributes
     * @param defStyleRes  default style resource
     */
    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        this.contextRef = context

        secondaryKey = key + "_id"
        settingsManager = SettingsManager(context)

        readAttrs(attrs, defStyleAttr, defStyleRes)

        setupDialog()
    }

    /**
     * Setup the internal [BottomSheetDialog]
     */
    @SuppressLint("InflateParams")
    private fun setupDialog() {
        dialog = BottomSheetDialog(contextRef!!)
        val inflater = LayoutInflater.from(contextRef)

        dialogLayout = inflater.inflate(R.layout.bottom_sheet, null, false) as LinearLayout
        itemListContainer = dialogLayout!!.findViewById(R.id.dialog_item_list_container)

        setText(dialogLayout!!.findViewById(R.id.dialog_title), title)
        setText(dialogLayout!!.findViewById(R.id.dialog_caption), caption)

        for (i in itemList.indices) {
            val dialogItemLayout = inflater.inflate(R.layout.bottom_sheet_item, itemListContainer, false) as LinearLayout

            // add the item's view at the specified index
            itemListContainer!!.addView(dialogItemLayout, i)

            setupItemView(dialogItemLayout, i)
        }

        dialog!!.setContentView(dialogLayout)

        // Open up the dialog fully by default
        dialog!!.behavior.state = STATE_EXPANDED
    }

    /**
     * Initialises an item layout. Sets text if needed, and marks selected/unselected as well
     *
     * @param dialogItemLayout parent linear layout
     * @param index            item index
     */
    private fun setupItemView(dialogItemLayout: LinearLayout, index: Int) {
        val item = itemList[index]

        val titleView = dialogItemLayout.findViewById<TextView>(R.id.dialog_item_title)
        val subtitleView = dialogItemLayout.findViewById<TextView>(R.id.dialog_item_subtitle)

        setText(titleView, item.title)
        setText(subtitleView, item.subtitle)

        dialogItemLayout.setOnClickListener { setValueIndex(index) }

        val currentValue = settingsManager!!.getPreference(key, "")

        if (item.value == currentValue) {
            markItemSelected(index)
        } else {
            markItemUnselected(index)
        }
    }

    private fun redrawItemView(index: Int) {
        val dialogItemLayout = itemListContainer!!.getChildAt(index) as LinearLayout

        setupItemView(dialogItemLayout, index)
    }

    /**
     * Reads attributes defined in XML and sets relevant fields
     *
     * @param attrs        the attributes
     * @param defStyleAttr default style attribute
     * @param defStyleRes  default style resource
     */
    private fun readAttrs(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a = contextRef!!.obtainStyledAttributes(attrs, R.styleable.BottomSheetPreference,
                defStyleAttr, defStyleRes)

        title = TypedArrayUtils.getString(a, R.styleable.BottomSheetPreference_title,
                R.styleable.BottomSheetPreference_android_title)
        caption = a.getString(R.styleable.BottomSheetPreference_caption)

        val entries = a.getTextArray(R.styleable.BottomSheetPreference_android_entries)
        val entryValues = a.getTextArray(R.styleable.BottomSheetPreference_android_entryValues)

        val titleEntries = a.getTextArray(R.styleable.BottomSheetPreference_titleEntries)
        val subtitleEntries = a.getTextArray(R.styleable.BottomSheetPreference_subtitleEntries)
        val resourceId = a.getResourceId(R.styleable.BottomSheetPreference_secondaryEntryValues, 0)

        itemList = ArrayList()

        val intEntryValues = if (resourceId != 0)
            contextRef!!.resources.getIntArray(resourceId)
        else
            null

        val numberOfItems = entries?.size ?: (titleEntries?.size ?: 0)

        for (i in 0 until numberOfItems) {
            val item = BottomSheetItem(value = entryValues[i].toString())

            val title = if (entries != null) entries[i] else titleEntries!![i]
            val subtitle = if (subtitleEntries != null) subtitleEntries[i] else null
            val secondaryValue = if (intEntryValues != null) intEntryValues[i].toLong() else null

            if (title != null) {
                item.title = title.toString()
            }

            if (subtitle != null) {
                item.subtitle = subtitle.toString()
            }

            if (secondaryValue != null) {
                item.secondaryValue = secondaryValue
            }

            itemList.add(item)
        }

        a.recycle()
    }

    /**
     * Utility method that sets text if the supplied text is not null, otherwise hides the [TextView]
     *
     * @param textView the TextView
     * @param text     the text
     */
    private fun setText(textView: TextView, text: String?) {
        if (text != null) {
            textView.visibility = VISIBLE
            textView.text = text
        } else {
            textView.visibility = GONE
        }
    }

    /**
     * Finds index of item with newValue or newSecondaryValue and calls [.setValueIndex]
     *
     * @param newValue          the new value
     * @param newSecondaryValue the new integer value
     */
    private fun setValues(newValue: String?, newSecondaryValue: Any?) {
        var selectedIndex = -1

        for (i in itemList.indices) {
            val item = itemList[i]
            val value = item.value
            val secondaryValue = item.secondaryValue

            if (value != null && value == newValue || secondaryValue != null && secondaryValue == newSecondaryValue) {
                selectedIndex = i
                break
            }
        }

        if (selectedIndex != -1) {
            setValueIndex(selectedIndex)
        }
    }

    /**
     * Sets the value of the key. secondaryValue is optional.
     *
     *
     * Also marks previous item as unselected and new item as selected
     *
     * @param selectedIndex the selected index
     */
    fun setValueIndex(selectedIndex: Int) {
        val item = itemList[selectedIndex]

        val newValue = item.value
        val newSecondaryValue = item.secondaryValue

        // Always persist/notify the first time.
        val changed = !TextUtils.equals(value, newValue)

        if (changed || !valueSet) {
            value = newValue
            secondaryValue = newSecondaryValue
            valueSet = true

            persistString(newValue)

            if (newSecondaryValue != null) {
                settingsManager!!.savePreference(secondaryKey!!, newSecondaryValue)
            }

            if (changed) {
                // redraw previous selected and new selected layouts
                remarkItems(newValue)
                onChange()
            }
        }
    }

    fun setSecondaryKey(secondaryKey: String) {
        this.secondaryKey = secondaryKey
    }

    override fun setTitle(title: CharSequence) {
        this.title = title.toString()

        setText(dialogLayout!!.findViewById(R.id.dialog_title), title.toString())

        super.setTitle(title)
    }

    public override fun onClick() {
        if (dialog != null) {
            dialog!!.show()
        }
    }

    /**
     * Sets the callback to be invoked when this preference is changed by the user (but before
     * the internal state has been updated).
     *
     * @param onPreferenceChangeListener The callback to be invoked
     */
    override fun setOnPreferenceChangeListener(onPreferenceChangeListener: OnPreferenceChangeListener) {
        mOnChangeListener = onPreferenceChangeListener
        super.setOnPreferenceChangeListener(onPreferenceChangeListener)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val newValue = getPersistedString(defaultValue as String?)
        val newSecondaryValue = settingsManager!!.getPreference<Any>(secondaryKey!!, "")

        setValues(newValue, newSecondaryValue)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        val myState = SavedState(superState)
        myState.value = value
        myState.secondaryValue = secondaryValue
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.superState)

        setValues(myState.value, myState.secondaryValue)
    }

    fun setCaption(caption: String) {
        this.caption = caption

        setText(dialogLayout!!.findViewById(R.id.dialog_caption), caption)
    }

    fun setEntries(entries: Array<CharSequence>) {
        // don't care if items and entries aren't equal in length
        // replace title for all indices that are common
        val length = min(itemList.size, entries.size)

        for (i in 0 until length) {
            itemList[i].title = entries[i].toString()

            redrawItemView(i)
        }

        onSetInitialValue(null)
    }

    fun setTitleEntries(titles: Array<CharSequence>) {
        // don't care if items and titles aren't equal in length
        // replace title for all indices that are common
        val length = min(itemList.size, titles.size)

        for (i in 0 until length) {
            itemList[i].title = titles[i].toString()

            redrawItemView(i)
        }

        onSetInitialValue(null)
    }

    fun setSubtitleEntries(subtitles: Array<CharSequence>) {
        // don't care if items and subtitles aren't equal in length
        // replace subtitle for all indices that are common
        val length = min(itemList.size, subtitles.size)

        for (i in 0 until length) {
            itemList[i].subtitle = subtitles[i].toString()

            redrawItemView(i)
        }
    }

    fun setEntryValues(entryValues: Array<CharSequence>) {
        // don't care if items and entryValues aren't equal in length
        // replace value for all indices that are common
        val length = min(itemList.size, entryValues.size)

        for (i in 0 until length) {
            itemList[i].value = entryValues[i].toString()
        }
    }

    fun setSecondaryEntryValues(objectEntryValues: Array<Any>) {
        // don't care if items and objectEntryValues aren't equal in length
        // replace secondaryValue for all indices that are common
        val length = min(itemList.size, objectEntryValues.size)

        for (i in 0 until length) {
            itemList[i].secondaryValue = objectEntryValues[i]
        }
    }

    fun setItemList(itemList: List<BottomSheetItem>) {
        this.itemList = ArrayList()
        // copy list instead of shallow-referencing it
        this.itemList.addAll(itemList)

        setupDialog()

        onSetInitialValue(null)
    }

    /**
     * Iterates over [.itemList] to remark items as selected/unselected
     *
     * @param value the new value
     */
    private fun remarkItems(value: String?) {
        for (i in itemList.indices) {
            val item = itemList[i]

            if (item.value == value) {
                markItemSelected(i)
            } else {
                markItemUnselected(i)
            }
        }
    }

    /**
     * Hides the checkmark icon and resets background to [android.R.attr.selectableItemBackground]
     *
     * @param selectedIndex index of the selected item
     */
    private fun markItemUnselected(selectedIndex: Int) {
        if (selectedIndex != -1) {
            val dialogItemLayout = itemListContainer!!.getChildAt(selectedIndex) as LinearLayout
            val checkmarkView = dialogItemLayout.findViewById<ImageView>(R.id.dialog_item_checkmark)

            checkmarkView.visibility = INVISIBLE
            val outValue = TypedValue()
            contextRef!!.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            dialogItemLayout.setBackgroundResource(outValue.resourceId)
        }
    }

    /**
     * Shows the checkmark icon and sets background to [R.drawable.rounded_overlay]
     *
     * @param selectedIndex index of the selected item
     */
    private fun markItemSelected(selectedIndex: Int) {
        if (selectedIndex != -1) {
            val dialogItemLayout = itemListContainer!!.getChildAt(selectedIndex) as LinearLayout
            val checkmarkView = dialogItemLayout.findViewById<ImageView>(R.id.dialog_item_checkmark)

            checkmarkView.visibility = VISIBLE
            dialogItemLayout.setBackgroundResource(R.drawable.rounded_overlay)
        }
    }

    /**
     * Called when value changes. Updates summary, closes the [.dialog], an notifies on change listeners
     */
    private fun onChange() {
        summary = value

        dialog!!.cancel()

        if (mOnChangeListener != null) {
            mOnChangeListener!!.onPreferenceChange(this, value)
        }
    }

    private class SavedState : BaseSavedState {

        internal var value: String? = null
        internal var secondaryValue: Any? = null

        internal constructor(source: Parcel) : super(source) {
            value = source.readString()
            secondaryValue = source.readLong()
        }

        internal constructor(superState: Parcelable) : super(superState) {}

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(value)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}


