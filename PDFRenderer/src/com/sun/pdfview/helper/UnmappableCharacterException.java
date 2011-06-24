//#preprocessor

//#ifndef NATIVE_CHARSET_ENCODER

/*
 * File: UnmappableCharacterException.java
 * Version: 1.0
 * Initial Creation: May 10, 2010 12:59:36 PM
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


/**
 * An {@code UnmappableCharacterException} is thrown when an unmappable character for the given charset is encountered.
 */
public class UnmappableCharacterException extends CharacterCodingException
{
	// The length of the unmappable character
	private int inputLength;
	
	/**
	 * Constructs a new {@code UnmappableCharacterException}.
	 * 
	 * @param length the length of the unmappable character.
	 */
	public UnmappableCharacterException(int length)
	{
		this.inputLength = length;
	}
	
	/**
	 * Gets the length of the unmappable character.
	 * 
	 * @return the length of the unmappable character.
	 */
	public int getInputLength()
	{
		return this.inputLength;
	}
	
	/**
	 * Gets a message describing this exception.
	 * 
	 * @return a message describing this exception.
	 */
	public String getMessage()
	{
		return com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.HELPER_UNMAPPABLE_CHAR_EXP_CHAR_LEN, new Object[]{new Integer(this.inputLength)});
	}
}

//#endif
