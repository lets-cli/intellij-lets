package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

object LetsCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val configUtils = LetsConfigUtils()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        when {
            configUtils.isRootLevel(parameters) -> {
                result.addAllElements(TOP_LEVEL_KEYWORDS.map { keyword -> createLookupElement(keyword) })
            }
            configUtils.isCommandLevel(parameters) -> {
                result.addAllElements(COMMAND_LEVEL_KEYWORDS.map { keyword -> createLookupElement(keyword) })
            }
            configUtils.isDependsLevel(parameters) -> {
                result.addAllElements(
                    configUtils.getCommandsNames(parameters).map {
                        keyword ->
                        createLookupElement(keyword)
                    }
                )
            }
        }
    }

    private fun createLookupElement(text: String): LookupElement {
        return LookupElementBuilder
            .create(text)
            .withIcon(Icons.LetsYaml)
    }
}
