GitHub Actions Manager for JetBrains IDEs
=========================================

[![Version][version-badge]][marketplace]
[![Downloads][download-badge]][marketplace]

You can view a demo of most of the features explained in this video: [YouTube][youtube-video]

<!-- Plugin description -->
This plugin creates a tool-window on JetBrains products (IntelliJ, PyCharm, ...) where you can view GitHub Actions
workflow-runs of the repository. This plugin is a good alternative to alt-tabbing for every time you push some changes
to the branch and want to see whether the repository's checks are passing on your changes.

You can support the development of the plugin and additional advanced features by
[purchasing the plugin on the JetBrains marketplace][marketplace].

# Features

### Free Features (No license required)

- View Workflow Runs
    - See real-time and historical workflow runs, jobs, and logs directly in the IDE.
    - Logs are organized step-by-step for easier debugging.
    - Track live runs as they progress or review past runs at a glance.
    - Filter runs by branch, actor, event, or status—individually or in combination. Includes a current-branch filter
      that updates automatically when you switch branches.
    - New runs appear automatically (refreshes every 30s by default; configurable).
    - Access all available workflow run details.-
- Settings & Configuration
    - Use a custom token instead of GitHub’s default authentication.
    - Control auto-refresh frequency.
    - Choose which repositories to show or hide.
    - Assign a GitHub account per repository, and switch assignments anytime via plugin settings.-

### Paid Features (License required)

- View and download workflow run artifacts.
- Open the pull request that triggered a run (in IDE or browser).
- Approve workflow-run deployments that require approval.
- Manually trigger workflow runs for dispatchable workflows, with full input
  support ([all GitHub input types][workflow-inputs]).
- Rerun or cancel a workflow run—or even just a specific job.
- Open the workflow file associated with a run.
- Generate GitHub links to specific file lines from the IDE.

### Editing workflow files

- Highlight outdated GitHub Actions as warnings, with quick-fix support to update to the latest version.
- Flag unknown actions.
- Navigate directly to an action’s repository (for remote actions).
- Jump to the action file (for local actions).

<!-- Plugin description end -->

# Installation

Using IDE built-in plugin system:

<kbd>Settings/Preferences</kbd> >
<kbd>Plugins</kbd> >
<kbd>Marketplace</kbd> >
<kbd>Search for "GitHub Actions Manager"</kbd> >
<kbd>Install Plugin</kbd>

# Screenshots

### View workflow-runs/jobs/logs

![](docs/screenshot-new-ui-light.png) ![](docs/screenshot-new-ui-dark.png)
![](docs/screenshot-vertical.jpg)

### Dispatch a workflow that has inputs

![](docs/workflow-dispatch.jpg)

### See outdated action in workflow files

![](docs/outdated-action-version.jpg)

### Quickfix to update the action version in workflow files

![](docs/quickfix-action.jpg)

### Approve deployment

![](docs/approve-deployment.gif)

### Create a link of selected file lines in GitHub

![](docs/ghactions-mgr-link-to-gh.gif)

[version-badge]:https://img.shields.io/jetbrains/plugin/v/com.dsoftware.ghtoolbar.svg

[download-badge]:https://img.shields.io/jetbrains/plugin/d/com.dsoftware.ghtoolbar.svg

[youtube-video]:https://youtu.be/nFrs8W2gSC8

[gh-docs-workflow-inputs]:https://docs.github.com/en/enterprise-cloud@latest/actions/using-workflows/workflow-syntax-for-github-actions#onworkflow_dispatchinputs

[marketplace]: https://plugins.jetbrains.com/plugin/19347-github-actions-manager

[workflow-inputs]:https://docs.github.com/en/enterprise-cloud@latest/actions/using-workflows/workflow-syntax-for-github-actions#onworkflow_dispatchinputs