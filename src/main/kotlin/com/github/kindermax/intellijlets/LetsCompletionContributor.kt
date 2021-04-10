package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import org.jetbrains.yaml.YAMLLanguage

open class LetsCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE),
            LetsCompletionProvider
        )
    }
}
