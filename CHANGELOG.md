<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ghactions-toolbar Changelog

## [Unreleased]
## [1.1.1] - 2022-08-25
### Fixed
- Bug serializing status of job
- Viewing job logs while it is in progress #34
### Changed
- Gradle version building project
- Cleanup code

## [1.1.0] - 2022-08-25
### Added
- Jobs panel view

### Fixed
- Allow empty conclusion, support in progress json - fix #30) by @cunla in #32
- Multiple instances bug by @cunla in #31


## [1.0.1]
### Added
- Support for IntelliJ 2022.2
### Fixed
- Issue with `GithubApiRequestExecutorManager.getExecutor`
 
**Full Changelog**: https://github.com/cunla/ghactions-toolbar/compare/v0.0.8...v1.0.1


## [0.0.8] - 2022-06-26
### Fixed
- Issue with `GithubApiRequestExecutorManager.getExecutor`

**Full Changelog**: https://github.com/cunla/ghactions-toolbar/compare/v0.0.7...v0.0.8


## [0.0.7] - 2022-06-26
### Added
* Add a link to GitHub accounts settings in case GitHub account is not set #19
* Add a link from toolbar window to Toolbar Settings #21
* Toolbar settings - Resolve #18 by @cunla in https://github.com/cunla/ghactions-toolbar/pull/25

### Fixed
* Fix memory leak issue #22

**Full Changelog**: https://github.com/cunla/ghactions-toolbar/compare/v0.0.6...v0.0.7

## [0.0.6] - 2022-06-19
### Added
- Contribution guide.
- Documentation: Screenshots on README, contribution guide, etc.
- Message when there is no github account configured.
- Message when there is no repository in the project.

### Changed
- Improved code structure

Full Changelog: https://github.com/cunla/github-actions-jetbrains-plugin/commits/v0.0.6

## [0.0.5] - 2022-06-14
### Added
- Plugin Icon
- Add tab per repo
- Add log folding
- Remove redundant code and deprecated code
- Migrated code from https://github.com/Otanikotani/view-github-actions-idea-plugin
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
  
Full Changelog: https://github.com/cunla/github-actions-jetbrains-plugin/commits/v0.0.5


<!--
## [Unreleased]
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
-->