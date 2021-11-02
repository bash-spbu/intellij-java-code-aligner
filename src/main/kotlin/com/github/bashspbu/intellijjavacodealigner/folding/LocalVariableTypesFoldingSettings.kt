package com.github.bashspbu.intellijjavacodealigner.folding

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage


@State(name = "AdvancedExpressionFoldingSettings", storages = [Storage("editor.codeinsight.xml")])
class LocalVariableTypesFoldingSettings : PersistentStateComponent<LocalVariableTypesFoldingSettings.State> {

    private val myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState.doLocalVariableTypesFolding = state.doLocalVariableTypesFolding
        myState.finalVarPlaceholder = state.finalVarPlaceholder
        myState.varPlaceholder = state.varPlaceholder
    }

    class State : BaseState() {

        var doLocalVariableTypesFolding: Boolean by property(false)

        var finalVarPlaceholder: String? by string("val")

        var varPlaceholder: String? by string("var")

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as State

            if (doLocalVariableTypesFolding != other.doLocalVariableTypesFolding) return false
            if (finalVarPlaceholder != other.finalVarPlaceholder) return false
            if (varPlaceholder != other.varPlaceholder) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + doLocalVariableTypesFolding.hashCode()
            result = 31 * result + (finalVarPlaceholder?.hashCode() ?: 0)
            result = 31 * result + (varPlaceholder?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "State(" +
                "doLocalVariableTypesFolding=$doLocalVariableTypesFolding, " +
                "finalVarPlaceholder=$finalVarPlaceholder, " +
                "varPlaceholder=$varPlaceholder" +
                ")"
        }
    }

    companion object {
        val instance: LocalVariableTypesFoldingSettings
            get() = ApplicationManager.getApplication().getService(LocalVariableTypesFoldingSettings::class.java)
    }
}