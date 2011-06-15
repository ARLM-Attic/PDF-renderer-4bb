/*
 * File: Properties.java
 * Version: 1.0
 * Initial Creation: May 15, 2010 10:45:29 PM
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.pdfview.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Hashtable;

/**
 * Partial implementation of java.util.Properties
 * @author Vincent Simonetti
 */
public class Properties extends Hashtable
{
	private static final int NONE = 0, SLASH = 1, UNICODE = 2, CONTINUE = 3, KEY_DONE = 4, IGNORE = 5;
	
	/**
	 * Create a new Properties instance.
	 */
	public Properties()
	{
		super();
	}
	
	/**
	 * Loads properties from the specified {@code InputStream}.
	 * @param in The {@code InputStream}.
	 * @throws IOException If error occurs during reading from the {@code InputStream}.
	 */
	public synchronized void load(InputStream in) throws IOException
	{
		if (in == null)
		{
			throw new NullPointerException();
		}
		//TODO: Should a check be done to see if Unicode is used (like in the original source code) or will everything be handled by the default encoding?
		load(new InputStreamReader(in));
	}
	
	/**
	 * Loads properties from the specified InputStream. The properties are of the form <code>key=value</code>, one property per line. It may be not encode 
	 * as 'ISO-8859-1'.The {@code Properties} file is interpreted according to the following rules:
	 * <ul>
	 * <li>Empty lines are ignored.</li>
	 * <li>Lines starting with either a "#" or a "!" are comment lines and are ignored.</li>
	 * <li>A backslash at the end of the line escapes the following newline character ("\r", "\n", "\r\n"). If there's a whitespace after the backslash it will just 
	 * escape that whitespace instead of concatenating the lines. This does not apply to comment lines.</li>
	 * <li>A property line consists of the key, the space between the key and the value, and the value. The key goes up to the first whitespace, "=" or ":" that is 
	 * not escaped. The space between the key and the value contains either one whitespace, one "=" or one ":" and any number of additional whitespaces before and 
	 * after that character. The value starts with the first character after the space between the key and the value.</li>
	 * <li>Following escape sequences are recognized: "\ ", "\\", "\r", "\n", "\!", "\#", "\t", "\b", "\f", and "&#92;uXXXX" (unicode character).</li>
	 * </ul>
	 * @param reader The input reader.
	 * @throws IOException
	 */
	public synchronized void load(Reader reader) throws IOException
	{
		int mode = NONE, unicode = 0, count = 0;
		char nextChar, buf[] = new char[40];
		int offset = 0, keyLength = -1, intVal;
		boolean firstChar = true;
		
		while (true)
		{
			intVal = reader.read();
			if (intVal == -1)
			{
				break;
			}
			nextChar = (char)intVal;
			
			if (offset == buf.length)
			{
				char[] newBuf = new char[buf.length * 2];
				System.arraycopy(buf, 0, newBuf, 0, offset);
				buf = newBuf;
			}
			if (mode == UNICODE)
			{
				int digit = Character.digit(nextChar, 16);
				if (digit >= 0)
				{
					unicode = (unicode << 4) + digit;
					if (++count < 4)
					{
						continue;
					}
				}
				else if (count <= 4)
				{
					throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_PROPERTIES_INV_UNI_CHAR));
				}
				mode = NONE;
				buf[offset++] = (char) unicode;
				if (nextChar != '\n' && nextChar != '\u0085')
				{
					continue;
				}
			}
			if (mode == SLASH)
			{
				mode = NONE;
				switch (nextChar)
				{
					case '\r':
						mode = CONTINUE; // Look for a following \n
						continue;
					case '\u0085':
					case '\n':
						mode = IGNORE; // Ignore whitespace on the next line
						continue;
					case 'b':
						nextChar = '\b';
						break;
					case 'f':
						nextChar = '\f';
						break;
					case 'n':
						nextChar = '\n';
						break;
					case 'r':
						nextChar = '\r';
						break;
					case 't':
						nextChar = '\t';
						break;
					case 'u':
						mode = UNICODE;
						unicode = count = 0;
						continue;
				}
			}
			else
			{
				switch (nextChar)
				{
					case '#':
					case '!':
						if (firstChar)
						{
							while(true)
							{
								intVal = reader.read();
								if (intVal == -1)
								{
									break;
								}
								nextChar = (char)intVal; // & 0xff
								// not
								// required
								if (nextChar == '\r' || nextChar == '\n' || nextChar == '\u0085')
								{
									break;
								}
							}
							continue;
						}
						break;
					case '\n':
						if (mode == CONTINUE) // Part of a \r\n sequence
						{
							mode = IGNORE; // Ignore whitespace on the next line
							continue;
						}
						// fall into the next case
					case '\u0085':
					case '\r':
						mode = NONE;
						firstChar = true;
						if (offset > 0 || (offset == 0 && keyLength == 0))
						{
							if (keyLength == -1)
							{
								keyLength = offset;
							}
							String temp = new String(buf, 0, offset);
							put(temp.substring(0, keyLength), temp.substring(keyLength));
						}
						keyLength = -1;
						offset = 0;
						continue;
					case '\\':
						if (mode == KEY_DONE)
						{
							keyLength = offset;
						}
						mode = SLASH;
						continue;
					case ':':
					case '=':
						if (keyLength == -1) // if parsing the key
						{
							mode = NONE;
							keyLength = offset;
							continue;
						}
						break;
				}
				if (PDFUtil.Character_isWhiteSpace(nextChar))
				{
					if (mode == CONTINUE)
					{
						mode = IGNORE;
					}
					// if key length == 0 or value length == 0
					if (offset == 0 || offset == keyLength || mode == IGNORE)
					{
						continue;
					}
					if (keyLength == -1) // if parsing the key
					{
						mode = KEY_DONE;
						continue;
					}
				}
				if (mode == IGNORE || mode == CONTINUE)
				{
					mode = NONE;
				}
			}
			firstChar = false;
			if (mode == KEY_DONE)
			{
				keyLength = offset;
				mode = NONE;
			}
			buf[offset++] = nextChar;
		}
		if (mode == UNICODE && count <= 4)
		{
			throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_PROPERTIES_INV_UNI_FORMAT));
		}
		if (keyLength == -1 && offset > 0)
		{
			keyLength = offset;
		}
		if (keyLength >= 0)
		{
			String temp = new String(buf, 0, offset);
			String key = temp.substring(0, keyLength);
			String value = temp.substring(keyLength);
			if (mode == SLASH)
			{
				value += '\u0000';
			}
			put(key, value);
		}
	}
	
	/**
	 * Searches for the property with the specified name. If the property is not
	 * found, {@code null} is returned.
	 * @param name The name of the property to find.
	 * @return The named property value, or {@code null} if it can't be found.
	 */
	public String getProperty(String name)
	{
		Object result = super.get(name);
		return result instanceof String ? (String)result : null;
	}
}
