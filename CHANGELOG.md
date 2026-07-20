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

## 2026.1.11

### 🚀 Features

- Opening a workflow file in the editor now opens the GitHub Actions tool window with the runs list filtered to that
  workflow. Switching between already-open editor tabs updates the filter when the tool window is visible. The behavior
  can be turned off in the plugin settings ("Show runs of a workflow file when it is opened?"); it is on by default.

### 🐛 Bug Fixes

- Fixed action navigation from `uses:` in workflow files doing nothing on IntelliJ IDEA Ultimate 2026.2. The platform
  now bundles its own GitHub Actions support that contributes a competing reference on `uses:` values, which suppressed
  the plugin's own reference resolution entirely. The plugin's reference provider is now registered at a higher
  priority.

### 🧰 Maintenance

- The plugin now targets the 2026.2 (262) platform and is compiled to a Java 25 bytecode target to match its JBR 25
  baseline. IDEs on older platforms continue to receive the 2026.1.x releases.
- Updated Kotlin to 2.4.10, the IntelliJ Platform Gradle plugin to 2.18.1, and JUnit to 6.1.2.

## 2026.1.10

### 🐛 Bug Fixes

- Fixed the plugin failing to load with `UnsupportedClassVersionError` (class file version 69.0) on IDEs running JBR 21,
  such as Android Studio 2026.1.1. The plugin is now compiled to a Java 21 bytecode target to match the platform
  baseline. ([#305](https://github.com/dsoftwareinc/ghactions-manager/issues/305))

## 2026.1.9

### 🚀 Features

- New "Concurrency Groups" section in the repository-settings dialog: lists the repository's workflow concurrency
  groups, and selecting a group shows the workflow runs and jobs currently holding or waiting for that group's lock.
- Repository settings: when the caches or concurrency-groups list fails to load (e.g. HTTP 402
  "payment method required"), GitHub's error message is shown with a Retry link instead of an empty table.
- New "Runners" section in the repository-settings dialog for organization-owned repos: lists the organization's
  GitHub-hosted runners (platform, image, machine size, status) and self-hosted runners (OS, online/busy status, labels,
  agent version).

### 🐛 Bug Fixes

- The annotations-panel toggle is now remembered per project across IDE restarts.
- The license invitation in the annotations banner is shown at most once per IDE session instead of on every job
  selection.
- The workflow-run tooltip now shows a live duration for in-progress runs instead of a value frozen at the last refresh.
- No more error balloon on every repository-settings open when the cache usage or storage-limit endpoints fail (e.g.
  HTTP 402); the caches header simply omits the usage line.
- Fixed an `IncorrectOperationException` ("parent ... has already been disposed") when a deferred tool-window content
  refresh ran after the tool window was disposed, e.g. while closing the project.
  ([#302](https://github.com/dsoftwareinc/ghactions-manager/issues/302))
- Repository tabs are now sorted alphabetically by display name, so the tab strip and the "Show Hidden Tabs" list are
  navigable in multi-repo projects. ([#304](https://github.com/dsoftwareinc/ghactions-manager/issues/304))
- The unlicensed annotations banner no longer shows a literal `<hyperlink>` tag; only the word "license" is rendered as
  the link. ([#303](https://github.com/dsoftwareinc/ghactions-manager/issues/303))
- The "please upvote this issue" link shown while a job is in progress now actually opens the GitHub discussion in the
  browser. ([#303](https://github.com/dsoftwareinc/ghactions-manager/issues/303))
- In-progress step names are shown in readable amber instead of pure yellow, which was illegible on light themes.
  ([#303](https://github.com/dsoftwareinc/ghactions-manager/issues/303))
- The "Open in browser" actions use the generic web icon instead of the Chrome logo.
  ([#303](https://github.com/dsoftwareinc/ghactions-manager/issues/303))

### 🧰 Maintenance

- Subscribe `FileOpenedListener` to the `FileEditorManagerListener.FILE_EDITOR_MANAGER` message-bus topic at project
  startup instead of the declarative `<projectListeners>` registration deprecated in newer platform builds.

### Improvements

- Faster runs-list rendering: the hover tooltip is built only on hover instead of on every repaint (in-progress rows
  repaint continuously while their status icon animates).

## 2026.1.8

### 🚀 Features

- View a job's GitHub Actions annotations (failures/warnings/notices) in a banner above the step log; clicking an
  annotation with a file path opens that line in the editor. (License required.)
- Hover over a job in the jobs tree to see a details tooltip: runner name/group, runner labels, attempt, branch and
  commit, and timing.
- Hover over a workflow run to see a details tooltip: trigger event, attempt, total duration, and (for the selected run)
  artifact count.

### 🧰 Maintenance

- fix: update publishPlugin task for configuration cache compatibility

## 2026.1.7

### 🐛 Bug Fixes

- Resolve of local actions/workflows #301

### 🧰 Maintenance

- Migrate from the deprecated `kotlinx.datetime.Instant`/`Clock` to the stable `kotlin.time` equivalents
- Replace the deprecated `@RunInEdt` test annotation with a local `RunInEdtInterceptor` JUnit 5 extension

## 2026.1.6

### 🚀 Features

- Add a **Rerun Failed Jobs** action to the workflow-run popup menu, shown for finished runs

### 🧰 Maintenance

- Remove usage of deprecated API `SimpleListCellRenderer.create`

## 2026.1.5 - 2026-06-12

### 🚀 Features

- Manage repository collaborators (view/add/remove) from the repository-settings dialog
- Manage repository environments from the repository-settings dialog: environments appear as a tree, and selecting one
  shows its deployment protection rules, environment secrets, and (editable) environment variables
- Refresh button on the repository-settings panels, with a loading indicator shown while data reloads
- Manage GitHub Actions caches from the repository-settings dialog: a new **Caches** section shows the repo's total
  cache usage and storage limit, groups cache entries by key (showing entry count, total size, created and last-accessed
  times), lets you filter by branch, sort by any column, and delete all caches for a key to reclaim storage

### 🐛 Bug Fixes

- Repository-settings panels (variables, collaborators, environments) no longer block the UI thread while loading from
  GitHub
- Fix repository-secret deletion sending a malformed URL
- Fix a possible error when removing or editing a variable with no row selected

### 🧰 Maintenance

- Make request retry-tracking thread-safe across concurrent API calls

## 2026.1.4 - 2026-06-08

### 🐛 Bug Fixes

- A completed job's log is now refreshed once when it finishes, so the final log lines are captured
- A completed workflow-run's jobs and artifacts are refreshed once on completion instead of being polled indefinitely
- Using GitHub PAT works

### 🧰 Maintenance

- Cache the request executor per repository instead of re-resolving the account token on every access

## 2026.1.3 - 2026-05-27

### 🐛 Bug Fixes

- Fix private workflow hasn’t been resolved, jackson error in log #300

## 2026.1.2 - 2026-05-25

### 🚀 Features

- Job list is shown as a tree of jobs

## 2026.1.1 - 2026-05-19

### 🐛 Bug Fixes

- fix:NPE on showRegisterDialog #299
- remove usage of internal API PluginManagerCore.getPlugin

## 2026.1.0 - 2026-05-11

### 🚀 Features

- Add the ability to dispatch workflow from the editor

### 🐛 Bug Fixes

- Crash when trying to open a pull-request in the IDE
- Error showing action is already taken #298

### Maintenance

- Update dependencies
- Add multiple test-cases

## 2025.2.15 - 2026-04-21

### 🐛 Bug Fixes

- fix: getting actions data from GitHub
- fix: do not notify about repeating errors

## 2025.2.14 - 2026-03-31

### 🐛 Bug Fixes

- fix:access getContentManager from EDT only #292

## 2025.2.13 - 2026-03-21

### 🐛 Bug Fixes

- fix: Plugin freezes PhpStorm 2025.3: PhpStorm is not responding #287
- fix: opening settings raises an exception #286
- Opening GitHub Settings from the plugin causes exception #283

### Maintenance

- Remove usage of deprecated runReadAction #31

## 2025.2.11 - 2026-02-25

### 🐛 Bug Fixes

- fix: exception when changing repo #279

## 2025.2.10 - 2026-02-17

### 🚀 Features

- Update the open tab based on the file opened in the editor

### Maintenance

- Update dependencies

## 2025.2.9 - 2026-01-30

### 🐛 Bug Fixes

- Support multiple GitHub accounts with the same alias #269

## 2025.2.8 - 2026-01-21

### 🐛 Bug Fixes

- Error when opening IDE settings #270
- Allow having multiple repositories with the same name #274
- Do not pop up errors when requests keep failing #271
- Support multiple GitHub accounts with the same alias #269

## 2025.2.7

### 🐛 Bug Fixes

- fix: Open dialog actions in BGT #268
- fix: Catching index data initialization failed exception #267
- fix: Ability to change keyboard shortcut for opening GitHub Actions Manager #260

### Maintenance

- Update dependencies (IntelliJ Platform 2025.3, IntelliJ Platform Gradle Plugin 2.9.0, etc.)
- Improve test coverage

## 2025.2.6

### 🚀 Features

- Show starting time for jobs
- Show steps when a selected job is still running (without logs due to GitHub API limitations)

### 🐛 Bug Fixes

- Using EDT in LogConsolePanelWrapper.updateLogs #262
- Change the shortcut to open GitHub Actions Manager from alt-G #260

## 2025.2.5

### 🚀 Features

- Removed timestamp from logs #259

## 2025.2.4

### 🚀 Features

- workflow input with `environment` type shows environments’ dropdown

### 🐛 Bug Fixes

- fix:workflow inputs are passing to dispatch workflow popup #258
- fix:refresh jobs button is working

## 2025.2.3

### 🚀 Features

- Keyboard Shortcut alt+G/option+G to activate the plugin window
- When the plugin window is activated, alt/option+E to open the dispatch workflow popup

### 🐛 Bug Fixes

- fix:handling Tools.getYamlElementsWithKey raising exception #255

### Maintenance

- improve test coverage
- Update to latest intellij platform Gradle plugin (2.8.0)

## 2025.2.2 - 2025-08-15

### 🚀 Features

- Link from file lines to GitHub

### 🧰 Maintenance

- Updated test suites to run in 2025.2

## 2025.2.1 - 2025-08-02

### 🐛 Bug Fixes

- Issue with checklicense opening #250

## 2025.2.0 - 2025-08-01

### 🐛 Bug Fixes

- Issue running on v2025.1.4 #234
- Disposing background job when service is disposed #236

### 🧰 Maintenance

- Remove usage of deprecated API.

## 2025.1.4 - 2025-08-01

### 🐛 Bug Fixes

- Download artifact is not blocking UI #226

## 2025.1.3 - 2025-06-10

### 🚀 Features

- Disable the workflows’ button when the repository has no workflows
- Skip workflow parameters popup when the workflow has no parameters
- Show when logs are gone from the server
- fix:allow max 999 secs between executions #219
- fix: support for 2025.2 EAP

### 🐛 Bug Fixes

- null cast to YAMLMapping on workflow analysis #223
- Wrapped log-model reset in runWriteAction #224
- Respect using PAT instead of IDE GitHub settings for repo config #220

## 2025.1.2 - 2025-05-10

### 🚀 Features

- Show head commit of PR with different color for the branch

### 🐛 Bug Fixes

- Fix: Workflow run artifacts icon is not working properly #216
- Fix: YAML exception in ActionsAnnotator #215

## 2025.1.1 - 2025-04-25

### 🐛 Bug Fixes

- ConcurrentModificationException happening on reset #213
- Access is allowed from Event Dispatch Thread #212
- Update ToolWindow on updating GitHub accounts
- Comparing repo-mapping using a repo-path to avoid NoSuchElementException #210

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

- Fix the issue with updating the log panel out of EDT #201
- Fix the issue when the workflow is not in `.github/workflows` #200

## 2024.3.3 - 2024-12-20

### 🐛 Bug Fixes

- Fixed the ability to configure the default filter for workflow runs #198

### 🧰 Maintenance

- Update to latest intellij platform Gradle plugin

## 2024.3.2 - 2024-12-09

### 🚀 Features

- Added the ability to configure the default filter for workflow runs #198

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

- Disable `Open workflow file` action when the workflow does not have a file in the repository.
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

- Add the ability to reject pending deployments for workflow-runs in waiting for deployment status

### 🐛 Bug Fixes

- Fixed the error reporter to send the relevant part of the stack trace

## 2024.2.9 - 2024-09-20

### 🚀 Features

- Add the ability to review deployments for workflow-runs in waiting for deployment status #178
- Add the ability to sort jobs by showing in-progress jobs first

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

- Workflow dispatcher: Mark currently has workflows that can be manually dispatched.
- Disposing LogConsolePanelWrapper before LogConsolePanel components #177

## 2024.2.6 - 2024-07-21

### 🐛 Bug Fixes

- Workflow dispatcher: Disable workflow types that cannot be dispatched manually #172

## 2024.2.5 - 2024-07-15

### 🐛 Bug Fixes

- Workflow dispatcher: Disable workflow types that cannot be dispatched manually #172
- Prevent API abuse when action with a complex name fails resolving #173

## 2024.2.4 - 2024-07-08

### 🐛 Bug Fixes

- Fix the issue with RepositoryContext background task hasn’t been canceled properly #171

## 2024.2.3 - 2024-06-30

### Improvements

- Migrated to a view-model for jobs-panel and logs-panel - significant performance improvement.
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

- (Paid feature) Open a pull request from a selected workflow run (when pull-request is open)

### 🐛 Bug Fixes

- Support for reusable workflows in workflow files
- Showing the download artifacts button only when relevant

### Improvements

- Using kotlin 2.0, IntelliJ Gradle Plugin v2, java 21

## 2024.1.3 - 2024-05-27

### 🐛 Bug Fixes

- Fix crash when opening a GH actions file #160
- Fix marking unknown actions when GitHub settings are not set

## 2024.1.2 - 2024-05-15

### 🐛 Bug Fixes

- Using a concurrent set to prevent concurrent modification exception #154

## 2024.1.1 - 2024-05-11

### 🚀 Features

- Add the ability to download artifacts from the jobs panel (Paid feature)

### Improvements

- Highlighting action names only in the `uses` field
- Update the actions’ cache when GitHub settings change.

### 🧰 Maintenance

- Update to the latest dependencies

## 1.22.0 - 2024-05-15

### 🚀 Features

- Highlighting unknown actions in workflow files
- Clicking on action names:
    - opens the browser to the action's GitHub page for remote actions
    - opens the action file for local actions

### 🐛 Bug Fixes

- Fix identifying composite action files
- Fix opening file in the root directory # 142

- Fix the bug with concurrent adding/removing actions to resolve #150

### 🧰 Maintenance

- Update reporting issues to GitHub

## 1.21.6 - 2024-04-27

### 🐛 Bug Fixes

- Fix the bug failing to load annotator #131
- Fix scanning actions in composite action files

### 🧰 Maintenance

- Implement tests for startup scanning
- Implement tests for highlighting and quickfixes of outdated actions

## 1.21.5 - 2024-04-24

### 🐛 Bug Fixes

- Fix the bug of jobs not being refreshed while the workflow is running

## 1.21.4 - 2024-04-23

### 🐛 Bug Fixes

- Fix memory leak with the log panel
- Workflow runs list is getting updated with the latest status

## 1.21.3 - 2024-04-19

### 🐛 Bug Fixes

- Fix the bug loading null value to cache #128
- Show go to GitHub settings when an error occurs
- Show the reload jobs button

## 1.21.2 - 2024-04-18

### 🐛 Bug Fixes

- Fix a bug scanning workflow files #126 #125

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

- Tooltip on the tab showing real repo and GitHub account
- Prefer a GitHub account that is the owner of the repository

### 🐛 Bug Fixes

- Using new error handlers.
- Create a GitHub Request executor that supports redirects. # 119

### 🧰 Maintenance

- Update deprecated code.

## 1.19.0 - 2024-04-04

- Update license to GPL-3.0

### 🚀 Features

- Support for JetBrains IDEs v2024.1

### 🧰 Maintenance

- Improve tests and increase coverage
- Update dependencies to the latest versions

## 1.18.0 - 2024-03-12

### 🚀 Features

- Add quick-filter to show runs based on the current branch (updates when the branch is updated) #115
- Add the ability to position the workflow runs list on top of jobs list #116
- Show a link to the step log when the log is too large #118

### 🧰 Maintenance

- Major refactoring better performance and code quality

### 🐛 Bug Fixes

- Fix not allowing custom repositories in plugin settings

## 1.17.0 - 2024-02-14

### 🚀 Features

- Extract all messages to i18n file #114
- Update info bar on the jobs-panel

### 🐛 Bug Fixes

- Minor bug when unable to parse Instant

### 🧰 Maintenance

- Update dependencies to the latest versions
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

- Update dependencies to the latest versions

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

- Fix refreshing of jobs’ list.
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
- Showing the right status for jobs that haven't started
- Improved filters behavior

## 1.12.0

### 🚀 Features

- Simplified Jobs panel logic
- Add the ability to filter workflow runs by actor, branch and status.

## 1.11.0

### 🚀 Features

- Add an info bar with the number of jobs loaded

### 🐛 Bug Fixes

- Fix the bug showing seconds without padding #88
- Fix jobs request pagination, now pagesize=100 #89
- Fix updating job logs during theme change #85

## 1.10.4

### 🐛 Bug Fixes

- Fix the bug when the step does not have logs #83
- Support for 2023.2-EAP

## 1.10.3

### 🐛 Bug Fixes

- Fix icons for a new UI look

## 1.10.2

### 🐛 Bug Fixes

- Fix the error when using IntelliJ 2023.1.RC #83
- Using GHAccountManager instead of the deprecated GithubAuthenticationManager

## 1.10.1

### 🐛 Bug Fixes

- Fix the bug requiring to pick a job after logs are loaded.

## 1.10.0

### 🚀 Features

- Add the ability to configure the number of workflow runs on the list.
- Add the ability to configure GitHub token instead of using IDE GitHub settings.

### Changed

- Support 2023.1 EAP release

## 1.9.2

### Changed

- Fix bugs refreshing workflow runs #81
- Fix bugs calling getComponent from non-dispatch thread #78
- Add an icon for action_required conclusion.

## 1.9.1

### Changed

- Upgrade Gradle to `7.6`
- Upgrade `org.jetbrains.kotlin.jvm` from 1.7.22 to 1.8.0

## 1.9.0

### Changed

- Clean up code on `GhActionsManagerConfigurable` by @cunla in #71
- LogPanel wrap by @cunla in #72

## 1.7.0

### 🚀 Features

- Make ghactions-manager available during indexing (#66)
- Sorting jobs by completed date or started date, else run id (#61)

### 🐛 Bug Fixes

- Fix the deadlock when refreshing workflow runs (#64)

## 1.6.1

### Fixed

- Link to pull-request
- Reset log when the workflow runs unselected
- Keep workflow-run selected after refresh

## 1.6.0

### Added

- Step logs – Showing failed step title in red
- Refresh of runs only for the active tab

### Changed

- Better GitHub REST API error handling

### Fixed

- Update the jobs panel and log panel to loading state when a new run is selected
- Clean up more code

## 1.5.4

### Added

- Open workflow file action

### Fixed

- Update the workflow run state

## 1.5.1

### Fixed

- Running refresh in the background

## 1.5.0

### Added

- Ability to change how often refresh of workflow runs is done
- Refresh jobs of workflow run if still in progress

### Changed

- Allowing job log to be beneath the jobs’ list – configurable in plugin settings

### Fixed

- Showing logs of the selected job once logs are loaded

## 1.4.0

### Added

- Refresh workflow runs status in the background

## 1.3.0 - 2022-10-07

### Added

- Refresh jobs and rerun the workflow run by @cunla in #40
- Update cancelled icon by @cunla in #44
- Add cancel workflow action #43 by @cunla in #46
- Guess GitHub account per repo when there are multiple GitHub accounts by @cunla in #48

## 1.2.0 - 2022-10-01

### Added

- New icons for in-progress /queued workflows
- Ability to configure a tab name for each repo (Fix #38)

### Fixed

- Exception when a workflow is in progress #35

### Changed

- Logs less verbose

## 1.1.1 - 2022-08-27

### Fixed

- Bug serializing status of a job
- Viewing job logs while it is in progress #34

### Changed

- Gradle version building project
- Cleanup code

## 1.1.0 - 2022-08-25

### Added

- Jobs panel view

### Fixed

- Allow empty conclusion, support in progress JSON - fix #30) by @cunla in #32
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

- Add a link to GitHub accounts settings in case the GitHub account is not set #19
- Add a link from the toolbar window to Toolbar Settings #21
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
