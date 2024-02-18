package com.oxygenupdater.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.LineHeightForTextStyle
import com.oxygenupdater.get
import com.oxygenupdater.ui.common.OutlinedIconButtonTestTag
import com.oxygenupdater.ui.common.OutlinedIconButton_TextTestTag

open class ModalBottomSheetTest : ComposeBaseTest() {

    /**
     * Validates layout & behaviour of header node tagged [BottomSheet_HeaderTestTag]
     * (usually [SheetHeader]) and content node tagged [BottomSheet_ContentTestTag]
     * (usually [androidx.compose.material3.Text].
     *
     * @param headerResIdOrString either a [StringRes] or a [String]
     * @param contentResIdOrString either a [StringRes] or a [String]
     * @param captionResIdOrString (optional) either a [StringRes] or a [String]
     * @param additionalContentValidations (optional) caller can run their own
     *   validations on the content node ([BottomSheet_ContentTestTag])
     */
    protected fun validateHeaderContentCaption(
        @StringRes headerResIdOrString: Any,
        @StringRes contentResIdOrString: Any?,
        @StringRes captionResIdOrString: Any? = null,
        additionalContentValidations: SemanticsNodeInteraction.() -> Unit = {},
    ) {
        rule[BottomSheet_HeaderTestTag].run {
            assertHasTextExactly(headerResIdOrString)
            fetchSemanticsNode().assertMaxLines(LineHeightForTextStyle.titleMedium)
        }

        rule[BottomSheet_ContentTestTag].run {
            assertExists()
            contentResIdOrString?.let { assertHasTextExactly(contentResIdOrString) }
            additionalContentValidations()
        }

        if (captionResIdOrString != null) rule[BottomSheet_CaptionTestTag].run {
            assertHasTextExactly(captionResIdOrString)
        }
    }

    /**
     * Common code to test [SheetButtons] layout & behaviour.
     *
     * @param dismissResId [StringRes] for the dismiss button
     * @param confirmResId (optional) [StringRes] for the confirm button.
     *   If null, it is assumed this button should be asserted as not existing.
     * @param hidden (optional) lazy Boolean value
     * @param result lazy nullable Boolean value
     * @param resetHidden (optional) should be provided by caller to set `hidden = false` if needed
     */
    protected fun validateButtons(
        @StringRes dismissResId: Int,
        @StringRes confirmResId: Int? = null,
        hidden: (() -> Boolean) = { true },
        result: () -> Boolean?,
        resetHidden: () -> Unit = {},
    ) {
        val dismissButtonNode = validateDismissButton(
            dismissResId = dismissResId,
            hidden = hidden,
            result = result,
        )

        if (confirmResId == null) {
            rule[OutlinedIconButtonTestTag].assertDoesNotExist()
        } else {
            resetHidden()
            validateConfirmButton(
                dismissButton = dismissButtonNode,
                confirmResId = confirmResId,
                hidden = hidden,
                result = result,
                resultFailureMessage = { "Confirm result condition must be true" },
            )
        }
    }

    protected fun validateDismissButton(
        @StringRes dismissResId: Int,
        hidden: () -> Boolean,
        result: () -> Boolean?,
    ): SemanticsNodeInteraction {
        val button = rule[BottomSheet_DismissButtonTestTag].run {
            assertHasTextExactly(dismissResId)
            performClick()
        }

        assert(result() == false) { "Dismiss result condition must not be true" }
        assert(hidden()) { "hide() was not invoked" }

        return button
    }

    protected fun validateConfirmButton(
        dismissButton: SemanticsNodeInteraction,
        @StringRes confirmResId: Int,
        hidden: () -> Boolean,
        result: () -> Boolean?,
        resultFailureMessage: () -> String,
    ) {
        /** Must be before [OutlinedIconButtonTestTag] just in case clicking leaves the app */
        rule[OutlinedIconButton_TextTestTag, true].run {
            assertHasTextExactly(confirmResId)
        }

        rule[OutlinedIconButtonTestTag].run {
            // Verify row layout. `onParent().validateRowLayout(2)` won't work because `Row` is inline.
            // Must be before `performClick` just in case clicking leaves the app.
            val left = fetchSemanticsNode().positionInRoot.x
            val previousRightEdge = dismissButton.fetchSemanticsNode().let { it.positionInRoot.x + it.size.width }
            assert(left >= previousRightEdge) {
                "Row layout validation failed. Previous right edge: $previousRightEdge, current left edge: $left."
            }

            performClick()
            assert(result() == true, resultFailureMessage)
            assert(hidden()) { "hide() was not invoked" }
        }
    }
}
