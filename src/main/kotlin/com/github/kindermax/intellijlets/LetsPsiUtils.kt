package com.github.kindermax.intellijlets

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.*

object LetsPsiUtils {
    fun findCommandsInFile(yamlFile: YAMLFile): List<Command> {
        val commandsKV = PsiTreeUtil.findChildrenOfType(yamlFile, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "commands" } ?: return emptyList()

        return PsiTreeUtil.findChildrenOfType(commandsKV, YAMLKeyValue::class.java)
            .filter { it.parent.parent == commandsKV }
            .mapNotNull { keyValue -> ConfigParser.parseCommand(keyValue) }
    }

    fun findCommandsInMixins(yamlFile: YAMLFile): List<Command> {
        val mixinFiles = findMixinFiles(yamlFile)
        return mixinFiles.flatMap { findCommandsInFile(it) }
    }

    /**
     * Find all commands in the given YAML file and its mixins.
     * If a command is defined in both the file and its mixins, commands are merged.
     * Merge rules are: ???
     */
    fun findAllCommands(yamlFile: YAMLFile): List<Command> {
        val localCommands = findCommandsInFile(yamlFile)
        val mixinCommands = findCommandsInMixins(yamlFile)
        // TODO: implement merge rules
        return localCommands + mixinCommands
    }

    /**
     * Find all mixin files referenced in the given YAML file.
     * It searches for the "mixins" key in the root of the file,
     * and returns the resolved YAML files (files must exist to end up in result).
     */
    fun findMixinFiles(yamlFile: YAMLFile): List<YAMLFile> {
        val mixinsKey = PsiTreeUtil.findChildrenOfType(yamlFile, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "mixins" } ?: return emptyList()

        return mixinsKey.value?.children
            ?.mapNotNull { it as? YAMLSequenceItem }
            ?.mapNotNull { it.value as? YAMLScalar }
            ?.mapNotNull { LetsMixinReference(it).resolve() as? YAMLFile }
            ?: emptyList()
    }

    /**
     * Find the command that the given position is in.
     * If position is inside a command, return the command, otherwise return null.
     */
    fun findCurrentCommand(
        position: PsiElement
    ): Command? {
        val currentKeyValue = PsiTreeUtil.getParentOfType(position, YAMLKeyValue::class.java) ?: return null
        if (!COMMAND_LEVEL_KEYWORDS.contains(currentKeyValue.keyText)) {
            return null
        }
        val parentCommand = currentKeyValue.parent.parent as? YAMLKeyValue ?: return null
        return ConfigParser.parseCommand(parentCommand)
    }

    fun getUsedKeywords(yamlFile: YAMLFile): List<String> {
        val usedKeywords = mutableListOf<String>()

        val rootMapping = yamlFile.documents.firstOrNull()?.topLevelValue as? YAMLMapping ?: return emptyList()

        for (keyValue in rootMapping.keyValues) {
            if (keyValue.keyText in TOP_LEVEL_KEYWORDS) {
                usedKeywords.add(keyValue.keyText)
            }
        }

        return usedKeywords
    }

    fun getGlobalEnvVariables(yamlFile: YAMLFile): List<String> {
        val envKey = PsiTreeUtil.findChildrenOfType(yamlFile, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "env" } ?: return emptyList()

        return (envKey.value as? YAMLMapping)
            ?.keyValues
            ?.mapNotNull { it.keyText }
            ?: emptyList()
    }
}