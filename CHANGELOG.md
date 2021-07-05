<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# intellij-lets Changelog

## [Unreleased]
## [0.0.7]
### Added
- add some default completions for `shell`
- enhance completions for yaml keys (add :, new line, multiline)
- add more tests
### Updated
- drop support for 193 IDE version
- refactor completion provider
- refactor config, parse gracefully

## [0.0.6]
### Updated
- updated gradle config and wrapper
- use new kotlin runtime version 11
- support 211 IDE version

## [0.0.5]
### Added
- completion for depends directive

## [0.0.4]
### Added
- specified idea-version
### Changed
- delete deprecated

## [0.0.3]
### Added
- pluginIcon svg
### Changed
- replace png filetype icon with svg

## [0.0.2]
### Added
- completion for command level fields

## [0.0.1]
### Added
- file type recognition - `lets.yaml` and `lets.*.yaml` files will be recognized as lets configs
- basic completion - for now it will autocomplete only top level keywords
