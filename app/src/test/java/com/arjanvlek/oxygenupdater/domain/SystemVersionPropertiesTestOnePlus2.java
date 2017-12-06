package com.arjanvlek.oxygenupdater.domain;

import junit.framework.Assert;

import org.junit.Test;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class SystemVersionPropertiesTestOnePlus2 extends SystemVersionPropertiesTest {

    @Test
    public void testSupportedDevice_OxygenOS200() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        // Fails due to the following:
        // Device can not be detected as ro.build.series is not present in this firmware. TODO: detect device using ro.build.series, ro.build.product in this order.
        Assert.assertTrue(isSupportedDevice("op2", "2.0.0", "OnePlus2", "OnePlus2", NO_OXYGEN_OS, "OnePlus2Oxygen_14_1507251956", "OnePlus2Oxygen_14.A.02_GLO_002_1507251956"));
    }

    @Test
    public void testSupportedDevice_OxygenOS310() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("op2", "3.1.0", "OnePlus2", "OnePlus2", NO_OXYGEN_OS, "OnePlus2Oxygen_14_1608262242", "OnePlus2Oxygen_14.A.20_GLO_020_1608262242"));
    }

    @Test
    public void testSupportedDevice_OxygenOS361() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("op2", "3.6.1", "OnePlus2", "OnePlus2", NO_OXYGEN_OS, "OnePlus2Oxygen_14_1710240102", "OnePlus2Oxygen_14.A.32_GLO_032_1710240102"));
    }
}
