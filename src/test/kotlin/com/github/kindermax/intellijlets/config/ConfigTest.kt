package com.github.kindermax.intellijlets.config

import com.github.kindermax.intellijlets.Command
import com.github.kindermax.intellijlets.CommandParseException
import com.github.kindermax.intellijlets.Config
import com.github.kindermax.intellijlets.ConfigParseException
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.junit.Test

open class ConfigTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/resources/config"
    }

    @Test
    fun testParseConfigSuccess() {
        val letsFile = myFixture.copyFileToProject("/lets.yaml")
        myFixture.configureFromExistingVirtualFile(letsFile)
        val file = myFixture.file

        val config = Config.parseFromPSI(file)

        TestCase.assertEquals(config.shell, "bash")
        TestCase.assertEquals(config.before, "echo Before")
        TestCase.assertEquals(config.env, mapOf("DEBUG" to "false"))
        TestCase.assertEquals(config.evalEnv, mapOf("DAY" to "`echo Moday`"))
        TestCase.assertEquals(
            config.commands,
            listOf<Command>(
                Command(
                    "run",
                    "echo Run",
                    mapOf("DEV" to "true"),
                    mapOf("UID" to "`echo 1`"),
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
            )
        )
    }

    fun testParseBrokenConfig() {
        val letsFile = myFixture.copyFileToProject("/lets_invalid_config.yaml")
        myFixture.configureFromExistingVirtualFile(letsFile)
        val file = myFixture.file

        try {
            Config.parseFromPSI(file)
        } catch (exc: ConfigParseException) {
            TestCase.assertEquals(exc.message, "failed to parse config: not a valid document")
        }
    }

    fun testParseBrokenCommand() {
        val letsFile = myFixture.copyFileToProject("/lets_invalid_command.yaml")
        myFixture.configureFromExistingVirtualFile(letsFile)
        val file = myFixture.file

        try {
            Config.parseFromPSI(file)
        } catch (exc: CommandParseException) {
            TestCase.assertEquals(exc.message, "failed to parse command run")
        }
    }
}
