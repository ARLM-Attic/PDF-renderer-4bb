/*
 * File: ICC_Profile.java
 * Version: 1.0
 * Initial Creation: May 13, 2010 6:19:35 PM
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
package com.sun.pdfview.helper.graphics.color;

import java.io.IOException;
import java.io.InputStream;

/**
 * Partial implementation of java.awt.color.ICC_Profile
 * @author Oleg V. Khaschansky
 */
public class ICC_Profile
{
	// NOTE: Constant field values are noted in 1.5 specification.
	
	/**
	 * ICC Profile Header Location: profile size in bytes.
	 */
	public static final int icHdrSize = 0;
	
	/**
	 * ICC Profile Header Location: icMagicNumber.
	 */
	public static final int icHdrMagic = 36;
	
	/**
	 * Size of a profile header
	 */
	private static final int headerSize = 128;
	
	/**
	 * header magic number
	 */
	private static final int headerMagicNumber = 0x61637370;
	
	private ICC_Profile(byte[] data)
	{
		//TODO: Figure out what to do with the data and how to use it.
		
		//http://www.color.org/ICC1v42_2006-05.pdf : pg 27
	}
	
	/**
	 * Used to instantiate dummy ICC_ProfileStub objects
	 */
	ICC_Profile(){}
	
	/**
	 * Constructs an ICC_Profile corresponding to the data in an InputStream.
	 * @param s The input stream from which to read the profile data.
	 * @return an ICC_Profile object corresponding to the data in the specified InputStream.
	 * @throws IOException If an I/O error occurs while reading the stream. 
	 * @throws IllegalArgumentException If the stream does not contain valid ICC Profile data.
	 */
	public static ICC_Profile getInstance(InputStream s) throws IOException
	{
		byte[] header = new byte[headerSize];
		String invalidDataMessage = "Invalid ICC Profile Data";
		
		// Get header from the input stream
		if (s.read(header) != headerSize)
		{
			throw new IllegalArgumentException(invalidDataMessage);
		}
		
		// Check the profile data for consistency
		if (getBigEndianFromByteArray(header, icHdrMagic) != headerMagicNumber)
		{
			throw new IllegalArgumentException(invalidDataMessage);
		}
		
		// Get profile size from header, create an array for profile data
		int profileSize = getBigEndianFromByteArray(header, icHdrSize);
		byte[] profileData = new byte[profileSize];
		
		// Copy header into it
		System.arraycopy(header, 0, profileData, 0, headerSize);
		
		// Read the profile itself
		if (s.read(profileData, headerSize, profileSize - headerSize) != profileSize - headerSize)
		{
			throw new IllegalArgumentException(invalidDataMessage);
		}
		
		return getInstance(profileData);
	}
	
	/**
	 * Constructs an ICC_Profile object corresponding to the data in a byte array.
	 * @param data the specified ICC Profile data
	 * @return an ICC_Profile object corresponding to the data in the specified data array.
	 */
	public static ICC_Profile getInstance(byte[] data)
	{
		ICC_Profile res = null;
		
		try
		{
			res = new ICC_Profile(data);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Invalid ICC Profile Data");
		}
		
		//Original source code had check for Microsoft Windows... As if that is would be on a BlackBerry...
		
		return res;
	}
	
	//Includes utility methods for reading ICC profile data.
	
	/**
	 * Utility method.
	 * Gets big endian integer value from the byte array
	 * @param byteArray - byte array
	 * @param idx - byte offset
	 * @return integer value
	 */
	public static int getBigEndianFromByteArray(byte[] byteArray, int idx)
	{
		return ( (byteArray[idx] & 0xFF) << 24)		|
				((byteArray[idx+1] & 0xFF) << 16)	|
				((byteArray[idx+2] & 0xFF) << 8)	|
				( byteArray[idx+3] & 0xFF);
	}
}
