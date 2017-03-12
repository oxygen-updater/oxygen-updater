package com.arjanvlek.oxygenupdater.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.support.Logger;
import com.arjanvlek.oxygenupdater.support.SettingsManager;
import com.arjanvlek.oxygenupdater.support.Utils;

import static com.arjanvlek.oxygenupdater.support.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.support.SettingsManager.PROPERTY_UPDATE_METHOD_ID;


public class SetupActivity extends AppCompatActivity {

    private Fragment step3Fragment;
    private Fragment step4Fragment;
    private SettingsManager settingsManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        this.settingsManager = new SettingsManager(getApplicationContext());

        if (!settingsManager.getPreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)) {
            ApplicationData applicationData = ((ApplicationData) getApplication());
            applicationData.getServerConnector().getDevices(result -> {
                if (!Utils.isSupportedDevice(applicationData.getSystemVersionProperties(), result)) {
                    displayUnsupportedDeviceMessage();
                }
            });
        }

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = (ViewPager) findViewById(R.id.tutorialActivityPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position == 2) {
                    if(step3Fragment != null) {
                        SetupStep3Fragment setupStep3Fragment = (SetupStep3Fragment) step3Fragment;
                        setupStep3Fragment.fetchDevices();
                    }
                }
                if (position == 3) {
                    if (step4Fragment != null) {
                        SetupStep4Fragment setupStep4Fragment = (SetupStep4Fragment) step4Fragment;
                        setupStep4Fragment.fetchUpdateMethods();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    public void displayUnsupportedDeviceMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
        builder.setTitle(getString(R.string.unsupported_device_warning_title));
        builder.setMessage(getString(R.string.unsupported_device_warning_message));

        builder.setPositiveButton(getString(R.string.download_error_close), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a SimpleTutorialFragment (defined as a static inner class below).
            return newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 5 total pages.
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
    }

    public Fragment newInstance(int sectionNumber) {
        if (sectionNumber == 3) {
            step3Fragment = new SetupStep3Fragment();
            return step3Fragment;
        }
        if (sectionNumber == 4) {
            step4Fragment = new SetupStep4Fragment();
            return step4Fragment;
        }
        Bundle args = new Bundle();
        args.putInt("section_number", sectionNumber);
        SimpleTutorialFragment simpleTutorialFragment = new SimpleTutorialFragment();
        simpleTutorialFragment.setArguments(args);
        return simpleTutorialFragment;
    }


    /**
     * Contains the basic / non interactive tutorial fragments.
     */
    @SuppressLint("ValidFragment")
    public static class SimpleTutorialFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Bundle args = getArguments();
            int sectionNumber = args.getInt(ARG_SECTION_NUMBER, 0);
            if (sectionNumber == 1) {
                return inflater.inflate(R.layout.fragment_setup_1, container, false);
            } else if (sectionNumber == 2) {
                return inflater.inflate(R.layout.fragment_setup_2, container, false);
            } else if (sectionNumber == 5) {
                return inflater.inflate(R.layout.fragment_setup_5, container, false);

            }
            return null;
        }
    }

    public void closeInitialTutorial(View view) {
        if (settingsManager.checkIfSetupScreenIsFilledIn()) {
            settingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true);
            NavUtils.navigateUpFromSameTask(this);
        } else {
            Long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L);
            Long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);
            Logger.logWarning("SetupActivity", "Setup screen did *NOT* save settings correctly. Selected device id: " + deviceId + ", selected update method id: " + updateMethodId);
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show();
        }
    }
}
