package it.frech.jhealth;

import static org.junit.Assert.*;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.junit.Before;
import org.junit.Test;

public class AttributeAccessTest {

	private MBeanServer mbeanServer;

	@Before
	public void setUp() throws Exception {
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
	}

	@Test
	public void testSimpleAccess() {
		String mbeanName = "java.lang:type=Runtime";
		String attributeName = "StartTime";
		Object attribute = MBeanUtil.readAttribute(mbeanServer,mbeanName,attributeName);
		assertNotNull(attribute);
		assertTrue(attribute instanceof Number);
		long value = ((Number) attribute).longValue();
		assertTrue(value > 0);
	}

	@Test
	public void testCompositeAccess() {
		String mbeanName = "java.lang:type=Memory";
		String attributeName = "HeapMemoryUsage/used";
		Object attribute = MBeanUtil.readAttribute(mbeanServer,mbeanName,attributeName);
		assertNotNull(attribute);
		assertTrue(attribute instanceof Number);
		long value = ((Number) attribute).longValue();
		assertTrue(value > 0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCompositeError() {
		String mbeanName = "java.lang:type=Memory";
		String attributeName = "HeapMemoryUsageDoesNotExist/used";
		MBeanUtil.readAttribute(mbeanServer,mbeanName,attributeName);
	}
	@Test
	public void testNotCompositeType() {
		String mbeanName = "java.lang:type=Memory";
		String attributeName = "ObjectPendingFinalizationCount/used";
		Object attribute = MBeanUtil.readAttribute(mbeanServer,mbeanName,attributeName);
		assertNotNull(attribute);
		assertFalse(attribute instanceof Number);
		assertTrue(attribute.toString().contains("CompositeData"));
	}

}
