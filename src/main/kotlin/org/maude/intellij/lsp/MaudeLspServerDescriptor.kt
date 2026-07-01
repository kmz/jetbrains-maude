package org.maude.intellij.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.util.EnvironmentUtil
import java.io.File

class MaudeLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "Maude") {

    override fun isSupportedFile(file: VirtualFile): Boolean = file.extension == "maude"

    override val lspGoToDefinitionSupport: Boolean get() = true
    override val lspHoverSupport: Boolean get() = true

    override fun createCommandLine(): GeneralCommandLine =
        GeneralCommandLine("node", unpackServer(), "--stdio")
            .withEnvironment(EnvironmentUtil.getEnvironmentMap())

    private fun unpackServer(): String {
        val resource = javaClass.getResourceAsStream("/lsp/server.js")
            ?: error("Bundled Maude LSP server (/lsp/server.js) not found in the plugin.")
        val target = File(PathManager.getTempPath(), "maude-lsp-server.js")
        resource.use { input -> target.outputStream().use { input.copyTo(it) } }
        return target.absolutePath
    }
}
