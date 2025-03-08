package com.github.kindermax.intellijlets.config

import com.github.kindermax.intellijlets.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

open class ConfigParserTest : BasePlatformTestCase() {
    fun testParseCommand() {
         val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            
            commands:
              run:
                depends:
                  - install
                env:
                  DEV: true
                  UID:
                    sh: `echo 1`
                cmd: echo Run

              install:
                cmd: echo Install

              build:
                cmd:
                  - echo
                  - Build
              dev:
                cmd:
                  app: echo App
                  db: echo Db
            """.trimIndent()
        ) as YAMLFile

        val commandsKV = PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "commands" }!!

        val commands = PsiTreeUtil.findChildrenOfType(commandsKV, YAMLKeyValue::class.java)
            .filter { it.parent.parent == commandsKV }
            .map { ConfigParser.parseCommand(it) }

        val elements = PsiTreeUtil.findChildrenOfType(commandsKV, YAMLKeyValue::class.java)
            .filter { it.parent.parent == commandsKV }

        assertEquals(
            commands,
            listOf<Command>(
                Command(
                    "run",
                    "echo Run",
                    emptyMap(),
                    mapOf(
                        "DEV" to EnvValue.StringValue("true"),
                        "UID" to EnvValue.ShMode("`echo 1`")
                    ),
                    listOf("install"),
                    elements[0]
                ),
                Command(
                    "install",
                    "echo Install",
                    emptyMap(),
                    emptyMap(),
                    emptyList(),
                    elements[1]
                ),
                Command(
                    "build",
                    "echo Build",
                    emptyMap(),
                    emptyMap(),
                    emptyList(),
                    elements[2],
                ),
                Command(
                    "dev",
                    "",
                    mapOf(
                        "app" to "echo App",
                        "db" to "echo Db",
                    ),
                    emptyMap(),
                    emptyList(),
                    elements[3],
                ),
            )
        )
    }

    fun testParseEnv() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            env:
              DEBUG: false
              DAY:
                sh: `echo Moday`
              SELF_CHECKSUM:
                checksum: [lets.yaml]
              SELF_CHECKSUM_MAP:
                checksum:
                  self:
                    - lets.yaml
            commands:
              build:
                cmd: echo Build
            """.trimIndent()
        ) as YAMLFile

        val envKV = PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "env" }!!

        val env = ConfigParser.parseEnv(envKV)

        assertEquals(env, mapOf(
            "DEBUG" to EnvValue.StringValue("false"),
            "DAY" to EnvValue.ShMode("`echo Moday`"),
            "SELF_CHECKSUM" to EnvValue.ChecksumMode(listOf("lets.yaml")),
            "SELF_CHECKSUM_MAP" to EnvValue.ChecksumMapMode(mapOf("self" to listOf("lets.yaml"))),
        ))
    }

    fun testParseMixins() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins:
              - lets.mixin.yaml
              - url: https://lets-cli.org/mixins/lets.mixin.yaml
                version: 1
            commands:
              build:
                cmd: echo Build
            """.trimIndent()
        ) as YAMLFile

        val mixinsKV = PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "mixins" }!!

        val mixins = ConfigParser.parseMixins(mixinsKV)

        assertEquals(
            mixins,
            listOf(
                Mixin.Local("lets.mixin.yaml"),
                Mixin.Remote("https://lets-cli.org/mixins/lets.mixin.yaml", "1")
            )
        )
    }

    fun testParseShell() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            """.trimIndent()
        ) as YAMLFile

        val shellKV = PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "shell" }!!

        val shell = ConfigParser.parseShell(shellKV)
        assertEquals(shell, "bash")
    }

    fun testParseBefore() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            before: echo Before
            init: echo Init
            """.trimIndent()
        ) as YAMLFile

        val beforeKV = PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "before" }!!

        val before = ConfigParser.parseBefore(beforeKV)
        assertEquals(before, "echo Before")
    }

    fun testParseInit() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            before: echo Before
            init: echo Init
            """.trimIndent()
        ) as YAMLFile

        val initKV = PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "init" }!!

        val init = ConfigParser.parseBefore(initKV)
        assertEquals(init, "echo Init")
    }
}
