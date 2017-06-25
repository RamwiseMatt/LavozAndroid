package com.ramwise.lavoz.utils.webviews

import android.util.DisplayMetrics


class LavozHTML {
    private fun stripAllHTML(html: String) : String {
        return html.replace("\\<[^>]*>","")
    }

    private fun convertLavozMarkupLanguage(markedText: String) : String {
        var result = "<p>" + markedText

        result = result.replace("\n\n", "</p><p>")
        result = result.replace("\n", "<br/>")

        result = result.replace("[title]", "<span class='lv_title'>")
        result = result.replace("[/title]", "</span>")

        result = result.replace("[b]", "<span class='bold'>")
        result = result.replace("[/b]", "</span>")

        result = result.replace("[i]", "<span class='italic'>")
        result = result.replace("[/i]", "</span>")

        result = result.replace("[quote]", "</p><blockquote>")
        result = result.replace("[/quote]", "</blockquote><p>")

        result += "</p>"

        return result
    }

    private fun wrapInMarginBox(html: String) : String {
        return "<div class='marginBox'>" + html + "</div>"
    }

    fun htmlLavozStyle(html: String) : String {
        val backgroundColor = "#181818"
        val fontColor = "#ffffff"
        val accentColor = "#cf000f"
        val complementColor = "#444444"
        val fadedTextColor = "#cccccc"
        val harshFadedTextColor = "#676767"
        val twitterColor = "#4099FF"

        val header = "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no' />" +
                "<style>" +
                "body {" +
                "margin: 16px 0;" +
                "padding: 0;" +
                "background-color: " + backgroundColor + ";" +
                "font-size: 12pt;" +
                "line-height: 150%;" +
                "color: " + fontColor + ";" +
                "}" +
                "p {" +
                "margin: 24px 0;" +
                "}" +
                "hr {" +
                "display: block;" +
                "height: 1px;" +
                "border: 0;" +
                "border-top: 1px dashed " + accentColor + ";" +
                "margin: 1em 0;" +
                "padding: 0; " +
                "}" +
                "p:first-of-type {" +
                "margin: 0;" +
                "}" +
                "a {" +
                "color: " + accentColor + ";" +
                "text-decoration: none;" +
                "}" +
                "table {" +
                "width: 100%;" +
                "border-collapse: collapse;" +
                "}" +
                "th {" +
                "text-align: left;" +
                "}" +
                "table, th, td {" +
                "border: 1px solid " + complementColor + ";" +
                "padding: 3px;" +
                "}" +
                "span.lv_title {" +
                "font-size: 18pt;" +
                "}" +
                "span.lv_subtitle {" +
                "font-size: 14pt;" +
                "}" +
                "span.bold {" +
                "font-weight: 700;" +
                "}" +
                "span.italic {" +
                "font-style: italic;" +
                "}" +
                ".faded_text_harsh {" +
                "color: " + harshFadedTextColor + ";" +
                "}" +
                ".faded_text {" +
                "color: " + fadedTextColor + ";" +
                "}" +
                ".small_paragraph {" +
                "display: inline-block;" +
                "margin: 12px 0;" +
                "}" +
                "blockquote {" +
                "margin-left: 0px;" +
                "padding-left: 15px;" +
                "border-left: 3px solid " + complementColor + ";" +
                "}" +
                "blockquote.twitter {" +
                "margin-left: 0px;" +
                "padding-left: 15px;" +
                "border-left: 3px solid " + twitterColor + ";" +
                "}" +
                "img {" +
                "width: 100%;" +
                "max-width: 100%;" +
                "height: auto;" +
                "}" +
                ".marginBox {" +
                "margin: 16px;" +
                "}" +
                ".youtubeIframeContainer {" +
                "position: relative;" +
                "width: 100%;" +
                "height: 0;" +
                "padding-bottom: 56.25%;" +
                "}" +
                "iframe.youtubeIframe {" +
                "position: absolute;" +
                "top: 0;" +
                "left: 0;" +
                "width: 100%;" +
                "height: 100%;" +
                "}" +
                "</style></head>" +
                "<body><div id='marginBox'>"

        val footer = "</div></body></html>"

        return (header + html + footer)
    }

    fun generateTextHTML(rawText: String?, allowRawHTML: Boolean = false) : String {
        if (rawText == null) return ""

        var html = rawText
        if (!allowRawHTML)
            html = stripAllHTML(html)

        html = convertLavozMarkupLanguage(html)
        html = wrapInMarginBox(html)

        return html
    }

    fun generateImageHTML(url: String?, description: String?, acknowledgement: String?) : String {
        if (url == null) return ""

        var html = "<p><img src='" + url + "' />"

        var descriptionText: String? = null

        if (description != null) {
            if (acknowledgement != null)
                descriptionText = description + " (" + acknowledgement + ")"
            else
                descriptionText = description
        } else {
            if (acknowledgement != null)
                descriptionText = acknowledgement
        }

        if (descriptionText != null)
            html += "<span class='faded_text_harsh small_paragraph'>" + descriptionText + "</span>"

        html += "</p>"

        return wrapInMarginBox(html)
    }

    fun generateTweetHTML(body: String?, dateStr: String?, name: String?, username: String?) :
            String {
        if (body == null || dateStr == null || name == null || username == null) return ""

        var html = "<blockquote class='twitter'><span class='lv_subtitle'>" +
                body.replace("\n", "<br/>") + "</span><br />" +
                "<span class='small_paragraph faded_text'>" + dateStr + "</span><br/>" +
                "<span class='small_paragraph'>" + name +
                    " <span class='faded_text'>(" + username + ")</span><" +
                "/span><br/>" +
                "</blockquote>"

        html = wrapInMarginBox(html)

        return html
    }

    fun generateYoutubeHTML(youtubeKey: String?, displayMetrics: DisplayMetrics) : String {
        if (youtubeKey == null) return ""

        val width = displayMetrics.widthPixels
        val height = width * 9 / 16  // maintain 16:9 aspect ratio.
        val html = "<div class='youtubeIframeContainer'><iframe class='youtubeIframe' type='text/html' style='display: block; width: 100%; max-width: 100%;' src='https://www.youtube.com/embed/" + youtubeKey + "?fs=1' frameborder='0' webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe></div>"

        return html
    }

    fun generateConclusionSeperatorHTML(seperatorText: String) : String {
        return convertLavozMarkupLanguage("<hr />" + wrapInMarginBox("[title] " + seperatorText + "[/title]"))
    }
}