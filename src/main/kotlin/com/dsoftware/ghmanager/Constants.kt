package com.dsoftware.ghmanager

import com.intellij.ide.BrowserUtil
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.StatusText

object Constants {
    const val LOG_MSG_PICK_JOB = "Pick a job to view logs"
    const val LOG_MSG_MISSING = "Job logs missing for: "
    const val LOG_MSG_JOB_IN_PROGRESS = "Job is still in progress, can't view logs."

    fun emptyTextMessage(logValue: String): Boolean {
        return (logValue.startsWith(LOG_MSG_MISSING)
            || logValue.startsWith(LOG_MSG_PICK_JOB)
            || logValue.startsWith(
            LOG_MSG_JOB_IN_PROGRESS
        ))
    }

    fun updateEmptyText(logValue: String, emptyText: StatusText): Boolean {
        if (logValue.startsWith(LOG_MSG_MISSING)
            || logValue.startsWith(LOG_MSG_PICK_JOB)
        ) {
            emptyText.text = logValue
            return true
        }
        if (logValue.startsWith(LOG_MSG_JOB_IN_PROGRESS)) {
            emptyText.text = "Job is still in progress, can't view logs."
            emptyText.appendSecondaryText(
                "Please upvote this issue on  GitHub so they will prioritize it.",
                SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES
            ) {
                BrowserUtil.browse("https://github.com/orgs/community/discussions/75518")
            }
            return true
        }
        return false
    }
}
