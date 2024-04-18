GitHub Actions Manager for JetBrains IDEs
=========================================

![Build](https://github.com/cunla/ghactions-manager/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/com.dsoftware.ghtoolbar.svg)](https://plugins.jetbrains.com/plugin/19347-github-actions-toolbar)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/com.dsoftware.ghtoolbar.svg)](https://plugins.jetbrains.com/plugin/19347-github-actions-toolbar)

# Screenshots

                                 Horizontal                                 |  Vertical

:--------------------------------------------------------------------------:|:-------------------------:
![](docs/screenshot-new-ui-light.png) ![](docs/screenshot-new-ui-dark.png) |  ![](docs/screenshot-vertical.jpg)

![](docs/outdated-action-version.jpg)

![](docs/quickfix-action.jpg)


<!-- Plugin description -->
This plugin creates a tool-window on JetBrains products (IntelliJ, PyCharm, ...) where you can view GitHub workflow runs
of the repository.
This plugin is a good alternative to alt-tabbing for every time you push some changes to the branch and want to see
whether the repository's checks are passing on your changes.

# GitHub Actions Tool Window Features

- View the latest workflow runs of the repository.
- Filter the workflow runs by the current branch.
- Filter the workflow runs by status, user who triggered the workflow, and the workflow name.
- Trigger a workflow from the tool window.
- Rerun a workflow run.
- Open the workflow file in the project.

# Editing GitHub Workflow files Features

- Highlight outdated actions.
- Update to latest major version of the action.

# Sponsor

GitHub Actions Manager for JetBrains products is developed for free.

Support this project by becoming a sponsor using [GitHub sponsors](https://github.com/sponsors/cunla).

Alternatively, register to @cunla's feed on polar.sh:

<a href="https://polar.sh/cunla/subscribe">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://polar.sh/embed/subscribe.svg?org=cunla&label=Subscribe&darkmode">
      <img alt="Subscribe on Polar" src="https://polar.sh/embed/subscribe.svg?org=cunla&label=Subscribe">
    </picture>
</a>

# Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> >
  <kbd>Plugins</kbd> >
  <kbd>Marketplace</kbd> >
  <kbd>Search for "GitHub Actions Manager"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/cunla/ghactions-manager/releases/latest) and install it manually
  using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

<!-- Plugin description end -->

# Repository activity

![Alt](https://repobeats.axiom.co/api/embed/756410507b3575fdcf5b04cc7acc32148f3481b5.svg "Repobeats analytics image")

# Contributions

Contributions are welcome. Please see the
[contributing guide](https://github.com/cunla/ghactions-manager//blob/master/.github/CONTRIBUTING.md) for more details.
The maintainer generally has very little time to work on ghactions-manager, so the
best way to get a bug fixed is to contribute a pull request.

If you'd like to help out, you can start with any of the issues
labeled with `Help wanted`.
