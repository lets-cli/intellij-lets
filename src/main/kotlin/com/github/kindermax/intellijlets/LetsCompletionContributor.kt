package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentsOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.impl.YAMLBlockSequenceImpl

open class LetsCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE), LetsCompletionProvider)
    }

    // TODO add validation for values. For example `version` must be valid semver string
    //  (maybe load all available versions from github in different thread)
    // TODO add variants for cmd as string, multiline string, array
    // TODO maybe add syntax highlighting
    // TODO maybe add more patterns in init method
    private object LetsCompletionProvider : CompletionProvider<CompletionParameters>() {
        private val TOP_LEVEL_KEYWORDS = arrayOf(
                "shell",
                "before",
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
                "depends",
                "after"
        )

        /**
         * Return current command name. Must be called only when in command scope
         */
        fun getCurrentCommand(parameters: CompletionParameters): String {
            val yamlKeyValueParents = parameters.position.parentsOfType<YAMLKeyValue>(false).toList()

            return yamlKeyValueParents[1].keyText
        }

        /**
         * Return current's element parent found by name
         * e.g
         * commands:
         *   run:
         *     cmd: echo | <- cursor are here
         *
         * findParentKeyValueWithName(parameters, "commands") -> returns `commands` node
         */
        fun findParentKeyValueWithName(parameters: CompletionParameters, name: String): YAMLKeyValue? {
            val yamlKeyValueParents = parameters.position.parentsOfType<YAMLKeyValue>(false).toList()

            return yamlKeyValueParents.find { item -> item.name == name }
        }

        /**
         * Return `commands` node
         *
         * ```yaml
         * commands: <- this node
         *   ...
         * ```
         */
        fun getCommandsNode(parameters: CompletionParameters): YAMLKeyValue? {
            return findParentKeyValueWithName(parameters, "commands")
        }

        /**
         * Return command PsiElement
         * e.g
         * commands:
         *   run:
         *     cmd: echo
         *
         * findCommandByName(commandsNode, "run") -> returns `run` command node
         */
        fun findCommandByName(commandsNode: PsiElement, commandName: String): YAMLKeyValue? {
            val commandsMapping: YAMLMapping = commandsNode.children[0] as YAMLMapping
            return commandsMapping.keyValues
                    .find{ c -> c.name == commandName}
        }

        /**
         * Return command's directive PsiElement
         * e.g
         * commands:
         *   run:
         *     cmd: echo
         *     depends: [lint]
         *
         * findDirectiveInCommandNode(commandsNode, "depends") -> returns `depends` command node
         * You need to cast result to proper Yaml type
         */
        fun findDirectiveInCommandNode(commandNode: YAMLKeyValue, directiveName: String): PsiElement? {
            // TODO type casts is not reliable
            val mapping: YAMLMapping = commandNode.children[0] as YAMLMapping
            return mapping.keyValues.find { child -> child.name == directiveName }
        }

        /**
         * Return command's depends list
         * e.g
         * commands:
         *   run:
         *     cmd: echo
         *     depends: [lint, test]
         *
         * getDependsListForCommand(commandsNode, "run") -> returns `[lint, test]`
         */
        fun getDependsListForCommand(commandsNode: PsiElement, commandName: String): List<String> {
            val node = findCommandByName(commandsNode, commandName) ?: return emptyList()
            val dependsNode = findDirectiveInCommandNode(node, "depends") ?: return emptyList()
            val dependsChildren: YAMLSequence = dependsNode.children[0] as YAMLSequence
            return dependsChildren.items.mapNotNull { item -> item.value?.text }
        }

        fun getCommandsNames(parameters: CompletionParameters): List<String> {
            val curCommand = getCurrentCommand(parameters);

            val commandsNode = getCommandsNode(parameters) ?: return emptyList()
            val dependsNode = findParentKeyValueWithName(parameters, "depends") ?: return emptyList()

            val commandsMapping: YAMLMapping = commandsNode.children[0] as YAMLMapping
            val dependsSequence: YAMLSequence = dependsNode.children[0] as YAMLSequence

            val allCommandsNames = commandsMapping.keyValues.mapNotNull { item -> item.name }
            val currDependsItems = dependsSequence.items.mapNotNull { item -> item.value?.text }

            val commandsDependsOnCurrent = mutableSetOf<String>()

            for (command in allCommandsNames.filter { c -> c != curCommand }) {
                if (getDependsListForCommand(commandsNode, command).contains(curCommand)) {
                    commandsDependsOnCurrent.add(command)
                }
            }

            val excludeList = mutableSetOf<String>()
            // exclude itself
            excludeList.add(curCommand)
            // exclude commands already in depends list
            excludeList.addAll(currDependsItems)
            // exclude commands which depends on current command (eliminate dead lock)
            excludeList.addAll(commandsDependsOnCurrent)


            return allCommandsNames
                    .filterNot { command -> excludeList.contains(command) }
                    .toSet()
                    .toList()
        }
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
            val yamlKeyValueParents = parameters.position.parentsOfType<YAMLKeyValue>(false).toList()

            if (yamlKeyValueParents.size == 2) {
                return yamlKeyValueParents[1].name == "commands"
            }
            return false
        }

        fun isRootLevel(parameters: CompletionParameters): Boolean {
            return parameters.position.parent.parent.parent is YAMLDocument
        }

        fun isDependsLevel(parameters: CompletionParameters): Boolean {
            val yamlKeyValueParents = parameters.position.parentsOfType<YAMLKeyValue>(false).toList()

            if (yamlKeyValueParents.size == 3) {
                return yamlKeyValueParents[0].name == "depends"
            }

            return false
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
            } else if (isDependsLevel(parameters)) {
                for (keyword in getCommandsNames(parameters)) {
                    result.addElement(LookupElementBuilder.create(keyword))
                }
            }
        }
    }
}