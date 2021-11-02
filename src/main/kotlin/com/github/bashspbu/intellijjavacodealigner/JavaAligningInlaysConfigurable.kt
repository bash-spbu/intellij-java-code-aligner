@file:Suppress("UnstableApiUsage")

package com.github.bashspbu.intellijjavacodealigner

import com.github.bashspbu.intellijjavacodealigner.util.myIntTextField
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import kotlin.reflect.KMutableProperty0


class JavaAligningInlaysConfigurable(private val settings: JavaAligningInlaysSettings) : ImmediateConfigurable {

    override fun createComponent(listener: ChangeListener): JComponent {
        return panel {
            titledRow(OurBundle.message("hints.settings.align.grouping.rule.title")) {
                alignGroupingRuleComboBox(
                    OurBundle.message("hints.settings.fields.align.grouping.rule.label"),
                    listener,
                    settings::fieldsAlignGroupingRule
                )
                alignGroupingRuleComboBox(
                    OurBundle.message("hints.settings.params.align.grouping.rule.label"),
                    listener,
                    settings::paramsAlignGroupingRule
                )
                alignGroupingRuleComboBox(
                    OurBundle.message("hints.settings.locals.align.grouping.rule.label"),
                    listener,
                    settings::localsAlignGroupingRule
                )
                alignGroupingRuleComboBox(
                    OurBundle.message("hints.settings.assignments.align.grouping.rule.label"),
                    listener,
                    settings::assignmentsAlignGroupingRule
                )
            }
            titledRow(OurBundle.message("hints.settings.max.length.title")) {
                maxColumnLengthIntField(
                    OurBundle.message("hints.settings.max.anno.length.label"),
                    listener,
                    settings::maxAnnoCellLength
                )
                maxColumnLengthIntField(
                    OurBundle.message("hints.settings.max.type.length.label"),
                    listener,
                    settings::maxTypeCellLength
                )
                maxColumnLengthIntField(
                    OurBundle.message("hints.settings.max.name.length.label"),
                    listener,
                    settings::maxNameCellLength
                )
            }
            row {
                checkBox(
                    OurBundle.message("hints.settings.debug.mode.label") + " \uD83D\uDEA7",
                    settings::debugMode,
                    comment = OurBundle.message("hints.settings.debug.mode.explanatory.comment")
                )
                    .component
                    .apply {
                        addActionListener {
                            settings.debugMode = isSelected
                            listener.settingsChanged()
                        }
                    }
            }
        }
    }

    private fun Row.maxColumnLengthIntField(labelName: String, listener: ChangeListener, settingsProperty: KMutableProperty0<Int>) {
        row {
            label(labelName)
            myIntTextField(settingsProperty, range = 5..1000)
                .component
                .let { intTextField ->
                    intTextField.document.addDocumentListener(object : DocumentAdapter() {
                        override fun textChanged(e: DocumentEvent) {
                            intTextField.text.toIntOrNull()?.let {
                                settingsProperty.set(it)
                                listener.settingsChanged()
                            }
                        }
                    })
                }
            ContextHelpLabel.create(OurBundle.message("hints.settings.max.length.explanatory.comment"))()
        }
    }

    private fun Row.alignGroupingRuleComboBox(
        labelName: String,
        listener: ChangeListener,
        settingsProperty: KMutableProperty0<AlignGroupingRule>
    ) {
        row {
            label(labelName)
            comboBox(EnumComboBoxModel(AlignGroupingRule::class.java), settingsProperty)
                .component
                .apply {
                    addActionListener {
                        settingsProperty.set(selectedItem as AlignGroupingRule)
                        listener.settingsChanged()
                    }
                }
        }
    }
}