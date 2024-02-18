package com.oxygenupdater.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.performClick
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.get
import org.junit.Test

class RememberSaveableStateTest : ComposeBaseTest() {

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun rememberSaveableState_retentionAfterRestoration() {
        restorationTester.setContent {
            var show by rememberSaveableState("show", false) // generic
            var value by rememberSaveableState("value", 0) // Int overload
            Button(
                onClick = { show = true; value++ },
                modifier = Modifier.testTag(ButtonTestTag)
            ) {}

            if (show) Box(Modifier.testTag(BoxTestTag))
            Text("$value", Modifier.testTag(TextTestTag))
        }

        // First we test initial values
        rule[BoxTestTag].assertDoesNotExist()
        rule[TextTestTag].assertHasTextExactly("0")

        // Then we test if values change as expected
        rule[ButtonTestTag].performClick()
        rule[BoxTestTag].assertExists()
        rule[TextTestTag].assertHasTextExactly("1")

        // Then we test if values are retained after restoration
        restorationTester.emulateSavedInstanceStateRestore()
        rule[BoxTestTag].assertExists()
        rule[TextTestTag].assertHasTextExactly("1")

        // Finally, we perform one more click just to be sure
        rule[ButtonTestTag].performClick()
        rule[BoxTestTag].assertExists()
        rule[TextTestTag].assertHasTextExactly("2")
    }

    companion object {
        private const val ButtonTestTag = "Button"
        private const val BoxTestTag = "Box"
        private const val TextTestTag = "Text"
    }
}
