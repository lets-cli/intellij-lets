package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.psi.util.parentsOfType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

object LetsCompletionHelper {
    private fun isInTopLevelDirective(name: String, parameters: CompletionParameters): Boolean {
        val yamlKeyValueParents = parameters.position.parentsOfType<YAMLKeyValue>(false).toList()

        if (yamlKeyValueParents.size == 1) {
            return yamlKeyValueParents[0].name == name
        }
        return false
    }

    /**
     * Check if current position is in command context. It means:
     * commands:
     *   echo:
     *     | -> cursor is here
     */
    fun isCommandLevel(parameters: CompletionParameters): Boolean {
        val yamlKeyValueParents = parameters.position.parentsOfType<YAMLKeyValue>(false).toList()

        if (yamlKeyValueParents.size == 2) {
            return yamlKeyValueParents[1].name == "commands"
        }
        return false
    }

    /**
     * Check if current position is in shell context. It means:
     * shell: | -> cursor is here
     */
    fun isShellLevel(parameters: CompletionParameters): Boolean {
        return isInTopLevelDirective("shell", parameters)
    }

    fun isRootLevel(parameters: CompletionParameters): Boolean {
        return (
            parameters.position.parent.parent.parent is YAMLFile ||
                parameters.position.parent.parent.parent.parent is YAMLFile
            )
    }

    fun isDependsLevel(parameters: CompletionParameters): Boolean {
        val yamlKeyValueParents = parameters.position.parentsOfType<YAMLKeyValue>(false).toList()

        if (yamlKeyValueParents.size == DEPENDS_LEVEL) {
            return yamlKeyValueParents[0].name == "depends"
        }

        return false
    }

    fun isRefLevel(parameters: CompletionParameters): Boolean {
        val yamlKeyValueParents = parameters.position.parentsOfType<YAMLKeyValue>(false).toList()

        if (yamlKeyValueParents.size == REF_LEVEL) {
            return yamlKeyValueParents[0].name == "ref"
        }

        return false
    }

    /**
     * Get all possible commands suggestions for a `depends`, except:
     * - itself
     * - already specified commands in depends
     * - other commands which depend on current command
     */
    fun getDependsSuggestions(parameters: CompletionParameters): List<String> {
        val yamlFile = parameters.originalFile as? YAMLFile ?: return emptyList()
        val allCommands = LetsPsiUtils.findAllCommands(yamlFile)
        val currentCommand = LetsPsiUtils.findCurrentCommand(parameters.position) ?: return emptyList()

        val excludeList = mutableSetOf<String>()
        // exclude itself
        excludeList.add(currentCommand.name)
        // exclude commands already in depends list
        excludeList.addAll(currentCommand.depends)

        // exclude commands which depends on current command (eliminate recursive dependencies)
        for (command in allCommands.filter { c -> c.name != currentCommand.name }) {
            if (command.depends.contains(currentCommand.name)) {
                excludeList.add(command.name)
            }
        }

        return allCommands
            .filterNot { command -> excludeList.contains(command.name) }
            .map { it.name }
            .toList()
    }

    /**
     * Get all possible commands suggestions for a `ref`, except:
     * - itself
     * Since ref is a YAMLScalar, only one command is suggested.
     */
    fun getRefSuggestions(parameters: CompletionParameters): List<String> {
        val yamlFile = parameters.originalFile as? YAMLFile ?: return emptyList()
        val allCommands = LetsPsiUtils.findAllCommands(yamlFile)
        val currentCommand = LetsPsiUtils.findCurrentCommand(parameters.position) ?: return emptyList()
        // Exclude itself from suggestions and return only one suggestion
        return allCommands.filterNot { it.name == currentCommand.name }
            .map { it.name }
    }
}
