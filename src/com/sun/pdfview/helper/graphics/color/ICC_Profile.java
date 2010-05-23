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

import com.sun.pdfview.helper.ColorSpace;

/**
 * Partial implementation of java.awt.color.ICC_Profile
 * @author Oleg V. Khaschansky
 */
public class ICC_Profile
{
	// NOTE: Constant field values are noted in 1.5 specification.
	
	/** Profile class is input.*/
	public static final int CLASS_INPUT = 0;
	/** Profile class is display.*/
	public static final int CLASS_DISPLAY = 1;
	/** Profile class is input.*/
	public static final int CLASS_OUTPUT = 2;
	/** Profile class is device link.*/
	public static final int CLASS_DEVICELINK = 3;
	/** Profile class is color space conversion.*/
	public static final int CLASS_COLORSPACECONVERSION = 4;
	/** Profile class is abstract.*/
	public static final int CLASS_ABSTRACT = 5;
	/** Profile class is named color.*/
	public static final int CLASS_NAMEDCOLOR = 6;
	
	/** ICC Profile Header Location: profile size in bytes.*/
	public static final int icHdrSize = 0;
	/** ICC Profile Header Location: icMagicNumber.*/
	public static final int icHdrMagic = 36;
	/** ICC Profile Header Location: type of profile.*/
	public static final int icHdrDeviceClass = 12;
	/** ICC Profile Header Location: color space of data.*/
	public static final int icHdrColorSpace = 16;
	
	/** ICC Profile Color Space Type Signature: 'CMYK'.*/
	public static final int icSigCmykData = 1129142603;
	/** ICC Profile Color Space Type Signature: 'GRAY'.*/
	public static final int icSigGrayData = 1196573017;
	/** ICC Profile Color Space Type Signature: 'Lab '.*/
	public static final int icSigLabData = 1281450528;
	/** ICC Profile Color Space Type Signature: 'RGB '.*/
	public static final int icSigRgbData = 1380401696;
	/** ICC Profile Class Signature: 'abst'.*/
	public static final int icSigAbstractClass = 1633842036;
	/** ICC Profile Class Signature: 'link'.*/
	public static final int icSigLinkClass = 1818848875;
	/** ICC Profile Class Signature: 'mntr'.*/
	public static final int icSigDisplayClass = 1835955314;
	/** ICC Profile Class Signature: 'nmcl'.*/
	public static final int icSigNamedColorClass = 1852662636;
	/** ICC Profile Class Signature: 'prtr'.*/
	public static final int icSigOutputClass = 1886549106;
	/** ICC Profile Class Signature: 'scnr'.*/
	public static final int icSigInputClass = 1935896178;
	/** ICC Profile Class Signature: 'spac'.*/
	public static final int icSigColorSpaceClass = 1936744803;
	/** ICC Profile Tag Signature: 'head' - special.*/
	public static final int icSigHead = 1751474532;
	
	/** Size of a profile header*/
	protected static final int headerSize = 128;
	/** header magic number*/
	private static final int headerMagicNumber = 0x61637370;
	
	/**
	 * Cached header data
	 */
	private transient byte[] headerData = null;
	private CMM cmm;
	
	private ICC_Profile(byte[] data)
	{
		try
		{
			cmm = new CMM(data);
		}
		catch(IOException ioe)
		{
			cmm = null;
		}
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
	
	/**
	 * Returns the profile class.
	 * @return One of the predefined profile class constants.
	 */
	public int getProfileClass()
	{
		int deviceClassSignature = getIntFromHeader(icHdrDeviceClass);
		
		switch (deviceClassSignature)
		{
			case icSigColorSpaceClass:
				return CLASS_COLORSPACECONVERSION;
			case icSigDisplayClass:
				return CLASS_DISPLAY;
			case icSigOutputClass:
				return CLASS_OUTPUT;
			case icSigInputClass:
				return CLASS_INPUT;
			case icSigLinkClass:
				return CLASS_DEVICELINK;
			case icSigAbstractClass:
				return CLASS_ABSTRACT;
			case icSigNamedColorClass:
				return CLASS_NAMEDCOLOR;
		}
		
		// Not an ICC profile class
		throw new IllegalArgumentException("Profile class does not comply with ICC specification");
	}
	/**
	 * Returns the color space type.
	 * @return One of the color space type constants defined in the ColorSpace class.
	 */
	public int getColorSpaceType()
	{
		return csFromSignature(getIntFromHeader(icHdrColorSpace));
	}
	
	/**
	 *  Converts ICC color space signature to the java predefined color space type
	 */
	private int csFromSignature(int signature)
	{
		switch (signature)
		{
			case icSigRgbData:
				return ColorSpace.TYPE_RGB;
			case icSigCmykData:
				return ColorSpace.TYPE_CMYK;
			case icSigLabData:
				return ColorSpace.TYPE_Lab;
			case icSigGrayData:
				return ColorSpace.TYPE_GRAY;
			case 1482250784: //icSigXYZData
			case 1212961568: //icSigHlsData
			case 1282766368: //icSigLuvData
			case 1497588338: //icSigYCbCrData
			case 1501067552: //icSigYxyData
			case 1213421088: //icSigHsvData
			case 1129142560: //icSigCmyData
			case 843271250: //icSigSpace2CLR
			case 860048466: //icSigSpace3CLR
			case 876825682: //icSigSpace4CLR
			case 893602898: //icSigSpace5CLR
			case 910380114: //icSigSpace6CLR
			case 927157330: //icSigSpace7CLR
			case 943934546: //icSigSpace8CLR
			case 960711762: //icSigSpace9CLR
			case 1094929490: //icSigSpaceACLR
			case 1111706706: //icSigSpaceBCLR
			case 1128483922: //icSigSpaceCCLR
			case 1145261138: //icSigSpaceDCLR
			case 1162038354: //icSigSpaceECLR
			case 1178815570: //icSigSpaceFCLR
				//Not supported
				return -1;
		}
		
		throw new IllegalArgumentException("Color space doesn't comply with ICC specification");
	}
	
	public int getNumComponents()
	{
		switch (getIntFromHeader(icHdrColorSpace))
		{
			// The most common cases go first to increase speed
			case icSigRgbData:
			case icSigLabData:
				return 3;
			case icSigCmykData:
				return 4;
			// Then all other
			case icSigGrayData:
				return 1;
			case 1482250784: //icSigXYZData
			case 1212961568: //icSigHlsData
			case 1282766368: //icSigLuvData
			case 1497588338: //icSigYCbCrData
			case 1501067552: //icSigYxyData
			case 1213421088: //icSigHsvData
			case 1129142560: //icSigCmyData
			case 843271250: //icSigSpace2CLR
			case 860048466: //icSigSpace3CLR
			case 876825682: //icSigSpace4CLR
			case 893602898: //icSigSpace5CLR
			case 910380114: //icSigSpace6CLR
			case 927157330: //icSigSpace7CLR
			case 943934546: //icSigSpace8CLR
			case 960711762: //icSigSpace9CLR
			case 1094929490: //icSigSpaceACLR
			case 1111706706: //icSigSpaceBCLR
			case 1128483922: //icSigSpaceCCLR
			case 1145261138: //icSigSpaceDCLR
			case 1162038354: //icSigSpaceECLR
			case 1178815570: //icSigSpaceFCLR
				//Not supported
				return -1;
		}
		
		throw new IllegalArgumentException("Color space doesn't comply with ICC specification");
	}
	
	/**
	 * Reads integer from the profile header at the specified position
	 * @param idx - offset in bytes from the beginning of the header
	 */
	private int getIntFromHeader(int idx)
	{
		if (headerData == null)
		{
			headerData = getData(icSigHead);
		}
		
		return  ((headerData[idx]   & 0xFF) << 24)|
				((headerData[idx+1] & 0xFF) << 16)|
				((headerData[idx+2] & 0xFF) << 8) |
				((headerData[idx+3] & 0xFF));
	}
	
	/**
	 * Returns a particular tagged data element from the profile as a byte array.
	 * @param tagSignature The ICC tag signature for the data element you want to get.
	 * @return A byte array that contains the tagged data element. Returns null if the specified tag doesn't exist.
	 */
	public byte[] getData(int tagSignature)
	{
		int tagSize = 0;
		try
		{
			tagSize = cmm.cmmGetProfileElementSize(tagSignature);
		}
		catch (Exception e)
		{
			// We'll get this exception if there's no element with the specified tag signature
			return null;
		}
		
		byte[] data = new byte[tagSize];
		try
		{
			cmm.cmmGetProfileElement(tagSignature, data);
		}
		catch(IOException ioe)
		{
		}
		return data;
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
