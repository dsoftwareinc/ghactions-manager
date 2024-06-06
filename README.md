GitHub Actions Manager for JetBrains IDEs
=========================================

[![Version][1]][2]
[![Downloads][3]][4]

You can view a demo of most of the features explained in this video: [YouTube][5]

<!-- Plugin description -->
This plugin creates a tool-window on JetBrains products (IntelliJ, PyCharm, ...) where you can view GitHub workflow runs
of the repository. This plugin is a good alternative to alt-tabbing for every time you push some changes to the branch
and want to see whether the repository's checks are passing on your changes.

You can support the development of the plugin and get a few extra features by purchasing the plugin on the JetBrains for
$2 a month.

# Features

### Main use-case

- View the latest workflow runs and their statuses.
    - Automatically refreshes every 30 seconds.
    - Can filter by: workflow-type, user who initiated, branch and event.
    - Can filter by the current-branch, i.e., update workflow-runs when branch changes, and current user.
- View jobs of a workflow-run and their statuses.
- View logs of a job, separated by each step result.
- (Paid feature) View and download the workflow run's artifacts.
- (Paid feature) Open pull-request that triggered workflow (in IDE/browser)..

### Additional use-cases

- Trigger a workflow-run.
- Rerun a workflow run or a job.
- Open the workflow file of the workflow-run.
- Configuring settings
    - Use a customized token instead of GitHub settings
    - Frequency of auto-refresh.
    - Customize repositories that should be presented/hidden.

### Editing workflow files

- Highlight outdated actions as warnings
    - Generate quickfix: update them to the latest version.
- Highlight unknown actions
- Navigate to action repository for remote actions
- Navigate to action file for local actions

<!-- Plugin description end -->

# Screenshots

|                                 Horizontal                                 |             Vertical              |
|:--------------------------------------------------------------------------:|:---------------------------------:|
| ![](docs/screenshot-new-ui-light.png) ![](docs/screenshot-new-ui-dark.png) | ![](docs/screenshot-vertical.jpg) |
|                   ![](docs/outdated-action-version.jpg)                    |                                   |
|                       ![](docs/quickfix-action.jpg)                        |                                   |

# Sponsor

GitHub Actions Manager for JetBrains IDEs is developed for free.

Support this project by becoming a sponsor using [GitHub sponsors][6].

Alternatively, register to @cunla's feed on polar.sh:

<a href="https://polar.sh/cunla/subscribe">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://polar.sh/embed/subscribe.svg?org=cunla&label=Subscribe&darkmode">
      <img alt="Subscribe on Polar" src="https://polar.sh/embed/subscribe.svg?org=cunla&label=Subscribe">
    </picture>
</a>

# Installation

Using IDE built-in plugin system:

<kbd>Settings/Preferences</kbd> >
<kbd>Plugins</kbd> >
<kbd>Marketplace</kbd> >
<kbd>Search for "GitHub Actions Manager"</kbd> >
<kbd>Install Plugin</kbd>


[1]:https://img.shields.io/jetbrains/plugin/v/com.dsoftware.ghtoolbar.svg
[2]:https://plugins.jetbrains.com/plugin/19347-github-actions-toolbar
[3]:https://img.shields.io/jetbrains/plugin/d/com.dsoftware.ghtoolbar.svg
[4]:https://plugins.jetbrains.com/plugin/19347-github-actions-toolbar
[5]:https://youtu.be/nFrs8W2gSC8
[6]:https://github.com/sponsors/cunla
