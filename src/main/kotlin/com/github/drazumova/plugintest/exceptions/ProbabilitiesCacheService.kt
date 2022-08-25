package com.github.drazumova.plugintest.exceptions

import com.github.drazumova.plugintest.models.LinearModel
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@Service
class ProbabilitiesCacheService(private val project: Project) {
    private val cache: MutableMap<String, List<Double>> = mutableMapOf()
    private val infoCache: MutableMap<String, Info> = mutableMapOf()
    private val modelConnection = LinearModel()

    private suspend fun computeResult(info: Info) {
        val probabilities = modelConnection.getProbabilities(info)
        synchronized(cache) {
            cache[info.fullText] = probabilities
        }
    }

    private fun notifyAction() {
        val notificationType =
            NotificationGroup(
                "demo.notifications.stickyBalloon",
                NotificationDisplayType.BALLOON,
                true
            )
        val notification = Notification(
            notificationType.displayId,
            "Loading predictions completed",
            "Now it's possible to highlight console output",
            NotificationType.INFORMATION
        )
        notification.notify(project)
    }

    fun checkResultForStacktrace(info: Info): List<Double>? {
        synchronized(cache) {
            infoCache[info.fullText] = info
            if (!cache.containsKey(info.fullText)) {
                GlobalScope.launch {
                    computeResult(info)
                    notifyAction()
                }
            }
            return cache[info.fullText]
        }
    }

    fun clear(info: Info) {
        synchronized(cache) {
            cache.remove(info.fullText)
        }
    }
}
