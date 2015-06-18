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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;

public class JmxAgent {
	public static void premain(String agentArgs) {

		final LinkedList<Runnable> shutdownTasks = new LinkedList<Runnable>();

		int port = 0;
		String path = null;
		
		if (agentArgs != null) {
			String[] args = agentArgs.split(",");
			for (String arg : args) {
				if (arg.startsWith("port=")) {
					port = Integer.parseInt(arg.substring(5));
					continue;
				}
				if (arg.startsWith("path=")) {
					path = arg.substring(5);
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

		// final LoggerThread logThread = new LoggerThread(System.out);
		// logThread.setDaemon(true);
		// logThread.start();
		// shutdownTasks.add(new Runnable() {
		// @Override
		// public void run() {
		// logThread.stopThread();
		// }
		// });

		final GCEventThread gcEventThread = new GCEventThread(2000, path);
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
