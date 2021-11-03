@file:Suppress("UnstableApiUsage")

package com.github.bashspbu.intellijjavacodealigner

import com.github.bashspbu.intellijjavacodealigner.JavaAligningInlaysCollector.AlignCellType.*
import com.github.bashspbu.intellijjavacodealigner.util.*
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.util.descendants
import com.intellij.psi.util.siblings
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.castSafelyTo
import kotlin.math.abs


class JavaAligningInlaysCollector(
    private val editor: Editor,
    private val settings: JavaAligningInlaysSettings
) : InlayHintsCollector {

    private val myAlignCellsCache = mutableMapOf<PsiElement, List<AlignCell>>()
    private val myAlignGroupsCache = mutableMapOf<PsiElement, List<PsiElement>>()
    private val myAlignedColumnLengthsCache = mutableMapOf<PsiElement, Map<AlignCellType, Int>>()

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        return when (element) {
            is PsiField                -> handleField(element, editor, sink)
            is PsiParameter            -> handleParameter(element, editor, sink)
            is PsiDeclarationStatement -> handleLocalVar(element, editor, sink)
            is PsiExpressionStatement  -> handleAssignment(element, editor, sink)
            else                       -> true
        }
    }

    private fun checkModifiersAreInCanonicalOrder(element: PsiField): Boolean {
        return getFirstMisorderedModifier(element.modifierList) == null
    }

    private fun handleField(element: PsiField, editor: Editor, sink: InlayHintsSink): Boolean {
        if (!checkModifiersAreInCanonicalOrder(element)) {
            return true
        }
        handleCommon(element, sink, editor, doAlignLastCell = element.hasInitializer()) {
            it is PsiField && checkModifiersAreInCanonicalOrder(it)
        }
        return true
    }

    private fun handleParameter(element: PsiParameter, editor: Editor, sink: InlayHintsSink): Boolean {
        handleCommon(element, sink, editor, doAlignLastCell = false) { it is PsiParameter }
        return true
    }

    private fun handleLocalVar(element: PsiDeclarationStatement, editor: Editor, sink: InlayHintsSink): Boolean {
        val doAlignLastCell = element.declaredElements
            .firstOrNull()
            ?.castSafelyTo<PsiLocalVariable>()
            ?.hasInitializer()
            ?: false
        handleCommon(element, sink, editor, doAlignLastCell) { it is PsiDeclarationStatement }
        return true
    }

    private fun handleAssignment(element: PsiExpressionStatement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (isAssignStatement(element)) {
            handleCommon(element, sink, editor, doAlignLastCell = true, ::isAssignStatement)
        }
        return true
    }

    private fun handleCommon(
        element: PsiElement,
        sink: InlayHintsSink,
        editor: Editor,
        doAlignLastCell: Boolean,
        doIncludeToAlignGroup: (PsiElement) -> Boolean = { true }
    ) {
        if (settings.getAlignGroupingRuleFor(element) == AlignGroupingRule.NONE
            || ignoreElementForAligning(editor.document, element)) {
            return
        }

        val alignedColumnLengths = getAlignedColumnLengthsFor(element, doIncludeToAlignGroup)
        val currentElementsCells = splitOnAlignCells(element)

        var currOffset = getElementStartOffsetSkippingAnnotationsOnSeparateLines(editor.document, element)
        val lastNonEmptyCellIndex = currentElementsCells.indexOfLast { it.length > 0 } + (if (doAlignLastCell) 1 else 0)
        for (i in 0 until lastNonEmptyCellIndex) {
            val (type, length) = currentElementsCells[i]
            currOffset += length
            val expectedLength = alignedColumnLengths[type]!!
            if (length > expectedLength) {
                break // [length] exceeds [type.maxColumnLength], we won't align the rest of the line
            }
            val fillerLength = expectedLength - length + if (length == 0 && expectedLength > 0) 1 else 0
            if (fillerLength > 0) {
                sink.addInlineElement(
                    currOffset,
                    true,
                    JavaAligningInlaysPresentation(editor, fillerLength, settings),
                    false)
            }
            currOffset += if (length > 0) 1 else 0
        }
    }

    private fun getAlignedColumnLengthsFor(
        element: PsiElement,
        doIncludeToAlignGroup: (PsiElement) -> Boolean = { true }
    ): Map<AlignCellType, Int> {
        return myAlignedColumnLengthsCache.getOrElse(element) {
            val alignGroup = getAlignGroupFor(element, doIncludeToAlignGroup)
            val alignedColumnLengths = alignGroup
                .map { splitOnAlignCells(it) }
                .flatMap { it.asSequence() }
                .groupingBy { it.type }
                .fold(0) { acc, it -> if (it.length > it.type.maxColumnLength) acc else maxOf(acc, it.length) }
            for (member in alignGroup) {
                myAlignedColumnLengthsCache[member] = alignedColumnLengths
            }
            alignedColumnLengths
        }
    }

    private val AlignCellType.maxColumnLength: Int
        get() {
            return when (this) {
                VARIABLE_ANNOTATIONS,
                TYPE_ANNOTATIONS -> settings.maxAnnoCellLength
                TYPE             -> settings.maxTypeCellLength
                VARIABLE_NAME    -> settings.maxNameCellLength
                else             -> Int.MAX_VALUE
            }
        }

    private fun getAlignGroupFor(
        element: PsiElement,
        doIncludeToAlignGroup: (PsiElement) -> Boolean = { true }
    ): List<PsiElement> {
        return myAlignGroupsCache.getOrElse(element) {
            val group = mutableListOf<PsiElement>().apply {
                addAll(getSiblingsForAlignment(element, false, doIncludeToAlignGroup))
                add(element)
                addAll(getSiblingsForAlignment(element, true, doIncludeToAlignGroup))
            }
            for (member in group) {
                myAlignGroupsCache[member] = group
            }
            group
        }
    }

    private fun getSiblingsForAlignment(
        element: PsiElement,
        forward: Boolean,
        doIncludeToAlignGroup: (PsiElement) -> Boolean = { true }
    ): Sequence<PsiElement> {
        val alignGroupingRule = settings.getAlignGroupingRuleFor(element)
        var lastElementInGroup = element
        return element.siblings(forward, withSelf = false)
            .takeWhile { curr ->
                doEndAlignGroup(curr, lastElementInGroup, alignGroupingRule, forward, doIncludeToAlignGroup).also {
                    if (doIncludeToAlignGroup(curr) && !ignoreElementForAligning(editor.document, curr)) {
                        lastElementInGroup = curr
                    }
                }
            }
            .filter { doIncludeToAlignGroup(it) && !ignoreElementForAligning(editor.document, it) }
    }

    private fun doEndAlignGroup(
        curr: PsiElement,
        lastElementInGroup: PsiElement,
        alignGroupingRule: AlignGroupingRule,
        forward: Boolean,
        doIncludeToAlignGroup: (PsiElement) -> Boolean = { true }
    ): Boolean {
        return when (alignGroupingRule) {
            AlignGroupingRule.NONE                                     -> {
                false
            }
            AlignGroupingRule.CONSECUTIVE_SIBLINGS                     -> {
                when (curr) {
                    is PsiWhiteSpace -> curr.text.count { it == '\n' } <= 1
                    is PsiComment    -> false
                    is PsiJavaToken  -> curr.text == ","
                    else             ->
                        (if (forward) doesNotHaveDocComment(curr) else doesNotHaveDocComment(lastElementInGroup))
                            && areOnConsecutiveLines(curr, lastElementInGroup)
                            && doesMeetCommonRequirementsForAlignGroupMember(curr, lastElementInGroup, doIncludeToAlignGroup)
                }
            }
            AlignGroupingRule.SIBLINGS_ACROSS_EMPTY_LINES              -> {
                when (curr) {
                    is PsiWhiteSpace -> true
                    is PsiComment    -> false
                    is PsiJavaToken  -> curr.text == ","
                    else             ->
                        (if (forward) doesNotHaveDocComment(curr) else doesNotHaveDocComment(lastElementInGroup))
                            && doesMeetCommonRequirementsForAlignGroupMember(curr, lastElementInGroup, doIncludeToAlignGroup)
                }
            }
            AlignGroupingRule.SIBLINGS_ACROSS_COMMENTS                 -> {
                when (curr) {
                    is PsiWhiteSpace -> curr.text.count { it == '\n' } <= 1
                    is PsiComment    -> true
                    is PsiJavaToken  -> curr.text == ","
                    else             ->
                        doesMeetCommonRequirementsForAlignGroupMember(curr, lastElementInGroup, doIncludeToAlignGroup)
                }
            }
            AlignGroupingRule.SIBLINGS_ACROSS_EMPTY_LINES_AND_COMMENTS -> {
                when (curr) {
                    is PsiWhiteSpace -> true
                    is PsiComment    -> true
                    is PsiJavaToken  -> curr.text == ","
                    else             ->
                        doesMeetCommonRequirementsForAlignGroupMember(curr, lastElementInGroup, doIncludeToAlignGroup)
                }
            }
            AlignGroupingRule.ALL_SIBLINGS                             -> {
                true
            }
        }
    }

    private fun areOnConsecutiveLines(element1: PsiElement, element2: PsiElement): Boolean {
        val document = editor.document
        val element1BegLine = document.getLineNumber(element1.startOffset)
        val element1EndLine = document.getLineNumber(element1.endOffset)
        val element2BegLine = document.getLineNumber(element2.startOffset)
        val element2EndLine = document.getLineNumber(element2.endOffset)
        return abs(element1EndLine - element2BegLine) == 1 || abs(element2EndLine - element1BegLine) == 1
    }

    private fun doesMeetCommonRequirementsForAlignGroupMember(
        element: PsiElement,
        lastElementInGroup: PsiElement,
        doIncludeToAlignGroup: (PsiElement) -> Boolean
    ): Boolean {
        return (doIncludeToAlignGroup(element)
            && getOffsetFromLineStartSkippingAnnotationsOnSeparateLines(editor.document, element)
            == getOffsetFromLineStartSkippingAnnotationsOnSeparateLines(editor.document, lastElementInGroup))
    }

    private fun doesNotHaveDocComment(curr: PsiElement): Boolean {
        return when (curr) {
            is PsiDocCommentOwner -> curr.docComment == null
            else                  -> true
        }
    }

    private fun splitOnAlignCells(element: PsiElement): List<AlignCell> {
        return myAlignCellsCache.getOrPut(element) { doSplitOnAlignCells(element) }
    }

    private fun doSplitOnAlignCells(element: PsiElement): List<AlignCell> {
        val modifiers = mutableListOf<PsiJavaToken>()
        val variableAnnotations = mutableListOf<PsiAnnotation>()
        val typeAnnotations = mutableListOf<PsiAnnotation>()

        val modifierListOwner = when (element) {
            is PsiDeclarationStatement -> element.firstChild
            else                       -> element
        }
        if (modifierListOwner is PsiModifierListOwner) {
            for (child in modifierListOwner.modifierList?.children ?: emptyArray<PsiElement>()) {
                if (child is PsiJavaToken) {
                    modifiers.add(child)
                }
                if (child is PsiAnnotation) {
                    if (modifiers.isNotEmpty()) {
                        typeAnnotations.add(child)
                    }
                    else {
                        variableAnnotations.add(child)
                    }
                }
            }
        }

        val cells = AlignCellType.getPossibleAlignCellTypesFor(element)
            .associateWithTo(sortedMapOf<AlignCellType, Int>()) { 0 }

        val elementLine = getElementLineNumberSkippingAnnotationsOnSeparateLines(editor.document, element)
        variableAnnotations.removeIf { getElementLineNumber(editor.document, it) != elementLine }
        if (variableAnnotations.isNotEmpty()) {
            cells[VARIABLE_ANNOTATIONS] = variableAnnotations.sumOf { it.textLength } + variableAnnotations.size - 1
        }

        for (modifier in modifiers) {
            AlignCellType.fromText(modifier.text)?.let { cells[it] = modifier.textLength }
        }

        if (typeAnnotations.isNotEmpty()) {
            cells[TYPE_ANNOTATIONS] = typeAnnotations.sumOf { it.textLength } + typeAnnotations.size - 1
        }

        when (element) {
            is PsiVariable             -> {
                cells[TYPE] = element.typeElement?.textLength ?: 0
                cells[VARIABLE_NAME] = element.nameIdentifier?.textLength ?: 0
            }
            is PsiDeclarationStatement -> {
                element.declaredElements
                    .firstOrNull()
                    ?.castSafelyTo<PsiLocalVariable>()
                    ?.let {
                        cells[TYPE] = it.typeElement.textLength
                        cells[VARIABLE_NAME] = it.nameIdentifier?.textLength ?: 0
                    }
            }
            is PsiExpressionStatement  -> {
                val assignment = element.children.first() as PsiAssignmentExpression
                cells[VARIABLE_NAME] = assignment.lExpression.textLength
            }
        }

        if (EQUAL_SIGN in cells) {
            element.children.asSequence().filterIsInstance<PsiJavaToken>().firstOrNull { it.text == "=" }.let {
                cells.put(EQUAL_SIGN, it?.textLength ?: 0)
            }
        }

        return cells.mapTo(mutableListOf()) { (type, length) -> AlignCell(type, length) }
    }

    private data class AlignCell(val type: AlignCellType, val length: Int)

    private enum class AlignCellType {
        VARIABLE_ANNOTATIONS,
        VISIBILITY,
        ABSTRACT,
        DEFAULT,
        STATIC,
        MUTABILITY, // final or volatile
        TRANSIENT,
        SYNCHRONIZED,
        NATIVE,
        STRICTFP,
        TRANSITIVE,
        SEALED,
        NON_SEALED,
        TYPE_ANNOTATIONS,
        TYPE,
        VARIABLE_NAME,
        EQUAL_SIGN;

        companion object {
            fun fromText(text: String) = when (text) {
                PsiModifier.PUBLIC,
                PsiModifier.PROTECTED,
                PsiModifier.PRIVATE      -> VISIBILITY
                PsiModifier.ABSTRACT     -> ABSTRACT
                PsiModifier.DEFAULT      -> DEFAULT
                PsiModifier.STATIC       -> STATIC
                PsiModifier.FINAL        -> MUTABILITY
                PsiModifier.VOLATILE     -> MUTABILITY
                PsiModifier.TRANSIENT    -> TRANSIENT
                PsiModifier.SYNCHRONIZED -> SYNCHRONIZED
                PsiModifier.NATIVE       -> NATIVE
                PsiModifier.STRICTFP     -> STRICTFP
                PsiModifier.TRANSITIVE   -> TRANSITIVE
                PsiModifier.SEALED       -> SEALED
                PsiModifier.NON_SEALED   -> NON_SEALED
                else                     -> null
            }

            val fieldTypes get() = values().toList()

            val paramTypes = listOf(
                VARIABLE_ANNOTATIONS,
                MUTABILITY,
                TYPE_ANNOTATIONS,
                TYPE,
                VARIABLE_NAME
            )

            val localVarTypes = listOf(
                VARIABLE_ANNOTATIONS,
                MUTABILITY,
                TYPE_ANNOTATIONS,
                TYPE,
                VARIABLE_NAME,
                EQUAL_SIGN
            )

            val assignmentTypes = listOf(
                VARIABLE_ANNOTATIONS,
                VARIABLE_NAME,
                EQUAL_SIGN
            )

            fun getPossibleAlignCellTypesFor(element: PsiElement) = when {
                element is PsiField                -> fieldTypes
                element is PsiParameter            -> paramTypes
                element is PsiDeclarationStatement -> localVarTypes
                element is PsiExpressionStatement
                    && isAssignStatement(element)  -> assignmentTypes
                else                               -> error("Unsupported element: $element")
            }
        }
    }
}

private fun ignoreElementForAligning(document: Document, element: PsiElement): Boolean {
    return containsIntermediateComments(element)
        || spansSeveralLinesIgnoringAnnotations(document, element)
}

/**
 * E.g. the following returns `true`
 * ```java
 * @SuppressWarnings("unchecked") // Arrays and generics don't get along.
 * Node<K, V>[] newTable = new Node[oldCapacity * 2];
 * ```
 *
 * and the following returns `true`
 * ```java
 * AvlIterator<K, V> iterator /* some comment */ = new AvlIterator<K, V>();
 * ```
 *
 * but the following returns `false`, we do allow single line comments at the end of the line
 * ```java
 * AvlIterator<K, V> iterator = new AvlIterator<K, V>(); // no problem
 * ```
 */
private fun containsIntermediateComments(element: PsiElement): Boolean {
    val docComment = element.castSafelyTo<PsiDocCommentOwner>()?.docComment
    return element.descendants()
        .takeWhile { !(it is PsiJavaToken && (it.text == ";" || it.text == ",")) }
        .any { it is PsiComment && it != docComment }
}

/**
 * E.g. the following returns `true`, because annotations
 * on separate lines are OK
 * ```java
 * @NotNull
 * @SuppressWarnings("unchecked")
 * private final List<String> names = new ArrayList();
 * ```
 *
 * as well as the following returns `true`, because initializers
 * on separate lines is a pretty common practice:
 * ```java
 * private final List<String> names =
 *   new ArrayList();
 * ```
 *
 * but the following returns `false`, because element itself is split on several lines:
 * ```java
 * private final List<String>
 *     names = new ArrayList();
 * ```
 */
private fun spansSeveralLinesIgnoringAnnotations(document: Document, element: PsiElement): Boolean {
    return (getElementLineNumberSkippingAnnotationsOnSeparateLines(document, element)
        != document.getLineNumber(element.textOffset))
}

private fun isAssignStatement(element: PsiElement) =
    element is PsiExpressionStatement && element.children.firstOrNull() is PsiAssignmentExpression