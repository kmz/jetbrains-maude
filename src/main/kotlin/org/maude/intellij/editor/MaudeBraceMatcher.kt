package org.maude.intellij.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.maude.intellij.MaudeTokenTypes as T

class MaudeBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(T.LPAREN, T.RPAREN, false),
        BracePair(T.LBRACKET, T.RBRACKET, false),
        BracePair(T.LBRACE, T.RBRACE, true)
    )

    override fun getPairs(): Array<BracePair> = pairs
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, next: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
