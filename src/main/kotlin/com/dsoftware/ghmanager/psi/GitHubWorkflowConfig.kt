package com.dsoftware.ghmanager.psi

import java.util.regex.Pattern

object GitHubWorkflowConfig {
    val PATTERN_GITHUB_OUTPUT: Pattern =
        Pattern.compile("echo\\s+\"(\\w+)=(.*?)\"\\s*>>\\s*\"?\\$\\w*:?\\{?GITHUB_OUTPUT\\}?\"?")
    val PATTERN_GITHUB_ENV: Pattern =
        Pattern.compile("echo\\s+\"(\\w+)=(.*?)\"\\s*>>\\s*\"?\\$\\w*:?\\{?GITHUB_ENV\\}?\"?")
    const val FIELD_ON: String = "on"
    const val FIELD_IF: String = "if"
    const val FIELD_ID: String = "id"
    const val FIELD_ENVS: String = "env"
    const val FIELD_RUN: String = "run"
    const val FIELD_RUNS: String = "runs"
    const val FIELD_JOBS: String = "jobs"
    const val FIELD_VARS: String = "vars"
    const val FIELD_WITH: String = "with"
    const val FIELD_USES: String = "uses"
    const val FIELD_NEEDS: String = "needs"
    const val FIELD_STEPS: String = "steps"
    const val FIELD_RUNNER: String = "runner"
    const val FIELD_GITHUB: String = "github"
    const val FIELD_INPUTS: String = "inputs"
    const val FIELD_OUTPUTS: String = "outputs"
    const val FIELD_SECRETS: String = "secrets"
    const val FIELD_CONCLUSION: String = "conclusion"
    const val FIELD_OUTCOME: String = "outcome"

}