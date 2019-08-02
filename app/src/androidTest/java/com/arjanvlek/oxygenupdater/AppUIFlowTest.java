package com.arjanvlek.oxygenupdater;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.arjanvlek.oxygenupdater.news.NewsActivity;
import com.arjanvlek.oxygenupdater.settings.SettingsActivity;
import com.arjanvlek.oxygenupdater.setupwizard.SetupActivity;
import com.arjanvlek.oxygenupdater.views.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import java8.util.stream.StreamSupport;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.doubleClick;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

/**
 * Oxygen Updater - © 2017 Arjan Vlek
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppUIFlowTest {

    @Rule
    public ActivityTestRule<SetupActivity> setupActivityRule = new ActivityTestRule<>(SetupActivity.class, false, false);

    @Rule
    public ActivityTestRule<MainActivity> mainActivityRule = new ActivityTestRule<>(MainActivity.class, false, false);
    @Rule
    public ActivityTestRule<NewsActivity> newsActivityRule = new ActivityTestRule<>(NewsActivity.class, false, false);
    @Rule
    public ActivityTestRule<SettingsActivity> settingsActivityRule = new ActivityTestRule<>(SettingsActivity.class, false, false);


    @Before
    public void clearAppState() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getTargetContext());
        SharedPreferences.Editor preferencesEditor = preferences.edit();

        StreamSupport.stream(preferences.getAll().entrySet()).forEach(entry -> preferencesEditor.remove(entry.getKey()));
        preferencesEditor.commit();

        File stackTraceFile = new File(InstrumentationRegistry.getTargetContext().getFilesDir(), "error.txt");

        if (stackTraceFile.exists()) {
            stackTraceFile.delete();
        }
    }

    @Test
    public void testIfAppWorks() throws InterruptedException {
        // Open the setup screen, which the user gets when first opening the app.
        SetupActivity activity = setupActivityRule.launchActivity(null);

        // Swipe through the first 2 screens of the setup wizard
        onView(withId(R.id.introduction_step_1_text_block_1)).perform(swipeLeft());
        Thread.sleep(1000);

        onView(withId(R.id.introduction_step_2_text_block_1)).perform(swipeLeft());
        Thread.sleep(1000);

        // Select the second device in the dropdown in the third screen of the setup wizard.
        onData(anything())
                .inAdapterView(withId(R.id.introduction_step_3_device_dropdown))
                .atPosition(1)
                .perform(doubleClick());

        onView(withId(R.id.introduction_step_3_text_block_1)).perform(swipeLeft());
        Thread.sleep(1000);

        // Dismiss root popup box
        try {
            onView(withText("CLOSE")).perform(click());
        } catch (Exception e) {
            onView(withText("SLUITEN")).perform(click());
        }

        Thread.sleep(3000);

        // Select the second update method in the dropdown in the fourth screen of the setup wizard.
        onData(anything())
                .inAdapterView(withId(R.id.introduction_step_4_update_method_dropdown))
                .atPosition(0)
                .perform(doubleClick());
        onView(withId(R.id.introduction_step_4_text_block_1)).perform(swipeLeft());
        Thread.sleep(1000);

        onView(withId(R.id.introduction_step_5_close_button)).perform(click());

        // Open the main screen
        MainActivity mainActivity = mainActivityRule.launchActivity(null);
        Thread.sleep(3000);

        // Click on "View update information".
        try {
            onView(withText("VIEW UPDATE INFORMATION")).perform(click());
        } catch (Exception e) {
            onView(withText("BEKIJK UPDATE-INFORMATIE")).perform(click());
        }
    }

}
