package com.github.drazumova.plugintest.exceptions

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys

class UpdateHighlightingAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val consoleView = e.getData(LangDataKeys.CONSOLE_VIEW) as? ConsoleViewImpl ?: return
        consoleView.rehighlightHyperlinksAndFoldings()
        consoleView.repaint()
    }
}
