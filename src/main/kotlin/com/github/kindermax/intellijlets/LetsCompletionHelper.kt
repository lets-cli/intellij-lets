package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

object LetsCompletionHelper {
    sealed class YamlContextType {
        object RootLevel : YamlContextType()
        object CommandLevel : YamlContextType()
        object ShellLevel : YamlContextType()
        object DependsLevel : YamlContextType()
        object RefLevel : YamlContextType()
        object Unknown : YamlContextType()
    }

    fun detectContext(position: PsiElement): YamlContextType {
        if (isRootLevel(position)) return YamlContextType.RootLevel

        val keyValue = PsiTreeUtil.getParentOfType(position, YAMLKeyValue::class.java) ?: return YamlContextType.Unknown

        return when {
            isInCommandKey(keyValue) -> YamlContextType.CommandLevel
            isInTopLevelKey(keyValue) -> YamlContextType.RootLevel
            keyValue.keyText == "shell" -> YamlContextType.ShellLevel
            keyValue.keyText == "depends" -> YamlContextType.DependsLevel
            keyValue.keyText == "ref" -> YamlContextType.RefLevel
            else -> YamlContextType.Unknown
        }
    }

    private fun isRootLevel(position: PsiElement): Boolean {
        return (
            position.parent.parent.parent is YAMLFile ||
            position.parent.parent.parent.parent is YAMLFile
        )
    }
    private fun isInTopLevelKey(keyValue: YAMLKeyValue): Boolean {
        return keyValue.keyText in TOP_LEVEL_KEYWORDS && keyValue.parent?.parent is YAMLFile
    }

    private fun isInCommandKey(keyValue: YAMLKeyValue): Boolean {
        val parentKeyValue = keyValue.parent?.parent as? YAMLKeyValue ?: return false
        return parentKeyValue.keyText == "commands"
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
