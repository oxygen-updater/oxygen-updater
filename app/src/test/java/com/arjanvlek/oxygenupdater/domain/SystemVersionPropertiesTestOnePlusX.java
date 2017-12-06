package com.arjanvlek.oxygenupdater.domain;

import junit.framework.Assert;

import org.junit.Test;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class SystemVersionPropertiesTestOnePlusX extends SystemVersionPropertiesTest {

    @Test
    public void testSupportedDevice_OxygenOS214() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("opx", "2.1.4", "OnePlus X", "OnePlus", NO_OXYGEN_OS, "OnePlusOxygen_14_201512161752", "OnePlusOxygen_14.A.07_GLO_007_201512161752"));
    }

    @Test
    public void testSupportedDevice_OxygenOS223() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("opx", "2.2.3", "OnePlus X", "OnePlus", NO_OXYGEN_OS, "OnePlusOxygen_14_201609012031", "OnePlusXOxygen_14.A.13_GLO_013_201609012031"));
    }

    @Test
    public void testSupportedDevice_OxygenOS314() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        Assert.assertTrue(isSupportedDevice("opx", "3.1.4", "OnePlus X", "OnePlus", NO_OXYGEN_OS, "OnePlusXOxygen_14_201611071506", "OnePlusXOxygen_14.A.19_GLO_019_201611071506"));
    }
    
}
