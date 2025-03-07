package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.util.TextRange
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.Language
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiFile

/**
 * This class provides completions for environment variables in Lets YAML files.
 * Supports:
 * - Completion of `$` in `options` key
 * - Completion of `$` and `$L` in `cmd` key (only works as a fallback
 *  if the cmd script is not detected as shell script, see LetsEnvVariableShellScriptCompletionContributor)
 */
class LetsEnvVariableCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inside(YAMLScalar::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val element = parameters.position
                    val caret = parameters.editor.caretModel.currentCaret
                    val lineOffset = caret.visualLineStart
                    val prefixText = parameters.editor.document.getText(TextRange(lineOffset, caret.offset))

                    val keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java) ?: return

                    when (keyValue.keyText) {
                        "options" -> {
                            val prefixMatcher = result.withPrefixMatcher("$")
                            prefixMatcher.addElement(
                                createEnvVariableLookupElement("LETS_COMMAND_NAME")
                            )
                        }
                        "cmd" -> {
                            if (prefixText.endsWith("$")) {
                                val prefixMatcher = result.withPrefixMatcher("$")
                                BUILTIN_ENV_VARIABLES.forEach {
                                    prefixMatcher.addElement(
                                        createEnvVariableLookupElement(it)
                                    )
                                }
                            } else if (prefixText.endsWith("\$L")) {
                                val prefixMatcher = result.withPrefixMatcher("\$L")
                                BUILTIN_ENV_VARIABLES.forEach {
                                    prefixMatcher.addElement(
                                        createEnvVariableLookupElement(it)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val document = context.editor.document

        // Ensure `$` is treated as a valid trigger for completion
        if (offset > 0 && document.charsSequence[offset - 1] == '$') {
            context.dummyIdentifier = "$" // This forces completion when `$` is typed
        }
    }
}

/**
 * This class provides completions for environment variables in Lets YAML files,
 * specifically for shell scripts in `cmd` key.
 * In order for this completion contributor to work, cmd must be detected as the shell script language.
 * If not detected as shell script, the completion will fallback to LetsEnvVariableCompletionContributor.
 */
class LetsEnvVariableShellScriptCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(Language.findLanguageByID("Shell Script")!!),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val element = parameters.position

                    // Retrieve the original YAML file from the injected Bash script
                    val injectedLanguageManager = InjectedLanguageManager.getInstance(element.project)
                    val yamlFile: PsiFile = injectedLanguageManager.getInjectionHost(element)?.containingFile ?: return

                    // Ensure it's a YAML file
                    if (yamlFile !is org.jetbrains.yaml.psi.YAMLFile) return

                    // Retrieve the correct offset in the original YAML file
                    val hostOffset = injectedLanguageManager.injectedToHost(element, element.textOffset)

                    // Find the corresponding element in the original YAML file
                    val elementAtOffset = yamlFile.findElementAt(hostOffset) ?: return
                    val yamlKeyValue = PsiTreeUtil.getParentOfType(elementAtOffset, YAMLKeyValue::class.java) ?: return

                     // Ensure we are inside `cmd`
                    if (yamlKeyValue.keyText != "cmd") return

                    val prefixText = parameters.editor.document.getText(TextRange(parameters.offset - 1, parameters.offset))

                    if (prefixText.endsWith("$")) {
                        val prefixMatcher = result.withPrefixMatcher("$")
                        BUILTIN_ENV_VARIABLES.forEach {
                            prefixMatcher.addElement(
                                createEnvVariableLookupElement(it)
                            )
                        }
                    } else if (prefixText.endsWith("\$L")) {
                        val prefixMatcher = result.withPrefixMatcher("\$L")
                        BUILTIN_ENV_VARIABLES.forEach {
                            prefixMatcher.addElement(
                                createEnvVariableLookupElement(it)
                            )
                        }
                    }
                }
            }
        )
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val document = context.editor.document

        // Ensure `$` is treated as a valid trigger for completion
        if (offset > 0 && document.charsSequence[offset - 1] == '$') {
            context.dummyIdentifier = "$" // This forces completion when `$` is typed
        }
    }
}


private fun createEnvVariableLookupElement(name: String): LookupElementBuilder {
    return LookupElementBuilder.create("\${$name}")
        .withPresentableText(name)
        .withIcon(Icons.LetsYaml)
}