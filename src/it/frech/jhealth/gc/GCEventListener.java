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

import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.NotificationListener;

final class GCEventListener implements NotificationListener {
		
		@Override
		public void handleNotification(Notification notification, Object handback) {
			@SuppressWarnings("unchecked")
			Deque<AtomicLong> counterList = (Deque<AtomicLong>) handback;
			AtomicLong currentCounter = counterList.getFirst();
			currentCounter.incrementAndGet();
			
//			System.out.println("Notification: ["+notification.getSequenceNumber()+"] "+notification.getType()+": "+notification.getMessage());
			
//					Object userData = notification.getUserData();
//					if (userData instanceof CompositeData) {
//						CompositeData data = (CompositeData) userData;
//						CompositeType compositeType = data.getCompositeType();
//						System.out.println(compositeType.getTypeName());
//						Set<String> keySet = compositeType.keySet();
//						for (String key : keySet) {
//							
//						}

//						Collection<?> values = data.values();
				
//					}
		}
	}