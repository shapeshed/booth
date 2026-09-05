package com.shapeshed.booth.data

import org.jsoup.Jsoup
import org.jsoup.parser.Parser

private const val OpmlVersion = "2.0"

data class OpmlFeed(
    val title: String,
    val url: String,
    val tags: String?,
)

fun parseOpmlFeeds(body: String): List<OpmlFeed> =
    Jsoup.parse(body, "", Parser.xmlParser())
        .select("outline[xmlUrl]")
        .mapNotNull { outline ->
            val url = outline.attr("xmlUrl").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            OpmlFeed(
                title = outline.attr("text").ifBlank { outline.attr("title") }.ifBlank { url },
                url = url,
                tags = outline.inheritedTags(),
            )
        }

private fun org.jsoup.nodes.Element.inheritedTags(): String? {
    val parentTags = parents()
        .asReversed()
        .asSequence()
        .filter { it.normalName() == "outline" && !it.hasAttr("xmlUrl") }
        .flatMap { parent -> parent.outlineTagCandidates() }
    val ownTags = outlineTagCandidates()
    return (parentTags + ownTags)
        .flatMap { it.split(',', '#') }
        .map { it.trim().removePrefix("#") }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .joinToString(", ")
        .takeIf { it.isNotBlank() }
}

private fun org.jsoup.nodes.Element.outlineTagCandidates(): Sequence<String> = sequenceOf(
    attr("text").takeUnless { hasAttr("xmlUrl") },
    attr("title").takeUnless { hasAttr("xmlUrl") },
    attr("category"),
).filterNotNull()

fun buildPodcastOpml(podcasts: List<PodcastEntity>): String = buildString {
    appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    appendLine("<opml version=\"$OpmlVersion\">")
    appendLine("  <head>")
    appendLine("    <title>Booth subscriptions</title>")
    appendLine("  </head>")
    appendLine("  <body>")
    podcasts.forEach { podcast ->
        append("    <outline type=\"rss\"")
        append(" text=\"").append(podcast.title.xmlEscaped()).append("\"")
        append(" title=\"").append(podcast.title.xmlEscaped()).append("\"")
        append(" xmlUrl=\"").append(podcast.feedUrl.xmlEscaped()).append("\"")
        podcast.siteUrl?.takeIf { it.isNotBlank() }?.let {
            append(" htmlUrl=\"").append(it.xmlEscaped()).append("\"")
        }
        podcastTags(listOf(podcast)).joinToString(", ").takeIf { it.isNotBlank() }?.let {
            append(" category=\"").append(it.xmlEscaped()).append("\"")
        }
        appendLine(" />")
    }
    appendLine("  </body>")
    appendLine("</opml>")
}

private fun String.xmlEscaped(): String =
    replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
