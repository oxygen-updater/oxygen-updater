package com.arjanvlek.oxygenupdater.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.preference.PreferenceManager;

import com.arjanvlek.oxygenupdater.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * Overridden class to add custom functionality (setting title/message, custom on click listeners for dismissing).
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
public class BottomSheetPreference extends Preference {

	private Context context;
	private SharedPreferences sharedPreferences;
	private OnPreferenceChangeListener mOnChangeListener;

	private LinearLayout dialogLayout;
	private BottomSheetDialog dialog;

	private String title;
	private String caption;
	private List<BottomSheetItem> itemList = new ArrayList<>();
	private boolean valueSet;
	private String value;
	private Integer intValue;
	private int previousSelectedIndex;

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

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

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

		setText(dialogLayout.findViewById(R.id.dialog_header), title);
		setText(dialogLayout.findViewById(R.id.dialog_caption), caption);

		for (int i = 0; i < itemList.size(); i++) {
			LinearLayout dialogItemLayout = (LinearLayout) inflater.inflate(R.layout.bottom_sheet_item, dialogLayout, false);

			// add the item's view at the specified index
			// start at 1 because there's a TextView at index 0 (title)
			dialogLayout.addView(dialogItemLayout, i + 1);

			setupItemView(dialogItemLayout, i);
		}

		dialog.setContentView(dialogLayout);
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

		dialogItemLayout.setOnClickListener(view -> setValue(index));

		String currentValue = sharedPreferences.getString(getKey(), null);

		if (item.getValue().equals(currentValue)) {
			markItemSelected(index);
		} else {
			markItemUnselected(index);
		}
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
		int resourceId = a.getResourceId(R.styleable.BottomSheetPreference_intEntryValues, 0);

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
			Integer intValue = intEntryValues != null ? intEntryValues[i] : null;

			if (title != null) {
				item.setTitle(title.toString());
			}

			if (subtitle != null) {
				item.setSubtitle(subtitle.toString());
			}

			if (intValue != null) {
				item.setIntValue(intValue);
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
	 * Finds index of item with newValue or newIntValue and calls {@link #setValue(int)}
	 *
	 * @param newValue    the new value
	 * @param newIntValue the new integer value
	 */
	private void setValues(String newValue, Integer newIntValue) {
		int selectedIndex = -1;

		for (int i = 0; i < itemList.size(); i++) {
			BottomSheetItem item = itemList.get(i);
			String value = item.getValue();
			Integer intValue = item.getIntValue();

			if ((value != null && value.equals(newValue)) || (intValue != null && intValue.equals(newIntValue))) {
				selectedIndex = i;
				break;
			}
		}

		if (selectedIndex != -1) {
			setValue(selectedIndex);
		}
	}

	/**
	 * Sets the value of the key. intValue is optional.
	 * <p>
	 * Also marks previous item as unselected and new item as selected
	 *
	 * @param selectedIndex the selected index
	 */
	private void setValue(int selectedIndex) {
		BottomSheetItem item = itemList.get(selectedIndex);

		String newValue = item.getValue();
		Integer newIntValue = item.getIntValue();

		// Always persist/notify the first time.
		boolean changed = !TextUtils.equals(value, newValue);

		if (changed && valueSet) {
			// redraw previous selected and new selected layouts
			markItemUnselected(previousSelectedIndex);
			markItemSelected(selectedIndex);
		}

		if (changed || !valueSet) {
			value = newValue;
			intValue = newIntValue;
			valueSet = true;

			persistString(newValue);

			if (newIntValue != null) {
				// by default int values will be mapped to a key = getKey() + "_id"
				sharedPreferences.edit()
						.putInt(getKey() + "_id", newIntValue)
						.apply();
			}

			if (changed) {
				onChange();
			}
		}

		previousSelectedIndex = selectedIndex;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public void setItemList(List<BottomSheetItem> itemList) {
		this.itemList = itemList;
	}

	/**
	 * Hides the checkmark icon and resets background to {@link android.R.attr#selectableItemBackground}
	 *
	 * @param selectedIndex index of the selected item
	 */
	private void markItemUnselected(int selectedIndex) {
		if (selectedIndex != -1) {
			LinearLayout dialogItemLayout = (LinearLayout) dialogLayout.getChildAt(selectedIndex + 1);
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
			LinearLayout dialogItemLayout = (LinearLayout) dialogLayout.getChildAt(selectedIndex + 1);
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
	public void onClick() {
		if (dialog != null) {
			dialog.show();
		}
	}

	@Override
	protected void onSetInitialValue(Object defaultValue) {
		String newValue = getPersistedString((String) defaultValue);
		int newIntValue = sharedPreferences.getInt(getKey() + "_id", -1);

		setValues(newValue, newIntValue != -1 ? newIntValue : null);
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
		myState.intValue = intValue;
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

		setValues(myState.value, myState.intValue);
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
		Integer intValue;

		SavedState(Parcel source) {
			super(source);
			value = source.readString();
			intValue = source.readInt();
		}

		SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(@NonNull Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeString(value);
			dest.writeInt(intValue);
		}
	}
}


