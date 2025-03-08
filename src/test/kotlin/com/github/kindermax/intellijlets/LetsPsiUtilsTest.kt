package com.github.kindermax.intellijlets

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile

class LetsPsiUtilsTest : BasePlatformTestCase() {
    fun testFindCommandsInFile() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              hello:
                cmd: echo Hello
                depends: [world]
              hi:
                cmd: echo Hi
              lol:
                cmd: echo Lol
              world:
                depends: [<caret>]
                cmd: echo World
            """.trimIndent()
        )
        val commands = LetsPsiUtils.findCommandsInFile(file as YAMLFile)

        val expected = listOf("hello", "hi", "lol", "world")

        assertEquals(expected.sorted(), commands.map { it.name }.sorted())
    }

    fun testCommandsInMixins() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins: [lets.mixin1.yaml, lets.mixin2.yaml]
            commands:
              world:
                cmd: echo World
            """.trimIndent()
        )

        myFixture.configureByText(
            "lets.mixin1.yaml",
            """
            shell: bash
            commands:
              hello:
                cmd: echo Hello
            """.trimIndent()
        )

        myFixture.configureByText(
            "lets.mixin2.yaml",
            """
            shell: bash
            commands:
              bye:
                cmd: echo Bye
            """.trimIndent()
        )
        val commands = LetsPsiUtils.findCommandsInMixins(file as YAMLFile)

        val expected = listOf("hello", "bye")

        assertEquals(expected.sorted(), commands.map { it.name }.sorted())
    }

    fun testFindAllCommands() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins: [lets.mixin.yaml]
            commands:
              world:
                cmd: echo World
            """.trimIndent()
        )

        myFixture.configureByText(
            "lets.mixin.yaml",
            """
            shell: bash
            commands:
              hello:
                cmd: echo Hello
            """.trimIndent()
        )
        val commands = LetsPsiUtils.findAllCommands(file as YAMLFile)

        val expected = listOf("hello", "world")

        assertEquals(expected.sorted(), commands.map { it.name }.sorted())
    }

    fun testFindMixinFiles() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins: [lets.mixin1.yaml, lets.mixin2.yaml]
            commands:
              world:
                cmd: echo World
            """.trimIndent()
        )

        myFixture.configureByText(
            "lets.mixin1.yaml",
            """
            shell: bash
            commands:
              hello:
                cmd: echo Hello
            """.trimIndent()
        )

        myFixture.configureByText(
            "lets.mixin2.yaml",
            """
            shell: bash
            commands:
              bye:
                cmd: echo Bye
            """.trimIndent()
        )
        val mixins = LetsPsiUtils.findMixinFiles(file as YAMLFile)

        val expected = listOf("lets.mixin1.yaml", "lets.mixin2.yaml")

        assertEquals(expected.sorted(), mixins.map { it.name }.sorted())
    }

    fun testFindCurrentCommand() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              world:
                cmd: echo <caret>World
                depends: [hello]
            """.trimIndent()
        )

        val element = file.findElementAt(myFixture.caretOffset)!!
        var command = LetsPsiUtils.findCurrentCommand(element)
        assertNotNull(command)
        command = command!!

        assertEquals(command.name, "world")
        assertEquals(command.depends.sorted(), listOf("hello").sorted())
    }

    fun testFindCurrentCommandWithContextType() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              world:
                cmd: echo World
                depends: [hello]
                op<caret>
            """.trimIndent()
        )

        val element = file.findElementAt(myFixture.caretOffset - 1)!!
        var command = LetsPsiUtils.findCurrentCommand(element, YamlContextType.CommandLevel)
        assertNotNull(command)
        command = command!!

        assertEquals(command.name, "world")
        assertEquals(command.depends.sorted(), listOf("hello").sorted())
    }

    fun testFindCurrentCommandInDepends() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              world:
                cmd: echo World
                depends: [hello, <caret>]
            """.trimIndent()
        )

        val element = file.findElementAt(myFixture.caretOffset)!!
        var command = LetsPsiUtils.findCurrentCommand(element)
        assertNotNull(command)
        command = command!!

        assertEquals(command.name, "world")
        assertEquals(command.depends.sorted(), listOf("hello").sorted())
    }

    fun testUsedKeywords() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins: [lets.mixin1.yaml]
            before: echo Before
            commands:
              world:
                cmd: echo World
                env:
                  KEY: VALUE
            """.trimIndent()
        )

        val usedKeywords = LetsPsiUtils.getUsedKeywords(file as YAMLFile)
        assertEquals(usedKeywords.sorted(), listOf("shell", "mixins", "before", "commands").sorted())
    }

    fun testFindGlobalEnv() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            env:
              OS: Darwin
              DEV: true
              
            commands:
              world:
                cmd: echo World
                env:
                  FOO: BAR
            """.trimIndent()
        )

        val envKeys = LetsPsiUtils.getGlobalEnvVariables(file as YAMLFile)
        assertEquals(envKeys.toSet(), setOf("OS", "DEV"))
    }
}