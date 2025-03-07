package com.github.kindermax.intellijlets

import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

sealed class EnvValue {
    data class StringValue(val value: String) : EnvValue()
    data class ShMode(val sh: String) : EnvValue()
    data class ChecksumMode(val files: List<String>) : EnvValue()
    data class ChecksumMapMode(val files: Map<String, List<String>>) : EnvValue()
}

typealias Env = Map<String, EnvValue>

sealed class Mixin {
    data class Local(val path: String) : Mixin()
    data class Remote(val url: String, val version: String) : Mixin()
}

typealias Mixins = List<Mixin>

data class Command(
    val name: String,
    val cmd: String,
    val cmdAsMap: Map<String, String>,
    val env: Env,
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
    val before: String,
    val init: String,
    val mixins: Mixins,
    // Keywords that are used in the config
    val keywordsInConfig: Set<String>,
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
                "",
                "",
                emptyList(),
                emptySet(),
            )
        }

        private fun parseEnv(keyValue: YAMLKeyValue): Env {
            return when (val value = keyValue.value) {
                is YAMLMapping -> value.keyValues.associate {
                    kv -> kv.keyText to when (kv.value) {
                        is YAMLScalar -> EnvValue.StringValue(kv.valueText)
                        is YAMLMapping -> {
                            val kvv = kv.value as YAMLMapping
                            kvv.getKeyValueByKey("sh")?.let {
                                EnvValue.ShMode(it.valueText)
                            } ?: kvv.getKeyValueByKey("checksum")?.let {
                                when (it.value) {
                                    is YAMLSequence -> {
                                        EnvValue.ChecksumMode((it.value as YAMLSequence).items.mapNotNull { it.value?.text })
                                    }

                                    is YAMLMapping -> {
                                        val checksumMap = it.value as YAMLMapping
                                        EnvValue.ChecksumMapMode(checksumMap.keyValues.associate { entry ->
                                            entry.keyText to (entry.value as YAMLSequence).items.mapNotNull { it.value?.text }
                                        })
                                    }

                                    else -> {
                                        EnvValue.ChecksumMode(emptyList())
                                    }
                                }
                            } ?: EnvValue.StringValue("")
                        }
                        else -> EnvValue.StringValue("")
                    }
                }
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

        private fun parseInit(keyValue: YAMLKeyValue): String {
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
                depends,
            )
        }

        @Suppress("NestedBlockDepth")
        private fun parseConfigFromMapping(mapping: YAMLMapping): Config {
            var shell = ""
            val mixins = mutableListOf<Mixin>()
            val commands = mutableListOf<Command>()
            val commandsMap = mutableMapOf<String, Command>()
            var env: Env = emptyMap()
            var before = ""
            var init = ""
            val keywordsInConfig = mutableSetOf<String>()

            mapping.keyValues.forEach {
                kv ->
                when (kv.keyText) {
                    "shell" -> {
                        shell = parseShell(kv)
                    }
                    "mixins" -> {
                        when (val value = kv.value) {
                            is YAMLSequence -> {
                                mixins.addAll(
                                    value.items.mapNotNull { it.value }
                                        .map { when (it) {
                                            is YAMLScalar -> Mixin.Local(it.textValue)
                                            is YAMLMapping -> {
                                                val url = it.getKeyValueByKey("url")?.valueText ?: ""
                                                val version = it.getKeyValueByKey("version")?.valueText ?: ""
                                                Mixin.Remote(url, version)
                                            }
                                            else -> Mixin.Local("")
                                        } }
                                )
                            }
                        }
                    }
                    "env" -> {
                        env = parseEnv(kv)
                    }
                    "before" -> {
                        before = parseBefore(kv)
                    }
                    "init" -> {
                        init = parseInit(kv)
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
                keywordsInConfig.add(kv.keyText)
            }

            return Config(
                shell,
                commands,
                commandsMap,
                env,
                before,
                init,
                mixins,
                keywordsInConfig,
            )
        }
    }
}
