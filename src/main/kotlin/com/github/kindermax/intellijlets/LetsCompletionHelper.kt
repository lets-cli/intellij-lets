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

    private fun getDependsParentName(parameters: CompletionParameters): String? {
        val yamlKeyValueParents = parameters.position.parentsOfType<YAMLKeyValue>(false).toList()

        if (yamlKeyValueParents.size == DEPENDS_LEVEL) {
            return yamlKeyValueParents[1].name
        }

        return ""
    }

    /**
     * Get all possible depends suggestions for a command, except:
     * - itself
     * - already specified commands in depends
     * - other commands which depend on current command
     */
    fun getDependsSuggestions(parameters: CompletionParameters, config: Config): List<String> {
        val cmdName = getDependsParentName(parameters) ?: return emptyList()
        val cmd = config.commandsMap[cmdName] ?: return emptyList()

        val excludeList = mutableSetOf<String>()
        // exclude itself
        excludeList.add(cmdName)
        // exclude commands already in depends list
        excludeList.addAll(cmd.depends)

        // exclude commands which depends on current command (eliminate recursive dependencies)
        for (command in config.commands.filter { c -> c.name != cmdName }) {
            if (command.depends.contains(cmdName)) {
                excludeList.add(command.name)
            }
        }

        return config.commandsMap.keys
            .filterNot { command -> excludeList.contains(command) }
            .toList()
    }
}
