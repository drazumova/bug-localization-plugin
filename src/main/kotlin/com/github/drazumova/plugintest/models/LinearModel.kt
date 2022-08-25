package com.github.drazumova.plugintest.models

import com.github.drazumova.plugintest.dataset.Features
import com.github.drazumova.plugintest.dataset.FullStacktraceLineInformation
import com.github.drazumova.plugintest.dataset.features
import com.github.drazumova.plugintest.exceptions.Info
import com.github.drazumova.plugintest.vcs.VCSAnnotationProvider
import kotlinx.serialization.json.*

class LinearModel : PredictionModel {
    private val annotationProvider = VCSAnnotationProvider.INSTANCE
    private val weights: List<Double>

    init {
        val textOther = LinearModel::class.java.classLoader.getResource("model_params.json")
        val text = textOther!!.openStream().bufferedReader().readText()
        weights = Json.parseToJsonElement(text).jsonObject["weights"]?.jsonArray?.toList()?.map {
            it.jsonPrimitive.double
        }
            ?: emptyList()
    }

    private fun apply(features: List<Features>): List<Double> {
        return features.map { line ->
            if (line.editable == 0) return@map 0.0
            val vector = listOf(line.isFirstLine, line.isLastModified, 1).map { it.toDouble() }

            if (weights.isEmpty()) return@map vector.sum()
            return@map vector.zip(weights).sumOf { (a: Double, b: Double) -> a * b }
        }
    }


    override suspend fun getProbabilities(info: Info): List<Double> {
        val fullLines = info.exceptionLines.map {
            val annotation = annotationProvider.annotate(it.file, it.psiFile.project)
            val text = it.psiFile.text
            FullStacktraceLineInformation(it.lineNumber, it.file.name, it.methodName, annotation, text)
        }
        val features = features(fullLines)
        return apply(features)
    }
}
