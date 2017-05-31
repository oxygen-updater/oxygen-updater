package com.arjanvlek.oxygenupdater.installation.manual;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.installation.InstallActivity;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.arjanvlek.oxygenupdater.ApplicationData.LOCALE_DUTCH;
import static com.arjanvlek.oxygenupdater.ApplicationData.NUMBER_OF_INSTALL_GUIDE_PAGES;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public class InstallGuideFragment extends Fragment {
    /**
     * The fragment argument representing the page number for this
     * fragment.
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

    /**
     * Returns a new instance of this fragment for the given page
     * number.
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View installGuideView = inflater.inflate(R.layout.fragment_install_guide, container, false);

        int pageNumber = getArguments().getInt(ARG_PAGE_NUMBER, 1);
        boolean isFirstPage = getArguments().getBoolean(ARG_IS_FIRST_PAGE, false);

        SettingsManager settingsManager = new SettingsManager(getContext());
        long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L);
        long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);

        SparseArray<InstallGuidePage> cache = ((InstallActivity) getActivity()).getInstallGuideCache();

        if(cache.get(pageNumber) == null) {
            ServerConnector connector = ((ApplicationData) getActivity().getApplication()).getServerConnector();
            connector.getInstallGuidePage(deviceId, updateMethodId, pageNumber, (page) -> {
                cache.put(pageNumber, page);
                displayInstallGuide(installGuideView, page, pageNumber, isFirstPage);
            });
        } else {
            displayInstallGuide(installGuideView, cache.get(pageNumber), pageNumber, isFirstPage);
        }

        return installGuideView;
    }

    private class DownloadCustomImage extends AsyncTask<Object, Void, List<Object>> {

        @Override
        public List<Object> doInBackground(Object...params) {
            ImageView imageView = (ImageView) params[0];
            InstallGuidePage installGuidePage = (InstallGuidePage) params[1];

            Bitmap image;

            SparseArray<Bitmap> cache = ((InstallActivity)getActivity()).getInstallGuideImageCache();

            try {
                assert cache != null;
                if (cache.get(installGuidePage.getPageNumber()) != null) {
                    image = cache.get(installGuidePage.getPageNumber());
                } else {
                    InputStream in = completeImageUrl(installGuidePage.getImageUrl(), installGuidePage.getFileExtension()).openStream();
                    image = BitmapFactory.decodeStream(in);
                    cache.put(installGuidePage.getPageNumber(), image);
                }
            } catch(Exception e) {
                image = null;
                Logger.logError("InstallGuideFragment", "Error loading custom image: ", e);
            }

            List<Object> result = new ArrayList<>();
            result.add(0, imageView);
            result.add(1, image);

            return result;
        }

        @Override
        public void onPostExecute(List<Object> result) {
            ImageView imageView = (ImageView) result.get(0);
            Bitmap image = (Bitmap) result.get(1);
            if(image == null) {
                loadErrorImage(imageView);
            } else {
                loadCustomImage(imageView, image);
            }
        }
    }

    private void displayInstallGuide(View installGuideView, InstallGuidePage installGuidePage, int pageNumber, boolean isFirstPage) {

        // Display a reminder to write everything down on the first page.
        if(isFirstPage) {
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

        final TextView titleTextView = (TextView) installGuideView.findViewById(R.id.installGuideTitle);
        final TextView contentsTextView = (TextView) installGuideView.findViewById(R.id.installGuideText);
        titleTextView.setVisibility(View.VISIBLE);
        contentsTextView.setVisibility(View.VISIBLE);

        // Display the "Close" button on the last page.
        if (pageNumber == NUMBER_OF_INSTALL_GUIDE_PAGES) {
            Button closeButton = (Button) installGuideView.findViewById(R.id.installGuideCloseButton);
            closeButton.setOnClickListener((__) -> getActivity().finish());
            closeButton.setVisibility(View.VISIBLE);
        }

    }

    private void displayDefaultInstallGuide(View installGuideView, int pageNumber) {
        final TextView titleTextView = (TextView) installGuideView.findViewById(R.id.installGuideTitle);
        final TextView contentsTextView = (TextView) installGuideView.findViewById(R.id.installGuideText);

        int titleResourceId = getResources().getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TITLE, RESOURCE_ID_PACKAGE_STRING, getActivity().getPackageName());
        int contentsResourceId = getResources().getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TEXT, RESOURCE_ID_PACKAGE_STRING, getActivity().getPackageName());

        titleTextView.setText(getString(titleResourceId));
        contentsTextView.setText(getString(contentsResourceId));

        loadDefaultImage((ImageView)installGuideView.findViewById(R.id.installGuideImage), pageNumber);
    }

    private void displayCustomInstallGuide(View installGuideView, int pageNumber, InstallGuidePage installGuidePage) {
        final TextView titleTextView = (TextView) installGuideView.findViewById(R.id.installGuideTitle);
        final TextView contentsTextView = (TextView) installGuideView.findViewById(R.id.installGuideText);

        final String appLocale = Locale.getDefault().getDisplayLanguage();

        if(appLocale.equals(LOCALE_DUTCH)) {
            titleTextView.setText(installGuidePage.getDutchTitle());
            contentsTextView.setText(installGuidePage.getDutchText());
        } else {
            titleTextView.setText(installGuidePage.getEnglishTitle());
            contentsTextView.setText(installGuidePage.getEnglishText());
        }

        ImageView imageView = (ImageView)installGuideView.findViewById(R.id.installGuideImage);

        if (installGuidePage.getUseCustomImage()) {
            new DownloadCustomImage().execute(imageView, installGuidePage);
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

    private void loadCustomImage (ImageView view, Bitmap image) {
        view.setImageBitmap(image);
        view.setVisibility(View.VISIBLE);
    }

    private void loadDefaultImage (ImageView view, int pageNumber) {
        int imageResourceId = getResources().getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_IMAGE, RESOURCE_ID_PACKAGE_DRAWABLE, getActivity().getPackageName());
        Drawable image = ResourcesCompat.getDrawable(getResources(), imageResourceId, null);
        view.setImageDrawable(image);
        view.setVisibility(View.VISIBLE);
    }

    private void loadErrorImage (ImageView view) {
        Drawable errorImage = ResourcesCompat.getDrawable(getResources(), R.drawable.error_image, null);
        view.setImageDrawable(errorImage);
        view.setVisibility(View.VISIBLE);
    }
}