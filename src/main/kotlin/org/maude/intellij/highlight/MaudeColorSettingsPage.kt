package org.maude.intellij.highlight

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.maude.intellij.MaudeIcons
import javax.swing.Icon

class MaudeColorSettingsPage : ColorSettingsPage {
    private val descriptors = arrayOf(
        AttributesDescriptor("Keyword", MaudeColors.KEYWORD),
        AttributesDescriptor("Attribute", MaudeColors.ATTRIBUTE),
        AttributesDescriptor("Comment", MaudeColors.COMMENT),
        AttributesDescriptor("String", MaudeColors.STRING),
        AttributesDescriptor("Number", MaudeColors.NUMBER),
        AttributesDescriptor("Operator", MaudeColors.OPERATOR),
        AttributesDescriptor("Identifier", MaudeColors.IDENTIFIER),
        AttributesDescriptor("Parentheses", MaudeColors.PARENS),
        AttributesDescriptor("Brackets", MaudeColors.BRACKETS),
        AttributesDescriptor("Braces", MaudeColors.BRACES)
    )

    override fun getIcon(): Icon = MaudeIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = MaudeSyntaxHighlighter()
    override fun getDemoText(): String = """
        *** ROT13 example
        fmod FOO is
          pr INT .
          sort Foo .
          op f : Int -> Foo [ctor] .
          var X : Int .
          eq f(X) = f(X + 1) [owise] .
        endfm
        reduce f(3) .
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = descriptors
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = "Maude"
}
