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
        val commandName = PsiTreeUtil.getParentOfType(keyValue, YAMLKeyValue::class.java)?.keyText

        if (keyValue.keyText == "cmd" && keyValue.value is YAMLScalar) {
            val bashLanguage = Language.findLanguageByID("Shell Script") ?: return
            val text = context.text

            val hostTextRange = context.textRange

            // Calculate the actual text content length for injection
            val startOffset = if (text.startsWith("|")) 1 else 0
            val endOffset = minOf(context.textLength - startOffset, hostTextRange.length - startOffset)
            val injectionTextRange = TextRange(startOffset, endOffset)

            if (!hostTextRange.contains(injectionTextRange.shiftRight(hostTextRange.startOffset))) {
                // The injection range is outside of the host text range
                return
            }
            registrar.startInjecting(bashLanguage)
                .addPlace(null, null, context as PsiLanguageInjectionHost, injectionTextRange)
                .doneInjecting()
        }
    }
}