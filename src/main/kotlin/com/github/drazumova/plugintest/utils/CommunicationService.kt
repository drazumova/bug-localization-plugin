package com.github.drazumova.plugintest.utils

import com.github.drazumova.plugintest.points.ExceptionLine
import com.github.drazumova.plugintest.points.Info
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import messages.Messages
import messages.ModelGrpcKt
import java.io.Closeable
import java.util.concurrent.TimeUnit

class PredictionModelConnection {
    companion object {
        const val host: String = "0.0.0.0"
        const val port: Int = 8080
    }

    suspend fun getProbabilities(info: Info): List<Double> {
        val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
        val client = ModelClient(channel)

        return client.getProbabilities(info)
    }
}

@Suppress("UnstableApiUsage")
private fun Info.toExceptionReport(): Messages.Exception {
    val exceptionLinesInfo = exceptionLines.map { it.toExceptionLineInfo() }
    val exceptionBuilder = Messages.Exception.newBuilder().setClassName(exceptionInfo.exceptionClassName)
        .setMessage(exceptionInfo.exceptionMessage)
    exceptionLinesInfo.forEach { exceptionLine ->
        exceptionBuilder.addStacktrace(exceptionLine)
    }
    return exceptionBuilder.build()
}

private fun ExceptionLine.toExceptionLineInfo(): Messages.Line {
    val vcsAnnotationProvider = VCSAnnotationProvider.INSTANCE
    val time = vcsAnnotationProvider.lastModifiedTime(file, lineNumber, psiFile.project)
    return Messages.Line.newBuilder().setClassName(className)
        .setMethodName(methodName).setLastModificationTime(time).setText(lineText).build()
}

class ModelClient(private val channel: ManagedChannel) : Closeable {
    private val stub: ModelGrpcKt.ModelCoroutineStub = ModelGrpcKt.ModelCoroutineStub(channel)

    suspend fun getProbabilities(info: Info): List<Double> {
        val exception = info.toExceptionReport()
        val request = Messages.PredictionRequest.newBuilder().setInfo(exception).build()
        val response = stub.getPrediction(request)
        return response.probabilitiesList
    }

    override fun close() {
        channel.shutdown().awaitTermination(1, TimeUnit.SECONDS)
    }
}
