package com.arjanvlek.oxygenupdater.domain;

import org.junit.Assert;
import org.junit.Test;

public class SystemVersionPropertiesTestOnePlus5 extends SystemVersionPropertiesTest {

    @Test
    public void testSupportedDevice_OxygenOS4514() {
        Assert.assertTrue(isSupportedDevice("op5", "4.5.14", "OnePlus 5", "OnePlus 5", "OnePlus5", "4.5.14", "OnePlus5Oxygen_23.O.19_GLO_019_1710311604", false));
    }

    @Test
    public void testSupportedDevice_OxygenOS50b1() {
        // This is still a beta, as OOS 5.0 is not available as of 2017/12/7
        Assert.assertTrue(isSupportedDevice("op5", "5.0.b1", "OnePlus 5", "OnePlus 5", "OnePlus5", "OP5_O2_Open_1", "OnePlus5Oxygen_23.X.01_GLO_001_1711250301", false));
    }

    
}
