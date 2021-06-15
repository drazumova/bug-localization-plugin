package com.github.drazumova.plugintest.points

import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import java.awt.Component

class ErrorHandler : ErrorReportSubmitter() {
    override fun getReportActionText(): String {
        return "~ ! ~"
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        return super.submit(events, additionalInfo, parentComponent, consumer)
    }
}