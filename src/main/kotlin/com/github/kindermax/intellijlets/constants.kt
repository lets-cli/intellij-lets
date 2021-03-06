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
    "commands",
    "env",
    "eval_env",
    "version",
    "mixins"
)

val COMMAND_LEVEL_KEYWORDS = listOf(
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
