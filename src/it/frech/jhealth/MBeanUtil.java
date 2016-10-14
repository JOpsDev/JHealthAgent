package it.frech.jhealth;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

public class MBeanUtil {

	public static Object readAttribute(MBeanServer mbeanServer, String mbeanName, String attributeName)
			throws IllegalArgumentException {

		try {
			ObjectName objectName = new ObjectName(mbeanName);
			return readAttribute(mbeanServer, objectName, attributeName);
		} catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}

	}

	public static Object readAttribute(MBeanServer mbeanServer, ObjectName objectName, String attributeName) throws IllegalArgumentException {
		try {
			int slashPos = attributeName.indexOf("/");
			Object attribute;
			if (slashPos < 0) {
				attribute = mbeanServer.getAttribute(objectName, attributeName);
			} else {
				String prefix = attributeName.substring(0, slashPos);
				attribute = mbeanServer.getAttribute(objectName, prefix);
				if (attribute instanceof CompositeData) {
					CompositeData data = (CompositeData) attribute;
					attribute = data.get(attributeName.substring(slashPos + 1));
				} else {
					attribute = "No CompositeData found on attribute named " + prefix + ". Type is "
							+ attribute.getClass().getName();
				}
			}
			return attribute;
		} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
