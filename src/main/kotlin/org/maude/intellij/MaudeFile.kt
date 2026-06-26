package org.maude.intellij

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType

class MaudeFile(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, MaudeLanguage) {
    override fun getFileType() = MaudeFileType
    override fun toString() = "Maude File"

    companion object {
        @JvmField val FILE = IFileElementType(MaudeLanguage)
    }
}
