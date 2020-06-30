package com.github.kindermax.intellijlets

import com.intellij.openapi.fileTypes.LanguageFileType

object LetsFileType : LanguageFileType(LetsLanguage) {
    val DEFAULT_EXTESION: String = "yaml"

    override fun getIcon() = Icons.LetsYaml

    override fun getName() = "lets"

    override fun getDefaultExtension() = DEFAULT_EXTESION

    override fun getDescription() = "Lets yaml config"

    override fun toString() = name
}