package com.github.kindermax.intellijlets.completion

import com.github.kindermax.intellijlets.BUILTIN_ENV_VARIABLES
import com.intellij.testFramework.fixtures.BasePlatformTestCase

open class CompleteEnvTest : BasePlatformTestCase() {

    fun testBuiltInEnvCompletion() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
    
            commands:
              run:
                cmd: Echo $<caret>
            """.trimIndent()
        )
        val variants = myFixture.getCompletionVariants("lets.yaml")
        assertNotNull(variants)

        val expected = BUILTIN_ENV_VARIABLES.map { "\${$it}" }.toSet()

        assertEquals(expected, variants?.toSet())
    }

    fun testEnvFromOptionsEnvCompletion() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
    
            commands:
              run:
                options: |
                  Usage: lets run <env>
                cmd: Echo $<caret>
            """.trimIndent()
        )
        val variants = myFixture.getCompletionVariants("lets.yaml")
        assertNotNull(variants)

        val expected = BUILTIN_ENV_VARIABLES.map { "\${$it}" }.toMutableSet()
        expected.add("\${LETSOPT_ENV}")
        expected.add("\${LETSCLI_ENV}")

        assertEquals(expected, variants?.toSet())
    }

    fun testCompleteFromGlobalEnv() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            env:
              DEV: true 
    
            commands:
              run:
                cmd: Echo $<caret>
            """.trimIndent()
        )
        val variants = myFixture.getCompletionVariants("lets.yaml")
        assertNotNull(variants)

        val expected = BUILTIN_ENV_VARIABLES.map { "\${$it}" }.toMutableSet()
        expected.add("\${DEV}")

        assertEquals(expected, variants?.toSet())
    }
}
