package com.arjanvlek.oxygenupdater.views;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.MenuItem;

import com.arjanvlek.oxygenupdater.Model.InstallGuideData;
import com.arjanvlek.oxygenupdater.R;

import static com.arjanvlek.oxygenupdater.ApplicationContext.NUMBER_OF_INSTALL_GUIDE_PAGES;

public class InstallGuideActivity extends AppCompatActivity {

    private final SparseArray<InstallGuideData> installGuideCache = new SparseArray<>();
    private final SparseArray<Bitmap> installGuideImageCache = new SparseArray<>();

    public static final String INTENT_SHOW_DOWNLOAD_PAGE = "show_download_page";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_guide);

        final boolean showDownloadPage = getIntent() == null || getIntent().getBooleanExtra(INTENT_SHOW_DOWNLOAD_PAGE, true);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), showDownloadPage);

        setTitle(getString(R.string.install_guide_title, 1, showDownloadPage ? NUMBER_OF_INSTALL_GUIDE_PAGES : NUMBER_OF_INSTALL_GUIDE_PAGES - 1));

        ViewPager mViewPager = (ViewPager) findViewById(R.id.updateInstallationInstructionsPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setTitle(getString(R.string.install_guide_title, (position + 1), showDownloadPage ? NUMBER_OF_INSTALL_GUIDE_PAGES : NUMBER_OF_INSTALL_GUIDE_PAGES - 1));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private boolean showDownloadPage;

        SectionsPagerAdapter(FragmentManager fm, boolean showDownloadPage) {
            super(fm);
            this.showDownloadPage = showDownloadPage;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a InstallGuideFragment.
            int startingPage = position + (showDownloadPage ? 1 : 2);
            return InstallGuideFragment.newInstance(startingPage, position == 0);
        }

        @Override
        public int getCount() {
            // Show the predefined amount of total pages.
            return showDownloadPage ? NUMBER_OF_INSTALL_GUIDE_PAGES : NUMBER_OF_INSTALL_GUIDE_PAGES - 1;
        }
    }



    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected SparseArray<InstallGuideData> getInstallGuideCache() {
        return this.installGuideCache;
    }

    protected SparseArray<Bitmap> getInstallGuideImageCache () {
        return this.installGuideImageCache;
    }
}
