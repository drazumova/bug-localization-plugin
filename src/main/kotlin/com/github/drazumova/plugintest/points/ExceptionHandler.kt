package com.github.drazumova.plugintest.points

import com.intellij.codeInsight.CustomExceptionHandler
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement


class ExceptionHandler : CustomExceptionHandler() {

    override fun isHandled(element: PsiElement?, exceptionType: PsiClassType, topElement: PsiElement?): Boolean {
        println(element?.containingFile)
        println(element?.toString())
        print(exceptionType.className)
        println(topElement?.toString())

        return false
    }
}