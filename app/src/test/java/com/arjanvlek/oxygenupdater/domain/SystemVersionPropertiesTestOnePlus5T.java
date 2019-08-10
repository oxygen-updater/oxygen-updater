package com.arjanvlek.oxygenupdater.domain;

import org.junit.Assert;
import org.junit.Test;

public class SystemVersionPropertiesTestOnePlus5T extends SystemVersionPropertiesTest {

	@Test
	public void testSupportedDevice_OxygenOS474() {
		Assert.assertTrue(isSupportedDevice("op5t", "4.7.4", "OnePlus 5T", "OnePlus 5T", "OnePlus5T", "4.7.4", "OnePlus5TOxygen_43.O.06_GLO_006_1712062242", false));
		Assert.assertEquals("OnePlus 5T", getSupportedDevice("op5t", "4.7.4").getName());
	}

}
