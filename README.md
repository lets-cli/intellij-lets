# intellij-lets

![Build](https://github.com/lets-cli/intellij-lets/workflows/Build/badge.svg)

<!-- Plugin description -->
This IntelliJ plugin provides support for https://github.com/lets-cli/lets task runner.

[Plugin](https://plugins.jetbrains.com/plugin/14639-lets) on JetBrains Marketplace

## Supports

File type recognition for `lets.yaml` and `lets.*.yaml` configs

- **Completion**
  - [x] Complete command `options` with code snippet
  - [x] Complete commands in `depends` with code snippet
  - [ ] Complete commands in `depends` from mixins
  - [ ] Complete env mode in `env` with code snippet
  - [x] Complete `LETS*` environment variables in cmd scripts
  - [ ] Complete environment variables for checksum
- **Go To Definition**
  - [x] Navigate to definitions of `mixins` files
  - [x] Navigate to definitions of optional `mixins` files (with - at the beginning)
  - [x] Navigate to definitions of `mixins` remote files (as http links)
  - [x] Navigate to definitions of commands in `depends`
  - [ ] Navigate to definitions of commands in `depends` from mixins
  - [ ] Navigate to files in `checksum`
- **Highlighting**
  - [x] Highlighting for shell script in `cmd`
  - [ ] Highlighting for shell script in `before`
  - [ ] Highlighting for shell script in `init`
- **Diagnostic**
  - [ ] Diagnostic for missing `depends` commands
  - [ ] Diagnostic for missing `mixins` files
  - [ ] Diagnostic for errors in shell script

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

### Changelog

We are using [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format for changelog. This is the reason we 
have `[Unreleased]` section in `CHANGELOG.md`. We must keep it up to date with every change we make. After release,
CI automatically moves all changes from `[Unreleased]` to new section with version number.

## IDE Breaking change upgrades

### Minimal IDE version

If plugin needs to use some new feature from IDE, we need to update `pluginSinceBuild` in `gradle.properties` to new IDE version.
When new IDE version is released we need to release new `lets` plugin version.

### Maximum IDE version

We do not restrict maximum IDE version, so plugin should work with all future versions of IDE.

If our plugin happens to be incompatible with future version of IDE because of some backward-incompatible changes, we have two options:

1. Fix incompatibility in plugin code in a way that still keeps support for all versions specified by `pluginSinceBuild` and all future versions.
2. Set `pluginUntilBuild` in `gradle.properties` to new IDE version and release new plugin version. Use this option only if there is no other way to fix incompatibility.
 
> We never set `pluginUntilBuild` in `gradle.properties` to new IDE version, because if we do so,
this will force us to release new version of plugin every time new IDE version is released.

### How to upgrade supported IDE version

1. Open `gradle.properties`
  - maybe change `pluginSinceBuild` to new ide version
  - maybe change `platformVersion`
  - maybe change `pluginVerifierIdeVersions`
  - increment `pluginVersion`
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

- insert not padded strings, but yaml elements