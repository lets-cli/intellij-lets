package com.github.kindermax.intellijlets.completion.field

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.junit.Test

open class FieldsTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/resources/completion"
    }

    @Test
    fun testRootCompletion() {
        val letsFile = myFixture.copyFileToProject("/root/lets.yaml")
        myFixture.configureFromExistingVirtualFile(letsFile)
        val variants = myFixture.getCompletionVariants("/root/lets.yaml")
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
    fun testCommandCompletionWithCLetter() {
        val letsFile = myFixture.copyFileToProject("/command/lets.yaml")
        myFixture.configureFromExistingVirtualFile(letsFile)
        val variants = myFixture.getCompletionVariants("/command/lets.yaml")
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
        val letsFile = myFixture.copyFileToProject("/depends/lets.yaml")
        myFixture.configureFromExistingVirtualFile(letsFile)
        val variants = myFixture.getCompletionVariants("/depends/lets.yaml")
            ?: return TestCase.fail("completion variants must not be null")

        val expected = listOf(
            "hi", "lol"
        )

        TestCase.assertEquals(expected.sorted(), variants.sorted())
    }
}
