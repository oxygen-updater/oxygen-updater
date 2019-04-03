package com.arjanvlek.oxygenupdater.domain;

import org.junit.Assert;
import org.junit.Test;

public class SystemVersionPropertiesTestOnePlus3 extends SystemVersionPropertiesTest {

    @Test
    public void testSupportedDevice_OxygenOS312() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("op3", "3.1.2", "OnePlus 3", "OnePlus 3", "OnePlus3", "OnePlus3Oxygen_16_1606062247", "OnePlus3Oxygen_16.A.07_GLO_007_1606062247", false));
    }

    @Test
    public void testSupportedDevice_OxygenOS451() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("op3", "4.5.1", "OnePlus 3", "OnePlus 3", "OnePlus3", "OnePlus3Oxygen_16_1710122310", "OnePlus3Oxygen_16.A.57_GLO_057_1710122310", false));
    }

    @Test
    public void testSupportedDevice_OxygenOS500() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("op3", "5.0.0", "OnePlus 3", "OnePlus 3", "OnePlus3", "OnePlus3Oxygen_16_1711160505", "OnePlus3Oxygen_16.A.60_GLO_060_1711160505", false));
    }

    
}
