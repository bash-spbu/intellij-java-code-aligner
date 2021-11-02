package com.github.bashspbu.intellijjavacodealigner.folding

import com.github.bashspbu.intellijjavacodealigner.OurBundle
import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.openapi.options.UiDslConfigurable
import com.intellij.ui.layout.RowBuilder
import com.intellij.ui.layout.selected


class LocalVariableTypesFoldingOptionsProvider : UiDslConfigurable.Simple(), CodeFoldingOptionsProvider {

    override fun RowBuilder.createComponentRow() {
        val settings = LocalVariableTypesFoldingSettings.instance
        titledRow(OurBundle.message("folding.settings.title")) {
            row {
                val doFoldingCheckBox = checkBox(
                    OurBundle.message("folding.settings.do.local.variable.types.folding.label"),
                    settings.state::doLocalVariableTypesFolding
                ).component

                row(OurBundle.message("folding.settings.var.placeholder.label")) {
                    textField({ settings.state.varPlaceholder!! }, { settings.state.varPlaceholder = it })
                        .enableIf(doFoldingCheckBox.selected)
                }
                row(OurBundle.message("folding.settings.final.var.placeholder.label")) {
                    textField({ settings.state.finalVarPlaceholder!! }, { settings.state.finalVarPlaceholder = it })
                        .enableIf(doFoldingCheckBox.selected)
                }
            }
        }
    }
}