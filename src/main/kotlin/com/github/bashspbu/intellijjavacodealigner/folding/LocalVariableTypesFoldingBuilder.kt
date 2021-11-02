package com.github.bashspbu.intellijjavacodealigner.folding

import com.intellij.codeInsight.daemon.impl.analysis.HighlightControlFlowUtil
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil


class LocalVariableTypesFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (quick || !LocalVariableTypesFoldingSettings.instance.state.doLocalVariableTypesFolding) {
            return emptyArray()
        }

        val result = mutableListOf<FoldingDescriptor>()
        MyVisitor(null, result::add).visitElement(root)

        return result.toTypedArray()
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        return null
    }

    class MyVisitor(
        private val foldingGroup: FoldingGroup?,
        private val resultDescriptorsConsumer: (FoldingDescriptor) -> Unit
    ) : JavaRecursiveElementVisitor() {

        override fun visitLocalVariable(variable: PsiLocalVariable?) {
            processVariable(variable)
        }

        override fun visitResourceVariable(variable: PsiResourceVariable?) {
            super.visitResourceVariable(variable)
        }

        private fun processVariable(variable: PsiVariable?) {
            if (variable != null
                && variable.name != null
                && variable.typeElement != null
                && (variable.initializer != null || variable.parent is PsiForeachStatement)
                && variable.textRange.startOffset < variable.typeElement!!.textRange.endOffset
            ) {
                val isFinal = isEffectivelyFinal(variable)
                val typeElementRange = TextRange.create(variable.textRange.startOffset, variable.typeElement!!.textRange.endOffset + 1)
                val settings = LocalVariableTypesFoldingSettings.instance.state
                resultDescriptorsConsumer(FoldingDescriptor(
                    variable.node,
                    typeElementRange,
                    foldingGroup,
                    (if (isFinal) settings.finalVarPlaceholder!! else settings.varPlaceholder!!) + " "
                ))
            }
        }
    }
}

private fun isEffectivelyFinal(element: PsiVariable): Boolean {
    // explicitly marked as final
    if (element.modifierList?.hasExplicitModifier(PsiModifier.FINAL) == true) {
        return true
    }
    // running constancy inference
    return when (val variableCodeBlock = PsiUtil.getVariableCodeBlock(element, null)) {
        null -> false
        else -> HighlightControlFlowUtil.isEffectivelyFinal(element, variableCodeBlock, null)
    }
}