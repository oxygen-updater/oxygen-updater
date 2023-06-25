package com.oxygenupdater.compose.ui.common

import android.graphics.Typeface
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.oxygenupdater.compose.ui.theme.DefaultTextStyle

/**
 * @param type Defaults to [RichTextType.Html]
 * @param custom required when [type] is [RichTextType.Custom]
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun RichText(
    text: String?,
    modifier: Modifier = Modifier, // `weight` won't work; put that in a Spacer instead
    textAlign: TextAlign? = null,
    contentColor: Color = LocalContentColor.current.copy(alpha = ContentAlpha.high),
    type: RichTextType = RichTextType.Html,
    custom: ((String, Color, Color) -> AnnotatedString)? = null,
) = SelectionContainer(modifier) {
    @Suppress("NAME_SHADOWING")
    val text = text ?: ""

    val uriHandler = LocalUriHandler.current
    val urlColor = MaterialTheme.colors.primary
    val typography = MaterialTheme.typography
    val annotated = remember(text, type) {
        when (type) {
            RichTextType.Custom -> custom?.invoke(text, contentColor, urlColor) ?: AnnotatedString(text)
            RichTextType.Html -> htmlToAnnotatedString(
                text, typography,
                contentColor = contentColor, urlColor = urlColor,
            )

            RichTextType.Markdown -> changelogToAnnotatedString(
                text, typography,
                contentColor = contentColor, urlColor = urlColor,
            )

            else -> TODO("invalid rich text type: $type")
        }
    }

    ClickableText(annotated, style = DefaultTextStyle.run {
        if (textAlign != null) copy(textAlign = textAlign) else this
    }) {
        val range = annotated.getUrlAnnotations(it, it).firstOrNull()
        if (range != null) uriHandler.openUri(range.item.url)
    }
}

/**
 * Converts an HTML string into an [AnnotatedString], keeping as much formatting as possible.
 *
 * Note: doesn't support styling for some spans, they'll be rendered as-is:
 * - [android.text.style.AbsoluteSizeSpan]
 * - [android.text.style.BulletSpan]
 * - [android.text.style.QuoteSpan]
 * - [android.text.style.TypefaceSpan]
 * - etc.
 *
 * @see <a href="https://developer.android.com/guide/topics/resources/string-resource#StylingWithHTML">Supported HTML elements in Android string resources</string>
 */
@OptIn(ExperimentalTextApi::class)
private fun htmlToAnnotatedString(
    html: String,
    typography: Typography,
    contentColor: Color, urlColor: Color,
    spanStyle: SpanStyle = typography.body2.toSpanStyle().copy(color = contentColor),
    paragraphStyle: ParagraphStyle = typography.body2.toParagraphStyle(),
) = buildAnnotatedString {
    val fontSize = spanStyle.fontSize
    val spanned = HtmlCompat.fromHtml(
        html.replace("\n", "<br>"),
        HtmlCompat.FROM_HTML_MODE_COMPACT
    )

    append(spanned.toString())

    // Can be done now itself, because this AnnotatedString is already full
    val length = spanned.length
    addStyle(spanStyle, 0, length)
    addStyle(paragraphStyle, 0, length)

    spanned.getSpans(0, length, Any::class.java).forEach { span ->
        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        if (start == -1 || end == -1) return@forEach

        when (span) {
            is StyleSpan -> when (span.style) {
                Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                else -> null
            }

            is URLSpan -> SpanStyle(
                color = urlColor,
                textDecoration = TextDecoration.Underline,
            ).also {
                addUrlAnnotation(UrlAnnotation(span.url), start, end)
            }

            is BackgroundColorSpan -> SpanStyle(background = Color(span.backgroundColor))
            is ForegroundColorSpan -> SpanStyle(color = Color(span.foregroundColor))
            is RelativeSizeSpan -> SpanStyle(fontSize = fontSize * span.sizeChange)
            is StrikethroughSpan -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            is SubscriptSpan -> SpanStyle(fontSize = fontSize * .8, baselineShift = BaselineShift.Subscript)
            is SuperscriptSpan -> SpanStyle(fontSize = fontSize * .8, baselineShift = BaselineShift.Superscript)
            is UnderlineSpan -> SpanStyle(textDecoration = TextDecoration.Underline)

            else -> null
        }?.let {
            addStyle(it, start, end)
        }
    }
}

/**
 * Converts an update's changelog string (rudimentary Markdown) into an [AnnotatedString],
 * keeping minimal formatting with preference to performance.
 */
@OptIn(ExperimentalTextApi::class)
private fun changelogToAnnotatedString(
    changelog: String,
    typography: Typography,
    contentColor: Color, urlColor: Color,
    spanStyle: SpanStyle = typography.body2.toSpanStyle().copy(color = contentColor),
    paragraphStyle: ParagraphStyle = typography.body2.toParagraphStyle(),
) = try {
    buildAnnotatedString {
        if (changelog.isBlank()) return AnnotatedString("")

        changelog.lineSequence().forEach { line ->
            var heading = false
            var lineToAppend = line
            var paraStyleToApply = paragraphStyle
            val spanStyleToApply = when {
                // Heading 1 => App's h5 typography (24sp; Google Sans Medium)
                // Note that lines that are likely version numbers are skipped, since they'll be displayed elsewhere
                H1.containsMatchIn(line) -> if (line.startsWith(OS_VERSION_LINE_HEADING)) return@forEach
                else typography.h5.toSpanStyle().also {
                    heading = true
                    lineToAppend = line.replace(H1, "$1")
                }
                // Heading 2 => App's subtitle1 (16sp; Google Sans Medium)
                H2.containsMatchIn(line) -> typography.subtitle1.toSpanStyle().also {
                    heading = true
                    lineToAppend = line.replace(H2, "$1")
                }
                // Heading 3 => App's body2 but bold
                H3.containsMatchIn(line) -> spanStyle.copy(fontWeight = FontWeight.Bold).also {
                    heading = true
                    lineToAppend = line.replace(H3, "")
                }
                // List item => App's body2 with bullet margins on wrap
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
                        addUrlAnnotation(UrlAnnotation(linkAddress), start, start + lineToAppend.length)
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

@Immutable
@JvmInline
value class RichTextType(val value: Int) {

    override fun toString() = when (this) {
        Custom -> "Custom"
        Html -> "Html"
        Markdown -> "Markdown"
        else -> "Invalid"
    }

    companion object {
        @Stable
        val Custom = RichTextType(0)

        @Stable
        val Html = RichTextType(1)

        @Stable
        val Markdown = RichTextType(2)
    }
}

/** Quirky method to add an approximate margin (obtained experimentally) to bulleted lines when they wrap */
val ListItemTextIndent = TextIndent(restLine = 9.05.sp)

private const val OS_VERSION_LINE_HEADING = "#"

private val H1 = "^ *#([^#])".toRegex()
private val H2 = "^ *##([^#])".toRegex()
private val H3 = "^ *###".toRegex()
private val LI = "^ *[*•]".toRegex()
