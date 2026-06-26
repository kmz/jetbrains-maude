package org.maude.intellij

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.extapi.psi.ASTWrapperPsiElement

class MaudeParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = MaudeLexerAdapter()

    override fun createParser(project: Project?): PsiParser =
        PsiParser { root, builder ->
            val mark = builder.mark()
            while (!builder.eof()) {
                builder.advanceLexer()
            }
            mark.done(root)
            builder.treeBuilt
        }

    override fun getFileNodeType(): IFileElementType = MaudeFile.FILE
    override fun getCommentTokens(): TokenSet = MaudeTokenSets.COMMENTS
    override fun getStringLiteralElements(): TokenSet = MaudeTokenSets.STRINGS

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = MaudeFile(viewProvider)
}
