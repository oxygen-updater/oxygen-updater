package com.arjanvlek.oxygenupdater.domain;

import org.junit.Assert;
import org.junit.Test;

public class SystemVersionPropertiesTestOnePlus6 extends SystemVersionPropertiesTest {

    @Test
    public void testSupportedDevice_OxygenOS904() {
        Assert.assertTrue(isSupportedDevice("op6", "9.0.4", "OnePlus 6", "OnePlus 6", "OnePlus6", "9.0.4", "OnePlus6Oxygen_22.O.29_GLO_029_1901231504", true));
    }
    
}
