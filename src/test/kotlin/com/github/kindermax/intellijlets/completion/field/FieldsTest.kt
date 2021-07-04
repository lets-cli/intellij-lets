package com.github.kindermax.intellijlets.completion.field

import com.github.kindermax.intellijlets.DEFAULT_SHELLS
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.junit.Test

open class FieldsTest : BasePlatformTestCase() {

    @Test
    fun testRootCompletion() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
    
            commands:
              run:
                cmd: Echo Run
            """.trimIndent()
        )
        val variants = myFixture.getCompletionVariants("lets.yaml")
            ?: return TestCase.fail("completion variants must not be null")
        val expected = listOf(
            "shell",
            "commands",
            "env",
            "eval_env",
            "version",
            "mixins",
            "before"
        )

        TestCase.assertEquals(expected.sorted(), variants.sorted())
    }

    @Test
    fun testShellCompletion() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: <caret>
            """.trimIndent()
        )
        val variants = myFixture.getCompletionVariants("lets.yaml")
            ?: return TestCase.fail("completion variants must not be null")
        TestCase.assertEquals(DEFAULT_SHELLS.sorted(), variants.sorted())
    }

    @Test
    fun testCommandCompletionWithCLetter() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              echo:
                c<caret>
            """.trimIndent()
        )

        val variants = myFixture.getCompletionVariants("lets.yaml")
            ?: return TestCase.fail("completion variants must not be null")
        val expected = listOf(
            "description",
            "checksum",
            "persist_checksum",
            "cmd"
        )

        TestCase.assertEquals(expected.sorted(), variants.sorted())
    }

    @Test
    fun testDependsCompletionWorks() {
        myFixture.configureByText(
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
        val variants = myFixture.getCompletionVariants("lets.yaml")
            ?: return TestCase.fail("completion variants must not be null")

        val expected = listOf(
            "hi", "lol"
        )

        TestCase.assertEquals(expected.sorted(), variants.sorted())
    }
}
