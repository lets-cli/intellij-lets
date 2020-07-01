package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage

open class LetsCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC,  PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE), LetsCompletionProvider)
    }

    // TODO add completion for command. For now it suggests same top level keywords if we under `commands` block
    // TODO add validation for values. For example `version` must be valid semver string
    //  (maybe load all available versions from github in different thread)
    // TODO maybe add syntax highlighting
    // TODO maybe add more patterns in init method
    private object LetsCompletionProvider : CompletionProvider<CompletionParameters>() {
        private val TOP_LEVEL_KEYWORDS = arrayOf(
            "shell",
            "commands",
            "env",
            "eval_env",
            "version",
            "mixins"
        )

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            for (keyword in TOP_LEVEL_KEYWORDS) {
                result.addElement(LookupElementBuilder.create(keyword))
            }
        }
    }
}