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
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class GCEventThread extends Thread {

	private Deque<AtomicLong> youngGcCounter = new ConcurrentLinkedDeque<AtomicLong>();
	private Deque<AtomicLong> tenuredGcCounter = new ConcurrentLinkedDeque<AtomicLong>();

	private long initialDelay;
	private boolean running = true;
	private String path;
	private long lastException;
	

	public GCEventThread(long initialDelay, String path) {
		super("JHealthAgent - GC Event Thread");
		this.initialDelay = initialDelay;
		this.path = path;

		for (int i = 0; i < 10; i++) {
			youngGcCounter.add(new AtomicLong(0));
			tenuredGcCounter.add(new AtomicLong(0));
		}
	}

	@Override
	public void run() {
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

		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		TimeZone tz = TimeZone.getTimeZone("UTC");
	    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
	    df.setTimeZone(tz);
		
		
		// from now on remove the last counter at the end and add a new one at the front
		while (running) {
			try {
				Thread.sleep(60 * 1000);
			} catch (InterruptedException e) {
			}

			if (path != null) {
				try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path, true)))) {
					StringBuilder s = new StringBuilder();

					//Time
				    s.append(df.format(new Date()));

				    // young GC
					long young = 0;
					Iterator<AtomicLong> iter = youngGcCounter.iterator();
					for (int i = 0; i < 2; i++) {
						young += iter.next().longValue();
					}
					s.append("|youngGcCount=" + young);
					// tenured GC
					long tenured = 0;
					iter = youngGcCounter.iterator();
					for (int i = 0; i < 2; i++) {
						tenured += iter.next().longValue();
					}
					s.append("|tenuredGcCount=" + tenured);
					
					// Thread count
					s.append("|threadCount=" + threadMXBean.getThreadCount() );
					

					out.println(s.toString());
				} catch (IOException e) {
					logException(e);
				}
			}

//			String s = "Young ";
//			for (Iterator<AtomicLong> iter = youngGcCounter.iterator(); iter.hasNext();) {
//				s += "[" + iter.next().toString() + "]";
//			}
//			s += "\nTenured ";
//			for (Iterator<AtomicLong> iter = tenuredGcCounter.iterator(); iter.hasNext();) {
//				s += "[" + iter.next().toString() + "]";
//			}
//			System.err.println(s);

			youngGcCounter.removeLast();
			youngGcCounter.addFirst(new AtomicLong(0));
			tenuredGcCounter.removeLast();
			tenuredGcCounter.addFirst(new AtomicLong(0));
		}
	}

	private void logException(IOException e) {
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
