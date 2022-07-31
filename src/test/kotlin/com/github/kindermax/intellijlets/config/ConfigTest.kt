package com.github.kindermax.intellijlets.config

import com.github.kindermax.intellijlets.Command
import com.github.kindermax.intellijlets.CommandParseException
import com.github.kindermax.intellijlets.Config
import com.github.kindermax.intellijlets.ConfigParseException
import com.intellij.testFramework.fixtures.BasePlatformTestCase

open class ConfigTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/resources/config"
    }

    fun testParseConfigSuccess() {
        val letsFile = myFixture.copyFileToProject("/lets.yaml")
        myFixture.configureFromExistingVirtualFile(letsFile)
        val file = myFixture.file

        val config = Config.parseFromPSI(file)

        assertEquals(config.shell, "bash")
        assertEquals(config.before, "echo Before")
        assertEquals(config.env, mapOf("DEBUG" to "false"))
        assertEquals(config.evalEnv, mapOf("DAY" to "`echo Moday`"))
        assertEquals(
            config.commands,
            listOf<Command>(
                Command(
                    "run",
                    "echo Run",
                    emptyMap(),
                    mapOf("DEV" to "true"),
                    mapOf("UID" to "`echo 1`"),
                    listOf("install")
                ),
                Command(
                    "install",
                    "echo Install",
                    emptyMap(),
                    emptyMap(),
                    emptyMap(),
                    emptyList()
                ),
                Command(
                    "build",
                    "echo Build",
                    emptyMap(),
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
