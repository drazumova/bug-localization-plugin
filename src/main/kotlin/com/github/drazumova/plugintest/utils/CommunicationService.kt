package com.github.drazumova.plugintest.utils

import com.github.drazumova.plugintest.points.ExceptionLine
import com.github.drazumova.plugintest.points.Info
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PredictionModelConnection {
    companion object {
        const val serverURL: String = "http://0.0.0.0:8080"
        const val SUCCESS_CODE = 200
    }

    fun getProbabilities(info: Info): List<Double> {
        val data = prepareData(info)

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val content = data.toString()
        val body = content.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(serverURL)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (response.code != SUCCESS_CODE) return emptyList()

        val receivedJson = response.body?.string() ?: return emptyList()
        val probabilities = Json.parseToJsonElement(receivedJson).jsonObject["probabilities"] ?: return emptyList()
        return Json.decodeFromJsonElement(probabilities)
    }

}

fun prepareData(info: Info): JsonElement {
    val report = info.toExceptionReport()
    return Json.encodeToJsonElement(report)
}

@Serializable
private data class ExceptionLineInfo(
    val lineText: String, val lineNumber: Int,
    val methodName: String, val className: String, val lastModifiedTime: Long
)

@Serializable
private data class ExceptionReport(val className: String, val message: String, val lines: List<ExceptionLineInfo>)

@Suppress("UnstableApiUsage")
private fun Info.toExceptionReport(): ExceptionReport {
    val exceptionLinesInfo = exceptionLines.map { it.toExceptionLineInfo() }
    return ExceptionReport(exceptionInfo.exceptionClassName, exceptionInfo.exceptionMessage, exceptionLinesInfo)
}

private fun ExceptionLine.toExceptionLineInfo(): ExceptionLineInfo {
    val vcsAnnotationProvider = VCSAnnotationProvider.INSTANCE
    val time = vcsAnnotationProvider.lastModifiedTime(file, lineNumber, psiFile.project)
    return ExceptionLineInfo(lineText, lineNumber, methodName, className, lastModifiedTime = time)
}