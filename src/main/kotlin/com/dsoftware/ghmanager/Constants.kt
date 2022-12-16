package com.dsoftware.ghmanager

object Constants {
    const val LOG_MSG_PICK_JOB = "Pick a job to view logs"
    const val LOG_MSG_MISSING = "Job logs missing for: "
    const val LOG_MSG_JOB_IN_PROGRESS = "Job is still in progress, can't view logs"

    fun emptyTextMessage(logValue: String): Boolean {
        return (logValue.startsWith(LOG_MSG_MISSING)
            || logValue.startsWith(LOG_MSG_PICK_JOB)
            || logValue.startsWith(LOG_MSG_JOB_IN_PROGRESS)
            )
    }

}
