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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_guide);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        ViewPager mViewPager = (ViewPager) findViewById(R.id.updateInstallationInstructionsPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a InstallGuideFragment.
            return InstallGuideFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show the predefined amount of total pages.
            return NUMBER_OF_INSTALL_GUIDE_PAGES;
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
