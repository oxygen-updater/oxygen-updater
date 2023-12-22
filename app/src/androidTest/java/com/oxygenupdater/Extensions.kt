@file:Suppress("NOTHING_TO_INLINE")

package com.oxygenupdater

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import com.oxygenupdater.internal.NotSetF

inline operator fun ComposeContentTestRule.get(
    testTag: String,
    useUnmergedTree: Boolean = false,
) = onNodeWithTag(testTag, useUnmergedTree)

fun SemanticsNodeInteraction.assertHasScrollAction() = assert(hasScrollAction())

fun SemanticsNodeInteraction.assertAndPerformClick(): SemanticsNodeInteraction {
    assertHasClickAction()
    return performClick()
}

fun SemanticsNodeInteraction.assertSizeIsEqualTo(expectedSize: Dp) {
    assertWidthIsEqualTo(expectedSize)
    assertHeightIsEqualTo(expectedSize)
}

fun SemanticsNodeInteraction.assertHasSemanticsKey(
    key: SemanticsPropertyKey<*>,
) = assert(SemanticsMatcher.keyIsDefined(key))

/** @return children [SemanticsNodeInteractionCollection] */
fun SemanticsNodeInteraction.validateLazyColumn(itemCount: Int) = onChildren().also {
    val nodes = it.fetchSemanticsNodes()

    /**
     * Ensure size is what we expect, to make sure we're populating LazyColumn properly.
     * However, as lazy lists don't present all children at once, we can't use the same
     * comparison logic as in [validateColumnLayout]. Instead, we ensure size is at max
     * [itemCount].
     *
     * Note: we can't assert against [androidx.compose.ui.semantics.CollectionInfo.rowCount]
     * because [androidx.compose.foundation.lazy.LazyLayoutSemanticState] always defines
     * rows/columns as -1 for lazy lists. This is weird, because RecyclerView always took
     * row/column counts from the adapter, which made testing easier.
     *
     * See https://issuetracker.google.com/issues/320397497.
     */
    assert(nodes.size <= itemCount) {
        "Lazy children count expected to be $itemCount max. Actual: ${nodes.size}."
    }

    // Validate children's column placement
    nodes.validateColumnLayout()

    // LazyColumn must be scrollable
    assertHasScrollAction()
}

/**
 * Meant to be called on some kind of a parent node, e.g. [onParent] or
 * [androidx.compose.ui.test.SemanticsNodeInteractionsProvider.onRoot].
 *
 * @return children [SemanticsNodeInteractionCollection] if the caller wants to act on it
 */
fun SemanticsNodeInteraction.validateColumnLayout(expectedChildren: Int) = onChildren().also {
    val nodes = it.fetchSemanticsNodes()

    // Ensure size is what we expect, as a check for the test code itself,
    // to make sure we're operating on the correct nodes. This means if we
    // ever change hierarchy, we'll need to update this test as well.
    // Note that it's >= because ConditionalNavBarPadding may be present.
    assert(nodes.size >= expectedChildren) {
        "Children count did not match. Expected: $expectedChildren, actual: ${nodes.size}."
    }

    nodes.validateColumnLayout()
}

/**
 * Meant to be called on some kind of a parent node, e.g. [onParent] or
 * [androidx.compose.ui.test.SemanticsNodeInteractionsProvider.onRoot].
 *
 * @return children [SemanticsNodeInteractionCollection] if the caller wants to act on it
 */
fun SemanticsNodeInteraction.validateRowLayout(expectedChildren: Int) = onChildren().also {
    val nodes = it.fetchSemanticsNodes()

    // Ensure size is what we expect, as a check for the test code itself,
    // to make sure we're operating on the correct nodes. This means if we
    // ever change hierarchy, we'll need to update this test as well.
    assert(nodes.size == expectedChildren) {
        "Children count did not match. Expected: $expectedChildren, actual: ${nodes.size}."
    }

    nodes.validateRowLayout()
}

private fun List<SemanticsNode>.validateColumnLayout() {
    var previousBottomEdge = NotSetF

    fastForEach {
        val top = it.positionInRoot.y
        // Verify column layout: ensure they're placed below each other
        if (previousBottomEdge != NotSetF) assert(top >= previousBottomEdge) {
            "Column layout validation failed. Previous bottom edge: $previousBottomEdge, current top edge: $top.\n" +
                    it.printToString()
        }
        previousBottomEdge = top + it.size.height
    }
}

private fun List<SemanticsNode>.validateRowLayout() {
    var previousRightEdge = NotSetF

    fastForEach {
        val left = it.positionInRoot.x
        // Verify row layout: ensure they're placed one after another
        if (previousRightEdge != NotSetF) assert(left >= previousRightEdge) {
            "Row layout validation failed. Previous right edge: $previousRightEdge, current left edge: $left.\n" +
                    it.printToString()
        }
        previousRightEdge = left + it.size.width
    }
}

/** Simplified from [androidx.compose.ui.test.printToStringInner] (private) */
private fun SemanticsNode.printToString() = buildString {
    append("Node #$id at ")
    with(Rect(positionInWindow, size.toSize())) {
        append("(l=$left, t=$top, r=$right, b=$bottom)px")
    }

    if (config.contains(SemanticsProperties.TestTag)) {
        append(", Tag: '")
        append(config[SemanticsProperties.TestTag])
        append("'")
    }

    appendConfigInfo(config)

    val childrenCount = children.size
    val siblingsCount = (parent?.children?.size ?: 1) - 1
    if (childrenCount > 0 || siblingsCount > 0) {
        appendLine()
        append("Has ")

        if (childrenCount > 1) append("$childrenCount children")
        else if (childrenCount == 1) append("$childrenCount child")

        if (siblingsCount > 0) {
            if (childrenCount > 0) append(", ")
            if (siblingsCount > 1) append("$siblingsCount siblings")
            else append("$siblingsCount sibling")
        }
    }
}

/** Simplified from [androidx.compose.ui.test.appendConfigInfo] (private) */
private fun StringBuilder.appendConfigInfo(config: SemanticsConfiguration) {
    val actions = mutableListOf<String>()
    val units = mutableListOf<String>()
    for ((key, value) in config) {
        if (key == SemanticsProperties.TestTag) continue

        if (value is AccessibilityAction<*> || value is Function<*>) {
            // Avoids printing stuff like "action = 'AccessibilityAction\(label=null, action=.*\)'"
            actions.add(key.name)
            continue
        }

        if (value is Unit) {
            // Avoids printing stuff like "Disabled = 'kotlin.Unit'"
            units.add(key.name)
            continue
        }

        appendLine()
        append(key.name)
        append(" = '")

        if (value is AnnotatedString) with(value) {
            if (paragraphStyles.isEmpty() && spanStyles.isEmpty() && getStringAnnotations(0, text.length).isEmpty()) {
                append(text)
            } else append(this) // Save space if we there is text only in the object
        } else append(value)

        append("'")
    }

    if (units.isNotEmpty()) {
        appendLine()
        append("[")
        append(units.joinToString(separator = ", "))
        append("]")
    }

    if (actions.isNotEmpty()) {
        appendLine()
        append("Actions = [")
        append(actions.joinToString(separator = ", "))
        append("]")
    }

    if (config.isMergingSemanticsOfDescendants) {
        appendLine()
        append("MergeDescendants = 'true'")
    }

    if (config.isClearingSemantics) {
        appendLine()
        append("ClearAndSetSemantics = 'true'")
    }
}
