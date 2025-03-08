package com.github.kindermax.intellijlets.completion

import com.github.kindermax.intellijlets.DEFAULT_SHELLS
import com.intellij.testFramework.fixtures.BasePlatformTestCase

open class CompleteKeywordTest : BasePlatformTestCase() {

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
            "version",
            "mixins",
            "before",
            "init"
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
            """
            shell: bash
            commands:
              echo:
                options: |
                  Usage: lets
            """.trimIndent() + " ",
            myFixture.file.text.trimIndent(),
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
            """
            shell: bash
            commands:
              echo:
                cmd: echo Hi
                options: |
                  Usage: lets 
            """.trimIndent(),
            myFixture.file.text.trimIndent(),
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

        val expected = listOf("hi", "lol")

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
            """
            shell: bash
            commands:
              echo:
                depends:
                  -
            """.trimIndent() + " ",
            myFixture.file.text,
        )
    }

    fun testRefCompletionWorks() {
        myFixture.addFileToProject(
            "mixins/lets.mixin.yaml",
            """
            shell: bash

            commands:
              build:
                cmd: echo Build
            """.trimIndent()
        )

        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins:
              - mixins/lets.mixin.yaml

            commands:
              build-dev:
                ref: <caret>
                args: --dev
            """.trimIndent()
        )

        val variants = myFixture.getCompletionVariants("lets.yaml")
        assertNotNull(variants)

        val expected = listOf("build")

        assertEquals(expected.sorted(), variants?.sorted())
    }
}
