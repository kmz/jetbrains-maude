package org.maude.intellij.highlight

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as D
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.maude.intellij.MaudeLexerAdapter
import org.maude.intellij.MaudeTokenTypes as T

object MaudeColors {
    val KEYWORD = createTextAttributesKey("MAUDE_KEYWORD", D.KEYWORD)
    val ATTRIBUTE = createTextAttributesKey("MAUDE_ATTRIBUTE", D.METADATA)
    val COMMENT = createTextAttributesKey("MAUDE_COMMENT", D.LINE_COMMENT)
    val STRING = createTextAttributesKey("MAUDE_STRING", D.STRING)
    val NUMBER = createTextAttributesKey("MAUDE_NUMBER", D.NUMBER)
    val OPERATOR = createTextAttributesKey("MAUDE_OPERATOR", D.OPERATION_SIGN)
    val IDENTIFIER = createTextAttributesKey("MAUDE_IDENTIFIER", D.IDENTIFIER)
    val PARENS = createTextAttributesKey("MAUDE_PARENS", D.PARENTHESES)
    val BRACKETS = createTextAttributesKey("MAUDE_BRACKETS", D.BRACKETS)
    val BRACES = createTextAttributesKey("MAUDE_BRACES", D.BRACES)
}

class MaudeSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = MaudeLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            T.KW_MODULE, T.KW_END, T.KW_DECL, T.KW_IMPORT, T.KW_CONTROL,
            T.KW_COMMAND, T.KW_OTHER -> arr(MaudeColors.KEYWORD)
            T.KW_ATTRIBUTE -> arr(MaudeColors.ATTRIBUTE)
            T.COMMENT_LINE, T.COMMENT_BLOCK -> arr(MaudeColors.COMMENT)
            T.STRING -> arr(MaudeColors.STRING)
            T.NUMBER -> arr(MaudeColors.NUMBER)
            T.OPERATOR -> arr(MaudeColors.OPERATOR)
            T.IDENTIFIER -> arr(MaudeColors.IDENTIFIER)
            T.LPAREN, T.RPAREN -> arr(MaudeColors.PARENS)
            T.LBRACKET, T.RBRACKET -> arr(MaudeColors.BRACKETS)
            T.LBRACE, T.RBRACE -> arr(MaudeColors.BRACES)
            else -> TextAttributesKey.EMPTY_ARRAY
        }

    private fun arr(key: TextAttributesKey) = arrayOf(key)
}
