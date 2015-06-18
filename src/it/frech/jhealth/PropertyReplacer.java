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

import java.io.File;

public class PropertyReplacer {
	public static final String NEWLINE = System.getProperty("line.separator",
			"\n");
	private static final String FILE_SEPARATOR = File.separator;
	private static final String PATH_SEPARATOR = File.pathSeparator;
	private static final String FILE_SEPARATOR_ALIAS = "/";
	private static final String PATH_SEPARATOR_ALIAS = ":";
	private static final int NORMAL = 0;
	private static final int SEEN_DOLLAR = 1;
	private static final int IN_BRACKET = 2;

	public static String replaceProperties(final String string) {
		final char[] chars = string.toCharArray();
		StringBuffer buffer = new StringBuffer();
		boolean properties = false;
		int state = NORMAL;
		int start = 0;
		for (int i = 0; i < chars.length; ++i) {
			char c = chars[i];

			if (c == '$' && state != IN_BRACKET)
				state = SEEN_DOLLAR;

			else if (c == '{' && state == SEEN_DOLLAR) {
				buffer.append(string.substring(start, i - 1));
				state = IN_BRACKET;
				start = i - 1;
			}

			else if (state == SEEN_DOLLAR)
				state = NORMAL;

			else if (c == '}' && state == IN_BRACKET) {
				if (start + 2 == i) {
					buffer.append("${}");
				} else {
					String value = null;

					String key = string.substring(start + 2, i);

					if (FILE_SEPARATOR_ALIAS.equals(key)) {
						value = FILE_SEPARATOR;
					} else if (PATH_SEPARATOR_ALIAS.equals(key)) {
						value = PATH_SEPARATOR;
					} else {
						value = System.getProperty(key);

						if (value == null) {
							int colon = key.indexOf(':');
							if (colon > 0) {
								String realKey = key.substring(0, colon);
								value = System.getProperty(realKey);

								if (value == null) {
									value = resolveCompositeKey(realKey);
									if (value == null)
										value = key.substring(colon + 1);
								}
							} else {
								value = resolveCompositeKey(key);
							}
						}
					}

					if (value != null) {
						properties = true;
						buffer.append(value);
					}
				}
				start = i + 1;
				state = NORMAL;
			}
		}

		if (properties == false)
			return string;

		if (start != chars.length)
			buffer.append(string.substring(start, chars.length));
		return buffer.toString();
	}

	private static String resolveCompositeKey(String key) {
		String value = null;

		int comma = key.indexOf(',');
		if (comma > -1) {
			if (comma > 0) {
				String key1 = key.substring(0, comma);
				value = System.getProperty(key1);
			}
			if (value == null && comma < key.length() - 1) {
				String key2 = key.substring(comma + 1);
				value = System.getProperty(key2);
			}
		}
		return value;
	}

}
