<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ghactions-manager Changelog

## [Unreleased]
## [1.5.4]
### Added
* Open workflow file action
### Fixed
* Update workflow run state


## [1.5.1]
### Fixed
* Running refresh in background

## [1.5.0]
### Added
* Ability to change how often refresh of workflow runs is done
* Refresh jobs of workflow run if still in progress
### Changed
* Allowing job log to be beneath jobs list - configurable in plugin settings
### Fixed
* Showing logs of selected job once logs are loaded

## [1.4.0]
### Added
* Refresh workflow runs status in the background


## [1.3.0] - 2022-10-07
### Added
* Refresh jobs + rerun workflow run by @cunla in https://github.com/cunla/ghactions-manager/pull/40
* Update cancelled icon by @cunla in https://github.com/cunla/ghactions-manager/pull/44
* Add cancel workflow action #43 by @cunla in https://github.com/cunla/ghactions-manager/pull/46
* Guess github account per repo when there are multiple GitHub accounts by @cunla in https://github.com/cunla/ghactions-manager/pull/48

**Full Changelog**: https://github.com/cunla/ghactions-manager/compare/v1.2.0...v1.3.0

## [1.2.0] - 2022-10-01
### Added
- New icons for in progress/queued workflows
- Ability to configure tab name for each repo (Fix #38)
### Fixed
- Exception when workflow is in progress #35
### Changed
- Logs less verbose

## [1.1.1] - 2022-08-27
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
 
**Full Changelog**: https://github.com/cunla/ghactions-manager/compare/v0.0.8...v1.0.1


## [0.0.8] - 2022-06-26
### Fixed
- Issue with `GithubApiRequestExecutorManager.getExecutor`

**Full Changelog**: https://github.com/cunla/ghactions-manager/compare/v0.0.7...v0.0.8


## [0.0.7] - 2022-06-26
### Added
* Add a link to GitHub accounts settings in case GitHub account is not set #19
* Add a link from toolbar window to Toolbar Settings #21
* Toolbar settings - Resolve #18 by @cunla in https://github.com/cunla/ghactions-manager/pull/25

### Fixed
* Fix memory leak issue #22

**Full Changelog**: https://github.com/cunla/ghactions-manager/compare/v0.0.6...v0.0.7

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