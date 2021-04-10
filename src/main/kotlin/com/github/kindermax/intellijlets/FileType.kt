package com.github.kindermax.intellijlets

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.yaml.YAMLLanguage

object FileType : LanguageFileType(YAMLLanguage.INSTANCE) {
    const val DEFAULT_EXTENSION: String = "yaml"

    override fun getIcon() = Icons.LetsYaml

    override fun getName() = "lets"

    override fun getDefaultExtension() = DEFAULT_EXTENSION

    override fun getDescription() = "Lets YAML"

    override fun toString() = name
}
