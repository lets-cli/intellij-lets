package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLFile

object LetsCompletionProvider : CompletionProvider<CompletionParameters>() {
    @Suppress("ComplexMethod")
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        when (LetsCompletionHelper.detectContext(parameters.position)) {
            LetsCompletionHelper.YamlContextType.RootLevel -> {
                val yamlFile = parameters.originalFile as YAMLFile
                val usedKeywords = LetsPsiUtils.getUsedKeywords(yamlFile)
                val suggestions = when (usedKeywords.size) {
                    0 -> TOP_LEVEL_KEYWORDS
                    else -> TOP_LEVEL_KEYWORDS.filterNot { usedKeywords.contains(it) }.toList()
                }
                result.addAllElements(
                    suggestions.map { keyword ->
                        when (keyword) {
                            "commands", "env" -> createRootKeyNewLineElement(keyword)
                            "before", "init" -> createRootKeyMultilineElement(keyword)
                            "mixins" -> createRootKeyArrayElement(keyword)
                            else -> createRootKeyElement(keyword)
                        }
                    }
                )
            }
            LetsCompletionHelper.YamlContextType.ShellLevel -> {
                result.addAllElements(DEFAULT_SHELLS.map { keyword -> createLookupElement(keyword) })
            }
            LetsCompletionHelper.YamlContextType.CommandLevel -> {
                result.addAllElements(
                    COMMAND_LEVEL_KEYWORDS.map { keyword ->
                        when (keyword) {
                            "options" -> createOptionsElement()
                            "depends" -> createDependsElement()
                            "env" -> createCommandKeyNewLineElement(keyword)
                            else -> createCommandKeyElement(keyword)
                        }
                    }
                )
            }
            LetsCompletionHelper.YamlContextType.DependsLevel -> {
                val suggestions = LetsCompletionHelper.getDependsSuggestions(parameters)
                result.addAllElements(
                    suggestions.map { keyword -> createLookupElement(keyword) }
                )
            }
            LetsCompletionHelper.YamlContextType.RefLevel -> {
                val suggestions = LetsCompletionHelper.getRefSuggestions(parameters)
                result.addAllElements(
                    suggestions.map { keyword -> createLookupElement(keyword) }
                )
            }
            LetsCompletionHelper.YamlContextType.Unknown -> return
        }
    }
}

private fun createLookupElement(text: String): LookupElement {
    return LookupElementBuilder
        .create(text)
        .withIcon(Icons.LetsYaml)
}

private fun createRootKeyElement(text: String): LookupElement {
    return LookupElementBuilder
        .create(text)
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(RootKeyInsertionHandler())
}

private fun createRootKeyNewLineElement(text: String): LookupElement {
    return LookupElementBuilder
        .create(text)
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(RootKeyInsertionHandler(newLine = true))
}

private fun createRootKeyMultilineElement(text: String): LookupElement {
    return LookupElementBuilder
        .create(text)
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(RootKeyInsertionHandler(multiline = true))
}

private fun createRootKeyArrayElement(text: String): LookupElement {
    return LookupElementBuilder
        .create(text)
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(RootKeyInsertionHandler(asArray = true))
}

private fun createCommandKeyElement(text: String): LookupElement {
    return LookupElementBuilder
        .create(text)
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(CommandKeyInsertionHandler())
}

private fun createCommandKeyNewLineElement(text: String): LookupElement {
    return LookupElementBuilder
        .create(text)
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(CommandKeyInsertionHandler(newLine = true))
}

private fun createOptionsElement(): LookupElement {
    return LookupElementBuilder
        .create("options")
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(OptionsInsertionHandler())
}

private fun createDependsElement(): LookupElement {
    return LookupElementBuilder
        .create("depends")
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(DependsInsertionHandler())
}

private class OptionsInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val padding = "".padStart(COMMAND_CHILD_PADDING)
        context.document.insertString(context.selectionEndOffset, ": |\n${padding}Usage: lets ")
        context.editor.caretModel.moveToOffset(context.selectionEndOffset)
    }
}

private class DependsInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val padding = "".padStart(COMMAND_CHILD_PADDING)
        context.document.insertString(context.selectionEndOffset, ":\n$padding- ")
        context.editor.caretModel.moveToOffset(context.selectionEndOffset)
    }
}

private class RootKeyInsertionHandler(
    val newLine: Boolean = false,
    val multiline: Boolean = false,
    val asArray: Boolean = false
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val padding = "".padStart(ROOT_PADDING)

        var tmpl = ": "
        if (newLine) {
            tmpl = ":\n$padding"
        }
        if (multiline) {
            tmpl = ": |\n$padding"
        }
        if (asArray) {
            tmpl = ":\n$padding- "
        }
        context.document.insertString(context.selectionEndOffset, tmpl)
        context.editor.caretModel.moveToOffset(context.selectionEndOffset)
    }
}

private class CommandKeyInsertionHandler(
    val newLine: Boolean = false,
    val multiline: Boolean = false
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val padding = "".padStart(COMMAND_CHILD_PADDING)

        var tmpl = ": "
        if (newLine) {
            tmpl = ":\n$padding"
        }
        if (multiline) {
            tmpl = ": |\n$padding"
        }
        context.document.insertString(context.selectionEndOffset, tmpl)
        context.editor.caretModel.moveToOffset(context.selectionEndOffset)
    }
}
