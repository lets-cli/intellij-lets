package com.github.kindermax.intellijlets.reference

import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

open class MinixsReferenceTest : BasePlatformTestCase() {
    fun testMixinFileReference() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins:
              - <caret>lets.mixin.yaml
    
            commands:
              run:
                cmd: echo Run
            """.trimIndent()
        )

        myFixture.configureByText(
            "lets.mixin.yaml",
            """
            shell: bash
    
            commands:
              test:
                cmd: echo Test
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition("lets.yaml")
        assertNotNull(ref)
        val resolvedFile = ref!!.resolve() as PsiFile
        assertNotNull(resolvedFile)
        assertEquals("lets.mixin.yaml", resolvedFile.name)
    }

    fun testMixinFileInDirReference() {
        myFixture.addFileToProject(
            "mixins/lets.mixin.yaml",
            """
            shell: bash

            commands:
              test:
                cmd: echo Test
            """.trimIndent()
        )
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins:
              - <caret>mixins/lets.mixin.yaml

            commands:
              run:
                cmd: echo Run
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition("lets.yaml")
        assertNotNull(ref)
        val resolvedFile = ref!!.resolve() as PsiFile
        assertNotNull(resolvedFile)
        assertEquals("lets.mixin.yaml", resolvedFile.name)
    }

    // When the mixin file has '-' at the beginning in the `mixins` directive
    fun testGitIgnoredMixinFileReference() {
        myFixture.addFileToProject(
            "lets.mixin.yaml",
            """
            shell: bash

            commands:
              test:
                cmd: echo Test
            """.trimIndent()
        )
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins:
              - -lets<caret>.mixin.yaml

            commands:
              run:
                cmd: echo Run
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition("lets.yaml")
        assertNotNull(ref)
        val resolvedFile = ref!!.resolve() as PsiFile
        assertNotNull(resolvedFile)
        assertEquals("lets.mixin.yaml", resolvedFile.name)
    }

    fun testNoMixinFile() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins:
              - <caret>lets.mixin.yaml

            commands:
              run:
                cmd: echo Run
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition("lets.yaml")
        assertNotNull(ref)
        val resolvedFile = ref!!.resolve()
        assertNull(resolvedFile)
    }
}

