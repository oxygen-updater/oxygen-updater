package com.arjanvlek.oxygenupdater.domain;

import org.junit.Assert;
import org.junit.Test;

public class SystemVersionPropertiesTestOnePlus6TGlobal extends SystemVersionPropertiesTest {

    @Test
    public void testSupportedDevice_OxygenOS9012() {
        Assert.assertTrue(isSupportedDevice("op6t", "9.0.12", "OnePlus 6T", "OnePlus 6T", "OnePlus6T", "9.0.12", "OnePlus6TOxygen_34.O.19_GLO_019_1901231347", true));
        Assert.assertEquals("OnePlus 6T (Global)", getSupportedDevice("op6t", "9.0.12").getName());
    }
    
}
