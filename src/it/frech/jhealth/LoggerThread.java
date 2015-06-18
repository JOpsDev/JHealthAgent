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

import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.List;

import javax.management.MBeanServer;

public class LoggerThread extends Thread {

	private static final int LOG_INTERVALL_SECS = 15;
	private boolean running = true;
	private long lastLogMillis;
	private List<GarbageCollectorMXBean> mxBeans;
	private PrintStream stream;
	

	public LoggerThread(PrintStream stream) {
		super("JHealthAgent Logger Thread");
		this.stream = stream;
		mxBeans = ManagementFactory.getGarbageCollectorMXBeans();
		
	}
	
	@Override
	public void run() {
		while (running) {
		
			long currentTimeMillis = System.currentTimeMillis();
			if ((currentTimeMillis - lastLogMillis) > LOG_INTERVALL_SECS*1000) {
				StringBuilder s = new StringBuilder();
				s.append(new Date(currentTimeMillis).toString());
				
				
				for (GarbageCollectorMXBean garbageCollectorMXBean : mxBeans) {
					s.append(",");
					s.append(""+garbageCollectorMXBean.getName()+"|c:"+garbageCollectorMXBean.getCollectionCount()+"|t:"+garbageCollectorMXBean.getCollectionTime());
				}
				stream.println(s);
				lastLogMillis = currentTimeMillis;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		
	}

	public void stopThread() {
		running = false;
	}

}
