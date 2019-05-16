package com.arjanvlek.oxygenupdater.domain;

import org.junit.Assert;
import org.junit.Test;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class SystemVersionPropertiesTestOnePlus1 extends SystemVersionPropertiesTest {

    @Test
    public void testSupportedDevice_OxygenOS100() {
        // Not supported in app versions 1.2.x - 2.2.x, but has been fixed in App 2.3.x
        Assert.assertTrue(isSupportedDevice("op1", "1.0.0", "One", NO_OXYGEN_OS, "One", "1.0.0", "ONEL_12.A.01_001_150403", false));
        Assert.assertEquals("OnePlus One", getSupportedDevice("op1", "1.0.0").getName());
    }

    @Test
    public void testSupportedDevice_OxygenOS103() {
        // Not supported in app versions 1.2.x - 2.2.x, but has been fixed in App 2.3.x
        Assert.assertTrue(isSupportedDevice("op1", "1.0.3", "One", NO_OXYGEN_OS, "One", "1.0.3", "ONEL_12.A.01_001_150827", false));
        Assert.assertEquals("OnePlus One", getSupportedDevice("op1", "1.0.3").getName());
    }

    @Test
    public void testSupportedDevice_OxygenOS214() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("op1", "2.1.4", "OnePlus", "OnePlus", "OnePlus", "ONELOxygen_12_201601190107", "ONELOxygen_12.A.02_GLO_002_201601190107", false));
        Assert.assertEquals("OnePlus One", getSupportedDevice("op1", "2.1.4").getName());
    }
}
