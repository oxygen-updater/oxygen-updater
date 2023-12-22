package com.oxygenupdater.ui.main

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.get
import com.oxygenupdater.models.ServerMessage
import com.oxygenupdater.ui.dialogs.BottomSheetTestTag
import com.oxygenupdater.ui.dialogs.PreviewServerMessagesList
import com.oxygenupdater.validateColumnLayout
import org.junit.Test

class MainMenuTest : ComposeBaseTest() {

    @Test
    fun mainMenu() {
        var serverMessages by mutableStateOf<List<ServerMessage>>(listOf())
        var showMarkAllRead by mutableStateOf(false)
        setContent {
            Row {
                MainMenu(
                    serverMessages = serverMessages,
                    showMarkAllRead = showMarkAllRead,
                    onMarkAllReadClick = {
                        trackCallback("onMarkAllReadClick")
                    },
                    onContributorEnrollmentChange = {
                        trackCallback("onContributorEnrollmentChange: $it")
                    },
                )
            }
        }

        // First we test for the initial empty list
        rule[MainMenu_AnnouncementsButtonTestTag].assertDoesNotExist()

        // Then for non-empty and the initial value of `showMarkAllRead = false`
        serverMessages = PreviewServerMessagesList
        rule[MainMenu_DropdownMenuTestTag].assertDoesNotExist()
        rule[BottomSheetTestTag].assertDoesNotExist()
        rule[MainMenu_AnnouncementsButtonTestTag].run {
            assertWidthIsEqualTo(40.dp)
            assertAndPerformClick() // should show sheet now
        }
        rule[BottomSheetTestTag].assertExists()

        // Then for true
        showMarkAllRead = true
        rule[MainMenu_OverflowButtonTestTag].run {
            assertWidthIsEqualTo(40.dp)
            assertAndPerformClick() // should show dropdown menu now
        }
        val children = rule[MainMenu_DropdownMenuTestTag].run {
            validateColumnLayout(1)
        }

        children[0].run {
            assertAndPerformClick() // should trigger onMarkAllReadClick() and hide dropdown menu
        }
        ensureCallbackInvokedExactlyOnce("onMarkAllReadClick")
        rule[MainMenu_DropdownMenuTestTag].assertDoesNotExist()

        /**
         * TODO(test/contributor): this can be tested only on rooted devices
         *  above API 29, because checkbox & buttons are guarded behind
         *  `ContributorUtils.isAtLeastQAndPossiblyRooted`.
         *  "PossiblyRooted" is determined by Shell.isAppGrantedRoot(),
         *  which is false for emulators and managed devices.
         */
        // // Should trigger onContributorEnrollmentChange() and hide dropdown menu
        // children[1].assertAndPerformClick()
        // ensureCallbackInvokedExactlyOnce("onContributorEnrollmentChange: false")
        // composeTestRule[MainMenu_DropdownMenuTestTag].assertDoesNotExist()
        // composeTestRule[BottomSheetTestTag].assertExists()
    }
}
