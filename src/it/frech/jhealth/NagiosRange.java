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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NagiosRange {

	private long min;
	private long max;
	private boolean negate;

	public NagiosRange(String s) {
		Pattern pattern = Pattern.compile("([@~:]?)(-?\\d*)(:?)(-?\\d*)");
		Matcher matcher = pattern.matcher(s);
		if (!matcher.matches())
			throw new IllegalArgumentException();
		String prefix = matcher.group(1);
		String first = matcher.group(2);
		String colon = matcher.group(3);
		String second = matcher.group(4);

		negate = "@".equals(prefix);
		
		if ("~".equals(prefix)) {
			min = Long.MIN_VALUE;
			max = Long.parseLong(second);
		} else {
			if (":".equals(colon)) {
				if (second.length() > 0) {
					min = Long.parseLong(first);
					max = Long.parseLong(second);
				} else {
					min = Long.parseLong(first);
					max = Long.MAX_VALUE;
				}
			} else {
				min = 0;
				max = Long.parseLong(first);
			}
		}
	}

	public boolean contains(long i) {
		return (i >= min && i <= max) ^ negate;
	}

}
