package org.maude.intellij

import com.intellij.psi.tree.IElementType

class MaudeTokenType(debugName: String) : IElementType(debugName, MaudeLanguage)

object MaudeTokenTypes {
    @JvmField val KW_MODULE = MaudeTokenType("KW_MODULE")
    @JvmField val KW_END = MaudeTokenType("KW_END")
    @JvmField val KW_DECL = MaudeTokenType("KW_DECL")
    @JvmField val KW_IMPORT = MaudeTokenType("KW_IMPORT")
    @JvmField val KW_ATTRIBUTE = MaudeTokenType("KW_ATTRIBUTE")
    @JvmField val KW_CONTROL = MaudeTokenType("KW_CONTROL")
    @JvmField val KW_COMMAND = MaudeTokenType("KW_COMMAND")
    @JvmField val KW_OTHER = MaudeTokenType("KW_OTHER")

    @JvmField val COMMENT_LINE = MaudeTokenType("COMMENT_LINE")
    @JvmField val COMMENT_BLOCK = MaudeTokenType("COMMENT_BLOCK")
    @JvmField val STRING = MaudeTokenType("STRING")
    @JvmField val NUMBER = MaudeTokenType("NUMBER")
    @JvmField val OPERATOR = MaudeTokenType("OPERATOR")
    @JvmField val IDENTIFIER = MaudeTokenType("IDENTIFIER")

    @JvmField val LPAREN = MaudeTokenType("LPAREN")
    @JvmField val RPAREN = MaudeTokenType("RPAREN")
    @JvmField val LBRACKET = MaudeTokenType("LBRACKET")
    @JvmField val RBRACKET = MaudeTokenType("RBRACKET")
    @JvmField val LBRACE = MaudeTokenType("LBRACE")
    @JvmField val RBRACE = MaudeTokenType("RBRACE")
}
