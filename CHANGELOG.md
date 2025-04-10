<!-- Keep a Changelog guide -> https://keepachangelog.com -->
<!--

### 🚀 Features

### 🐛 Bug Fixes

### 🧰 Maintenance

### Improvements

-->

# Changelog

All notable changes to this project will be documented in this file.

## Unreleased

## 2025.1.0 - 2025-04-10

### 🐛 Bug Fixes

- Fix minor bugs using null checks

### 🧰 Maintenance

- Reverted kotlin serialization to 1.7.3
- Updated dependencies

## 2024.3.5 - 2025-01-20

### 🧰 Maintenance

- Update kotlin serialization to 1.8.0
- Remove usage of API marked internal in 2025.1 EAP

## 2024.3.4 - 2025-01-02

### 🐛 Bug Fixes

- Fix issue with updating log panel out of EDT #201
- Fix issue when workflow is not in `.github/workflows` #200

## 2024.3.3 - 2024-12-20

### 🐛 Bug Fixes

- Fixed ability to configure default filter for workflow runs #198

### 🧰 Maintenance

- Update to latest intellij platform gradle plugin

## 2024.3.2 - 2024-12-09

### 🚀 Features

- Added ability to configure default filter for workflow runs #198

### 🐛 Bug Fixes

- Issue getting actions info from different sources requiring different GitHub accounts #197

## 2024.3.1 - 2024-12-04

### 🧰 Maintenance

- Support for build 243.21565.180

## 2024.3.0 - 2024-12-04

### 🚀 Features

- Ability to control which GitHub account is used for which repository in the project using the plugin settings. #193

### 🧰 Maintenance

- Remove usage of deprecated API.

## 2024.2.14 - 2024-12-01

### 🐛 Bug Fixes

- Disable `Open workflow file` action when workflow does not have a file in the repository.
- Fix creating scope under disposed parent #184/#190
- Improve the way action info is searched - scanning in different server paths #191
- Annotate properly reusable workflows in the same repo (`./.github/workflows/...`) and not only remote workflows #192

## 2024.2.13 - 2024-11-01

### 🐛 Bug Fixes

- Fix creating scope under disposed parent #183

### 🧰 Maintenance

- Remove usage of deprecated API
- Update dependencies used

## 2024.2.12 - 2024-10-01

### 🐛 Bug Fixes

- Support for version 2024.3-EAP
- Fixed using deprecated API

## 2024.2.11 - 2024-09-25

### 🐛 Bug Fixes

- Showing up to 100 jobs per workflow run #181
- Fix NPE when repo-context is refreshed #182

## 2024.2.10 - 2024-09-24

### 🚀 Features

- Add ability to reject pending deployments for workflow-runs in waiting for deployment status

### 🐛 Bug Fixes

- Fixed error reporter to send relevant part of the stack trace

## 2024.2.9 - 2024-09-20

### 🚀 Features

- Add ability to review deployments for workflow-runs in waiting for deployment status #178
- Add ability to sort jobs by showing in-progress jobs first

### Improvements

- Refactored using API scheduled for removal.

## 2024.2.8 - 2024-08-31

### 🚀 Features

- Add missing statuses to workflow runs filters #178

## 2024.2.7 - 2024-08-15

### 🚀 Features

- Dialog to enter workflow inputs when triggering a workflow run.
- View org/repository secrets.
- Manage (view/edit/delete) org/repository variables.

### Improvements

- Java 21 implementation.
- Significant performance improvements.
- Notifications on API errors.

### 🐛 Bug Fixes

- Workflow dispatcher: Mark currently workflows that can be manually dispatched.
- Disposing LogConsolePanelWrapper before LogConsolePanel components #177

## 2024.2.6 - 2024-07-21

### 🐛 Bug Fixes

- Workflow dispatcher: Disable workflow types that can not be dispatched manually #172

## 2024.2.5 - 2024-07-15

### 🐛 Bug Fixes

- Workflow dispatcher: Disable workflow types that can not be dispatched manually #172
- Prevent API abuse when action with complex name fails resolving #173

## 2024.2.4 - 2024-07-08

### 🐛 Bug Fixes

- Fix issue with RepositoryContext background task not cancelled properly #171

## 2024.2.3 - 2024-06-30

### Improvements

- Migrated to view-model for jobs-panel + logs-panel - significant performance improvement.
- Improved error handling for GitHub API requests.

### 🐛 Bug Fixes

- Issue with adding tab to disposed tool-window #166
- Issue showing filter when there is no current user #167

## 2024.2.2 - 2024-06-15

### 🐛 Bug Fixes

- Fix issue with blocked user-agent #164

## 2024.2.1 - 2024-06-06

### 🐛 Bug Fixes

- Fix saving settings issue

## 2024.2.0 - 2024-06-01

### 🚀 Features

- (Paid feature) Open pull request from selected workflow run (when pull-request is open)

### 🐛 Bug Fixes

- Support for reusable workflows in workflow files
- Showing download artifacts button only when relevant

### Improvements

- Using kotlin 2.0, IntelliJ Gradle Plugin v2, java 21

## 2024.1.3 - 2024-05-27

### 🐛 Bug Fixes

- Fix crash when opening a GH actions file #160
- Fix marking unknown actions when GitHub settings are not set

## 2024.1.2 - 2024-05-15

### 🐛 Bug Fixes

- Using concurrent set to prevent concurrent modification exception #154

## 2024.1.1 - 2024-05-11

### 🚀 Features

- Add ability to download artifacts from jobs panel (Paid feature)

### Improvements

- Highlighting action names only in the `uses` field
- Update actions cache when GitHub settings changes.

### 🧰 Maintenance

- Update to latest dependencies

## 1.22.0 - 2024-05-15

### 🚀 Features

- Highlighting unknown actions in workflow files
- Clicking on action names:
  - opens the browser to the action's GitHub page for remote actions
  - opens the action file for local actions

### 🐛 Bug Fixes

- Fix identifying composite action files
- Fix opening file in root directory #142
- Fix bug with concurrent adding/removing actions to resolve #150

### 🧰 Maintenance

- Update reporting issues to GitHub

## 1.21.6 - 2024-04-27

### 🐛 Bug Fixes

- Fix bug failing to load annotator #131
- Fix scanning actions in composite action files

### 🧰 Maintenance

- Implement tests for startup scanning
- Implement tests for highlighting and quickfixes of outdated actions

## 1.21.5 - 2024-04-24

### 🐛 Bug Fixes

- Fix bug of jobs not being refreshed while workflow is running

## 1.21.4 - 2024-04-23

### 🐛 Bug Fixes

- Fix memory leak with log panel
- Workflow runs list is getting updated with the latest status

## 1.21.3 - 2024-04-19

### 🐛 Bug Fixes

- Fix bug loading null value to cache #128
- Show go to GitHub settings when error occurs
- Show reload jobs button

## 1.21.2 - 2024-04-18

### 🐛 Bug Fixes

- Fix bug scanning workflow files #126 #125

## 1.21.1 - 2024-04-17

### 🚀 Features

- Highlight outdated actions in workflow files. #122
- Quick fix action to update to the latest major version of the action. #122
- Error reporting directly to GitHub issues.

### 🐛 Bug Fixes

- Storing action data in project settings instead of a separate file.

### 🧰 Maintenance

- Update bug-report template.

## 1.20.0 - 2024-04-06

### 🚀 Features

- Tooltip on tab showing real repo and GitHub account
- Prefer GitHub account that is the owner of the repository

### 🐛 Bug Fixes

- Using new error handlers.
- Create GitHub Request executor that supports redirects. # 119

### 🧰 Maintenance

- Update deprecated code.

## 1.19.0 - 2024-04-04

- Update license to GPL-3.0

### 🚀 Features

- Support for JetBrains IDEs v2024.1

### 🧰 Maintenance

- Improve tests and increase coverage
- Update dependencies to latest versions

## 1.18.0 - 2024-03-12

### 🚀 Features

- Add quick-filter to show runs based on the current branch (updates when branch is updated) #115
- Add ability to position workflow runs list on top of jobs list #116
- Show link to step log when log is too large #118

### 🧰 Maintenance

- Major refactoring better performance and code quality

### 🐛 Bug Fixes

- Fix not allowing custom repositories on plugin settings

## 1.17.0 - 2024-02-14

### 🚀 Features

- Extract all messages to i18n file #114
- Update info bar on jobs-panel

### 🐛 Bug Fixes

- Minor bug when unable to parse Instant

### 🧰 Maintenance

- Update dependencies to latest versions
- Major code refactoring
- Implement tests

## 1.16.1 - 2024-02-14

### 🐛 Bug Fixes

- Fix job log parsing dates #109

## 1.16.0 - 2024-02-14

### 🚀 Features

- Downloading job logs instead of entire run logs #108

## 1.15.2 - 2023-12-04

### 🐛 Bug Fixes

- Fix run in EDT thread #106

## 1.15.1

### 🧰 Maintenance

- Update dependencies to latest versions

## 1.15.0

### 🚀 Features

- Trigger workflow dispatch event #75

### 🐛 Bug Fixes

- Using camel-case variable names in POJOs #99
- WfRunsListPersistentSearchHistory state is not being saved properly #102
- Fix updating logs from non-EDT #101

## 1.14.0

### 🚀 Features

- Filter by workflow type #98

### 🧰 Maintenance

- Improve filter behavior

## 1.13.5

### 🧰 Maintenance

- Add support for build 223.3 and fixed a few warnings. @wyatt-herkamp #97

## 1.13.4

### 🐛 Bug Fixes

- Fix exception thrown due to EDT thread #95

## 1.13.3

### 🐛 Bug Fixes

- Limiting requests to contributors and branches.

## 1.13.2

### 🐛 Bug Fixes

- Fix refreshing of jobs list.
- Updated dependencies

## 1.13.1

### 🐛 Bug Fixes

- Fix time shown for workflow runs and jobs.

## 1.13.0

### 🚀 Features

- Add workflow runs filter based on event.

### 🐛 Bug Fixes

- Filters now work for future IDE versions.

## 1.12.2

### 🐛 Bug Fixes

- Showing all branches and all collaborators on filters #92

## 1.12.1

### 🐛 Bug Fixes

- Fix times for workflow-runs #91
- Showing right status for jobs that haven't started
- Improved filters behavior

## 1.12.0

### 🚀 Features

- Simplified Jobs panel logic
- Add ability to filter workflow runs by actor, branch and status.

## 1.11.0

### 🚀 Features

- Add info bar with number of jobs loaded

### 🐛 Bug Fixes

- Fix bug showing seconds without padding #88
- Fix jobs request pagination, now pagesize=100 #89
- Fix updating job logs during theme change #85

## 1.10.4

### 🐛 Bug Fixes

- Fix bug when step does not have logs #83
- Support for 2023.2-EAP

## 1.10.3

### 🐛 Bug Fixes

- Fix icons for new UI look

## 1.10.2

### 🐛 Bug Fixes

- Fix error when using IntelliJ 2023.1.RC #83
- Using GHAccountManager instead of deprecated GithubAuthenticationManager

## 1.10.1

### 🐛 Bug Fixes

- Fix bug requiring to pick job after logs are loaded.

## 1.10.0

### 🚀 Features

- Add ability to configure number of workflow runs on list.
- Add ability to configure GitHub token instead of using IDE GitHub settings.

### Changed

- Support 2023.1 EAP release

## 1.9.2

### Changed

- Fix bugs refreshing workflow runs #81
- Fix bugs calling getComponent from non dispatch thread #78
- Add icon for action_required conclusion.

## 1.9.1

### Changed

- Upgrade gradle to `7.6`
- Upgrade `org.jetbrains.kotlin.jvm` from 1.7.22 to 1.8.0

## 1.9.0

### Changed

- Clean up code on `GhActionsManagerConfigurable` by @cunla in #71
- LogPanel wrap by @cunla in #72

## 1.7.0

### 🚀 Features

- Make ghactions-manager available during indexing (#66)
- Sorting jobs by completed date, or started date, else run id (#61)

### 🐛 Bug Fixes

- Fix deadlock when refreshing workflow runs (#64)

## 1.6.1

### Fixed

- Link to pull-request
- Reset log when workflow-run unselected
- Keep workflow-run selected after refresh

## 1.6.0

### Added

- Step logs - Showing failed step title in red
- Refresh of runs only for active tab

### Changed

- Better github REST API error handling

### Fixed

- Update jobs panel and log panel to loading state when a new run is selected
- Clean up more code

## 1.5.4

### Added

- Open workflow file action

### Fixed

- Update workflow run state

## 1.5.1

### Fixed

- Running refresh in background

## 1.5.0

### Added

- Ability to change how often refresh of workflow runs is done
- Refresh jobs of workflow run if still in progress

### Changed

- Allowing job log to be beneath jobs list - configurable in plugin settings

### Fixed

- Showing logs of selected job once logs are loaded

## 1.4.0

### Added

- Refresh workflow runs status in the background

## 1.3.0 - 2022-10-07

### Added

- Refresh jobs + rerun workflow run by @cunla in #40
- Update cancelled icon by @cunla in #44
- Add cancel workflow action #43 by @cunla in #46
- Guess GitHub account per repo when there are multiple GitHub accounts by @cunla
  in #48

## 1.2.0 - 2022-10-01

### Added

- New icons for in progress/queued workflows
- Ability to configure tab name for each repo (Fix #38)

### Fixed

- Exception when workflow is in progress #35

### Changed

- Logs less verbose

## 1.1.1 - 2022-08-27

### Fixed

- Bug serializing status of job
- Viewing job logs while it is in progress #34

### Changed

- Gradle version building project
- Cleanup code

## 1.1.0 - 2022-08-25

### Added

- Jobs panel view

### Fixed

- Allow empty conclusion, support in progress json - fix #30) by @cunla in #32
- Multiple instances bug by @cunla in #31

## 1.0.1

### Added

- Support for IntelliJ 2022.2

### Fixed

- Issue with `GithubApiRequestExecutorManager.getExecutor`

## 0.0.8 - 2022-06-26

### Fixed

- Issue with `GithubApiRequestExecutorManager.getExecutor`

## 0.0.7 - 2022-06-26

### Added

- Add a link to GitHub accounts settings in case GitHub account is not set #19
- Add a link from toolbar window to Toolbar Settings #21
- Toolbar settings - Resolve #18 by @cunla in #25

### Fixed

- Fix memory leak issue #22

## 0.0.6 - 2022-06-19

### Added

- Contribution guide.
- Documentation: Screenshots on README, contribution guide, etc.
- Message when there is no GitHub account configured.
- Message when there is no repository in the project.

### Changed

- Improved code structure

## 0.0.5 - 2022-06-14

### Added

- Plugin Icon
- Add tab per repo
- Add log folding
- Remove redundant code and deprecated code
- Migrated code from https://github.com/Otanikotani/view-github-actions-idea-plugin
- Initial scaffold created
  from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
