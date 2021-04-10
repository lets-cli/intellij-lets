# intellij-lets

![Build](https://github.com/lets-cli/intellij-lets/workflows/Build/badge.svg)

<!-- Plugin description -->
This IntelliJ plugin provides support for https://github.com/lets-cli/lets task runner.

Is supports:

- file type recognition for `lets.yaml` and `lets.*.yaml` configs
- autocomplete for config

<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "lets"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/lets-cli/intellij-lets/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).

## Releases

Releases flow:

- add new features to CHANGELOG.md to `[Unreleased]` section 
- when ready to update version, go to `gradle.properties` and bump `pluginVersion`
- merge to master
- gitlab action will create a draft release with:
    - version from `gradle.properties`
    - changelog from `[Unreleased]` section
- when ready - publish draft release
- add new section to CHANGELOG.md with new published version

## Linting

Project uses several tools to maintain code quality
- https://ktlint.github.io/
- https://detekt.github.io

See https://detekt.github.io/detekt/ Rule Sets to get more info about `detekt` failed rules.
