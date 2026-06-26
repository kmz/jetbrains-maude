package org.maude.intellij

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.maude.intellij.editor.MaudeBraceMatcher

class MaudeEditorTest : BasePlatformTestCase() {
    fun testModuleFolding() {
        val psiFile = myFixture.configureByText(
            "a.maude",
            "fmod FOO is op f : -> Nat [ctor] . endfm\n"
        )
        val document = myFixture.getDocument(psiFile)
        // Call the builder directly — no folding daemon needed in tests.
        val descriptors = org.maude.intellij.editor.MaudeFoldingBuilder()
            .buildFoldRegions(psiFile, document, false)
        // At least the module fmod..endfm should be foldable.
        assertTrue("Expected at least one fold descriptor", descriptors.isNotEmpty())
        assertTrue(
            "Expected a fold region starting at offset 0 (the fmod keyword)",
            descriptors.any { it.range.startOffset == 0 }
        )
    }

    fun testBraceMatcherPairs() {
        val pairs = MaudeBraceMatcher().getPairs()
        val set = pairs.map { it.leftBraceType to it.rightBraceType }.toSet()
        assertTrue(set.contains(MaudeTokenTypes.LPAREN to MaudeTokenTypes.RPAREN))
        assertTrue(set.contains(MaudeTokenTypes.LBRACKET to MaudeTokenTypes.RBRACKET))
        assertTrue(set.contains(MaudeTokenTypes.LBRACE to MaudeTokenTypes.RBRACE))
    }

    fun testCommenter() {
        val c = MaudeFileType.language.let {
            com.intellij.lang.LanguageCommenters.INSTANCE.forLanguage(it)
        }
        assertEquals("*** ", c.lineCommentPrefix)
    }
}
