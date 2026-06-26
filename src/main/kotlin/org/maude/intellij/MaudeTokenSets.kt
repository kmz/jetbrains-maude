package org.maude.intellij

import com.intellij.psi.tree.TokenSet

object MaudeTokenSets {
    val COMMENTS: TokenSet = TokenSet.create(
        MaudeTokenTypes.COMMENT_LINE,
        MaudeTokenTypes.COMMENT_BLOCK
    )
    val STRINGS: TokenSet = TokenSet.create(MaudeTokenTypes.STRING)
}
