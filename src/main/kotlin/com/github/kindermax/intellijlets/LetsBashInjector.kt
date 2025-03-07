package com.github.kindermax.intellijlets

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

class LetsBashInjector : MultiHostInjector {
    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(YAMLScalar::class.java) // We only care about YAML string values
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        // Ensure we are in a `cmd` field inside `commands`
        val keyValue = PsiTreeUtil.getParentOfType(context, YAMLKeyValue::class.java) ?: return
        if (keyValue.keyText == "cmd") {
            val bashLanguage = Language.findLanguageByID("Shell Script") ?: return
            val text = context.text
            var startOffset = 0;
            if (text.startsWith("|")) {
                startOffset += 1
            }
            val endOffset = keyValue.endOffset - (keyValue.value?.startOffset ?: keyValue.endOffset)
            val injectionTextRange = TextRange(startOffset, endOffset)
            registrar.startInjecting(bashLanguage)
                .addPlace(null, null, context as PsiLanguageInjectionHost, injectionTextRange)
                .doneInjecting()
        }
    }
}