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
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem

open class LetsReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val yamlKeyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java) ?: return emptyArray()

                    return when (yamlKeyValue.keyText) {
                        "mixins" -> arrayOf(LetsMixinReference(element as YAMLScalar))
                        "depends" -> arrayOf(LetsDependsReference(element as YAMLScalar))
                        "ref" -> arrayOf(LetsRefReference(element as YAMLScalar))
                        else -> emptyArray()
                    }
                }
            }
        )
    }
}

open abstract class LetsCommandReference(element: YAMLScalar) :  PsiReferenceBase<YAMLScalar>(element) {
    /**
     * Recursively searches for the given command name in mixin files.
     */
    protected fun findCommandInMixins(yamlFile: YAMLFile, commandName: String, visitedFiles: MutableSet<YAMLFile>): PsiElement? {
        if (!visitedFiles.add(yamlFile)) {
            return null // Prevent infinite recursion
        }

        // If the current file is a mixin, retrieve the main lets.yaml file
        val mainConfigFile = findMainConfigFile(yamlFile) ?: return null

        // Find the mixins key in the main lets.yaml file
        val mixinsKey = PsiTreeUtil.findChildrenOfType(mainConfigFile, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "mixins" } ?: return null

        // Extract mixin file names from YAMLSequenceItems
        val mixinFiles = mixinsKey.value?.children
            ?.mapNotNull { it as? YAMLSequenceItem }
            ?.mapNotNull { it.value as? YAMLScalar }
            ?.mapNotNull { LetsMixinReference(it).resolve() as? YAMLFile } ?: return null

        // Search for the command in the resolved mixin files
        for (mixinFile in mixinFiles) {
            val command = PsiTreeUtil.findChildrenOfType(mixinFile, YAMLKeyValue::class.java)
                .firstOrNull { it.keyText == commandName && it.parent is YAMLMapping }

            if (command != null) {
                return command
            }

            // Recursively check mixins within this mixin
            val nestedCommand = findCommandInMixins(mixinFile, commandName, visitedFiles)
            if (nestedCommand != null) {
                return nestedCommand
            }
        }

        return null
    }

    /**
     * Finds the main lets.yaml configuration file, assuming it is located at the root.
     */
    private fun findMainConfigFile(currentFile: YAMLFile): YAMLFile? {
        val project = currentFile.project
        val mainFiles = FilenameIndex.getVirtualFilesByName("lets.yaml", GlobalSearchScope.projectScope(project))
        return mainFiles.mapNotNull { PsiManager.getInstance(project).findFile(it) as? YAMLFile }.firstOrNull()
    }
}

class LetsDependsReference(element: YAMLScalar) : LetsCommandReference(element) {
    override fun resolve(): PsiElement? {
        val commandName = myElement.textValue // Extracts the command name inside `depends`

        // Locate the command declaration in the same YAML file
        val yamlFile = myElement.containingFile as? YAMLFile ?: return null
        val localCommand = PsiTreeUtil.findChildrenOfType(yamlFile, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == commandName && it.parent is YAMLMapping }

        if (localCommand != null) {
            return localCommand
        }

        // Search for the command in mixin files (with recursive support)
        return findCommandInMixins(yamlFile, commandName, mutableSetOf())
    }
}

class LetsRefReference(element: YAMLScalar) : LetsCommandReference(element) {
    override fun resolve(): PsiElement? {
        val commandName = myElement.textValue // Extracts the command name inside `ref`

        // Locate the command declaration in the same YAML file
        val yamlFile = myElement.containingFile as? YAMLFile ?: return null
        val localCommand = PsiTreeUtil.findChildrenOfType(yamlFile, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == commandName && it.parent is YAMLMapping }

        if (localCommand != null) {
            return localCommand
        }

        // Search for the command in mixin files (with recursive support)
        return findCommandInMixins(yamlFile, commandName, mutableSetOf())
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
            .mapNotNull { file -> file.path.let { LookupElementBuilder.create(it.removePrefix(project.basePath ?: "")) } }

        return yamlFiles.toTypedArray()
    }

    /**
     * Searches for the mixin file anywhere in the project.
     * Supports both top-level files ("lets.build.yaml") and nested files ("lets/lets.docs.yaml").
     */
    private fun findMixinFile(project: Project, mixinPath: String): VirtualFile? {
        // Normalize paths (handle both "lets.mixin.yaml" and "lets/lets.mixin.yaml")
        val normalizedPath = mixinPath.trimStart('/')
        // Normalize git-ignored files (e.g. "-lets.mixin.yaml" -> "lets.mixin.yaml")
            .removePrefix("-")

        // Look for an exact match in the project
        return FilenameIndex.getVirtualFilesByName(
            PathUtil.getFileName(normalizedPath), GlobalSearchScope.allScope(project)
        ).firstOrNull { it.path.endsWith(normalizedPath) }
    }
}
