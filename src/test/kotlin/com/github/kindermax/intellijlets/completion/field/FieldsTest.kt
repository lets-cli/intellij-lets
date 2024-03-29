package com.github.kindermax.intellijlets.completion.field

import com.github.kindermax.intellijlets.DEFAULT_SHELLS
import com.intellij.testFramework.fixtures.BasePlatformTestCase

open class FieldsTest : BasePlatformTestCase() {

    fun testRootCompletion() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
    
            commands:
              run:
                cmd: Echo Run
            
            <caret>
            """.trimIndent()
        )
        val variants = myFixture.getCompletionVariants("lets.yaml")
        assertNotNull(variants)

        val expected = listOf(
            "env",
            "eval_env",
            "version",
            "mixins",
            "before"
        )

        assertEquals(expected.sorted(), variants?.sorted())
    }

    fun testShellCompletion() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: <caret>
            """.trimIndent()
        )
        val variants = myFixture.getCompletionVariants("lets.yaml")
        assertNotNull(variants)
        assertEquals(DEFAULT_SHELLS.sorted(), variants?.sorted())
    }

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
        assertNotNull(variants)

        val expected = listOf(
            "description",
            "checksum",
            "persist_checksum",
            "cmd"
        )

        assertEquals(expected.sorted(), variants?.sorted())
    }

    fun testCommandOptionsCompletionOnlyOptions() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              echo:
                op<caret>
            """.trimIndent()
        )

        myFixture.completeBasic()

        assertEquals(myFixture.caretOffset, 63)
        assertEquals(
            myFixture.file.text.trimIndent(),
            """
            shell: bash
            commands:
              echo:
                options: |
                  Usage: lets
            """.trimIndent()
        )
    }

    fun testCommandOptionsCompletionNotOnlyOptions() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              echo:
                cmd: echo Hi
                op<caret>
            """.trimIndent()
        )

        myFixture.completeBasic()

        assertEquals(myFixture.caretOffset, 80)
        assertEquals(
            myFixture.file.text.trimIndent(),
            """
            shell: bash
            commands:
              echo:
                cmd: echo Hi
                options: |
                  Usage: lets 
            """.trimIndent()
        )
    }

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
        assertNotNull(variants)

        val expected = listOf(
            "hi", "lol"
        )

        assertEquals(expected.sorted(), variants?.sorted())
    }

    fun testDependsCompletionWorksNoCommandYet() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              hi:
                cmd: echo Hi
              run:
                depends:
                  - <caret>
            """.trimIndent()
        )
        val variants = myFixture.getCompletionVariants("lets.yaml")
        assertNotNull(variants)

        val expected = listOf("hi")

        assertEquals(expected.sorted(), variants?.sorted())
    }

    fun testCommandDependsCompletionArray() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            commands:
              echo:
                dep<caret>
            """.trimIndent()
        )

        myFixture.completeBasic()

        assertEquals(
            myFixture.file.text,
            """
            shell: bash
            commands:
              echo:
                depends:
                  -
            """.trimIndent()
        )
    }
}
