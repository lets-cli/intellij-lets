package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import java.util.logging.Logger

object LetsCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val log: Logger = Logger.getLogger(LetsCompletionProvider.javaClass.name)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        // TODO cache parsing ?
        val config = try {
            Config.parseFromPSI(parameters.originalFile)
        } catch (exp: ConfigException) {
            log.warning(exp.toString())
            return
        }
        when {
            LetsCompletionHelper.isRootLevel(parameters) -> {
                result.addAllElements(TOP_LEVEL_KEYWORDS.map { keyword -> createLookupElement(keyword) })
            }
            LetsCompletionHelper.isShellLevel(parameters) -> {
                result.addAllElements(DEFAULT_SHELLS.map { keyword -> createLookupElement(keyword) })
            }
            LetsCompletionHelper.isCommandLevel(parameters) -> {
                result.addAllElements(COMMAND_LEVEL_KEYWORDS.map { keyword -> createLookupElement(keyword) })
            }
            LetsCompletionHelper.isDependsLevel(parameters) -> {
                val suggestions = LetsCompletionHelper.getDependsSuggestions(parameters, config)
                result.addAllElements(
                    suggestions.map { keyword -> createLookupElement(keyword) }
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
