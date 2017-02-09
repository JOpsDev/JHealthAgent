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
package it.frech.jhealth.gc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class GCEventThread extends Thread {

	private static final String DOMAIN_START_SIGN = "$";

	private static final String DOMAIN_REGEX = "\\" + DOMAIN_START_SIGN + "(.+?)\\{(.+?\\}{0,})(\\})*\\}";

	private static final String DOMAIN_TIME = "TIME";
	private static final String DOMAIN_SYSPROP = "SYSPROP";
	private static final String DOMAIN_JHEALTH_YOUNG_GC = "jhealth:type=YoungGC";
	private static final String DOMAIN_JHEALTH_TENURED_GC = "jhealth:type=TenuredGC";

	private static final String SUB_DOMAIN_TIMEZONE = "TIMEZONE";

	private static final String VALUE_NOT_AVAILABLE = "[n/a]";

	private Deque<AtomicLong> youngGcCounter = new ConcurrentLinkedDeque<AtomicLong>();
	private Deque<AtomicLong> tenuredGcCounter = new ConcurrentLinkedDeque<AtomicLong>();

	private long initialDelay;
	private boolean running = true;
	private String path;
	private long lastException;
	private String format;

	public GCEventThread(long initialDelay, String path, String format) {
		super("JHealthAgent - GC Event Thread");
		this.initialDelay = initialDelay;
		this.path = path;
		this.format = format;

		for (int i = 0; i < 10; i++) {
			youngGcCounter.add(new AtomicLong(0));
			tenuredGcCounter.add(new AtomicLong(0));
		}
	}

	@Override
	public void run() {

		// touch the output file
		if (path != null) {
			try {
				FileWriter fileWriter = new FileWriter(path, true);
				fileWriter.close();
			} catch (IOException e) {
				logException(e);
			}
		}

		// wait to make sure early construction of the MBean server does not
		// interfere with other software
		try {
			Thread.sleep(initialDelay);
		} catch (InterruptedException e) {
		}

		// loop through all garbage collectors and register to their event
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();

		for (GarbageCollectorMXBean gcMxBean : gcMxBeans) {
			String gcName = gcMxBean.getName();
			GarbageCollector gc = GarbageCollector.getByName(gcName);
			if (gc == null) {
				System.err.println("Garbage Collector '" + gcName + "' unknown so far, ignoring it!");
				continue;
			}

			// register the code to increase the counter on the MBean
			ObjectName objectName = gcMxBean.getObjectName();
			NotificationListener listener = new GCEventListener();
			Deque<AtomicLong> counterList = gc.isOld() ? tenuredGcCounter : youngGcCounter;
			try {
				mBeanServer.addNotificationListener(objectName, listener, null, counterList);
			} catch (InstanceNotFoundException e) {
				e.printStackTrace(System.err);
			}
		}


		// from now on remove the last counter at the end and add a new one at the front
		while (running) {
			try {
				Thread.sleep(60 * 1000);
			} catch (InterruptedException e) {
			}

			if (path != null) {
				try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path, true)))) {
				    Matcher matcher = createDomainMatcher(this.format);
					StringBuffer sb = new StringBuffer();
					while(matcher.find()) {
						String domain = matcher.group(1);
						String attribute = matcher.group(2);
						String replacement = VALUE_NOT_AVAILABLE;
						switch (domain) {
							case DOMAIN_TIME:
								replacement = getTime(attribute);
								break;
							case DOMAIN_SYSPROP:
								replacement = System.getProperty(attribute);
								break;
							case DOMAIN_JHEALTH_YOUNG_GC:
								replacement = getJHealthDomain(youngGcCounter,attribute);
								break;
							case DOMAIN_JHEALTH_TENURED_GC:
								replacement = getJHealthDomain(tenuredGcCounter,attribute);
								break;
							default:
								// other MBean
								Object value;
								try {
									value = mBeanServer.getAttribute(new ObjectName(domain), attribute);
									replacement = value.toString();
								} catch (AttributeNotFoundException | InstanceNotFoundException
										| MalformedObjectNameException | MBeanException | ReflectionException e) {
									logException(e);
									replacement = VALUE_NOT_AVAILABLE;
								}
								break;
						}
						matcher.appendReplacement(sb,replacement);
					}
					matcher.appendTail(sb);

					out.println(sb.toString());
				} catch (IOException e) {
					logException(e);
				}
			}

			// String s = "Young ";
			// for (Iterator<AtomicLong> iter = youngGcCounter.iterator(); iter.hasNext();) {
			// s += "[" + iter.next().toString() + "]";
			// }
			// s += "\nTenured ";
			// for (Iterator<AtomicLong> iter = tenuredGcCounter.iterator(); iter.hasNext();) {
			// s += "[" + iter.next().toString() + "]";
			// }
			// System.err.println(s);

			youngGcCounter.removeLast();
			youngGcCounter.addFirst(new AtomicLong(0));
			tenuredGcCounter.removeLast();
			tenuredGcCounter.addFirst(new AtomicLong(0));
		}
	}

	private Matcher createDomainMatcher(String searchBase){
		Pattern pattern = Pattern.compile(DOMAIN_REGEX);
		return pattern.matcher(searchBase);
	}

	private String getTime(String attributeForTime){
		Matcher subDomainMatcher = createDomainMatcher(attributeForTime);
		String formattedTime = VALUE_NOT_AVAILABLE;

		try{
			SimpleDateFormat sdf = null;

			if( subDomainMatcher.find() ){
				String subDomainName = subDomainMatcher.group(1);
				String attribute = subDomainMatcher.group(2);
				if( SUB_DOMAIN_TIMEZONE.equals( subDomainName )){
				    // remove the timezone-part from the original incoming value
				    // NOTE: we intentionally invoke trim() here so that we can use blank between value and sub-domain
				    // for better readability
				    // e.g. given: $TIME{dd.MM.yyyy HH:mm:ss $TIMEZONE{Europe/Berlin}}
				    // => blank between '...:ss $TIMEZONE{...'
					attributeForTime = subDomainMatcher.replaceFirst("").trim();
					sdf = new SimpleDateFormat(attributeForTime);
					sdf.setTimeZone(TimeZone.getTimeZone(attribute));
				}
			} else {
			    sdf = new SimpleDateFormat(attributeForTime);
			}

			formattedTime = sdf.format(new Date());
		} catch( Exception e ){
			logException(e);
		}

		return formattedTime;
	}

	private String getJHealthDomain(Deque<AtomicLong> gcCounter, String attribute) {
		if (attribute.startsWith("count-")) {
			int count = Integer.parseInt(attribute.substring(6));
			long sum = 0;
			Iterator<AtomicLong> iter = gcCounter.iterator();
			for (int i = 0; i < count; i++) {
				sum += iter.next().longValue();
			}
			return Long.toString(sum);
		}
		return VALUE_NOT_AVAILABLE;

	}

	private void logException(Exception e) {
		long millis = System.currentTimeMillis();
		if (millis - lastException > 1000 * 60 * 60) { // once each hour
			lastException = millis;
			e.printStackTrace(System.err);
		}
	}

	public void stopThread() {
		running = false;
	}
}
