# intellij-lets

![Build](https://github.com/lets-cli/intellij-lets/workflows/Build/badge.svg)

<!-- Plugin description -->
This IntelliJ plugin provides support for https://github.com/lets-cli/lets task runner.

[Plugin](https://plugins.jetbrains.com/plugin/14639-lets) on JetBrains Marketplace

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

## Issue new IDE compatible version

When new IDE version is released we need to release new `lets` plugin version.

1. Open `gradle.properties`
  - change `pluginUntilBuild` to new ide version (for example from `211.*` to `212.*`)
  - increment `pluginVersion`
  - maybe change `platformVersion`
  - maybe change `pluginVerifierIdeVersions`
2. Open `CHANGELOG.md`
  - Add info to `Unreleased` section.
3. Create new branch, merge into main.
4. GitHub will run tests and verification on master, and create new draft tag with version from `pluginVersion`
5. Open new draft tag and publish it.
6. GitHub will run an action for that published tag and:
  - will publish lets plugin to JetBrains Marketplace
  - update CHANGELOG.md

## Linting

Project uses several tools to maintain code quality
- https://ktlint.github.io/
- https://detekt.github.io

See https://detekt.github.io/detekt/ Rule Sets to get more info about `detekt` failed rules.

## Todo

- add highlighting for shell script in cmd - https://plugins.jetbrains.com/docs/intellij/file-view-providers.html
- read mixins - https://plugins.jetbrains.com/docs/intellij/psi-cookbook.html#how-do-i-find-a-file-if-i-know-its-name-but-don-t-know-the-path
- insert not padded strings, but yaml elements