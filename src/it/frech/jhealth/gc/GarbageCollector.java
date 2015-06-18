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

import java.util.HashMap;
import java.util.Map;

public enum GarbageCollector {
	//Throughput (default)
	SCAVENGE("PS Scavenge",false),
	MARKSWEEP("PS MarkSweep",true),
	//CMS
	PARNEW("ParNew",false),
	CMS("ConcurrentMarkSweep",true),
	//G1
	G1YOUNG("G1 Young Generation",false),
	G1OLD("G1 Old Generation",true),
	//Serial
	COPY("Copy",false),
	MARKSWEEPCOMPACT("MarkSweepCompact",true);
	

	private final String name;
	private boolean old;
	private static final Map<String,GarbageCollector> name2gc = new HashMap<String, GarbageCollector>(); 

	static {
		for (GarbageCollector gc : GarbageCollector.values()) {
			name2gc.put(gc.getName(), gc);
		}
	}
	public static GarbageCollector getByName(String name) {
		return name2gc.get(name);
	}
	
	GarbageCollector(String name, boolean isOld) {
		this.name = name;
		this.old = isOld;
	}
	
	public String getName() {
		return name;
	}
	public boolean isOld() {
		return old;
	}
}
