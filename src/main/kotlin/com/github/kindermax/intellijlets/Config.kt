package com.github.kindermax.intellijlets

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
    val yaml: YAMLKeyValue,
)

class ConfigParser {
    companion object {
//        @Suppress("NestedBlockDepth")
        fun parseCommand(obj: YAMLKeyValue): Command {
            val name = obj.keyText
            var depends = emptyList<String>()

            var cmd = ""
            var cmdAsMap = emptyMap<String, String>()
            var env: Env = emptyMap()

             when (val value = obj.value) {
                is YAMLMapping -> {
                    value.keyValues.forEach {
                        kv ->
                        when (kv.keyText) {
                            "depends" -> {
                                depends = parseDepends(kv)
                            }
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
                obj,
            )
        }

       fun parseDepends(obj: YAMLKeyValue): List<String> {
            return when (val value = obj.value) {
                is YAMLSequence -> value.items.mapNotNull { it.value?.text }
                else -> emptyList()
            }
       }

        fun parseEnv(keyValue: YAMLKeyValue): Env {
            val value = keyValue.value as? YAMLMapping ?: return emptyMap()

            return value.keyValues.associate { kv ->
                kv.keyText to parseEnvValue(kv)
            }
        }

        fun parseEnvValue(kv: YAMLKeyValue): EnvValue {
            return when (val envValue = kv.value) {
                is YAMLScalar -> EnvValue.StringValue(envValue.textValue)
                is YAMLMapping -> parseMappingEnvValue(envValue)
                else -> EnvValue.StringValue("")
            }
        }

        fun parseMappingEnvValue(value: YAMLMapping): EnvValue {
            value.keyValues.forEach { kv ->
                when (kv.keyText) {
                    "sh" -> return EnvValue.ShMode(kv.valueText)
                    "checksum" -> {
                        return when (val checksumValue = kv.value) {
                            is YAMLSequence -> EnvValue.ChecksumMode(
                                checksumValue.items.mapNotNull { it.value?.text }
                            )
                            is YAMLMapping -> EnvValue.ChecksumMapMode(
                                checksumValue.keyValues.associate { entry ->
                                    entry.keyText to (entry.value as YAMLSequence).items.mapNotNull { it.value?.text }
                                }
                            )
                            else -> EnvValue.StringValue("")
                        }
                    }
                }
            }
            return EnvValue.StringValue("")
        }

        fun parseShell(keyValue: YAMLKeyValue): String {
            return keyValue.valueText
        }

        fun parseCmd(keyValue: YAMLKeyValue): String {
            return when (val value = keyValue.value) {
                is YAMLScalar -> value.text
                is YAMLSequence -> value.items.mapNotNull { it.value?.text }.joinToString(" ")
                else -> ""
            }
        }

        fun parseBefore(keyValue: YAMLKeyValue): String {
            return when (val value = keyValue.value) {
                is YAMLScalar -> value.textValue
                else -> ""
            }
        }

        fun parseInit(keyValue: YAMLKeyValue): String {
            return when (val value = keyValue.value) {
                is YAMLScalar -> value.textValue
                else -> ""
            }
        }

        fun parseMixins(keyValue: YAMLKeyValue): Mixins {
            return when (val value = keyValue.value) {
                is YAMLSequence -> {
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
                }
                else -> emptyList()
            }
        }
    }
}
