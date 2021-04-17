package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentsOfType
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

const val DEPENDS_LEVEL = 3

val TOP_LEVEL_KEYWORDS = arrayOf(
    "shell",
    "before",
    "commands",
    "env",
    "eval_env",
    "version",
    "mixins"
)

val COMMAND_LEVEL_KEYWORDS = arrayOf(
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

object LetsConfigUtils {

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

        if (yamlKeyValueParents.size == DEPENDS_LEVEL) {
            return yamlKeyValueParents[0].name == "depends"
        }

        return false
    }

    /**
     * Return current command name. Must be called only when in command scope
     */
    fun getCurrentCommand(parameters: CompletionParameters): String {
        val yamlKVParents = parameters.position
            .parentsOfType<YAMLKeyValue>(false)
            .toList()
        return yamlKVParents[1].keyText
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
        return parameters.position
            .parentsOfType<YAMLKeyValue>(false)
            .find { item -> item.name == name }
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
            .find { c -> c.name == commandName }
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
        val curCommand = getCurrentCommand(parameters)

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
}

typealias Env = Map<String, String>

data class Command(
    val name: String,
    val cmd: String, // TODO can be array or map
    val env: Env,
    val evalEnv: Env,
    val depends: List<String>,
)

class ConfigParseException(message: String) : Exception(message)
class CommandParseException(message: String) : Exception(message)

class Config(
    val shell: String,
    val commands: List<Command>,
    val env: Env,
    val evalEnv: Env,
    val before: String,
) {

    companion object Parser {
        fun parseFromPSI(file: PsiFile): Config {
            return when (val child = file.firstChild) {
                is YAMLDocument -> {
                    when (val value = child.topLevelValue) {
                        is YAMLMapping -> parseConfigFromMapping(value)
                        else -> throw ConfigParseException("failed to parse config: not a valid document")
                    }
                }
                else -> throw ConfigParseException("failed to parse config: not a valid yaml")
            }
        }

        private fun parseEnv(keyValue: YAMLKeyValue): Env {
            return when (val value = keyValue.value) {
                is YAMLMapping -> value.keyValues.associate { kv -> kv.keyText to kv.valueText }
                else -> emptyMap()
            }
        }

        private fun parseShell(keyValue: YAMLKeyValue): String {
            return keyValue.valueText
        }

        // TODO handle cms as map
        private fun parseCmd(keyValue: YAMLKeyValue): String {
            return when (val value = keyValue.value) {
                is YAMLScalar -> value.text
                is YAMLSequence -> value.items.mapNotNull { it.value?.text }.joinToString(" ")
                else -> ""
            }
        }

        private fun parseDepends(keyValue: YAMLKeyValue): List<String> {
            return when (val value = keyValue.value) {
                is YAMLSequence -> value.items.mapNotNull { it.value?.text }
                else -> emptyList()
            }
        }

        private fun parseBefore(keyValue: YAMLKeyValue): String {
            return when (val value = keyValue.value) {
                is YAMLScalar -> value.textValue
                else -> ""
            }
        }

        private fun parseCommand(keyValue: YAMLKeyValue): Command {
            val name = keyValue.keyText
            var cmd = ""
            var env: Env = emptyMap()
            var evalEnv: Env = emptyMap()
            var depends = emptyList<String>()

            when (val value = keyValue.value) {
                is YAMLMapping -> {
                    value.keyValues.forEach {
                        kv ->
                        when (kv.keyText) {
                            "cmd" -> {
                                cmd = parseCmd(kv)
                            }
                            "env" -> {
                                env = parseEnv(kv)
                            }
                            "eval_env" -> {
                                evalEnv = parseEnv(kv)
                            }
                            "depends" -> {
                                depends = parseDepends(kv)
                            }
                        }
                    }
                }
                else -> throw CommandParseException("failed to parse command $name")
            }

            return Command(
                name,
                cmd,
                env,
                evalEnv,
                depends,
            )
        }

        private fun parseCommands(keyValue: YAMLKeyValue): List<Command> {
            val commands = mutableListOf<Command>()
            when (val value = keyValue.value) {
                is YAMLMapping -> {
                    value.keyValues.forEach { kv ->
                        commands.add(parseCommand(kv))
                    }
                }
            }
            return commands
        }

        private fun parseConfigFromMapping(mapping: YAMLMapping): Config {
            var shell = ""
            var commands = emptyList<Command>()
            var env: Env = emptyMap()
            var evalEnv: Env = emptyMap()
            var before = ""

            mapping.keyValues.forEach {
                kv ->
                when (kv.keyText) {
                    "shell" -> {
                        shell = parseShell(kv)
                    }
                    "env" -> {
                        env = parseEnv(kv)
                    }
                    "eval_env" -> {
                        evalEnv = parseEnv(kv)
                    }
                    "before" -> {
                        before = parseBefore(kv)
                    }
                    "commands" -> {
                        commands = parseCommands(kv)
                    }
                }
            }

            return Config(
                shell,
                commands,
                env,
                evalEnv,
                before,
            )
        }
    }
}
