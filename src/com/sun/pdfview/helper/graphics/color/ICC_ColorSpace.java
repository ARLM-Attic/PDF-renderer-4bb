/*
 * File: ICC_ColorSpace.java
 * Version: 1.0
 * Initial Creation: May 13, 2010 7:53:47 PM
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

//TODO: Finish and add documentation
//http://www.docjar.com/html/api/java/awt/color/ICC_ColorSpace.java.html

import com.sun.pdfview.helper.ColorSpace;

public class ICC_ColorSpace extends ColorSpace
{
	/**
	 * According to ICC specification (from http://www.color.org)
	 * "For the CIEXYZ encoding, each component (X, Y, and Z)
	 * is encoded as a u1Fixed15Number".
	 * This means that max value for this encoding is 1 + (32767/32768)
	 */
	private static final float MAX_XYZ = 1f + (32767f/32768f);
	private static final float MAX_SHORT = 65535f;
	private static final float INV_MAX_SHORT = 1f/MAX_SHORT;
	private static final float SHORT2XYZ_FACTOR = MAX_XYZ/MAX_SHORT;
	private static final float XYZ2SHORT_FACTOR = MAX_SHORT/MAX_XYZ;
	
	private ICC_Profile profile = null;
	private float minValues[] = null;
	private float maxValues[] = null;
	
	public ICC_ColorSpace(ICC_Profile pf)
	{
		super(pf.getColorSpaceType(), pf.getNumComponents());
		
		int pfClass = pf.getProfileClass();
		
		switch (pfClass)
		{
			case ICC_Profile.CLASS_COLORSPACECONVERSION:
			case ICC_Profile.CLASS_DISPLAY:
			case ICC_Profile.CLASS_OUTPUT:
			case ICC_Profile.CLASS_INPUT:
				break; // OK, it is color conversion profile
			default:
				throw new IllegalArgumentException("Invalid profile class.");
		}
		
		profile = pf;
		fillMinMaxValues();
	}
	
	/* (non-Javadoc)
	 * @see com.sun.pdfview.helper.ColorSpace#fromCIEXYZ(float[])
	 */
	public float[] fromCIEXYZ(float[] colorvalue) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.sun.pdfview.helper.ColorSpace#fromRGB(float[])
	 */
	public float[] fromRGB(float[] rgbvalue) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.sun.pdfview.helper.ColorSpace#toCIEXYZ(float[])
	 */
	public float[] toCIEXYZ(float[] colorvalue) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.sun.pdfview.helper.ColorSpace#toRGB(float[])
	 */
	public float[] toRGB(float[] colorvalue) {
		return null;
	}
}
