package com.dsoftware.ghmanager.api.model

enum class Status(val value: String) {
    QUEUED("queued"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    WAITING("waiting")
}

enum class Conclusion(val value: String) {
    SUCCESS("success"),
    FAILURE("failure"),
    NEUTRAL("neutral"),
    CANCELLED("cancelled"),
    SKIPPED("skipped"),
    TIMED_OUT("timed_out"),
    ACTION_REQUIRED("action_required")
}
