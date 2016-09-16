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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import javax.management.MBeanServer;

class CollectorAcceptingThread extends Thread {

	private boolean keepRunning;

	ServerSocket socket;
	
	String [] permittedIPs;

	// use a *synchronized* Map since multiple Threads may access it
	private Map<String, Long> lastValueMap = new java.util.Hashtable<String, Long>();

	private MBeanServer mbeanServer;

	public CollectorAcceptingThread(String permittedIPs) {
		setName("Collector MBean Accept-Thread");
		setDaemon(true);
		if (permittedIPs != null){
			this.permittedIPs = permittedIPs.split("&");
		}
	}

	@Override
	public void run() {
		keepRunning = true;
		while (keepRunning) {
			acceptRequests();
		}
	}

	private void acceptRequests() {
		
		if (permittedIPs != null){
		
			try {
				Socket reqSocket = socket.accept();
				boolean isPermitted = false;
				for (String ip :permittedIPs){
					if (reqSocket.getInetAddress().getHostAddress().equals(ip)){
						isPermitted = true;
					}
				}
				if (isPermitted){
				CollectorRequestHandlingThread thread = new CollectorRequestHandlingThread(reqSocket,lastValueMap, getMBeanServer());
				thread.setName("Collector request "+reqSocket.getRemoteSocketAddress());
				thread.setDaemon(true);
				thread.start();
				}else{
					reqSocket.close();
				}
			} catch (IOException e) {
				// the socket could have been closed -> ok
			}catch (SecurityException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	

	private MBeanServer getMBeanServer() {
		if (mbeanServer == null) {
			mbeanServer = ManagementFactory.getPlatformMBeanServer();
		}
		return mbeanServer;
	}

	public void stopThread() {
		keepRunning = false;
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				socket = null;
			}
		}
	}
}