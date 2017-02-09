/* Copyright 2015 Tobias Frech IT GmbH

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package it.frech.jhealth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;


public class CollectorRequestHandlingThread extends Thread {

	private static final String RESULT_WARNING = "WARNING ";
	private static final String RESULT_CRITICAL = "CRITICAL ";
	private static final String RESULT_OK = "OK ";
	private static final String RESULT_UNKNOWN = "UNKNOWN ";
	private static final String RESULT_NOTFOUND = "NOTFOUND";

	private static final String SPACE_REGEX = "\\+"; // ein Plus-Zeichen
	private static final String SEP_REGEX = " +"; // 1 bis n Leerzeichen

	private final Map<String, Long> lastValueMap;
	private final Socket reqSocket;
	private MBeanServer mbeanServer;

	public CollectorRequestHandlingThread(Socket socket, Map<String, Long> lastValueMap, MBeanServer mbeanServer) {
		this.reqSocket = socket;
		this.lastValueMap = lastValueMap;
		this.mbeanServer = mbeanServer;
	}

	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(reqSocket.getInputStream()));
			PrintWriter out = new PrintWriter(reqSocket.getOutputStream());
			String request = in.readLine();
			if (validRequest(request)) {
				String[] params = request.split(SEP_REGEX);

				try {
					String mbeanName = restoreWhitespaceAndReplaceVariables(params[0]);
					String attributeName = restoreWhitespaceAndReplaceVariables(params[1]);
					String warningCondition = params[2];
					String critialCondition = params[3];

					boolean singleton = false;
					if (mbeanName.startsWith("?") && mbeanName.length() > 1) {
						mbeanName = mbeanName.substring(1);
						singleton = true;
					}

					boolean diff = false;
					if (attributeName.startsWith("*") && attributeName.length() > 1) {
						attributeName = attributeName.substring(1);
						diff = true;
					}

					boolean size = false;
					if (attributeName.endsWith("[]") && attributeName.length() > 2) {
						attributeName = attributeName.substring(0, attributeName.length() - 2);
						size = true;
					}

					ObjectName objectName = new ObjectName(mbeanName);
					// invoking getMBeanInfo() works around a bug in getAttribute() that fails to
					// refetch the domains from the platform (JDK) bean server
					try {
						mbeanServer.getMBeanInfo(objectName);
					} catch (InstanceNotFoundException ex) {
						if (singleton) {
							// this is a check for a cluster singleton, which is not present on this node
							sendResult(out, attributeName, RESULT_NOTFOUND, null, null);
						} else {
							sendResult(out, attributeName, RESULT_UNKNOWN,
									"MBean " + mbeanName + " could not be found", null);
						}
						out.flush();
						return;
					}
					
					Object attribute = MBeanUtil.readAttribute(mbeanServer, objectName, attributeName);

					if (attribute instanceof Number) {
						long value = ((Number) attribute).longValue();
						NagiosRange nonCriticalRange = new NagiosRange(critialCondition);
						NagiosRange nonWarningRange = new NagiosRange(warningCondition);

						String key = mbeanName.replaceAll("=", "_") + ":" + attributeName;
						if (diff) {
							Long lastValue = lastValueMap.get(key);
							lastValueMap.put(key, value);

							if (lastValue != null) {
								value = value - lastValue;
							}
						}
						String code = RESULT_OK;
						if (!nonCriticalRange.contains(value)) {
							code = RESULT_CRITICAL;
						} else if (!nonWarningRange.contains(value)) {
							code = RESULT_WARNING;
						}
						String resType = diff ? "difference" : "value";
						sendResult(out, attributeName, code, "JMX attribute " + attributeName + " has "+resType+ " " + value,
								key + "=" + value);
					} else if (attribute instanceof Collection && size) {
						@SuppressWarnings("rawtypes")
						int colSize = ((Collection) attribute).size();
						NagiosRange nonCriticalRange = new NagiosRange(critialCondition);
						NagiosRange nonWarningRange = new NagiosRange(warningCondition);

						String code = RESULT_OK;
						if (!nonCriticalRange.contains(colSize)) {
							code = RESULT_CRITICAL;
						} else if (!nonWarningRange.contains(colSize)) {
							code = RESULT_WARNING;
						}
						sendResult(out, attributeName, code, attributeName + " has " + colSize + " entries",
								attributeName + "=" + colSize);
					} else {
						String attrText = attribute.toString();
						String code = RESULT_OK;
						if (attrText.contains(critialCondition)) {
							code = RESULT_CRITICAL;
						} else if (attrText.contains(warningCondition)) {
							code = RESULT_WARNING;
						}
						sendResult(out, attributeName, code, attributeName + " is " + attrText, null);
					}
				} catch (Exception e) {
					sendResult(out, "?", RESULT_UNKNOWN, "MBeanServer responded: " + e.getClass().getSimpleName() + " "
							+ e.getMessage(), null);
				}
			} else {
				sendResult(out, "?", RESULT_UNKNOWN, "request not valid", null);
			}
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				reqSocket.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void sendResult(PrintWriter out, String attributeName, String code, String message, String perfData) {
		String result = code + attributeName.toUpperCase() + " " + code;
		if (message != null) {
			result += "- " + message;
		}
		if (perfData != null) {
			result += "|" + perfData;
		}
		out.print(result);
	}

	private String restoreWhitespaceAndReplaceVariables(String mbeanName) {
		String whitespaceRestored = mbeanName.replaceAll(SPACE_REGEX, " ");
		return PropertyReplacer.replaceProperties(whitespaceRestored);
	}

	private boolean validRequest(String request) {
		if (request == null) {
			return false;
		}
		String[] strings = request.split(SEP_REGEX);
		return strings.length == 4;
	}
}
