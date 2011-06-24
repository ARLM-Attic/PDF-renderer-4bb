//#preprocessor

//#ifndef NATIVE_CHARSET_ENCODER

/*
 * File: MalformedInputException.java
 * Version: 1.0
 * Initial Creation: May 10, 2010 9:48:34 AM
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
 * A {@code MalformedInputException} is thrown when a malformed input is encountered, for example if a byte sequence is illegal for the given charset.
 */
public class MalformedInputException extends CharacterCodingException
{
	// the length of the malformed input
	private int inputLength;
	
	/**
	 * Constructs a new {@code MalformedInputException}.
	 * 
	 * @param length the length of the malformed input.
	 */
	public MalformedInputException(int length)
	{
		this.inputLength = length;
	}
	
	/**
	 * Gets the length of the malformed input.
	 * 
	 * @return the length of the malformed input.
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
		return com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.HELPER_MALFORMED_INPUT_EXP_IN_LEN, new Object[]{new Integer(this.inputLength)});
	}
}

//#endif
