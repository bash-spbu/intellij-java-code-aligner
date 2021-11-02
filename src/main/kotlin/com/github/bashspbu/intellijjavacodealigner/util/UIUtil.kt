package com.github.bashspbu.intellijjavacodealigner.util

import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.toBinding
import com.intellij.util.MathUtil
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import kotlin.reflect.KMutableProperty0


internal fun Cell.myIntTextField(
    prop: KMutableProperty0<Int>,
    columns: Int? = null,
    range: IntRange? = null,
    step: Int? = null
): CellBuilder<JBTextField> {
    return myIntTextField(prop.toBinding(), columns, range, step)
}

/**
 * Copy-pasted from the IntelliJ Community 213.
 * It changed from 212 to 213, so it was copied in order to be compatible with both 212 and 213.
 */
internal fun Cell.myIntTextField(
    binding: PropertyBinding<Int>,
    columns: Int? = null,
    range: IntRange? = null,
    step: Int? = null
): CellBuilder<JBTextField> {
    return textField(
        { binding.get().toString() },
        { value ->
            value.toIntOrNull()?.let { intValue ->
                binding.set(range?.let { intValue.coerceIn(it.first, it.last) } ?: intValue)
            }
        },
        columns
    )
        .withValidationOnInput {
            val value = it.text.toIntOrNull()
            when {
                value == null                    -> error(UIBundle.message("please.enter.a.number"))
                range != null && value !in range -> error(UIBundle.message("please.enter.a.number.from.0.to.1", range.first, range.last))
                else                             -> null
            }
        }
        .apply {
            step ?: return@apply
            component.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent?) {
                    val increment: Int = when (e?.keyCode) {
                        KeyEvent.VK_UP   -> step
                        KeyEvent.VK_DOWN -> -step
                        else             -> return
                    }

                    var value = component.text.toIntOrNull()
                    if (value != null) {
                        value += increment
                        if (range != null) {
                            value = MathUtil.clamp(value, range.first, range.last)
                        }
                        component.text = value.toString()
                        e.consume()
                    }
                }
            })
        }
}