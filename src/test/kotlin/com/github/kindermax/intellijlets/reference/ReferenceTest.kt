package com.github.kindermax.intellijlets.reference

import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLKeyValue

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

    fun testDependsCommandInMixinReference() {
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
              - mixins/lets.mixin.yaml
            
            commands:
              run:
                depends: [<caret>test]
                cmd: echo Run
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition("lets.yaml")
        assertNotNull("Reference should not be null", ref)

        val resolvedElement = ref!!.resolve()
        assertNotNull("Resolved element should not be null", resolvedElement)

        val resolvedFile = resolvedElement?.containingFile
        assertEquals("lets.mixin.yaml", resolvedFile?.name)

        val resolvedKey = resolvedElement as? YAMLKeyValue
        assertNotNull("Resolved element should be a YAMLKeyValue", resolvedKey)
        assertEquals("test", resolvedKey!!.keyText)
    }

    fun testDependsCommandCrossMixinReference() {
        myFixture.addFileToProject(
            "mixins/lets.build.yaml",
            """
            shell: bash
 
            commands:
              build:
                cmd: echo Build
            """.trimIndent()
        )

        myFixture.addFileToProject(
            "mixins/lets.deploy.yaml",
            """
            shell: bash
            
            commands:
              deploy:
                depends: [<caret>build]
                cmd: echo Deploy
            """.trimIndent()
        )

        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash
            mixins:
              - mixins/lets.build.yaml
              - mixins/lets.deploy.yaml
            
            commands:
              run:
                depends: [deploy]
                cmd: echo Run
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition("mixins/lets.deploy.yaml")
        assertNotNull("Reference should not be null", ref)

        val resolvedElement = ref!!.resolve()
        assertNotNull("Resolved element should not be null", resolvedElement)

        val resolvedFile = resolvedElement?.containingFile
        assertEquals("lets.build.yaml", resolvedFile?.name)

        val resolvedKey = resolvedElement as? YAMLKeyValue
        assertNotNull("Resolved element should be a YAMLKeyValue", resolvedKey)
        assertEquals("build", resolvedKey!!.keyText)
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

open class DependsReferenceTest : BasePlatformTestCase() {
    fun testDependsCommandReference() {
        myFixture.configureByText(
            "lets.yaml",
            """
            shell: bash

            commands:
              test:
                cmd: echo Test

              run:
                depends: [<caret>test]
                cmd: echo Run
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition("lets.yaml")
        assertNotNull("Reference should not be null", ref)

        val resolvedElement = ref!!.resolve()
        assertNotNull("Resolved element should not be null", resolvedElement)

        val resolvedFile = resolvedElement?.containingFile
        assertEquals("lets.yaml", resolvedFile?.name)

        val resolvedKey = resolvedElement as? YAMLKeyValue
        assertNotNull("Resolved element should be a YAMLKeyValue", resolvedKey)
        assertEquals("test", resolvedKey!!.keyText)
    }
}


open class RefReferenceTest : BasePlatformTestCase() {
    fun testRefCommandReference() {
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
                ref: <caret>build
                args: --dev
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition("lets.yaml")
        assertNotNull("Reference should not be null", ref)

        val resolvedElement = ref!!.resolve()
        assertNotNull("Resolved element should not be null", resolvedElement)

        val resolvedFile = resolvedElement?.containingFile
        assertEquals("lets.mixin.yaml", resolvedFile?.name)

        val resolvedKey = resolvedElement as? YAMLKeyValue
        assertNotNull("Resolved element should be a YAMLKeyValue", resolvedKey)
        assertEquals("build", resolvedKey!!.keyText)
    }
}
