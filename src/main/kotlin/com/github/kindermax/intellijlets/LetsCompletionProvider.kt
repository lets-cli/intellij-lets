package com.github.kindermax.intellijlets

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLFile

object LetsCompletionProvider : CompletionProvider<CompletionParameters>() {
    @Suppress("ComplexMethod")
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        when (LetsPsiUtils.detectContext(parameters.position)) {
            YamlContextType.RootLevel -> {
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

            YamlContextType.ShellLevel -> {
                result.addAllElements(DEFAULT_SHELLS.map { keyword -> createLookupElement(keyword) })
            }

            YamlContextType.CommandLevel -> {
                val currentCommand = LetsPsiUtils.findCurrentCommand(parameters.position, YamlContextType.CommandLevel)
                result.addAllElements(
                    COMMAND_LEVEL_KEYWORDS.map { keyword ->
                        when (keyword) {
                            "options" -> createOptionsElement(currentCommand?.name)
                            "depends" -> createDependsElement()
                            "env" -> createCommandKeyNewLineElement(keyword)
                            else -> createCommandKeyElement(keyword)
                        }
                    }
                )
            }

            YamlContextType.DependsLevel -> {
                val suggestions = getDependsSuggestions(parameters)
                result.addAllElements(
                    suggestions.map { keyword -> createLookupElement(keyword) }
                )
            }

            YamlContextType.RefLevel -> {
                val suggestions = getRefSuggestions(parameters)
                result.addAllElements(
                    suggestions.map { keyword -> createLookupElement(keyword) }
                )
            }

            YamlContextType.EnvLevel -> {
                result.addAllElements(
                    listOf(
                        createEnvStringElement(),
                        createEnvShellElement()
                    )
                )
            }

            YamlContextType.Unknown -> return
        }
    }

    /**
     * Get all possible commands suggestions for a `depends`, except:
     * - itself
     * - already specified commands in depends
     * - other commands which depend on current command
     */
    private fun getDependsSuggestions(parameters: CompletionParameters): List<String> {
        val yamlFile = parameters.originalFile as? YAMLFile ?: return emptyList()
        val allCommands = LetsPsiUtils.findAllCommands(yamlFile)
        val currentCommand = LetsPsiUtils.findCurrentCommand(parameters.position) ?: return emptyList()

        val excludeList = mutableSetOf<String>()
        // exclude itself
        excludeList.add(currentCommand.name)
        // exclude commands already in depends list
        excludeList.addAll(currentCommand.depends)

        // exclude commands which depends on current command (eliminate recursive dependencies)
        for (command in allCommands.filter { c -> c.name != currentCommand.name }) {
            if (command.depends.contains(currentCommand.name)) {
                excludeList.add(command.name)
            }
        }

        return allCommands
            .filterNot { command -> excludeList.contains(command.name) }
            .map { it.name }
            .toList()
    }

    /**
     * Get all possible commands suggestions for a `ref`, except:
     * - itself
     * Since ref is a YAMLScalar, only one command is suggested.
     */
    private fun getRefSuggestions(parameters: CompletionParameters): List<String> {
        val yamlFile = parameters.originalFile as? YAMLFile ?: return emptyList()
        val allCommands = LetsPsiUtils.findAllCommands(yamlFile)
        val currentCommand = LetsPsiUtils.findCurrentCommand(parameters.position) ?: return emptyList()
        // Exclude itself from suggestions and return only one suggestion
        return allCommands.filterNot { it.name == currentCommand.name }
            .map { it.name }
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

private fun createOptionsElement(name: String?): LookupElement {
    return LookupElementBuilder
        .create("options")
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(OptionsInsertionHandler(name))
}

private fun createDependsElement(): LookupElement {
    return LookupElementBuilder
        .create("depends")
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(DependsInsertionHandler())
}

private fun createEnvStringElement(): LookupElement {
    return LookupElementBuilder
        .create("")
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(EnvStringInsertionHandler())
        .withPresentableText("KEY: VALUE (Simple key-value pair)")
}

private fun createEnvShellElement(): LookupElement {
    return LookupElementBuilder
        .create("")
        .withIcon(Icons.LetsYaml)
        .withInsertHandler(EnvShellInsertionHandler())
        .withPresentableText("KEY: sh (Shell script value)")
}

/**
 * Creates template for environment variables with the following structure:
 * <KEY>: <VALUE>
 * User must replace `KEY` and `VALUE` with actual values.
 */
private class EnvStringInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val project = context.project
        val editor = context.editor

        // Create a live template
        val manager = TemplateManager.getInstance(project)
        val template = manager.createTemplate("", "")
        template.isToReformat = true

        // Add placeholders
        template.addTextSegment("")
        template.addVariable("KEY", TextExpression("ENV_KEY"), true)
        template.addTextSegment(": ")
        template.addVariable("VALUE", TextExpression("ENV_VALUE"), true)

        // Start the template
        manager.startTemplate(editor, template)
    }
}

/**
 * Creates template for environment variables in shell mode with the following structure:
 * <KEY>:
 *   sh: <caret>
 *
 * User must replace `KEY` with actual value and write shell script in the next line.
 */
private class EnvShellInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val padding = "".padStart(4)

        val project = context.project
        val editor = context.editor

        // Create a live template
        val manager = TemplateManager.getInstance(project)
        val template = manager.createTemplate("", "")
        template.isToReformat = true

        // Add placeholders
        template.addTextSegment("")
        template.addVariable("KEY", TextExpression("ENV_KEY"), true)
        template.addTextSegment(":\n${padding}sh: ")

        // Start the template
        manager.startTemplate(editor, template)
    }
}

private class OptionsInsertionHandler(name: String?) : InsertHandler<LookupElement> {
    val name = name ?: ""

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val padding = "".padStart(COMMAND_CHILD_PADDING)
        context.document.insertString(context.selectionEndOffset, ": |\n${padding}Usage: lets $name")
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
