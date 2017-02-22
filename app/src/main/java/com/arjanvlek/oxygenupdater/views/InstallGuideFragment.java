package com.arjanvlek.oxygenupdater.views;

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

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.Model.InstallGuideData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Server.ServerConnector;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.arjanvlek.oxygenupdater.ApplicationContext.LOCALE_DUTCH;
import static com.arjanvlek.oxygenupdater.ApplicationContext.NUMBER_OF_INSTALL_GUIDE_PAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

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
        View installGuideView =  inflater.inflate(R.layout.fragment_install_guide, container, false);

        int pageNumber = getArguments().getInt(ARG_PAGE_NUMBER, 1);
        boolean isFirstPage = getArguments().getBoolean(ARG_IS_FIRST_PAGE, false);

        SettingsManager settingsManager = new SettingsManager(getContext());
        long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID);
        long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID);

        SparseArray<InstallGuideData> cache = ((InstallGuideActivity)getActivity()).getInstallGuideCache();

        if(cache.get(pageNumber) == null) {
            ServerConnector connector = ((ApplicationContext)getActivity().getApplication()).getServerConnector();
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
            InstallGuideData installGuideData = (InstallGuideData) params[1];

            Bitmap image;

            SparseArray<Bitmap> cache = ((InstallGuideActivity)getActivity()).getInstallGuideImageCache();

            try {
                assert cache != null;
                if(cache.get(installGuideData.getPageNumber()) != null) {
                    image = cache.get(installGuideData.getPageNumber());
                } else {
                    InputStream in = completeImageUrl(installGuideData.getImageUrl(), installGuideData.getFileExtension()).openStream();
                    image = BitmapFactory.decodeStream(in);
                    cache.put(installGuideData.getPageNumber(), image);
                }
            } catch(Exception ignored) {
                image = null;
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

    private void displayInstallGuide(View installGuideView, InstallGuideData installGuideData, int pageNumber, boolean isFirstPage) {
        final TextView titleTextView = (TextView) installGuideView.findViewById(R.id.installGuideTitle);
        final TextView contentsTextView = (TextView) installGuideView.findViewById(R.id.installGuideText);

        if(isFirstPage) {
            installGuideView.findViewById(R.id.installGuideHeader).setVisibility(View.VISIBLE);
            installGuideView.findViewById(R.id.installGuideTip).setVisibility(View.VISIBLE);
        }

        if(installGuideData == null || installGuideData.getDeviceId() == null || installGuideData.getUpdateMethodId() == null) {
            displayDefaultInstallGuide(installGuideView, pageNumber);
        } else {
            displayCustomInstallGuide(installGuideView, pageNumber, installGuideData);
        }

        installGuideView.findViewById(R.id.installGuideLoadingScreen).setVisibility(View.GONE);
        titleTextView.setVisibility(View.VISIBLE);
        contentsTextView.setVisibility(View.VISIBLE);
        if (pageNumber == NUMBER_OF_INSTALL_GUIDE_PAGES) {
            Button closeButton = (Button) installGuideView.findViewById(R.id.installGuideCloseButton);
            closeButton.setOnClickListener(closeButtonOnClickListener());
            closeButton.setVisibility(View.VISIBLE);
        }

    }

    private void displayDefaultInstallGuide(View installGuideView, int pageNumber) {
        final TextView titleTextView = (TextView) installGuideView.findViewById(R.id.installGuideTitle);
        final TextView contentsTextView = (TextView) installGuideView.findViewById(R.id.installGuideText);

        final String appLocale = Locale.getDefault().getDisplayLanguage();

        int titleResourceId = getResources().getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TITLE, RESOURCE_ID_PACKAGE_STRING, getActivity().getPackageName());
        int contentsResourceId = getResources().getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TEXT, RESOURCE_ID_PACKAGE_STRING, getActivity().getPackageName());

        if(appLocale.equals(LOCALE_DUTCH)) {
            titleTextView.setText(getString(titleResourceId));
            contentsTextView.setText(getString(contentsResourceId));
        } else {
            titleTextView.setText(getString(titleResourceId));
            contentsTextView.setText(getString(contentsResourceId));
        }

        loadDefaultImage((ImageView)installGuideView.findViewById(R.id.installGuideImage), pageNumber);
    }

    private void displayCustomInstallGuide(View installGuideView, int pageNumber, InstallGuideData installGuideData) {
        final TextView titleTextView = (TextView) installGuideView.findViewById(R.id.installGuideTitle);
        final TextView contentsTextView = (TextView) installGuideView.findViewById(R.id.installGuideText);

        final String appLocale = Locale.getDefault().getDisplayLanguage();

        if(appLocale.equals(LOCALE_DUTCH)) {
            titleTextView.setText(installGuideData.getDutchTitle());
            contentsTextView.setText(installGuideData.getDutchText());
        } else {
            titleTextView.setText(installGuideData.getEnglishTitle());
            contentsTextView.setText(installGuideData.getEnglishText());
        }

        ImageView imageView = (ImageView)installGuideView.findViewById(R.id.installGuideImage);

        if(installGuideData.getUseCustomImage()) {
            new DownloadCustomImage().execute(imageView, installGuideData);
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
        Drawable errorImage = ResourcesCompat.getDrawable(getResources(), R.drawable.install_guide_error_image, null);
        view.setImageDrawable(errorImage);
        view.setVisibility(View.VISIBLE);
    }

    private View.OnClickListener closeButtonOnClickListener () {
        return v -> getActivity().finish();
    }
}