package com.dsoftware.ghmanager.psi

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class VersionCompareToolsTest {
    @Test
    fun testIsActionOutdated() {
        Assertions.assertFalse(VersionCompareTools.isActionOutdated("v4.0.0", "v4.0.0"))
        Assertions.assertFalse(VersionCompareTools.isActionOutdated("v4.0.0", null))
        Assertions.assertFalse(VersionCompareTools.isActionOutdated(null, "v4.0.0"))
        Assertions.assertFalse(VersionCompareTools.isActionOutdated(null, null))
        Assertions.assertFalse(VersionCompareTools.isActionOutdated("v4.0.0", "master"))
        Assertions.assertTrue(VersionCompareTools.isActionOutdated("v4.0.0", "v5.0.0"))
    }
}
