package com.github.bashspbu.intellijjavacodealigner.util

import com.intellij.codeInsight.AnnotationTargetUtil
import com.intellij.psi.*
import java.util.*

/**
 * Converted from the `MissortedModifiersInspection`
 */
internal fun getFirstMisorderedModifier(modifierList: PsiModifierList?): PsiElement? {
    if (modifierList == null) {
        return null
    }
    val modifiers: Deque<PsiElement> = ArrayDeque()
    var typeAnnotation: PsiAnnotation? = null
    for (child in modifierList.children) {
        if (child is PsiJavaToken) {
            if (typeAnnotation != null) return typeAnnotation
            val text = child.getText()
            if (!modifiers.isEmpty() && ModifierComparator.compare(text, modifiers.last.text) < 0) {
                while (!modifiers.isEmpty()) {
                    val first = modifiers.pollFirst()
                    if (ModifierComparator.compare(text, first.text) < 0) {
                        return first
                    }
                }
            }
            modifiers.add(child)
        }
        if (child is PsiAnnotation) {
            if (AnnotationTargetUtil.isTypeAnnotation(child) && !isMethodWithVoidReturnType(modifierList.parent)) {
                // type annotations go next to the type
                // see e.g. https://www.oracle.com/technical-resources/articles/java/ma14-architect-annotations.html
                if (!modifiers.isEmpty()) {
                    typeAnnotation = child
                }
                val targets = AnnotationTargetUtil.getTargetsForLocation(child.owner)
                if (AnnotationTargetUtil.findAnnotationTarget(child, targets[0]) == PsiAnnotation.TargetType.UNKNOWN) {
                    typeAnnotation = child
                }
                continue
            }
            if (!modifiers.isEmpty()) {
                //things aren't in order, since annotations come first
                return modifiers.first
            }
        }
    }
    return null
}

private fun isMethodWithVoidReturnType(element: PsiElement?): Boolean {
    return element is PsiMethod && PsiType.VOID == element.returnType
}

internal object ModifierComparator : Comparator<String> {

    private val MODIFIERS_CANONICAL_ORDER = arrayOf(
        PsiModifier.PUBLIC,
        PsiModifier.PROTECTED,
        PsiModifier.PRIVATE,
        PsiModifier.ABSTRACT,
        PsiModifier.DEFAULT,
        PsiModifier.STATIC,
        PsiModifier.FINAL,
        PsiModifier.TRANSIENT,
        PsiModifier.VOLATILE,
        PsiModifier.SYNCHRONIZED,
        PsiModifier.NATIVE,
        PsiModifier.STRICTFP,
        PsiModifier.TRANSITIVE,
        PsiModifier.SEALED,
        PsiModifier.NON_SEALED
    )

    override fun compare(modifier1: String, modifier2: String): Int {
        if (modifier1 == modifier2) return 0
        for (modifier in MODIFIERS_CANONICAL_ORDER) {
            if (modifier == modifier1) {
                return -1
            } else if (modifier == modifier2) {
                return 1
            }
        }
        return modifier1.compareTo(modifier2)
    }
}