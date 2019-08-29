package com.arjanvlek.oxygenupdater.installation.manual;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.installation.InstallActivity;
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask;
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;
import com.arjanvlek.oxygenupdater.internal.Worker;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.arjanvlek.oxygenupdater.internal.server.NetworkException;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static com.arjanvlek.oxygenupdater.ApplicationData.NUMBER_OF_INSTALL_GUIDE_PAGES;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public class InstallGuideFragment extends Fragment {
	/**
	 * The fragment argument representing the page number for this fragment.
	 */
	private static final String ARG_PAGE_NUMBER = "page_number";
	private static final String ARG_IS_FIRST_PAGE = "is_first_page";
	private static final String RESOURCE_ID_PREFIX = "install_guide_page_";
	private static final String RESOURCE_ID_TITLE = "_title";
	private static final String RESOURCE_ID_TEXT = "_text";
	private static final String RESOURCE_ID_IMAGE = "_image";
	private static final String RESOURCE_ID_PACKAGE_STRING = "string";
	private static final String RESOURCE_ID_PACKAGE_DRAWABLE = "drawable";
	private static final String IMAGE_VARIANT_LDPI = "ldpi";
	private static final String IMAGE_VARIANT_MDPI = "mdpi";
	private static final String IMAGE_VARIANT_TVDPI = "tvdpi";
	private static final String IMAGE_VARIANT_HDPI = "hdpi";
	private static final String IMAGE_VARIANT_XHDPI = "xhdpi";
	private static final String IMAGE_VARIANT_XXHDPI = "xxhdpi";
	private static final String IMAGE_VARIANT_XXXHDPI = "xxxhdpi";
	private static final String IMAGE_VARIANT_DEFAULT = "default";
	private static final String TAG = "InstallGuideFragment";

	/**
	 * Returns a new instance of this fragment for the given page number.
	 */
	public static InstallGuideFragment newInstance(int pageNumber, boolean isFirstPage) {
		InstallGuideFragment fragment = new InstallGuideFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_PAGE_NUMBER, pageNumber);
		args.putBoolean(ARG_IS_FIRST_PAGE, isFirstPage);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View installGuideView = inflater.inflate(R.layout.fragment_install_guide, container, false);

		int pageNumber;
		boolean isFirstPage;

		if (getArguments() != null) {
			pageNumber = getArguments().getInt(ARG_PAGE_NUMBER, 1);
			isFirstPage = getArguments().getBoolean(ARG_IS_FIRST_PAGE, false);
		} else {
			pageNumber = 1;
			isFirstPage = false;
		}

		SettingsManager settingsManager = new SettingsManager(getContext());
		long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L);
		long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);

		SparseArray<InstallGuidePage> cache;

		if (getActivity() != null && getActivity() instanceof InstallActivity) {
			cache = ((InstallActivity) getActivity()).getInstallGuideCache();
		} else {
			logWarning(TAG, new OxygenUpdaterException("getActivity() returned null or was not an instance of InstallActivity (onCreateView, getInstallGuideCache)"));
			cache = new SparseArray<>();
		}

		if (cache.get(pageNumber) == null) {
			if (getActivity() != null && getActivity().getApplication() != null && getActivity().getApplication() instanceof ApplicationData) {
				ServerConnector connector = ((ApplicationData) getActivity().getApplication()).getServerConnector();
				connector.getInstallGuidePage(deviceId, updateMethodId, pageNumber, page -> {
					cache.put(pageNumber, page);
					displayInstallGuide(installGuideView, page, pageNumber, isFirstPage);
				});
			}

		} else {
			displayInstallGuide(installGuideView, cache.get(pageNumber), pageNumber, isFirstPage);
		}

		return installGuideView;
	}

	private void displayInstallGuide(View installGuideView, InstallGuidePage installGuidePage, int pageNumber, boolean isFirstPage) {
		if (!isAdded()) {
			// Happens when a page is scrolled too far outside the screen (2 or more rows) and content then gets resolved from the server.
			logDebug(TAG, "isAdded() returned false (displayInstallGuide)");
			return;
		}

		if (getActivity() == null) {
			// Should not happen, but can occur when the fragment gets content resolved after the user exited the install guide and returned to another activity.
			logError(TAG, new OxygenUpdaterException("getActivity() returned null (displayInstallGuide)"));
			return;
		}

		// Display a reminder to write everything down on the first page.
		if (isFirstPage) {
			installGuideView.findViewById(R.id.installGuideHeader).setVisibility(View.VISIBLE);
			installGuideView.findViewById(R.id.installGuideTip).setVisibility(View.VISIBLE);
		}

		if (installGuidePage == null || installGuidePage.getDeviceId() == null || installGuidePage.getUpdateMethodId() == null) {
			displayDefaultInstallGuide(installGuideView, pageNumber);
		} else {
			displayCustomInstallGuide(installGuideView, pageNumber, installGuidePage);
		}

		// Hide the loading screen of the install guide page.
		installGuideView.findViewById(R.id.installGuideLoadingScreen).setVisibility(View.GONE);

		TextView titleTextView = installGuideView.findViewById(R.id.installGuideTitle);
		TextView contentsTextView = installGuideView.findViewById(R.id.installGuideText);
		titleTextView.setVisibility(View.VISIBLE);
		contentsTextView.setVisibility(View.VISIBLE);

		// Display the "Close" button on the last page.
		if (pageNumber == NUMBER_OF_INSTALL_GUIDE_PAGES) {
			Button closeButton = installGuideView.findViewById(R.id.installGuideCloseButton);
			closeButton.setOnClickListener(__ -> getActivity().finish());
			closeButton.setVisibility(View.VISIBLE);
		}

	}

	private void displayDefaultInstallGuide(View installGuideView, int pageNumber) {
		if (getActivity() == null) {
			// Should never happen.
			logError(TAG, new OxygenUpdaterException("getActivity() is null (displayDefaultInstallGuide)"));
			return;
		}

		TextView titleTextView = installGuideView.findViewById(R.id.installGuideTitle);
		TextView contentsTextView = installGuideView.findViewById(R.id.installGuideText);

		int titleResourceId = getResources().getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TITLE, RESOURCE_ID_PACKAGE_STRING, getActivity().getPackageName());
		int contentsResourceId = getResources().getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TEXT, RESOURCE_ID_PACKAGE_STRING, getActivity().getPackageName());

		titleTextView.setText(getString(titleResourceId));
		contentsTextView.setText(getString(contentsResourceId));

		loadDefaultImage(installGuideView.findViewById(R.id.installGuideImage), pageNumber);
	}

	private void displayCustomInstallGuide(View installGuideView, int pageNumber, InstallGuidePage installGuidePage) {
		TextView titleTextView = installGuideView.findViewById(R.id.installGuideTitle);
		TextView contentsTextView = installGuideView.findViewById(R.id.installGuideText);

		Locale appLocale = Locale.getLocale();

		if (appLocale == Locale.NL) {
			titleTextView.setText(installGuidePage.getDutchTitle());
			contentsTextView.setText(installGuidePage.getDutchText());
		} else {
			titleTextView.setText(installGuidePage.getEnglishTitle());
			contentsTextView.setText(installGuidePage.getEnglishText());
		}

		ImageView imageView = installGuideView.findViewById(R.id.installGuideImage);

		if (installGuidePage.getUseCustomImage()) {
			// Fetch the custom image from the server.
			new FunctionalAsyncTask<Void, Void, Bitmap>(Worker.NOOP, args -> {
				Bitmap image;

				try {
					SparseArray<Bitmap> cache;

					if (getActivity() != null) {
						cache = ((InstallActivity) getActivity()).getInstallGuideImageCache();
					} else {
						cache = new SparseArray<>();
					}

					// If the cache contains the image, return the image from the cache.
					if (cache.get(installGuidePage.getPageNumber()) != null) {
						image = cache.get(installGuidePage.getPageNumber());
					} else {
						// Otherwise, fetch the image from the server.
						if (isAdded()) {
							image = doGetCustomImage(completeImageUrl(installGuidePage.getImageUrl(), installGuidePage.getFileExtension()));
							cache.put(installGuidePage.getPageNumber(), image);
						} else {
							image = null;
						}
					}
				} catch (MalformedURLException e) {
					image = null;
					logError(TAG, new NetworkException(String.format("Error loading custom image: Invalid image URL <%s>", installGuidePage.getImageUrl())));
				}

				return image;
			}, image -> {
				// If there is no image, load a "no entry" sign to show that the image failed to load.
				if (image == null && isAdded()) {
					loadErrorImage(imageView);
				} else {
					loadCustomImage(imageView, image);
				}
			}).execute();
		} else {
			loadDefaultImage(imageView, pageNumber);
		}
	}

	private URL completeImageUrl(String imageUrl, String fileExtension) throws MalformedURLException {
		String imageVariant;

		switch (getResources().getDisplayMetrics().densityDpi) {
			case DisplayMetrics.DENSITY_LOW:
				imageVariant = IMAGE_VARIANT_LDPI;
				break;
			case DisplayMetrics.DENSITY_MEDIUM:
				imageVariant = IMAGE_VARIANT_MDPI;
				break;
			case DisplayMetrics.DENSITY_TV:
				imageVariant = IMAGE_VARIANT_TVDPI;
				break;
			case DisplayMetrics.DENSITY_HIGH:
				imageVariant = IMAGE_VARIANT_HDPI;
				break;
			case DisplayMetrics.DENSITY_280:
			case DisplayMetrics.DENSITY_XHIGH:
				imageVariant = IMAGE_VARIANT_XHDPI;
				break;
			case DisplayMetrics.DENSITY_360:
			case DisplayMetrics.DENSITY_400:
			case DisplayMetrics.DENSITY_420:
			case DisplayMetrics.DENSITY_XXHIGH:
				imageVariant = IMAGE_VARIANT_XXHDPI;
				break;
			case DisplayMetrics.DENSITY_560:
			case DisplayMetrics.DENSITY_XXXHIGH:
				imageVariant = IMAGE_VARIANT_XXXHDPI;
				break;
			default:
				imageVariant = IMAGE_VARIANT_DEFAULT;
		}

		return new URL(imageUrl + "_" + imageVariant + "." + fileExtension);
	}

	private void loadCustomImage(ImageView view, Bitmap image) {
		view.setImageBitmap(image);
		view.setVisibility(View.VISIBLE);
	}

	private Bitmap doGetCustomImage(URL imageUrl) {
		return doGetCustomImage(imageUrl, 0);
	}

	private Bitmap doGetCustomImage(URL imageUrl, int retryCount) {
		try {
			InputStream in = imageUrl.openStream();
			return BitmapFactory.decodeStream(in);
		} catch (Exception e) {
			if (retryCount < 5) {
				return doGetCustomImage(imageUrl, retryCount + 1);
			} else {
				logError(TAG, "Error loading custom install guide image", e);
				return null;
			}
		}
	}

	private void loadDefaultImage(ImageView view, int pageNumber) {
		if (getActivity() == null) {
			logError(TAG, new OxygenUpdaterException("getActivity() is null (loadDefaultImage)"));
			return;
		}

		int imageResourceId = getResources().getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_IMAGE, RESOURCE_ID_PACKAGE_DRAWABLE, getActivity().getPackageName());
		Drawable image = ResourcesCompat.getDrawable(getResources(), imageResourceId, null);
		view.setImageDrawable(image);
		view.setVisibility(View.VISIBLE);
	}

	private void loadErrorImage(ImageView view) {
		Drawable errorImage = ResourcesCompat.getDrawable(getResources(), R.drawable.error_image, null);
		view.setImageDrawable(errorImage);
		view.setVisibility(View.VISIBLE);
	}
}
