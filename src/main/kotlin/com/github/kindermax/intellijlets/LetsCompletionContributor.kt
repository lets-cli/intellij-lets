package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.parentsOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue

open class LetsCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC,  PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE), LetsCompletionProvider)
    }

    // TODO exclude from completion keywords which is already in document

    // TODO add validation for values. For example `version` must be valid semver string
    //  (maybe load all available versions from github in different thread)
    // TODO add variants for cmd as string, multiline string, array
    // TODO add depends autocomplete
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

        private val COMMAND_LEVEL_KEYWORDS = arrayOf(
            "description",
            "env",
            "eval_env",
            "options",
            "checksum",
            "persist_checksum",
            "cmd",
            "depends"
        )

        /**
         * Check if current position is in command context. It means:
         * commands:
         *   echo:
         *     | -> cursor is here
         *
         * TODO This is a very naive implementation
         * 2. Add tests
         */
        fun isCommandLevel(parameters: CompletionParameters): Boolean {
            val yamlKeyValueParents = parameters.position.parentsOfType(YAMLKeyValue::class.java).toList()

            if (yamlKeyValueParents.size == 2) {
                return yamlKeyValueParents[1].name == "commands"
            }

            return false
        }

        fun isRootLevel(parameters: CompletionParameters): Boolean {
            return parameters.position.parent.parent.parent is YAMLDocument
        }

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            if (isRootLevel(parameters)) {
                for (keyword in TOP_LEVEL_KEYWORDS) {
                    result.addElement(LookupElementBuilder.create(keyword))
                }
            } else if (isCommandLevel(parameters)) {
                for (keyword in COMMAND_LEVEL_KEYWORDS) {
                    result.addElement(LookupElementBuilder.create(keyword))
                }
            }
        }
    }
}