package com.github.kindermax.intellijlets

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.yaml.YAMLLanguage

object LetsFileType : LanguageFileType(YAMLLanguage.INSTANCE) {
    val DEFAULT_EXTESION: String = "yaml"

    override fun getIcon() = Icons.LetsYaml

    override fun getName() = "lets"

    override fun getDefaultExtension() = DEFAULT_EXTESION

    override fun getDescription() = "Lets YAML"

    override fun toString() = name
}