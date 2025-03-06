package com.github.kindermax.intellijlets

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

open class LetsReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val yamlKeyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java) ?: return emptyArray()
                    if (yamlKeyValue.keyText == "mixins") {
                        return arrayOf(LetsMixinReference(element as YAMLScalar))
                    }
                    return emptyArray()
                }
            }
        )
    }
}


class LetsMixinReference(element: YAMLScalar) : PsiReferenceBase<YAMLScalar>(element) {
    /**
     * This method is used to resolve the reference to the mixin file.
     * Currently, it supports only local files in the project.
     */
    override fun resolve(): PsiElement? {
        val project = myElement.project
        val mixinFilename = myElement.textValue // Extract the filename from the scalar value

        // Search for the mixin file in the project
        val virtualFile = FilenameIndex.getVirtualFilesByName(
            project,
            mixinFilename,
            GlobalSearchScope.allScope(project),
        ).firstOrNull() ?: return null

        return PsiManager.getInstance(project).findFile(virtualFile)
    }

    /**
     * This method is used to provide autocompletion suggestions for the mixin filenames
     * by suggesting YAML files in the project.
     */
    override fun getVariants(): Array<Any> {
        val project = myElement.project
        val mixinFiles = FilenameIndex.getAllFilesByExt(project, "yaml")
            .map { LookupElementBuilder.create(it.name) }

        return mixinFiles.toTypedArray()
    }
}