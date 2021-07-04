package com.github.kindermax.intellijlets

const val DEPENDS_LEVEL = 3

val DEFAULT_SHELLS = arrayOf(
    "sh",
    "bash",
    "zsh",
)

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