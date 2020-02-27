package com.arjanvlek.oxygenupdater.internal.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.TypedArrayUtils
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.preference.Preference
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.utils.Logger.logInfo
import com.arjanvlek.oxygenupdater.utils.Logger.logVerbose
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.java.KoinJavaComponent.inject
import java.util.*
import kotlin.math.min

/**
 * Overridden class to add custom functionality (setting title/message, custom on click listeners for dismissing).
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Suppress("unused")
class BottomSheetPreference : Preference {

    private lateinit var mContext: Context
    private lateinit var dialogLayout: LinearLayout
    private lateinit var itemListContainer: LinearLayout
    private lateinit var dialog: BottomSheetDialog

    private var mOnChangeListener: OnPreferenceChangeListener? = null
    private var secondaryKey: String? = null
    private var title: String? = null
    private var caption: String? = null
    private var itemList: MutableList<BottomSheetItem> = ArrayList()
    private var valueSet = false

    var value: Any? = null
        private set
    var secondaryValue: Any? = null
        private set

    private val settingsManager by inject(SettingsManager::class.java)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    /**
     * Initialise the preference
     *
     * @param context      the context
     * @param attrs        the attributes
     * @param defStyleAttr default style attributes
     * @param defStyleRes  default style resource
     */
    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        this.mContext = context

        readAttrs(attrs, defStyleAttr, defStyleRes)

        if (secondaryKey == null) {
            secondaryKey = key + "_id"
        }

        setupDialog()
    }

    /**
     * Setup the internal [BottomSheetDialog]
     */
    @SuppressLint("InflateParams")
    private fun setupDialog() = BottomSheetDialog(mContext).let {
        dialog = it

        val inflater = LayoutInflater.from(mContext)

        (inflater.inflate(R.layout.bottom_sheet_preference, null, false) as LinearLayout).apply {
            dialogLayout = this

            itemListContainer = findViewById(R.id.dialog_item_list_container)

            setText(findViewById(R.id.dialog_title), title)
            setText(findViewById(R.id.dialog_caption), caption)

            logVerbose(TAG, "Setup dialog with title='$title', subtitle='$caption', and '${itemList.size}' items")

            itemList.indices.forEach { index ->
                val dialogItemLayout = inflater.inflate(
                    R.layout.bottom_sheet_preference_item,
                    itemListContainer,
                    false
                ) as LinearLayout

                // add the item's view at the specified index
                itemListContainer.addView(dialogItemLayout, index)

                setupItemView(dialogItemLayout, index)
            }

            dialog.setContentView(this)

            // Open up the dialog fully by default
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
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

        logVerbose(TAG, "Setup item with title='$title', and subtitle='$caption'")

        dialogItemLayout.setOnClickListener { setValueIndex(index) }

        val currentValue = settingsManager.getPreference<Any?>(key, null)
        val currentSecondaryValue = settingsManager.getPreference<Any?>(secondaryKey, null)
        val secondaryValue = item.secondaryValue

        // value is mandatory, secondary value is optional
        if (item.value == currentValue || secondaryValue != null && secondaryValue == currentSecondaryValue) {
            markItemSelected(index)
        } else {
            markItemUnselected(index)
        }
    }

    private fun redrawItemView(index: Int) = setupItemView(
        itemListContainer.getChildAt(index) as LinearLayout,
        index
    )

    /**
     * Reads attributes defined in XML and sets relevant fields
     *
     * @param attrs        the attributes
     * @param defStyleAttr default style attribute
     * @param defStyleRes  default style resource
     */
    private fun readAttrs(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a = mContext.obtainStyledAttributes(attrs, R.styleable.BottomSheetPreference, defStyleAttr, defStyleRes)

        title = TypedArrayUtils.getString(a, R.styleable.BottomSheetPreference_title, R.styleable.BottomSheetPreference_android_title)
        caption = a.getString(R.styleable.BottomSheetPreference_caption)
        secondaryKey = a.getString(R.styleable.BottomSheetPreference_secondaryKey)

        val entries = a.getTextArray(R.styleable.BottomSheetPreference_android_entries)
        val entryValues = a.getTextArray(R.styleable.BottomSheetPreference_android_entryValues)
        val titleEntries = a.getTextArray(R.styleable.BottomSheetPreference_titleEntries)
        val subtitleEntries = a.getTextArray(R.styleable.BottomSheetPreference_subtitleEntries)
        val resourceId = a.getResourceId(R.styleable.BottomSheetPreference_secondaryEntryValues, 0)

        itemList = ArrayList()

        val intEntryValues = if (resourceId != 0) mContext.resources.getIntArray(resourceId) else null
        val numberOfItems = entries?.size ?: (titleEntries?.size ?: 0)

        for (i in 0 until numberOfItems) {
            val item = BottomSheetItem(value = entryValues[i].toString())

            val title = if (entries != null) entries[i] else titleEntries!![i]
            val subtitle = subtitleEntries?.get(i)
            val secondaryValue: Any? = intEntryValues?.get(i)

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
    private fun setText(textView: TextView, text: String?) = if (text != null) {
        textView.isVisible = true
        textView.text = text
    } else {
        textView.isVisible = false
    }

    /**
     * Finds index of item with newValue or newSecondaryValue and calls [.setValueIndex]
     *
     * @param newValue          the new value
     * @param newSecondaryValue the new integer value
     */
    private fun setValues(newValue: Any?, newSecondaryValue: Any?) {
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
        val changed = newValue != value
        if (changed || !valueSet) {
            value = newValue
            secondaryValue = newSecondaryValue
            valueSet = true
            settingsManager.savePreference(key, newValue)

            if (newSecondaryValue != null) {
                settingsManager.savePreference(secondaryKey, newSecondaryValue)
            }

            if (changed) {
                // redraw previous selected and new selected layouts
                remarkItems(newValue)
                onChange()
            }
        }
    }

    fun setSecondaryKey(secondaryKey: String?) {
        logInfo(TAG, String.format(Locale.getDefault(), "Updating secondaryKey: %s", secondaryKey))
        this.secondaryKey = secondaryKey
    }

    override fun setTitle(title: CharSequence) {
        logInfo(TAG, String.format(Locale.getDefault(), "Updating dialog title: %s", title))
        this.title = title.toString()
        setText(dialogLayout.findViewById(R.id.dialog_title), title.toString())
        super.setTitle(title)
    }

    public override fun onClick() = dialog.show()

    /**
     * Sets the callback to be invoked when this preference is changed by the user (but before
     * the internal state has been updated).
     *
     * @param onPreferenceChangeListener The callback to be invoked
     */
    override fun setOnPreferenceChangeListener(onPreferenceChangeListener: OnPreferenceChangeListener) = super.setOnPreferenceChangeListener(
        onPreferenceChangeListener
    ).let {
        mOnChangeListener = onPreferenceChangeListener
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val newValue = settingsManager.getPreference<Any?>(key, null)
        val newSecondaryValue = settingsManager.getPreference<Any?>(secondaryKey, null)
        setValues(newValue, newSecondaryValue)
    }

    override fun onSaveInstanceState(): Parcelable = super.onSaveInstanceState().let { superState ->
        if (isPersistent) {
            // No need to save instance state since it's persistent
            superState
        } else {
            SavedState(superState).also {
                it.value = value
                it.secondaryValue = secondaryValue
            }
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) { // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)

        setValues(myState.value, myState.secondaryValue)
    }

    fun setCaption(caption: String?) {
        logInfo(TAG, String.format(Locale.getDefault(), "Updating dialog caption: %s", caption))
        this.caption = caption
        setText(dialogLayout.findViewById(R.id.dialog_caption), caption)
    }

    fun setEntries(entries: Array<CharSequence>) {
        logInfo(TAG, String.format(Locale.getDefault(), "Updating titles for %d items", entries.size))

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
        logInfo(TAG, String.format(Locale.getDefault(), "Updating titles for %d items", titles.size))

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
        logInfo(TAG, String.format(Locale.getDefault(), "Updating subtitles for %d items", subtitles.size))

        // don't care if items and subtitles aren't equal in length
        // replace subtitle for all indices that are common
        val length = min(itemList.size, subtitles.size)
        for (i in 0 until length) {
            itemList[i].subtitle = subtitles[i].toString()
            redrawItemView(i)
        }
    }

    fun setEntryValues(entryValues: Array<CharSequence>) {
        logInfo(TAG, String.format(Locale.getDefault(), "Updating entryValues for %d items", entryValues.size))

        // don't care if items and entryValues aren't equal in length
        // replace value for all indices that are common
        val length = min(itemList.size, entryValues.size)
        for (i in 0 until length) {
            itemList[i].value = entryValues[i].toString()
        }
    }

    fun setSecondaryEntryValues(objectEntryValues: Array<Any?>) {
        logInfo(TAG, String.format(Locale.getDefault(), "Updating secondaryValues for %d items", objectEntryValues.size))

        // don't care if items and objectEntryValues aren't equal in length
        // replace secondaryValue for all indices that are common
        val length = min(itemList.size, objectEntryValues.size)
        for (i in 0 until length) {
            itemList[i].secondaryValue = objectEntryValues[i]
        }
    }

    fun setItemList(itemList: List<BottomSheetItem>) {
        logInfo(TAG, String.format(Locale.getDefault(), "Populating itemList with %d items", itemList.size))

        // copy list instead of shallow-referencing it
        this.itemList = ArrayList()
        this.itemList.addAll(itemList)

        setupDialog()
        onSetInitialValue(null)
    }

    /**
     * Iterates over [.itemList] to remark items as selected/unselected
     *
     * @param value the new value
     */
    private fun remarkItems(value: Any?) {
        logVerbose(TAG, "Remarking items as selected/unselected")

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
            val dialogItemLayout = itemListContainer.getChildAt(selectedIndex) as LinearLayout
            val checkmarkView = dialogItemLayout.findViewById<ImageView>(R.id.dialog_item_checkmark)

            checkmarkView.isInvisible = true

            TypedValue().apply {
                mContext.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
                dialogItemLayout.setBackgroundResource(resourceId)
            }

            logVerbose(TAG, String.format(Locale.getDefault(), "Item #%d marked unselected with title='%s', and subtitle='%s'", selectedIndex, title, caption))
        }
    }

    /**
     * Shows the checkmark icon and sets background to [R.drawable.rounded_overlay]
     *
     * @param selectedIndex index of the selected item
     */
    private fun markItemSelected(selectedIndex: Int) {
        if (selectedIndex != -1) {
            val dialogItemLayout = itemListContainer.getChildAt(selectedIndex) as LinearLayout
            val checkmarkView = dialogItemLayout.findViewById<ImageView>(R.id.dialog_item_checkmark)

            checkmarkView.isVisible = true
            dialogItemLayout.setBackgroundResource(R.drawable.rounded_overlay)

            logVerbose(TAG, String.format(Locale.getDefault(), "Item #%d marked selected with title='%s', and subtitle='%s'", selectedIndex, title, caption))
        }
    }

    /**
     * Called when value changes. Updates summary, closes the [.dialog], an notifies on change listeners
     */
    private fun onChange() {
        summary = value.toString()

        dialog.cancel()

        if (mOnChangeListener != null) {
            mOnChangeListener!!.onPreferenceChange(this, value)
        }
    }

    private class SavedState : BaseSavedState {

        var value: Any? = null
        var secondaryValue: Any? = null

        internal constructor(source: Parcel) : super(source) {
            value = source.readString()
            secondaryValue = source.readLong()
        }

        internal constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) = super.writeToParcel(dest, flags).let {
            dest.writeString(value.toString())
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState? = SavedState(parcel)

            override fun newArray(size: Int) = arrayOfNulls<SavedState?>(size)
        }

        override fun describeContents() = 0
    }

    companion object {
        private const val TAG = "BottomSheetPreference"
    }
}
