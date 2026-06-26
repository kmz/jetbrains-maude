package org.maude.intellij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object MaudeFileType : LanguageFileType(MaudeLanguage) {
    override fun getName(): String = "Maude"
    override fun getDescription(): String = "Maude source file"
    override fun getDefaultExtension(): String = "maude"
    override fun getIcon(): Icon = MaudeIcons.FILE
}
