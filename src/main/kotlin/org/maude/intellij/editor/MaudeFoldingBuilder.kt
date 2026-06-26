package org.maude.intellij.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.maude.intellij.MaudeTokenTypes as T

class MaudeFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val node = root.node ?: return emptyArray()

        // Collect leaf tokens in order.
        val leaves = mutableListOf<ASTNode>()
        fun walk(n: ASTNode) {
            if (n.firstChildNode == null) leaves.add(n)
            var c = n.firstChildNode
            while (c != null) { walk(c); c = c.treeNext }
        }
        walk(node)

        // Pair module-start keywords with the next end keyword.
        val starts = ArrayDeque<ASTNode>()
        for (leaf in leaves) {
            when (leaf.elementType) {
                T.KW_MODULE -> starts.addLast(leaf)
                T.KW_END -> {
                    val start = starts.removeLastOrNull() ?: continue
                    val range = TextRange(start.startOffset, leaf.startOffset + leaf.textLength)
                    if (range.length > 0) {
                        descriptors.add(FoldingDescriptor(start, range))
                    }
                }
                else -> {}
            }
        }

        // Block comments fold on their own node range.
        for (leaf in leaves) {
            if (leaf.elementType == T.COMMENT_BLOCK && leaf.textLength > 0) {
                descriptors.add(FoldingDescriptor(leaf, leaf.textRange))
            }
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String =
        when (node.elementType) {
            T.COMMENT_BLOCK -> "***(...)"
            else -> "..."
        }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
