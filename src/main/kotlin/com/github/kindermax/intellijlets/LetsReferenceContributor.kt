package com.github.kindermax.intellijlets

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PathUtil
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
        val mixinPath = myElement.textValue // Extract "lets.build.yaml" or "lets/lets.docs.yaml"

        // Search for the mixin file anywhere in the project
        val virtualFile = findMixinFile(project, mixinPath) ?: return null

        return PsiManager.getInstance(project).findFile(virtualFile)
    }

    /**
     * This method is used to provide autocompletion suggestions for the mixin filenames
     * by suggesting YAML files in the project.
     */
    override fun getVariants(): Array<Any> {
        val project = myElement.project

        // Collect all YAML files, including in subdirectories
        val yamlFiles = FilenameIndex.getAllFilesByExt(project, "yaml")
            .mapNotNull { file -> file.path?.let { LookupElementBuilder.create(it.removePrefix(project.basePath ?: "")) } }

        return yamlFiles.toTypedArray()
    }

        /**
     * Searches for the mixin file anywhere in the project.
     * Supports both top-level files ("lets.build.yaml") and nested files ("lets/lets.docs.yaml").
     */
    private fun findMixinFile(project: Project, mixinPath: String): VirtualFile? {
        // Normalize paths (handle both "lets.build.yaml" and "lets/lets.docs.yaml")
        val normalizedPath = mixinPath.trimStart('/')

        // Look for an exact match in the project
        return FilenameIndex.getVirtualFilesByName(
            PathUtil.getFileName(normalizedPath), GlobalSearchScope.allScope(project)
        ).firstOrNull { it.path.endsWith(normalizedPath) }
    }
}