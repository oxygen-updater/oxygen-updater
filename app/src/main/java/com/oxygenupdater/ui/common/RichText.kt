package com.oxygenupdater.ui.common

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.ui.theme.DefaultTextStyle

/**
 * @param type Defaults to [RichTextType.Html]
 * @param custom required when [type] is [RichTextType.Custom]
 */
@Composable
fun RichText(
    text: String?,
    modifier: Modifier = Modifier, // `weight` won't work; put that in a Spacer instead
    textAlign: TextAlign? = null,
    textIndent: TextIndent? = null,
    contentColor: Color = LocalContentColor.current,
    type: RichTextType = RichTextType.Html,
    custom: ((
        text: String,
        contentColor: Color,
        urlColor: Color,
        onUrlClick: LinkInteractionListener,
    ) -> AnnotatedString)? = null,
) = SelectionContainer(modifier.testTag(RichText_ContainerTestTag)) {
    @Suppress("NAME_SHADOWING")
    val text = text ?: ""

    val urlColor = MaterialTheme.colorScheme.primary
    val typography = MaterialTheme.typography

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val onUrlClick = remember(context, uriHandler) {
        LinkInteractionListener {
            val url = (it as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
            // Note: while this can be simplified to `context.openUrl(url)`, we must
            // use LocalUriHandler because its behaviour can be tested.
            try {
                uriHandler.openUri(url)
            } catch (e: Exception) {
                // e: ActivityNotFoundException | IllegalArgumentException
                // Fallback: copy to clipboard instead
                context.copyToClipboard(url)
            }
        }
    }

    if (type == RichTextType.Html) Text(
        text = remember(text, urlColor, onUrlClick) {
            htmlToAnnotatedString(
                html = text,
                urlColor = urlColor,
                onUrlClick = onUrlClick,
            )
        },
        style = typography.bodyMedium.merge(
            color = contentColor,
            textAlign = textAlign ?: TextAlign.Unspecified,
            textIndent = textIndent,
        ),
    ) else {
        val annotated = when (type) {
            RichTextType.Custom -> if (custom != null) remember(text, contentColor, urlColor, onUrlClick) {
                custom.invoke(text, contentColor, urlColor, onUrlClick)
            } else remember(text) { AnnotatedString(text) }

            RichTextType.Markdown -> remember(text, typography, contentColor, urlColor, onUrlClick) {
                changelogToAnnotatedString(
                    changelog = text,
                    typography = typography,
                    contentColor = contentColor,
                    urlColor = urlColor,
                    onUrlClick = onUrlClick,
                )
            }

            else -> TODO("invalid rich text type: $type")
        }

        Text(
            text = annotated,
            style = DefaultTextStyle.merge(
                textAlign = textAlign ?: TextAlign.Unspecified,
            ),
        )
    }
}

/**
 * Passthrough to [AnnotatedString.Companion.fromHtml].
 *
 * Note: doesn't support styling for some spans, they'll be rendered as-is:
 * - [android.text.style.AbsoluteSizeSpan]
 * - [android.text.style.BulletSpan]
 * - [android.text.style.QuoteSpan]
 * - etc.
 *
 * @see <a href="https://developer.android.com/guide/topics/resources/string-resource#StylingWithHTML">Supported HTML elements in Android string resources</string>
 */
private inline fun htmlToAnnotatedString(
    html: String,
    urlColor: Color,
    onUrlClick: LinkInteractionListener,
) = AnnotatedString.fromHtml(
    htmlString = html.replace("\n", "<br>"),
    linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = urlColor,
            textDecoration = TextDecoration.Underline,
        ),
    ),
    linkInteractionListener = onUrlClick,
)

/**
 * Converts an update's changelog string (rudimentary Markdown) into an [AnnotatedString],
 * keeping minimal formatting with preference to performance.
 */
private fun changelogToAnnotatedString(
    changelog: String,
    typography: Typography,
    contentColor: Color,
    urlColor: Color,
    onUrlClick: LinkInteractionListener,
): AnnotatedString {
    val bodyMedium = typography.bodyMedium
    val spanStyle = bodyMedium.toSpanStyle().copy(color = contentColor)
    val paragraphStyle = bodyMedium.toParagraphStyle()

    return try {
        buildAnnotatedString {
            if (changelog.isBlank()) return AnnotatedString("")

            changelog.lineSequence().forEach { line ->
                var heading = false
                var lineToAppend = line
                var paraStyleToApply = paragraphStyle
                val spanStyleToApply = when {
                    // Heading 1 => App's headlineSmall typography (24sp; Google Sans Medium)
                    // Note that lines that are likely version numbers are skipped, since they'll be displayed elsewhere
                    H1.containsMatchIn(line) -> if (line.startsWith(OsVersionLineHeading)) return@forEach
                    else typography.headlineSmall.toSpanStyle().also {
                        heading = true
                        lineToAppend = line.replace(H1, "$1")
                    }
                    // Heading 2 => App's titleMedium (16sp; Google Sans Medium)
                    H2.containsMatchIn(line) -> typography.titleMedium.toSpanStyle().also {
                        heading = true
                        lineToAppend = line.replace(H2, "$1")
                    }
                    // Heading 3 => App's bodyMedium but bold
                    H3.containsMatchIn(line) -> spanStyle.copy(fontWeight = FontWeight.Bold).also {
                        heading = true
                        lineToAppend = line.replace(H3, "")
                    }
                    // List item => App's bodyMedium with bullet margins on wrap
                    LI.containsMatchIn(line) -> spanStyle.also {
                        lineToAppend = line.replace(LI, "•")
                        // Quirky method to add an approximate margin to bullet lines when they wrap
                        paraStyleToApply = paragraphStyle.copy(textIndent = ListItemTextIndent)
                    }

                    else -> {
                        // Links
                        val urlStyle = line.indexOf('[').let { linkTitleStart ->
                            if (linkTitleStart < 0) return@let null
                            val linkTitleEnd = line.indexOf(']', linkTitleStart + 1)
                            if (linkTitleEnd < 0) return@let null

                            val linkAddressIndices = line.indexOf('(', linkTitleEnd + 1).let paren@{ start ->
                                if (start < 0) return@paren null
                                val end = line.indexOf(')', start + 1)
                                if (end < 0) return@paren null
                                start + 1 until end
                            } ?: line.indexOf('{', linkTitleEnd + 1).let brace@{ start ->
                                if (start < 0) return@let null
                                val end = line.indexOf('}', start + 1)
                                if (end < 0) return@let null
                                start + 1 until end
                            }

                            lineToAppend = line.substring(linkTitleStart + 1, linkTitleEnd)
                            val linkAddress = line.substring(linkAddressIndices)
                            val start = length // current length of AnnotatedString, before appending this line
                            val annotation = LinkAnnotation.Url(linkAddress, linkInteractionListener = onUrlClick)
                            addLink(annotation, start, start + lineToAppend.length)
                            SpanStyle(
                                color = urlColor,
                                textDecoration = TextDecoration.Underline,
                            )
                        }

                        // If this line isn't a link, treat as normal text: [lineToAppend] defaults to [line]
                        urlStyle
                    }
                }?.run {
                    // Override default with the supplied [contentColor]
                    if (color == Color.Unspecified) copy(color = contentColor.run {
                        if (heading) copy(alpha = 1f) else this
                    }) else this
                } ?: spanStyle

                append(lineToAppend)

                val end = length // current length of AnnotatedString after appending line
                val start = end - lineToAppend.length
                // SpanStyles override each other, so even though it would be more efficient, we can't
                // add the default one globally (via `addStyle(spanStyle, 0, length)`) after the loop
                addStyle(spanStyleToApply, start, end)

                // ParagraphStyles can't:
                // - Overlap: add a global para style, and for specific LI items in between
                // - Be applied out-of-order: add for LI item first (above), then for other text afterwards
                //   (e.g. outside the loop by keeping track of LI indices already dealt with)
                // So, we need to apply para styles on each iteration. By default, it's set to
                // [paragraphStyle] but can be overridden in the LI item section above.
                addStyle(paraStyleToApply, start, end)
            }
        }
    } catch (e: Exception) {
        AnnotatedString(changelog, spanStyle, paragraphStyle)
    }
}

@Immutable
@JvmInline
value class RichTextType private constructor(val value: Int) {

    override fun toString() = "RichTextType." + when (this) {
        Custom -> "Custom"
        Html -> "Html"
        Markdown -> "Markdown"
        else -> "Invalid"
    }

    companion object {
        val Custom = RichTextType(0)
        val Html = RichTextType(1)
        val Markdown = RichTextType(2)
    }
}

/** Quirky method to add an approximate margin (obtained experimentally) to bulleted lines when they wrap */
val ListItemTextIndent = TextIndent(restLine = 9.05.sp)

private const val TAG = "RichText"

@VisibleForTesting
const val RichText_ContainerTestTag = TAG + "_Container"

private const val OsVersionLineHeading = "#"

private val H1 = "^ *#([^#])".toRegex()
private val H2 = "^ *##([^#])".toRegex()
private val H3 = "^ *###".toRegex()
private val LI = "^ *[*•]".toRegex()
