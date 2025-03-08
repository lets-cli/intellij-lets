package com.github.kindermax.intellijlets

const val DEPENDS_LEVEL = 3

const val COMMAND_CHILD_PADDING = 6
const val ROOT_PADDING = 2

val DEFAULT_SHELLS = listOf(
    "sh",
    "bash",
    "zsh",
)

val TOP_LEVEL_KEYWORDS = listOf(
    "shell",
    "before",
    "init",
    "commands",
    "env",
    "version",
    "mixins"
)

val COMMAND_LEVEL_KEYWORDS = listOf(
    "description",
    "env",
    "options",
    "checksum",
    "persist_checksum",
    "cmd",
    "work_dir",
    "depends",
    "after",
    "ref",
    "args",
)

val BUILTIN_ENV_VARIABLES  = listOf(
    "LETS_COMMAND_NAME",
    "LETS_COMMAND_ARGS",
    "LETS_COMMAND_WORK_DIR",
    "LETS_CONFIG",
    "LETS_CONFIG_DIR",
    "LETS_SHELL",
)
