package com.arjanvlek.oxygenupdater.domain;

import junit.framework.Assert;

import org.junit.Test;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class SystemVersionPropertiesTestOnePlus1 extends SystemVersionPropertiesTest {

    @Test
    public void testSupportedDevice_OxygenOS100() {
        // Fails due to the following:
        // Device can not be detected as ro.build.series is not present in this firmware. Also, ro.build.product has 2 values for OP1: One and OnePlus. TODO: Change detection to ro.display.series, ro.build.product. Supported values are "One" for OOS 1.x and "OnePlus" for OOS 2.1.4.
        // Release-keys are not found in build tags: Only present in property ro.build.fingerprint. Causes unsupported device message even when offline. TODO: Change to ro.build.oemfingerprint, ro.build.fingerprint.
        Assert.assertTrue(isSupportedDevice("op1", "1.0.0", "OnePlus", "OnePlus", "1.0.0", "ONEL_12_150403", "ONEL_12.A.01_001_150403"));
    }

    @Test
    public void testSupportedDevice_OxygenOS103() {
        // Fails due to the following:
        // Device can not be detected as ro.build.series is not present in this firmware. Also, ro.build.product has 2 values for OP1: One and OnePlus. TODO: Change detection to ro.display.series, ro.build.product. Supported values are "One" for OOS 1.x and "OnePlus" for OOS 2.1.4.
        // Release-keys are not found in build tags: Only present in property ro.build.fingerprint. Causes unsupported device message even when offline. TODO: Change to ro.build.oemfingerprint, ro.build.fingerprint.
        Assert.assertTrue(isSupportedDevice("op1", "1.0.3", "OnePlus", "OnePlus", "1.0.3", "ONEL_12_150827", "ONEL_12.A.01_001_150827"));
    }

    @Test
    public void testSupportedDevice_OxygenOS214() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("op1", "2.1.4", "OnePlus", "OnePlus", NO_OXYGEN_OS, "ONELOxygen_12_201601190107", "ONELOxygen_12.A.02_GLO_002_201601190107"));
    }
}
