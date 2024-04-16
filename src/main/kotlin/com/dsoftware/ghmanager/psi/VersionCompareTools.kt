package com.dsoftware.ghmanager.psi

import com.intellij.openapi.diagnostic.logger

object VersionCompareTools {
    private val LOG = logger<VersionCompareTools>()

    fun isActionOutdated(current: String?, latest: String?): Boolean {
        if (current == null || latest == null) {
            return false
        }
        var currentVersion = current
        var latestVersion = latest
        if (latestVersion.startsWith("v")) {
            latestVersion = latestVersion.substring(1)
        }
        if (currentVersion.startsWith("v")) {
            currentVersion = currentVersion.substring(1)
        }
        LOG.debug(
            "Comparing versions: latestVersion=$latestVersion and currentVersion=$currentVersion",
            latestVersion,
            currentVersion
        )
        val majorLatest = latestVersion.split(".").dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        val majorCurrent = currentVersion.split(".").dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        return try {
            majorLatest.toInt() != majorCurrent.toInt()
        } catch (e: NumberFormatException) {
            false
        }
    }


}
