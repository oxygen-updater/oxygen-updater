package com.oxygenupdater

import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.getSystemService
import com.oxygenupdater.ui.theme.PreviewAppTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule

open class ComposeBaseTest {

    /** Must be public */
    @get:Rule
    val rule = createComposeRule()
    protected lateinit var activity: ComponentActivity

    @Suppress("TestFunctionName")
    @Before
    fun _setup() {
        activity = (rule as AndroidComposeTestRule<*, *>).activity
    }

    @Suppress("TestFunctionName")
    @After
    fun _tearDown() {
        callbackInvocations = mutableMapOf()
    }

    /**
     * Must be `inline`, otherwise some tests fail with:
     * ```
     * java.lang.ClassCastException: androidx.compose.runtime.internal.ComposableLambdaImpl cannot be cast to kotlin.jvm.functions.Function0
     * ```
     */
    @Suppress("NOTHING_TO_INLINE")
    protected inline fun setContent(
        allowImmediateCallbacks: Boolean = false,
        noinline content: @Composable () -> Unit,
    ) {
        rule.setContent { PreviewAppTheme(content) }
        // Callbacks should only be triggered after user action, not immediately after setting content.
        if (!allowImmediateCallbacks) ensureNoCallbacksWereInvoked()
    }

    /**
     * Map of callback name against their invocation counts. Useful to verify
     * if a certain callback has run when expected, and also how many times.
     */
    private var callbackInvocations = mutableMapOf<String, Int>()

    protected fun trackCallback(name: String) {
        callbackInvocations.merge(name, 1, Int::plus)
    }

    protected fun ensureNoCallbacksWereInvoked() = assert(callbackInvocations.isEmpty()) {
        "Expected no callbacks to be invoked, actual: ${callbackInvocations.keys}"
    }

    protected fun ensureCallbackInvokedExactlyOnce(vararg names: String) {
        names.forEach {
            /** Ensure callback with name was invoked only once */
            val value = callbackInvocations[it]
            val index = it.indexOf(':')
            // Some tests append result values to `names`, so construct the appropriate prefix
            val prefix = if (index > 0) {
                val name = it.substring(0, index).trim()
                val result = it.substring(index + 1).trim()
                "$name(): $result"
            } else "$it()"
            assert(value == 1) {
                "$prefix was " + if (value == null) "not invoked" else "invoked more than once: $value"
            }
        }

        // Ensure no other callbacks were invoked
        assert(callbackInvocations.size == names.size) {
            val invoked = names.joinToString()
            val suffix = callbackInvocations.keys.filter { it !in names }
            "The following callbacks were invoked: [$invoked], however others should not have been: $suffix"
        }

        // Reset map for future checks in the same test
        callbackInvocations.clear()
    }

    /** @param resIdOrString either [StringRes] or [String] */
    protected fun SemanticsNodeInteraction.assertHasTextExactly(
        vararg resIdOrString: Any?,
    ) = when {
        resIdOrString.isEmpty() || resIdOrString[0] == null -> hasTextExactly("")
        else -> hasTextExactly(
            textValues = resIdOrString.map {
                when (it) {
                    is String -> it
                    is Int -> activity.getString(it)
                    else -> throw IllegalArgumentException("$it must be Int/String: ${it!!.javaClass.name}")
                }
            }.toTypedArray(),
            includeEditableText = false,
        )
    }.let { assert(it) }

    /**
     * There's no direct way to check [androidx.compose.material3.Text]'s `maxLines`,
     * so we estimate it by dividing its [height][SemanticsNode.size] by [lineHeight]
     * in pixels.
     *
     * @param lineHeight should match [com.oxygenupdater.ui.theme.appTypography]
     * @param expectedMaxLines calculated line count should not exceed this value
     */
    protected fun SemanticsNode.assertMaxLines(
        lineHeight: LineHeightForTextStyle,
        expectedMaxLines: Int = 1,
    ) = with(rule.density) {
        val actualLineCount = size.height / lineHeight.value.roundToPx()
        assert(expectedMaxLines >= actualLineCount) {
            "Line count of $actualLineCount exceeds max lines $expectedMaxLines"
        }
    }

    protected fun assertCopiedToClipboard(expected: String) {
        val clip = activity.getSystemService<ClipboardManager>()?.primaryClip ?: return
        assert(clip.itemCount == 1) {
            "Clipboard item count did not match. Expected: 1, actual: ${clip.itemCount}.\n${clip.description}"
        }

        val actual = clip.getItemAt(0).text
        assert(expected == actual) {
            "Clipboard item text did not match. Expected: $expected, actual: $actual."
        }
    }

    /**
     * We need to do this specifically in tests that don't use any of the
     * [androidx.compose.ui.test.SemanticsNodeInteraction.fetchSemanticsNode]
     * methods (either directly or indirectly via something like
     * [androidx.compose.ui.test.SemanticsNodeInteraction.assertExists]), which
     * already involves syncing with UI.
     */
    protected fun advanceFrame() = rule.mainClock.advanceTimeByFrame()
}
