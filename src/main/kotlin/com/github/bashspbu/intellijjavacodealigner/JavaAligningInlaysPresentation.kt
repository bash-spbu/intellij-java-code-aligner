@file:Suppress("UnstableApiUsage")

package com.github.bashspbu.intellijjavacodealigner

import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D


data class JavaAligningInlaysPresentation(
    val editor: Editor,
    val fillerLength: Int,
    val settings: JavaAligningInlaysSettings
) : BasePresentation() {

    private val editorFontMetrics by lazy {
        val editorFont = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
        editor.contentComponent.getFontMetrics(editorFont)
    }

    private val editorBackgroundColor: Color
        get() = EditorColorsManager.getInstance().globalScheme.defaultBackground

    override val height: Int
        get() = editorFontMetrics.height

    override val width: Int
        get() = editorFontMetrics.stringWidth(" ".repeat(fillerLength))

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        if (settings.debugMode) {
            val oldColor = g.color
            val oldFont = g.font
            g.color = Color(176, 176, 176)
            g.fillRect(0, 0, width, height)
            g.color = Color(245, 245, 245)
            g.font = UIUtil.getLabelFont().let { it.deriveFont(Font.BOLD, it.size2D * 1.5f) }
            g.drawString(fillerLength.toString(), 0, height - 5)
            g.color = oldColor
            g.font = oldFont
        }
    }
}