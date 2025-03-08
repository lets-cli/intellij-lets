package com.github.kindermax.intellijlets.completion

import com.github.kindermax.intellijlets.LetsPsiUtils
import com.github.kindermax.intellijlets.YamlContextType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

open class DetectContextTest : BasePlatformTestCase() {
    fun testRootLevel() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
    
            <caret>
            commands:
              run:
                cmd: Echo Run
            """.trimIndent()
        )
        val position = file.findElementAt(myFixture.caretOffset)
        assertNotNull(position)

        val context = LetsPsiUtils.detectContext(position!!)
        assertEquals(context, YamlContextType.RootLevel)
    }

    fun testCommandLevel() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
    
            commands:
              run:
                c<caret>
            """.trimIndent()
        )
        val offset = myFixture.caretOffset
        val position = file.findElementAt(offset - 1)
        assertNotNull("PsiElement should not be null", position)

        val context = LetsPsiUtils.detectContext(position!!)
        assertEquals(context, YamlContextType.CommandLevel)
    }

    fun testShellLevel() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: b<caret>
            """.trimIndent()
        )
        val offset = myFixture.caretOffset
        val position = file.findElementAt(offset - 1)
        assertNotNull("PsiElement should not be null", position)

        val context = LetsPsiUtils.detectContext(position!!)
        assertEquals(context, YamlContextType.ShellLevel)
    }

    fun testDependsLevel() {
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
        val offset = myFixture.caretOffset
        val position = file.findElementAt(offset)
        assertNotNull("PsiElement should not be null", position)

        val context = LetsPsiUtils.detectContext(position!!)
        assertEquals(context, YamlContextType.DependsLevel)
    }

    fun testRefLevel() {
        val file = myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins:
              - mixins/lets.mixin.yaml

            commands:
              build-dev:
                ref: b<caret>
                args: --dev
            """.trimIndent()
        )
        val offset = myFixture.caretOffset
        val position = file.findElementAt(offset - 1)
        assertNotNull("PsiElement should not be null", position)

        val context = LetsPsiUtils.detectContext(position!!)
        assertEquals(context, YamlContextType.RefLevel)
    }
}
