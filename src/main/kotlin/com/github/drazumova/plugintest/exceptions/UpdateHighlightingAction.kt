package com.github.drazumova.plugintest.exceptions

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import java.awt.Color

class UpdateHighlightingAction : AnAction() {
    private fun addHighlight(editor: Editor, project: Project, line: Int) {
        val model = DocumentMarkupModel.forDocument(editor.document, project, false)
        val layer = HighlighterLayer.SYNTAX + 1


        val highlightAttributes = TextAttributes()
        highlightAttributes.backgroundColor = Color.RED
        highlightAttributes.foregroundColor = Color.BLUE
        model.addLineHighlighter(line, layer, highlightAttributes)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val consoleView = e.getData(LangDataKeys.CONSOLE_VIEW) as? ConsoleViewImpl ?: return
        val project = e.project ?: return
        val editor = consoleView.editor ?: return
        val allText = consoleView.text
        consoleView.rehighlightHyperlinksAndFoldings()
        consoleView.repaint()
    }
}