package org.maude.intellij

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.maude.intellij.highlight.MaudeColors
import org.maude.intellij.highlight.MaudeSyntaxHighlighter

class MaudeHighlighterTest : BasePlatformTestCase() {
    fun testKeywordHighlight() {
        val h = MaudeSyntaxHighlighter()
        val keys = h.getTokenHighlights(MaudeTokenTypes.KW_MODULE)
        assertEquals(1, keys.size)
        assertEquals(MaudeColors.KEYWORD, keys[0])
    }

    fun testCommentHighlight() {
        val h = MaudeSyntaxHighlighter()
        assertEquals(MaudeColors.COMMENT, h.getTokenHighlights(MaudeTokenTypes.COMMENT_LINE)[0])
        assertEquals(MaudeColors.COMMENT, h.getTokenHighlights(MaudeTokenTypes.COMMENT_BLOCK)[0])
    }

    fun testAttributeHighlight() {
        val h = MaudeSyntaxHighlighter()
        assertEquals(MaudeColors.ATTRIBUTE, h.getTokenHighlights(MaudeTokenTypes.KW_ATTRIBUTE)[0])
    }
}
