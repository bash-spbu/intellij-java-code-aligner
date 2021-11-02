package com.github.bashspbu.intellijjavacodealigner.util

import com.intellij.openapi.editor.Document
import com.intellij.psi.*
import com.intellij.psi.util.siblings


internal fun getElementLineNumber(document: Document, element: PsiElement): Int {
    return document.getLineNumber(getElementStartOffset(element))
}

internal fun getElementLineNumberSkippingAnnotationsOnSeparateLines(document: Document, element: PsiElement): Int {
    return document.getLineNumber(getElementStartOffsetSkippingAnnotationsOnSeparateLines(document, element))
}

internal fun getElementStartOffset(element: PsiElement): Int {
    return when (element) {
        is PsiDocCommentOwner ->
            element.children
                .firstOrNull { it !is PsiComment && it !is PsiWhiteSpace }
                ?.textRange
                ?.startOffset
                ?: element.textRange.startOffset
        else                  ->
            element.textRange.startOffset
    }
}

internal fun getElementStartOffsetSkippingAnnotationsOnSeparateLines(document: Document, element: PsiElement): Int {
    return when (element) {
        is PsiModifierListOwner -> {
            val elementLine = document.getLineNumber(element.textOffset)
            val modifierList = element.modifierList ?: return getElementStartOffset(element)
            modifierList.children.asSequence()
                .plus(modifierList.siblings(withSelf = false))
                .firstOrNull {
                    it !is PsiComment
                        && it !is PsiWhiteSpace
                        && (it !is PsiAnnotation || document.getLineNumber(it.textRange.startOffset) == elementLine)
                }
                ?.textRange
                ?.startOffset
                ?: getElementStartOffset(element)
        }
        is PsiDeclarationStatement ->
            element.declaredElements.singleOrNull()
                ?.let { getElementStartOffsetSkippingAnnotationsOnSeparateLines(document, it) }
                ?: getElementStartOffset(element)
        else                    ->
            getElementStartOffset(element)
    }
}

internal fun getOffsetFromLineStartSkippingAnnotationsOnSeparateLines(document: Document, element: PsiElement): Int {
    val elementStartOffset = getElementStartOffsetSkippingAnnotationsOnSeparateLines(document, element)
    return document.getLineStartOffset(document.getLineNumber(elementStartOffset)) - elementStartOffset
}
