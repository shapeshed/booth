package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [podcastFtsQuery] is the only thing between raw user input and an FTS4 MATCH expression, so it
 * needs coverage on both axes: that ordinary search behaves, and that FTS syntax a user types
 * cannot escape into the query language.
 */
class PodcastFtsQueryTest {
    @Test
    fun joinsWordsWithAndAndPrefixesEach() {
        assertEquals("kotlin* AND compose*", podcastFtsQuery("kotlin compose"))
    }

    @Test
    fun collapsesSurroundingAndRepeatedWhitespace() {
        assertEquals("a* AND b*", podcastFtsQuery("   a    b   "))
    }

    @Test
    fun nullWhenNothingSearchableRemains() {
        assertNull(podcastFtsQuery(""))
        assertNull(podcastFtsQuery("   "))
        assertNull(podcastFtsQuery("!!!"))
        assertNull(podcastFtsQuery("-"))
    }

    /**
     * A user pasting SQL is not a threat to FTS, which has no SQL dialect: the words simply become
     * search terms. What matters is the shape of the result, asserted by
     * [emitsOnlyBareTokensJoinedByAnd] below.
     */
    @Test
    fun treatsSqlKeywordsAsOrdinarySearchTerms() {
        assertEquals("DROP* AND TABLE* AND episodes*", podcastFtsQuery("'; DROP TABLE episodes; --"))
    }

    /** `OR`, `AND` and `NOT` are operators only in a query's syntax, never in a bare term. */
    @Test
    fun treatsFtsOperatorWordsAsSearchTerms() {
        assertEquals("a* AND OR* AND b*", podcastFtsQuery("a OR b*"))
        assertEquals("NEAR*", podcastFtsQuery("NEAR"))
        assertEquals("NOT*", podcastFtsQuery("NOT"))
    }

    /**
     * The actual safety property, whatever the user typed: every emitted term is letters and digits
     * followed by a single `*`, joined by ` AND `. Nothing else survives, so no quoting, operator or
     * column filter can reach FTS4's parser.
     */
    @Test
    fun emitsOnlyBareTokensJoinedByAnd() {
        val hostile = listOf(
            "\" OR 1=1 --",
            "'; DROP TABLE episodes; --",
            "title:secret",
            "a AND b OR NOT c",
            "{a} [b] (c) ^d *e",
            "a\"b'c",
            "-*-",
            "NEAR(a b)",
            "a\u0000b",
        )
        val allowed = Regex("[0-9A-Za-z]+\\*")

        for (input in hostile) {
            val result = podcastFtsQuery(input)
            if (result == null) continue
            for (term in result.split(" AND ")) {
                assert(allowed.matches(term)) { "input=$input produced term=$term in $result" }
            }
        }
    }

    @Test
    fun stripsQuotesSoTheyCannotCloseTheMatchExpression() {
        assertEquals("title*", podcastFtsQuery("\"title\""))
    }

    /** `column:value` is real FTS4 syntax, so the colon has to go. */
    @Test
    fun stripsColumnFilters() {
        assertEquals("titlekotlin*", podcastFtsQuery("title:kotlin"))
    }

    /** A hyphen is meaningful in FTS (column filter), so "kotlin-flow" must not become a filter. */
    @Test
    fun stripsHyphensRatherThanEmittingAColumnFilter() {
        assertEquals("kotlinflow*", podcastFtsQuery("kotlin-flow"))
    }

    @Test
    fun keepsDigitsAndLetters() {
        assertEquals("b12*", podcastFtsQuery("b12"))
    }
}
