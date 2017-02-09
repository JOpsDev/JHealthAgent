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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class FullGCTriggerTest {

	private static final double CHUNK_RATIO = .2;
	private static final double KEEP_RATIO = .8;
	private static final int MAX_CHUNKS = (int) (1 / CHUNK_RATIO * KEEP_RATIO);

	private static Random random = new Random();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("chunk ratio: " + CHUNK_RATIO
				+ " / max no. of chunks: " + MAX_CHUNKS);
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

		MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
		long max = heapMemoryUsage.getMax();

		Map<Long, String> map = new TreeMap<Long, String>();

		LinkedList<Object> list = new LinkedList<Object>();

		long index = 0;
		long maxIndex = Long.MAX_VALUE;

		boolean measuring = true;

		while (true) {
			String s = concatSomeStuff();
			map.put(index++, s);
//			map.remove(index / 2);
//			map.remove(index / 3);
//			map.remove(index / 5);

			if (measuring && index % 20 == 0) {

				heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

				if (measuring && heapMemoryUsage.getUsed() > max * CHUNK_RATIO) {
					measuring = false;
					maxIndex = index;
					System.out.println("MaxIndex is: " + maxIndex);

				}
			}
			if (index > maxIndex) {
				heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

				list.addFirst(map);
				if (list.size() >= MAX_CHUNKS) {
					list.removeLast();
				}

				index = 0;
				map = new HashMap<Long, String>();
				System.out.println("Starting new map, usage: "
						+ heapMemoryUsage.getUsed() / 1024 / 1024
						+ " MB  committed:" + heapMemoryUsage.getCommitted()
						/ 1024 / 1024 + " MB  max:" + heapMemoryUsage.getMax()
						/ 1024 / 1024 + " MB");
			}
		}
	}

	private static String concatSomeStuff() {
		int i = random.nextInt();
		long l = random.nextLong();
		double d = random.nextGaussian();
		String s = "I" + i + Integer.toOctalString(i)
				+ Integer.toBinaryString(i) + "L" + l + Long.toHexString(l)
				+ Long.toBinaryString(l) + "D" + d + Double.toHexString(d);
		
//		for (int j = 0; j < ; j++) {
//			s += s;
//		}

		if ((System.currentTimeMillis()/1000)%10 != 0) {
			for (int j = 0; j < 10; j++) {
				s += s;
			}
		}
		
		return s;
	}
}
