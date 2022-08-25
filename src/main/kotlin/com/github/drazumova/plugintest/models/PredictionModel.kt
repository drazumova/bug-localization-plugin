package com.github.drazumova.plugintest.models

import com.github.drazumova.plugintest.exceptions.Info

interface PredictionModel {
    suspend fun getProbabilities(info: Info): List<Double>
}