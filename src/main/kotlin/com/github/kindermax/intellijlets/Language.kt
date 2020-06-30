package com.github.kindermax.intellijlets

import com.intellij.lang.Language

object LetsLanguage : Language("lets") {
    override fun isCaseSensitive() = true
    override fun getDisplayName() = "Lets yaml config"
}