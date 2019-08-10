package com.arjanvlek.oxygenupdater.domain;

import org.junit.Assert;
import org.junit.Test;

public class SystemVersionPropertiesTestOnePlus2 extends SystemVersionPropertiesTest {

	@Test
	public void testSupportedDevice_OxygenOS200() {
		// Not supported in app versions 1.2.x - 2.2.x, but has been fixed in App 2.3.x
		Assert.assertTrue(isSupportedDevice("op2", "2.0.0", "OnePlus2", Companion.getNO_OXYGEN_OS(), "OnePlus2", "OnePlus2Oxygen_14_1507251956", "OnePlus2Oxygen_14.A.02_GLO_002_1507251956", false));
		Assert.assertEquals("OnePlus 2", getSupportedDevice("op2", "2.0.0").getName());
	}

	@Test
	public void testSupportedDevice_OxygenOS310() {
		// Neat OS display version is not present in the firmware, but that's not an issue.
		Assert.assertTrue(isSupportedDevice("op2", "3.1.0", "OnePlus2", "OnePlus2", "OnePlus2", "OnePlus2Oxygen_14_1608262242", "OnePlus2Oxygen_14.A.20_GLO_020_1608262242", false));
		Assert.assertEquals("OnePlus 2", getSupportedDevice("op2", "3.1.0").getName());
	}

	@Test
	public void testSupportedDevice_OxygenOS361() {
		// Neat OS display version is not present in the firmware, but that's not an issue.
		Assert.assertTrue(isSupportedDevice("op2", "3.6.1", "OnePlus2", "OnePlus2", "OnePlus2", "OnePlus2Oxygen_14_1710240102", "OnePlus2Oxygen_14.A.32_GLO_032_1710240102", false));
		Assert.assertEquals("OnePlus 2", getSupportedDevice("op2", "3.6.1").getName());
	}
}
