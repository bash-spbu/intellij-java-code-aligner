package com.github.bashspbu.intellijjavacodealigner

import com.intellij.openapi.components.BaseState
import com.intellij.psi.*
import org.jetbrains.annotations.Nls
import java.util.function.Supplier


class JavaAligningInlaysSettings : BaseState() {

    var fieldsAlignGroupingRule: AlignGroupingRule by enum(AlignGroupingRule.SIBLINGS_ACROSS_COMMENTS)

    var paramsAlignGroupingRule: AlignGroupingRule by enum(AlignGroupingRule.SIBLINGS_ACROSS_COMMENTS)

    var localsAlignGroupingRule: AlignGroupingRule by enum(AlignGroupingRule.SIBLINGS_ACROSS_COMMENTS)

    var assignmentsAlignGroupingRule: AlignGroupingRule by enum(AlignGroupingRule.SIBLINGS_ACROSS_COMMENTS)

    var maxTypeCellLength: Int by property(35)

    var maxNameCellLength: Int by property(35)

    var maxAnnoCellLength: Int by property(35)

    var debugMode: Boolean by property(false)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as JavaAligningInlaysSettings

        if (fieldsAlignGroupingRule != other.fieldsAlignGroupingRule) return false
        if (paramsAlignGroupingRule != other.paramsAlignGroupingRule) return false
        if (localsAlignGroupingRule != other.localsAlignGroupingRule) return false
        if (assignmentsAlignGroupingRule != other.assignmentsAlignGroupingRule) return false
        if (maxTypeCellLength != other.maxTypeCellLength) return false
        if (maxNameCellLength != other.maxNameCellLength) return false
        if (maxAnnoCellLength != other.maxAnnoCellLength) return false
        if (debugMode != other.debugMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fieldsAlignGroupingRule.hashCode()
        result = 31 * result + paramsAlignGroupingRule.hashCode()
        result = 31 * result + localsAlignGroupingRule.hashCode()
        result = 31 * result + assignmentsAlignGroupingRule.hashCode()
        result = 31 * result + maxTypeCellLength
        result = 31 * result + maxNameCellLength
        result = 31 * result + maxAnnoCellLength
        result = 31 * result + debugMode.hashCode()
        return result
    }

    override fun toString(): String {
        return "JavaAligningInlaysSettings(" +
            "fieldsAlignGroupingRule=$fieldsAlignGroupingRule, " +
            "paramsAlignGroupingRule=$paramsAlignGroupingRule, " +
            "localsAlignGroupingRule=$localsAlignGroupingRule, " +
            "assignmentsAlignGroupingRule=$assignmentsAlignGroupingRule, " +
            "maxTypeCellLength=$maxTypeCellLength, " +
            "maxNameCellLength=$maxNameCellLength, " +
            "maxAnnoCellLength=$maxAnnoCellLength, " +
            "debugMode=$debugMode" +
            ")"
    }
}

fun JavaAligningInlaysSettings.getAlignGroupingRuleFor(element: PsiElement) = when (element) {
    is PsiField                -> fieldsAlignGroupingRule
    is PsiParameter            -> paramsAlignGroupingRule
    is PsiDeclarationStatement -> localsAlignGroupingRule
    is PsiExpressionStatement  -> assignmentsAlignGroupingRule
    else                       -> error("Unsupported element: $element")
}

enum class AlignGroupingRule(private val myPresentableNamePointer: Supplier<@Nls String>) {

    NONE(OurBundle.messagePointer("align.grouping.rule.none.name")),

    CONSECUTIVE_SIBLINGS(OurBundle.messagePointer("align.grouping.rule.consecutive.siblings.name")),

    SIBLINGS_ACROSS_EMPTY_LINES(OurBundle.messagePointer("align.grouping.rule.siblings.across.empty.lines.name")),

    SIBLINGS_ACROSS_COMMENTS(OurBundle.messagePointer("align.grouping.rule.siblings.across.comments.name")),

    SIBLINGS_ACROSS_EMPTY_LINES_AND_COMMENTS(OurBundle.messagePointer("align.grouping.rule.siblings.across.empty.lines.and.comments.name")),

    ALL_SIBLINGS(OurBundle.messagePointer("align.grouping.rule.all.siblings.name"));

    override fun toString(): String {
        return myPresentableNamePointer.get()
    }
}