package com.arjanvlek.oxygenupdater.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.Preference;

import com.arjanvlek.oxygenupdater.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;
import static java.lang.Math.min;

/**
 * Overridden class to add custom functionality (setting title/message, custom on click listeners for dismissing).
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
@SuppressWarnings("WeakerAccess")
public class BottomSheetPreference extends Preference {

	private Context context;
	private OnPreferenceChangeListener mOnChangeListener;

	private LinearLayout dialogLayout;
	private LinearLayout itemListContainer;
	private BottomSheetDialog dialog;

	private String secondaryKey;
	private String title;
	private String caption;
	private List<BottomSheetItem> itemList = new ArrayList<>();
	private boolean valueSet;
	private String value;
	private Object secondaryValue;

	private SettingsManager settingsManager;

	public BottomSheetPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		init(context, attrs, defStyleAttr, defStyleRes);
	}

	public BottomSheetPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		init(context, attrs, defStyleAttr, 0);
	}

	public BottomSheetPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs, 0, 0);
	}

	public BottomSheetPreference(Context context) {
		super(context);

		init(context, null, 0, 0);
	}

	/**
	 * Initialise the preference
	 *
	 * @param context      the context
	 * @param attrs        the attributes
	 * @param defStyleAttr default style attributes
	 * @param defStyleRes  default style resource
	 */
	private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		this.context = context;

		secondaryKey = getKey() + "_id";
		settingsManager = new SettingsManager(context);

		readAttrs(attrs, defStyleAttr, defStyleRes);

		setupDialog();
	}

	/**
	 * Setup the internal {@link BottomSheetDialog}
	 */
	@SuppressLint("InflateParams")
	private void setupDialog() {
		dialog = new BottomSheetDialog(context);
		LayoutInflater inflater = LayoutInflater.from(context);

		dialogLayout = (LinearLayout) inflater.inflate(R.layout.bottom_sheet, null, false);
		itemListContainer = dialogLayout.findViewById(R.id.dialog_item_list_container);

		setText(dialogLayout.findViewById(R.id.dialog_title), title);
		setText(dialogLayout.findViewById(R.id.dialog_caption), caption);

		for (int i = 0; i < itemList.size(); i++) {
			LinearLayout dialogItemLayout = (LinearLayout) inflater.inflate(R.layout.bottom_sheet_item, itemListContainer, false);

			// add the item's view at the specified index
			itemListContainer.addView(dialogItemLayout, i);

			setupItemView(dialogItemLayout, i);
		}

		dialog.setContentView(dialogLayout);

		// Open up the dialog fully by default
		dialog.getBehavior().setState(STATE_EXPANDED);
	}

	/**
	 * Initialises an item layout. Sets text if needed, and marks selected/unselected as well
	 *
	 * @param dialogItemLayout parent linear layout
	 * @param index            item index
	 */
	private void setupItemView(LinearLayout dialogItemLayout, int index) {
		BottomSheetItem item = itemList.get(index);

		TextView titleView = dialogItemLayout.findViewById(R.id.dialog_item_title);
		TextView subtitleView = dialogItemLayout.findViewById(R.id.dialog_item_subtitle);

		setText(titleView, item.getTitle());
		setText(subtitleView, item.getSubtitle());

		dialogItemLayout.setOnClickListener(view -> setValueIndex(index));

		String currentValue = settingsManager.getPreference(getKey(), null);

		if (item.getValue().equals(currentValue)) {
			markItemSelected(index);
		} else {
			markItemUnselected(index);
		}
	}

	private void redrawItemView(int index) {
		LinearLayout dialogItemLayout = (LinearLayout) itemListContainer.getChildAt(index);

		setupItemView(dialogItemLayout, index);
	}

	/**
	 * Reads attributes defined in XML and sets relevant fields
	 *
	 * @param attrs        the attributes
	 * @param defStyleAttr default style attribute
	 * @param defStyleRes  default style resource
	 */
	private void readAttrs(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BottomSheetPreference, defStyleAttr, defStyleRes);

		title = TypedArrayUtils.getString(a, R.styleable.BottomSheetPreference_title, R.styleable.BottomSheetPreference_android_title);
		caption = a.getString(R.styleable.BottomSheetPreference_caption);

		CharSequence[] entries = a.getTextArray(R.styleable.BottomSheetPreference_android_entries);
		CharSequence[] entryValues = a.getTextArray(R.styleable.BottomSheetPreference_android_entryValues);

		CharSequence[] titleEntries = a.getTextArray(R.styleable.BottomSheetPreference_titleEntries);
		CharSequence[] subtitleEntries = a.getTextArray(R.styleable.BottomSheetPreference_subtitleEntries);
		int resourceId = a.getResourceId(R.styleable.BottomSheetPreference_secondaryEntryValues, 0);

		itemList = new ArrayList<>();

		int[] intEntryValues = resourceId != 0
				? context.getResources().getIntArray(resourceId)
				: null;

		int numberOfItems = entries != null
				? entries.length
				: titleEntries != null
				? titleEntries.length
				: 0;

		for (int i = 0; i < numberOfItems; i++) {
			BottomSheetItem item = BottomSheetItem.builder()
					.value(entryValues[i].toString())
					.build();

			CharSequence title = entries != null ? entries[i] : titleEntries[i];
			CharSequence subtitle = subtitleEntries != null ? subtitleEntries[i] : null;
			Object secondaryValue = intEntryValues != null ? ((long) intEntryValues[i]) : null;

			if (title != null) {
				item.setTitle(title.toString());
			}

			if (subtitle != null) {
				item.setSubtitle(subtitle.toString());
			}

			if (secondaryValue != null) {
				item.setSecondaryValue(secondaryValue);
			}

			itemList.add(item);
		}

		a.recycle();
	}

	/**
	 * Utility method that sets text if the supplied text is not null, otherwise hides the {@link TextView}
	 *
	 * @param textView the TextView
	 * @param text     the text
	 */
	private void setText(TextView textView, String text) {
		if (text != null) {
			textView.setVisibility(VISIBLE);
			textView.setText(text);
		} else {
			textView.setVisibility(GONE);
		}
	}

	/**
	 * Finds index of item with newValue or newSecondaryValue and calls {@link #setValueIndex(int)}
	 *
	 * @param newValue          the new value
	 * @param newSecondaryValue the new integer value
	 */
	private void setValues(String newValue, Object newSecondaryValue) {
		int selectedIndex = -1;

		for (int i = 0; i < itemList.size(); i++) {
			BottomSheetItem item = itemList.get(i);
			String value = item.getValue();
			Object secondaryValue = item.getSecondaryValue();

			if ((value != null && value.equals(newValue)) || (secondaryValue != null && secondaryValue.equals(newSecondaryValue))) {
				selectedIndex = i;
				break;
			}
		}

		if (selectedIndex != -1) {
			setValueIndex(selectedIndex);
		}
	}

	/**
	 * Sets the value of the key. secondaryValue is optional.
	 * <p>
	 * Also marks previous item as unselected and new item as selected
	 *
	 * @param selectedIndex the selected index
	 */
	public void setValueIndex(int selectedIndex) {
		BottomSheetItem item = itemList.get(selectedIndex);

		String newValue = item.getValue();
		Object newSecondaryValue = item.getSecondaryValue();

		// Always persist/notify the first time.
		boolean changed = !TextUtils.equals(value, newValue);

		if (changed || !valueSet) {
			value = newValue;
			secondaryValue = newSecondaryValue;
			valueSet = true;

			persistString(newValue);

			if (newSecondaryValue != null) {
				settingsManager.savePreference(secondaryKey, newSecondaryValue);
			}

			if (changed) {
				// redraw previous selected and new selected layouts
				remarkItems(newValue);
				onChange();
			}
		}
	}

	public void setSecondaryKey(String secondaryKey) {
		this.secondaryKey = secondaryKey;
	}

	@Override
	public void setTitle(CharSequence title) {
		this.title = title.toString();

		setText(dialogLayout.findViewById(R.id.dialog_title), title.toString());

		super.setTitle(title);
	}

	@Override
	public void onClick() {
		if (dialog != null) {
			dialog.show();
		}
	}

	/**
	 * Sets the callback to be invoked when this preference is changed by the user (but before
	 * the internal state has been updated).
	 *
	 * @param onPreferenceChangeListener The callback to be invoked
	 */
	@Override
	public void setOnPreferenceChangeListener(OnPreferenceChangeListener onPreferenceChangeListener) {
		mOnChangeListener = onPreferenceChangeListener;
		super.setOnPreferenceChangeListener(onPreferenceChangeListener);
	}

	@Override
	protected void onSetInitialValue(Object defaultValue) {
		String newValue = getPersistedString((String) defaultValue);
		Object newSecondaryValue = settingsManager.getPreference(secondaryKey, null);

		setValues(newValue, newSecondaryValue);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		if (isPersistent()) {
			// No need to save instance state since it's persistent
			return superState;
		}

		SavedState myState = new SavedState(superState);
		myState.value = value;
		myState.secondaryValue = secondaryValue;
		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());

		setValues(myState.value, myState.secondaryValue);
	}

	public void setCaption(String caption) {
		this.caption = caption;

		setText(dialogLayout.findViewById(R.id.dialog_caption), caption);
	}

	public void setEntries(CharSequence[] entries) {
		// don't care if items and entries aren't equal in length
		// replace title for all indices that are common
		int length = min(itemList.size(), entries.length);

		for (int i = 0; i < length; i++) {
			itemList.get(i).setTitle(entries[i].toString());

			redrawItemView(i);
		}

		onSetInitialValue(null);
	}

	public void setTitleEntries(CharSequence[] titles) {
		// don't care if items and titles aren't equal in length
		// replace title for all indices that are common
		int length = min(itemList.size(), titles.length);

		for (int i = 0; i < length; i++) {
			itemList.get(i).setTitle(titles[i].toString());

			redrawItemView(i);
		}

		onSetInitialValue(null);
	}

	public void setSubtitleEntries(CharSequence[] subtitles) {
		// don't care if items and subtitles aren't equal in length
		// replace subtitle for all indices that are common
		int length = min(itemList.size(), subtitles.length);

		for (int i = 0; i < length; i++) {
			itemList.get(i).setSubtitle(subtitles[i].toString());

			redrawItemView(i);
		}
	}

	public void setEntryValues(CharSequence[] entryValues) {
		// don't care if items and entryValues aren't equal in length
		// replace value for all indices that are common
		int length = min(itemList.size(), entryValues.length);

		for (int i = 0; i < length; i++) {
			itemList.get(i).setValue(entryValues[i].toString());
		}
	}

	public void setSecondaryEntryValues(Object[] objectEntryValues) {
		// don't care if items and objectEntryValues aren't equal in length
		// replace secondaryValue for all indices that are common
		int length = min(itemList.size(), objectEntryValues.length);

		for (int i = 0; i < length; i++) {
			itemList.get(i).setSecondaryValue(objectEntryValues[i]);
		}
	}

	public void setItemList(@NonNull List<BottomSheetItem> itemList) {
		this.itemList = new ArrayList<>();
		// copy list instead of shallow-referencing it
		this.itemList.addAll(itemList);

		setupDialog();

		onSetInitialValue(null);
	}

	public String getValue() {
		return value;
	}

	public Object getSecondaryValue() {
		return secondaryValue;
	}

	/**
	 * Iterates over {@link #itemList} to remark items as selected/unselected
	 *
	 * @param value the new value
	 */
	private void remarkItems(String value) {
		for (int i = 0; i < itemList.size(); i++) {
			BottomSheetItem item = itemList.get(i);

			if (item.getValue().equals(value)) {
				markItemSelected(i);
			} else {
				markItemUnselected(i);
			}
		}
	}

	/**
	 * Hides the checkmark icon and resets background to {@link android.R.attr#selectableItemBackground}
	 *
	 * @param selectedIndex index of the selected item
	 */
	private void markItemUnselected(int selectedIndex) {
		if (selectedIndex != -1) {
			LinearLayout dialogItemLayout = (LinearLayout) itemListContainer.getChildAt(selectedIndex);
			ImageView checkmarkView = dialogItemLayout.findViewById(R.id.dialog_item_checkmark);

			checkmarkView.setVisibility(INVISIBLE);
			TypedValue outValue = new TypedValue();
			context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
			dialogItemLayout.setBackgroundResource(outValue.resourceId);
		}
	}

	/**
	 * Shows the checkmark icon and sets background to {@link R.drawable#rounded_overlay}
	 *
	 * @param selectedIndex index of the selected item
	 */
	private void markItemSelected(int selectedIndex) {
		if (selectedIndex != -1) {
			LinearLayout dialogItemLayout = (LinearLayout) itemListContainer.getChildAt(selectedIndex);
			ImageView checkmarkView = dialogItemLayout.findViewById(R.id.dialog_item_checkmark);

			checkmarkView.setVisibility(VISIBLE);
			dialogItemLayout.setBackgroundResource(R.drawable.rounded_overlay);
		}
	}

	/**
	 * Called when value changes. Updates summary, closes the {@link #dialog}, an notifies on change listeners
	 */
	private void onChange() {
		setSummary(value);

		dialog.cancel();

		if (mOnChangeListener != null) {
			mOnChangeListener.onPreferenceChange(this, value);
		}
	}

	private static class SavedState extends BaseSavedState {
		public static final Parcelable.Creator<SavedState> CREATOR =
				new Parcelable.Creator<SavedState>() {
					@Override
					public SavedState createFromParcel(Parcel in) {
						return new SavedState(in);
					}

					@Override
					public SavedState[] newArray(int size) {
						return new SavedState[size];
					}
				};

		String value;
		Object secondaryValue;

		SavedState(Parcel source) {
			super(source);
			value = source.readString();
			secondaryValue = source.readLong();
		}

		SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(@NonNull Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeString(value);
		}
	}
}


