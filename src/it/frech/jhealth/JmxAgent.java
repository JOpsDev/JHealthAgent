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

import it.frech.jhealth.gc.GCEventThread;

import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.Properties;

public class JmxAgent {
	public static void premain(String agentArgs) {

		final LinkedList<Runnable> shutdownTasks = new LinkedList<Runnable>();

		int port = 0;
		String path = null;
		int delay = 5000;
		String format = "$TIME{yyyy-MM-dd HH:mm:ss};minorGcCount=$jhealth:type=YoungGC{count-2};majorGcCount=$jhealth:type=TenuredGC{count-2};threadCount=$java.lang:type=Threading{ThreadCount}";
		
		String sysProp = System.getProperty(Constants.PORT_PROPERTY);
		if (sysProp != null) {
			port = Integer.parseInt(sysProp);
		}
		
		sysProp = System.getProperty(Constants.PATH_PROPERTY);
		if (sysProp != null) {
			path = sysProp;
		}
		
		
		if (agentArgs != null) {
			String[] args = agentArgs.split(",");
			// preprocess the config file so that other javaagent-options overwrite this
			for (String arg : args) {
				if (arg.startsWith("config=")) {
					String config = arg.substring(7);
					Properties props = new Properties();
					try {
						props.load(new FileReader(config));
						String p;
						p = props.getProperty(Constants.PORT_PROPERTY);
						if (p != null) {
							port = Integer.parseInt(p);
						}
						p = props.getProperty(Constants.PATH_PROPERTY);
						if (p != null) {
							path = p;
						}
						p = props.getProperty(Constants.FORMAT_PROPERTY);
						if (p != null) {
							format = p;
						}
						p = props.getProperty(Constants.DELAY_PROPERTY);
						if (p != null) {
							delay = Integer.parseInt(p);
						}
					} catch (IOException e) {
						e.printStackTrace(System.err);
					}
				}
			}
			
			for (String arg : args) {
				if (arg.startsWith("port=")) {
					port = Integer.parseInt(arg.substring(5));
					continue;
				}
				if (arg.startsWith("path=")) {
					path = arg.substring(5);
					continue;
				}
				if (arg.startsWith("delay=")) {
					delay = Integer.parseInt(arg.substring(6));
					continue;
				}
				if (arg.startsWith("config=")) {
					// skip, already processed
					continue;
				}
				System.err.println("JHealthAgent: Could not parse option '"+arg+"'. Ignoring it.");
			}
		}


		if (port > 0) {
			try {
				final ServerSocket serverSocket = new ServerSocket(port);
				shutdownTasks.add(new Runnable() {
					@Override
					public void run() {
						try {
							serverSocket.close();
						} catch (IOException e) {
							// winding down, don't know why I should care here
						}
					}
				});

				final CollectorAcceptingThread thread = new CollectorAcceptingThread();
				thread.socket = serverSocket;
				thread.start();

				shutdownTasks.addFirst(new Runnable() {
					@Override
					public void run() {
						thread.stopThread();
					}
				});

			} catch (IOException e) {
				System.err.println("JHealthAgent: Could not create listening socket on port " + port + ": " + e.getMessage());
			}
		}

		final GCEventThread gcEventThread = new GCEventThread(delay, path, format);
		gcEventThread.setDaemon(true);
		gcEventThread.start();

		shutdownTasks.add(new Runnable() {
			@Override
			public void run() {
				gcEventThread.stopThread();
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				for (Runnable runnable : shutdownTasks) {
					try {
						runnable.run();
					} catch (Throwable t) {
						t.printStackTrace(System.err);
					}
				}
			}
		});

	}

}
