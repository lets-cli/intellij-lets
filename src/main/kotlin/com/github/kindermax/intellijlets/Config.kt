package com.github.kindermax.intellijlets

import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

typealias Env = Map<String, String>

data class Command(
    val name: String,
    val cmd: String,
    val cmdAsMap: Map<String, String>,
    val env: Env,
    val evalEnv: Env,
    val depends: List<String>,
)

open class ConfigException(message: String) : Exception(message)

class ConfigParseException(message: String) : ConfigException(message)
class CommandParseException(message: String) : ConfigException(message)

/**
 * Representation of current lets.yaml.
 * Note that since we parse config during completion, the config itself may be broken at that moment,
 * so we should parse gracefully.
 */
@Suppress("LongParameterList")
class Config(
    val shell: String,
    val commands: List<Command>,
    val commandsMap: Map<String, Command>,
    val env: Env,
    val evalEnv: Env,
    val before: String,
    val specifiedDirectives: Set<String>,
) {

    companion object Parser {
        // TODO parse mixins
        fun parseFromPSI(file: PsiFile): Config {
            return when (val child = file.firstChild) {
                is YAMLDocument -> {
                    when (val value = child.topLevelValue) {
                        is YAMLMapping -> parseConfigFromMapping(value)
                        else -> defaultConfig()
                    }
                }
                else -> defaultConfig()
            }
        }

        private fun defaultConfig(): Config {
            return Config(
                "",
                emptyList(),
                emptyMap(),
                emptyMap(),
                emptyMap(),
                "",
                emptySet(),
            )
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

        @Suppress("NestedBlockDepth")
        private fun parseCommand(keyValue: YAMLKeyValue): Command {
            val name = keyValue.keyText
            var cmd = ""
            var cmdAsMap = emptyMap<String, String>()
            var env: Env = emptyMap()
            var evalEnv: Env = emptyMap()
            var depends = emptyList<String>()

            when (val value = keyValue.value) {
                is YAMLMapping -> {
                    value.keyValues.forEach {
                        kv ->
                        when (kv.keyText) {
                            "cmd" -> {

                                when (val cmdValue = kv.value) {
                                    is YAMLMapping -> {
                                        cmdAsMap = cmdValue.keyValues.associate {
                                            cmdEntry ->
                                            cmdEntry.keyText to cmdEntry.valueText
                                        }
                                    }
                                    else -> {
                                        cmd = parseCmd(kv)
                                    }
                                }
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
            }

            return Command(
                name,
                cmd,
                cmdAsMap,
                env,
                evalEnv,
                depends,
            )
        }

        @Suppress("NestedBlockDepth")
        private fun parseConfigFromMapping(mapping: YAMLMapping): Config {
            var shell = ""
            val commands = mutableListOf<Command>()
            val commandsMap = mutableMapOf<String, Command>()
            var env: Env = emptyMap()
            var evalEnv: Env = emptyMap()
            var before = ""
            val specifiedDirectives = mutableSetOf<String>()

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
                        when (val value = kv.value) {
                            is YAMLMapping -> {
                                value.keyValues.forEach { rawCommand ->
                                    val command = parseCommand(rawCommand)
                                    commands.add(command)
                                    commandsMap[command.name] = command
                                }
                            }
                        }
                    }
                }
                specifiedDirectives.add(kv.keyText)
            }

            return Config(
                shell,
                commands,
                commandsMap,
                env,
                evalEnv,
                before,
                specifiedDirectives,
            )
        }
    }
}
