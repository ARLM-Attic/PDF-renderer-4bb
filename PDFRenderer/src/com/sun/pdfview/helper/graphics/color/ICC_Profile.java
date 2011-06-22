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

import com.sun.pdfview.ResourceManager;
import com.sun.pdfview.helper.ColorSpace;

import littlecms.internal.lcms2;

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
	
	/** ICC Profile Tag Signature: 'bkpt'.*/
	public static final int icSigMediaBlackPointTag = 1651208308;
	/** ICC Profile Tag Signature: 'wtpt'.*/
	public static final int icSigMediaWhitePointTag = 2004119668;
	/** ICC Profile Tag Signature: 'chad'.*/
	public static final int icSigChromaticAdaptationTag = 1667785060;
	/** ICC Profile Tag Signature: 'kTRC'.*/
	public static final int icSigGrayTRCTag = 1800688195;
	/** ICC Profile Tag Signature: 'rXYZ'.*/
	public static final int icSigRedColorantTag = 1918392666;
	/** ICC Profile Tag Signature: 'gXYZ'.*/
	public static final int icSigGreenColorantTag = 1733843290;
	/** ICC Profile Tag Signature: 'bXYZ'.*/
	public static final int icSigBlueColorantTag = 1649957210;
	/** ICC Profile Tag Signature: 'rTRC'.*/
	public static final int icSigRedTRCTag = 1918128707;
	/** ICC Profile Tag Signature: 'gTRC'.*/
	public static final int icSigGreenTRCTag = 1733579331;
	/** ICC Profile Tag Signature: 'bTRC'.*/
	public static final int icSigBlueTRCTag = 1649693251;
	/** ICC Profile Tag Signature: 'clrt'*/
	public static final int icSigColorantTableTag = 0x636C7274;
	/** ICC Profile Tag Signature: 'clot'*/
	public static final int icSigColorantTableOutTag = 0x636C6F74;
	
	/** ICC Profile Header Location: profile size in bytes.*/
	public static final int icHdrSize = 0;
	/** ICC Profile Header Location: format version number.*/
	public static final int icHdrVersion = 8;
	/** ICC Profile Header Location: type of profile.*/
	public static final int icHdrDeviceClass = 12;
	/** ICC Profile Header Location: color space of data.*/
	public static final int icHdrColorSpace = 16;
	/** ICC Profile Header Location: PCS - XYZ or Lab only.*/
	public static final int icHdrPcs = 20;
	/** ICC Profile Header Location: icMagicNumber.*/
	public static final int icHdrMagic = 36;
	/** ICC Profile Header Location: rendering intent.*/
	public static final int icHdrRenderingIntent = 64;
	
	/** ICC Profile Color Space Type Signature: 'CMYK'.*/
	public static final int icSigCmykData = 1129142603;
	/** ICC Profile Color Space Type Signature: 'GRAY'.*/
	public static final int icSigGrayData = 1196573017;
	/** ICC Profile Color Space Type Signature: 'Lab '.*/
	public static final int icSigLabData = 1281450528;
	/** ICC Profile Color Space Type Signature: 'RGB '.*/
	public static final int icSigRgbData = 1380401696;
	/** ICC Profile Color Space Type Signature: 'XYZ '.*/
	public static final int icSigXYZData = 1482250784;
	/** ICC Profile Color Space Type Signature: 'HLS'.*/
	public static final int icSigHlsData = 1212961568;
	/** ICC Profile Color Space Type Signature: 'Luv '.*/
	public static final int icSigLuvData = 1282766368;
	/** ICC Profile Color Space Type Signature: 'YCbr'.*/
	public static final int icSigYCbCrData = 1497588338;
	/** ICC Profile Color Space Type Signature: 'Yxy '.*/
	public static final int icSigYxyData = 1501067552;
	/** ICC Profile Color Space Type Signature: 'HSV'.*/
	public static final int icSigHsvData = 1213421088;
	/** ICC Profile Color Space Type Signature: 'CMY '.*/
	public static final int icSigCmyData = 1129142560;
	/** ICC Profile Color Space Type Signature: '2CLR'.*/
	public static final int icSigSpace2CLR = 843271250;
	/** ICC Profile Color Space Type Signature: '3CLR'.*/
	public static final int icSigSpace3CLR = 860048466;
	/** ICC Profile Color Space Type Signature: '4CLR'.*/
	public static final int icSigSpace4CLR = 876825682;
	/** ICC Profile Color Space Type Signature: '5CLR'.*/
	public static final int icSigSpace5CLR = 893602898;
	/** ICC Profile Color Space Type Signature: '6CLR'.*/
	public static final int icSigSpace6CLR = 910380114;
	/** ICC Profile Color Space Type Signature: '7CLR'.*/
	public static final int icSigSpace7CLR = 927157330;
	/** ICC Profile Color Space Type Signature: '8CLR'.*/
	public static final int icSigSpace8CLR = 943934546;
	/** ICC Profile Color Space Type Signature: '9CLR'.*/
	public static final int icSigSpace9CLR = 960711762;
	/** ICC Profile Color Space Type Signature: 'ACLR'.*/
	public static final int icSigSpaceACLR = 1094929490;
	/** ICC Profile Color Space Type Signature: 'BCLR'.*/
	public static final int icSigSpaceBCLR = 1111706706;
	/** ICC Profile Color Space Type Signature: 'CCLR'.*/
	public static final int icSigSpaceCCLR = 1128483922;
	/** ICC Profile Color Space Type Signature: 'DCLR'.*/
	public static final int icSigSpaceDCLR = 1145261138;
	/** ICC Profile Color Space Type Signature: 'ECLR'.*/
	public static final int icSigSpaceECLR = 1162038354;
	/** ICC Profile Color Space Type Signature: 'FCLR'.*/
	public static final int icSigSpaceFCLR = 1178815570;
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
	
	/** ICC Profile Rendering Intent: Perceptual.*/
	public static final int icPerceptual = 0;
	/** ICC Profile Rendering Intent: RelativeColorimetric.*/
    public static final int icRelativeColorimetric = 1;
    /** ICC Profile Rendering Intent: Saturation.*/
    public static final int icSaturation = 2;
    /** ICC Profile Rendering Intent: AbsoluteColorimetric.*/
    public static final int icAbsoluteColorimetric = 3;
	
	/** Size of a profile header*/
	protected static final int headerSize = 128;
	/** header magic number*/
	static final int headerMagicNumber = 0x61637370;
	
	// Cache of predefined profiles
	private static final long CS_sRGB_PROFILE_ID = 0x75312FEDB2463B0EL;
	private static final long CS_XYZ_PROFILE_ID = 0xA41D05A15386DDB6L;
	private static final long CS_GRAY_PROFILE_ID = 0xB32C55D66B185550L;
	//private static final long CS_PYCC_PROFILE_ID = 0x12C967DCC01358DDL;
	//private static final long CS_LIN_RGB_PROFILE_ID = 0xEEFB266E571D923CL;
	
    private static ICC_Profile sRGBProfile;
    private static ICC_Profile xyzProfile;
    private static ICC_Profile grayProfile;
    //private static ICC_Profile pyccProfile;
    //private static ICC_Profile linearRGBProfile;
	
    /*
	/**
	 * Cached header data
	 * /
	private byte[] headerData = null;
	*/
	
	lcms2.cmsHPROFILE profile;
	
	private ICC_Profile(byte[] data)
	{
		this.profile = lcms2.cmsOpenProfileFromMem(data, data.length);
		
		if(this.profile == null)
		{
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Used to instantiate dummy ICC_ProfileStub objects
	 */
	ICC_Profile(){}
	
	//Not really based on the same function from java.awt.color.ICC_Profile
	private static ICC_Profile getInstance(String resourceName) throws IOException
	{
		InputStream in = ResourceManager.getResource("helper.graphics.color").getStream(resourceName);
		
		ICC_Profile profile = getInstance(in);
		
		in.close();
		return profile;
	}
	
	/**
	 * Constructs an ICC_Profile corresponding to one of the specific color spaces defined by the ColorSpace class (for example CS_sRGB).
	 * @param cspace The type of color space to create a profile for. The specified type is one of the color space constants defined in the ColorSpace class.
	 * @return An ICC_Profile object corresponding to the specified ColorSpace type.
	 * @throws IllegalArgumentException If cspace is not one of the predefined color space types.
	 */
	public static ICC_Profile getInstance(int cspace)
	{
		try
		{
			switch (cspace)
			{
				case ColorSpace.CS_sRGB:
					if (sRGBProfile == null)
					{
						sRGBProfile = (ICC_Profile)ResourceManager.singletonStorageGet(CS_sRGB_PROFILE_ID);
						if (sRGBProfile == null)
						{
							sRGBProfile = getInstance("sRGB.pf");
							ResourceManager.singletonStorageSet(CS_sRGB_PROFILE_ID, sRGBProfile);
						}
					}
					return sRGBProfile;
				case ColorSpace.CS_CIEXYZ:
					if (xyzProfile == null)
					{
						xyzProfile = (ICC_Profile)ResourceManager.singletonStorageGet(CS_XYZ_PROFILE_ID);
						if (xyzProfile == null)
						{
							xyzProfile = getInstance("CIEXYZ.pf");
							ResourceManager.singletonStorageSet(CS_XYZ_PROFILE_ID, xyzProfile);
						}
					}
					return xyzProfile;
				case ColorSpace.CS_GRAY:
					if (grayProfile == null)
					{
						grayProfile = (ICC_Profile)ResourceManager.singletonStorageGet(CS_GRAY_PROFILE_ID);
						if (grayProfile == null)
						{
							grayProfile = getInstance("GRAY.pf");
							ResourceManager.singletonStorageSet(CS_GRAY_PROFILE_ID, grayProfile);
						}
					}
					return grayProfile;
					/*
				case ColorSpace.CS_PYCC:
					if (pyccProfile == null)
					{
						pyccProfile = (ICC_Profile)ResourceManager.singletonStorageGet(CS_PYCC_PROFILE_ID);
						if (pyccProfile == null)
						{
							pyccProfile = getInstance("PYCC.pf");
							ResourceManager.singletonStorageSet(CS_PYCC_PROFILE_ID, pyccProfile);
						}
					}
					return pyccProfile;
				case ColorSpace.CS_LINEAR_RGB:
					if (linearRGBProfile == null)
					{
						linearRGBProfile = (ICC_Profile)ResourceManager.singletonStorageGet(CS_LIN_RGB_PROFILE_ID);
						if (linearRGBProfile == null)
						{
							linearRGBProfile = getInstance("LINEAR_RGB.pf");
							ResourceManager.singletonStorageSet(CS_LIN_RGB_PROFILE_ID, linearRGBProfile);
						}
					}
					return linearRGBProfile;
					*/
			}
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COLOR_PRO_CANT_OPEN_PROFILE));
		}
		
		throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COLOR_PRO_NOT_PREDEFINED));
	}
	
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
		String invalidDataMessage = com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COLOR_PRO_INVALID_ICC);
		
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
			throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COLOR_PRO_INVALID_ICC));
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
		throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COLOR_PRO_BAD_ICC_PROFILE));
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
	        case icSigXYZData:
	            return ColorSpace.TYPE_XYZ;
	        case icSigCmykData:
	            return ColorSpace.TYPE_CMYK;
	        case icSigLabData:
	            return ColorSpace.TYPE_Lab;
	        case icSigGrayData:
	            return ColorSpace.TYPE_GRAY;
	        case icSigHlsData:
	            //return ColorSpace.TYPE_HLS;
	        case icSigLuvData:
	            //return ColorSpace.TYPE_Luv;
	        case icSigYCbCrData:
	            //return ColorSpace.TYPE_YCbCr;
	        case icSigYxyData:
	            //return ColorSpace.TYPE_Yxy;
	        case icSigHsvData:
	            //return ColorSpace.TYPE_HSV;
	        case icSigCmyData:
	            //return ColorSpace.TYPE_CMY;
	        case icSigSpace2CLR:
	            //return ColorSpace.TYPE_2CLR;
	        case icSigSpace3CLR:
	            //return ColorSpace.TYPE_3CLR;
	        case icSigSpace4CLR:
	            //return ColorSpace.TYPE_4CLR;
	        case icSigSpace5CLR:
	            //return ColorSpace.TYPE_5CLR;
	        case icSigSpace6CLR:
	            //return ColorSpace.TYPE_6CLR;
	        case icSigSpace7CLR:
	            //return ColorSpace.TYPE_7CLR;
	        case icSigSpace8CLR:
	            //return ColorSpace.TYPE_8CLR;
	        case icSigSpace9CLR:
	            //return ColorSpace.TYPE_9CLR;
	        case icSigSpaceACLR:
	            //return ColorSpace.TYPE_ACLR;
	        case icSigSpaceBCLR:
	            //return ColorSpace.TYPE_BCLR;
	        case icSigSpaceCCLR:
	            //return ColorSpace.TYPE_CCLR;
	        case icSigSpaceDCLR:
	            //return ColorSpace.TYPE_DCLR;
	        case icSigSpaceECLR:
	            //return ColorSpace.TYPE_ECLR;
	        case icSigSpaceFCLR:
	            //return ColorSpace.TYPE_FCLR;
	        	return -1; //Not implemented
		}
		
		throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COLOR_PRO_BAD_ICC_COLORSPACE));
	}
	
	public int getNumComponents()
	{
		switch (getIntFromHeader(icHdrColorSpace))
		{
			// The most common cases go first to increase speed
	        case icSigRgbData:
	        case icSigXYZData:
	        case icSigLabData:
	            return 3;
	        case icSigCmykData:
	            return 4;
	            // Then all other
	        case icSigGrayData:
	            return 1;
	        case icSigSpace2CLR:
	            return 2;
	        case icSigYCbCrData:
	        case icSigLuvData:
	        case icSigYxyData:
	        case icSigHlsData:
	        case icSigHsvData:
	        case icSigCmyData:
	        case icSigSpace3CLR:
	            return 3;
	        case icSigSpace4CLR:
	            return 4;
	        case icSigSpace5CLR:
	            return 5;
	        case icSigSpace6CLR:
	            return 6;
	        case icSigSpace7CLR:
	            return 7;
	        case icSigSpace8CLR:
	            return 8;
	        case icSigSpace9CLR:
	            return 9;
	        case icSigSpaceACLR:
	            return 10;
	        case icSigSpaceBCLR:
	            return 11;
	        case icSigSpaceCCLR:
	            return 12;
	        case icSigSpaceDCLR:
	            return 13;
	        case icSigSpaceECLR:
	            return 14;
	        case icSigSpaceFCLR:
	            return 15;
		}
		
		throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COLOR_PRO_BAD_ICC_COLORSPACE));
	}
	
	/**
	 * Reads integer from the profile header at the specified position
	 * @param idx - offset in bytes from the beginning of the header
	 */
	private int getIntFromHeader(int idx)
	{
		/*
		if (headerData == null)
		{
			headerData = getData(icSigHead);
		}
		
		return  ((headerData[idx]   & 0xFF) << 24)|
				((headerData[idx+1] & 0xFF) << 16)|
				((headerData[idx+2] & 0xFF) << 8) |
				((headerData[idx+3] & 0xFF));
		*/
		switch(idx)
		{
			case icHdrDeviceClass:
				return lcms2.cmsGetDeviceClass(this.profile);
			case icHdrColorSpace:
				return lcms2.cmsGetColorSpace(this.profile);
		}
		return 0;
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
			tagSize = lcms2.cmsReadRawTag(this.profile, tagSignature, null, 0);
		}
		catch (Exception e)
		{
			// We'll get this exception if there's no element with the specified tag signature
			return null;
		}
		if(tagSize <= 0)
		{
			return null;
		}
		
		byte[] data = new byte[tagSize];
		lcms2.cmsReadRawTag(this.profile, tagSignature, data, tagSize);
		return data;
	}
	
	/**
     * Used in ICC_Converter class to check the rendering intent of the profile
     * @param profile - ICC profile
     * @return rendering intent
     */
    public int getRenderingIntent()
    {
    	/*
        return getIntFromByteArray(this.getData(ICC_Profile.icSigHead), // pf header
        		ICC_Profile.icHdrRenderingIntent);
        */
    	return lcms2.cmsGetHeaderRenderingIntent(this.profile);
    }
	
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
	
	/**
	 * Utility method.
	 * Gets integer value from the byte array
	 * @param byteArray - byte array
	 * @param idx - byte offset
	 * @return integer value
	 */
	public static int getIntFromByteArray(byte[] byteArray, int idx)
	{
		return ( (byteArray[idx+3] & 0xFF) << 24)		|
				((byteArray[idx+2] & 0xFF) << 16)	|
				((byteArray[idx+1] & 0xFF) << 8)	|
				( byteArray[idx] & 0xFF);
	}
}
