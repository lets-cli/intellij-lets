<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# intellij-lets Changelog

## [Unreleased]

### Added

- Add reference for commands in `ref`
- Add completion for commands in `ref`
- Add completion for env variables in `cmd` scripts from global and command `env`

### Internal

- Refactor completions in a way to use LetsPsiUtils
- Drop `Config`, refactor into `ConfigParser
- Drop `LetsCompletionHelper`
- Introduce `YamlContextType` enum instead of boolean functions for determining context type (position type)

## [0.0.17] - 2025-03-08

### Added

- inject `Shell Script` language in `cmd` scripts
- resolve commands in `depends` including mixins
- complete Lets builtin env variables inside `cmd` scripts
- complete `LETSOPT_` and `LETSCLI_` env variables inside `cmd` scripts if `options` docopt is present

### Updated

- make go to definition work for gitignore `mixins` files (with '-' (dash) at the beginning)
- support global `init` in config parser and completion
- support `sh` and `checksum` modes in `env` in config parser
- support `ref` and `args` in command keyword completion
- support `mixins` (both local and remote) in config parser
- drop `eval_env`

### Chore

- add tests for reference contributor

## [0.0.16] - 2025-03-07

### Updated

- make go to definition work for `mixins` files inside directories

## [0.0.15] - 2025-03-06

### Updated

- disable `pluginUntilBuild` in `build.gradle.kts` to support all future versions of IDEs

## [0.0.14] - 2025-03-06

### Added

- add reference contributor for `mixins` files

### Updated

- actually make plugin support all future versions of IDEs

## [0.0.13]

### Updated

- make plugin support all future versions of IDEs (remove max version restriction)
- support 2024 IDE version
- drop support for 2023.*

## [0.0.11]

- support 221 IDE version

## [0.0.9]

- support 213 IDE version

## [0.0.8]

### Updated

- support 212 IDE version

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

[Unreleased]: https://github.com/lets-cli/intellij-lets/compare/v0.0.17...HEAD
[0.0.17]: https://github.com/lets-cli/intellij-lets/compare/v0.0.16...v0.0.17
[0.0.16]: https://github.com/lets-cli/intellij-lets/compare/v0.0.15...v0.0.16
[0.0.15]: https://github.com/lets-cli/intellij-lets/compare/v0.0.14...v0.0.15
[0.0.14]: https://github.com/lets-cli/intellij-lets/compare/v0.0.13...v0.0.14
[0.0.13]: https://github.com/lets-cli/intellij-lets/compare/v0.0.11...v0.0.13
[0.0.11]: https://github.com/lets-cli/intellij-lets/compare/v0.0.9...v0.0.11
[0.0.9]: https://github.com/lets-cli/intellij-lets/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/lets-cli/intellij-lets/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/lets-cli/intellij-lets/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/lets-cli/intellij-lets/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/lets-cli/intellij-lets/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/lets-cli/intellij-lets/compare/v0.0.3...v0.0.4
[0.0.3]: https://github.com/lets-cli/intellij-lets/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/lets-cli/intellij-lets/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/lets-cli/intellij-lets/commits/v0.0.1
