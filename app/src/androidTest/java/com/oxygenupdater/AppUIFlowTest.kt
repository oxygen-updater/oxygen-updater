package com.oxygenupdater

import android.preference.PreferenceManager
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.activities.NewsActivity
import com.oxygenupdater.activities.OnboardingActivity
import com.oxygenupdater.activities.SettingsActivity
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppUIFlowTest {

    @Rule
    var setupActivityRule = ActivityTestRule(OnboardingActivity::class.java, false, false)

    @Rule
    var mainActivityRule = ActivityTestRule(MainActivity::class.java, false, false)

    @Rule
    var newsActivityRule = ActivityTestRule(NewsActivity::class.java, false, false)

    @Rule
    var settingsActivityRule = ActivityTestRule(SettingsActivity::class.java, false, false)

    @Before
    fun clearAppState() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getTargetContext())
        val preferencesEditor = preferences.edit()

        preferences.all.entries.forEach {
            preferencesEditor.remove(it.key)
        }

        preferencesEditor.commit()

        val stackTraceFile = File(InstrumentationRegistry.getTargetContext().filesDir, "error.txt")

        if (stackTraceFile.exists()) {
            stackTraceFile.delete()
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIfAppWorks() { // Open the setup screen, which the user gets when first opening the app.
        val activity = setupActivityRule.launchActivity(null)

        // Swipe through the first 2 screens of the setup wizard
        Espresso.onView(ViewMatchers.withId(R.id.onboarding_page_1_text)).perform(ViewActions.swipeLeft())

        Thread.sleep(1000)

        Espresso.onView(ViewMatchers.withId(R.id.onboardingPage1Caption)).perform(ViewActions.swipeLeft())

        Thread.sleep(1000)

        // Select the second device in the dropdown in the third screen of the setup wizard.
        Espresso.onData(Matchers.anything())
            .inAdapterView(ViewMatchers.withId(R.id.introduction_step_3_device_dropdown))
            .atPosition(1)
            .perform(ViewActions.doubleClick())

        Espresso.onView(ViewMatchers.withId(R.id.introduction_step_3_text_block_1)).perform(ViewActions.swipeLeft())

        Thread.sleep(1000)

        // Dismiss root popup box
        try {
            Espresso.onView(ViewMatchers.withText("CLOSE")).perform(ViewActions.click())
        } catch (e: Exception) {
            Espresso.onView(ViewMatchers.withText("SLUITEN")).perform(ViewActions.click())
        }

        Thread.sleep(3000)

        // Select the second update method in the dropdown in the fourth screen of the setup wizard.
        Espresso.onData(Matchers.anything())
            .inAdapterView(ViewMatchers.withId(R.id.introduction_step_4_update_method_dropdown))
            .atPosition(0)
            .perform(ViewActions.doubleClick())

        Espresso.onView(ViewMatchers.withId(R.id.introduction_step_4_text_block_1)).perform(ViewActions.swipeLeft())

        Thread.sleep(1000)

        Espresso.onView(ViewMatchers.withId(R.id.onboardingPage4StartAppButton)).perform(ViewActions.click())

        // Open the main screen
        val mainActivity = mainActivityRule.launchActivity(null)

        Thread.sleep(3000)

        // Click on "View update information".
        try {
            Espresso.onView(ViewMatchers.withText("VIEW UPDATE INFORMATION")).perform(ViewActions.click())
        } catch (e: Exception) {
            Espresso.onView(ViewMatchers.withText("BEKIJK UPDATE-INFORMATIE")).perform(ViewActions.click())
        }
    }
}
