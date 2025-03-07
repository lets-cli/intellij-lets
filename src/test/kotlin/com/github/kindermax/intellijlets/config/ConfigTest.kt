package com.github.kindermax.intellijlets.config

import com.github.kindermax.intellijlets.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase

open class ConfigTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/resources/config"
    }

    fun testParseConfigOk() {
        val letsFile = myFixture.copyFileToProject("/lets.yaml")
        myFixture.configureFromExistingVirtualFile(letsFile)
        val file = myFixture.file

        val config = Config.parseFromPSI(file)

        assertEquals(config.shell, "bash")
        assertEquals(config.before, "echo Before")
        assertEquals(config.init, "echo Init")
        assertEquals(
            config.mixins,
            listOf(
                Mixin.Local("lets.mixin.yaml"),
                Mixin.Remote("https://lets-cli.org/mixins/lets.mixin.yaml", "1")
            )
        )
        assertEquals(config.env, mapOf(
            "DEBUG" to EnvValue.StringValue("false"),
            "DAY" to EnvValue.ShMode("`echo Moday`"),
            "SELF_CHECKSUM" to EnvValue.ChecksumMode(listOf("lets.yaml")),
            "SELF_CHECKSUM_MAP" to EnvValue.ChecksumMapMode(mapOf("self" to listOf("lets.yaml"))),
        ))
        assertEquals(
            config.commands,
            listOf<Command>(
                Command(
                    "run",
                    "echo Run",
                    emptyMap(),
                    mapOf(
                        "DEV" to EnvValue.StringValue("true"),
                        "UID" to EnvValue.ShMode("`echo 1`")
                    ),
                    listOf("install")
                ),
                Command(
                    "install",
                    "echo Install",
                    emptyMap(),
                    emptyMap(),
                    emptyList()
                ),
                Command(
                    "build",
                    "echo Build",
                    emptyMap(),
                    emptyMap(),
                    emptyList()
                ),
                Command(
                    "dev",
                    "",
                    mapOf(
                        "app" to "echo App",
                        "db" to "echo Db",
                    ),
                    emptyMap(),
                    emptyList()
                ),
            )
        )
    }

    fun testParseBrokenConfig() {
        myFixture.configureByText(
            "lets.yaml",
            """
            - aaa
            - bb
            """.trimIndent()
        )
        val file = myFixture.file

        try {
            Config.parseFromPSI(file)
        } catch (exc: ConfigParseException) {
            assertEquals(exc.message, "failed to parse config: not a valid document")
        }
    }

    fun testParseBrokenCommand() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              run:
                - foo
                - bar
            """.trimIndent()
        )
        val file = myFixture.file

        try {
            Config.parseFromPSI(file)
        } catch (exc: CommandParseException) {
            assertEquals(exc.message, "failed to parse command run")
        }
    }
}
