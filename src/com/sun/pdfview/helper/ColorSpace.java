/*
 * File: ColorSpace.java
 * Version: 1.0
 * Initial Creation: May 12, 2010 9:45:17 PM
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

import com.sun.pdfview.helper.graphics.color.ICC_ColorSpace;

/**
 * Implementation of java.awt.color.ColorSpace.
 * @author Vincent Simonetti
 */
public abstract class ColorSpace
{
	/** The sRGB color space defined at <a href="http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html">http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html</a>.*/
	public static final int CS_sRGB = 1000;
	/** The CIEXYZ conversion color space defined above.*/
	public static final int CS_CIEXYZ = 1001;
	/**  The built-in linear gray scale color space.*/
	public static final int CS_GRAY = 1003;
	
	/** Any of the family of Lab color spaces.*/
	public static final int TYPE_Lab = 1;
	/** Any of the family of RGB color spaces.*/
	public static final int TYPE_RGB = 5;
	/** Any of the family of GRAY color spaces.*/
	public static final int TYPE_GRAY = 6;
	/** Any of the family of CMYK color spaces.*/
	public static final int TYPE_CMYK = 9;
	
	private static ColorSpace cs_Gray = null;
	private static ColorSpace cs_CIEXYZ = null;
	private static ColorSpace cs_sRGB = null;
	
	private int type;
	private int numComponents;
	
	/**
	 * Constructs a ColorSpace object given a color space type and the number of components.
	 * @param type One of the ColorSpace type constants.
	 * @param numcomponents The number of components in the color space.
	 */
	protected ColorSpace(int type, int numcomponents)
	{
		this.numComponents = numcomponents;
		this.type = type;
	}
	
	/**
	 * Returns the name of the component given the component index.
	 * @param idx The component index.
	 * @return The name of the component at the specified index.
	 */
	public String getName(int idx)
	{
		if (idx < 0 || idx > numComponents - 1)
		{
			throw new IllegalArgumentException("Invalid component index: " + idx);
		}
		return "Unnamed color component #" + idx;
	}
	
	/**
	 * Transforms a color value assumed to be in this ColorSpace into a value in the default CS_sRGB color space.
	 * @param colorvalue A float array with length of at least the number of components in this ColorSpace.
	 * @return A float array of length 3.
	 * @throws ArrayIndexOutOfBoundsException If array length is not at least the number of components in this ColorSpace.
	 */
	public abstract float[] toRGB(float[] colorvalue);
	
	/**
	 * Transforms a color value assumed to be in this ColorSpace into the CS_CIEXYZ conversion color space.
	 * @param colorvalue A float array with length of at least the number of components in this ColorSpace.
	 * @return A float array of length 3.
	 * @throws ArrayIndexOutOfBoundsException If array length is not at least the number of components in this ColorSpace.
	 */
	public abstract float[] toCIEXYZ(float[] colorvalue);
	
	/**
	 * Transforms a color value assumed to be in the default CS_sRGB color space into this ColorSpace.
	 * @param rgbvalue A float array with length of at least 3.
	 * @return A float array with length equal to the number of components in this ColorSpace.
	 * @throws ArrayIndexOutOfBoundsException If array length is not at least 3.
	 */
	public abstract float[] fromRGB(float[] rgbvalue);
	
	/**
	 * Transforms a color value assumed to be in the CS_CIEXYZ conversion color space into this ColorSpace.
	 * @param colorvalue A float array with length of at least 3.
	 * @return A float array with length equal to the number of components in this ColorSpace.
	 * @throws ArrayIndexOutOfBoundsException If array length is not at least 3.
	 */
	public abstract float[] fromCIEXYZ(float[] colorvalue);
	
	/**
	 * Returns the minimum normalized color component value for the specified component.
	 * @param component The component index.
	 * @return The minimum normalized component value.
	 * @throws IllegalArgumentException If component is less than 0 or greater than numComponents - 1.
	 */
	public float getMinValue(int component)
	{
		if (component < 0 || component > numComponents - 1)
		{
			throw new IllegalArgumentException("Invalid component index: " + component);
		}
		return 0;
	}
	
	/**
	 * Returns the maximum normalized color component value for the specified component.
	 * @param component The component index.
	 * @return The maximum normalized component value.
	 * @throws IllegalArgumentException If component is less than 0 or greater than numComponents - 1.
	 */
	public float getMaxValue(int component)
	{
		if (component < 0 || component > numComponents - 1)
		{
			throw new IllegalArgumentException("Invalid component index: " + component);
		}
		return 1;
	}
	
	/**
	 * Returns true if the ColorSpace is CS_sRGB.
	 * @return <code>true</code> if this is a <code>CS_sRGB</code> color space, <code>false</code> if it is not.
	 */
	public boolean isCS_sRGB()
	{
		// If our color space is sRGB, then cs_sRGB is already initialized
		return (this == cs_sRGB);
	}

	
	/**
	 * Returns the color space type of this ColorSpace (for example TYPE_RGB, TYPE_XYZ, ...).
	 * @return The type constant that represents the type of this ColorSpace.
	 */
	public int getType()
	{
		return type;
	}
	
	/**
	 * Returns the number of components of this ColorSpace.
	 * @return The number of components in this ColorSpace.
	 */
	public int getNumComponents()
	{
		return numComponents;  
	}
	
	/**
	 * Returns a ColorSpace representing one of the specific predefined color spaces.
	 * @param colorspace A specific color space identified by one of the predefined class constants (e.g. CS_sRGB, CS_LINEAR_RGB, CS_CIEXYZ, CS_GRAY, or CS_PYCC).
	 * @return The requested ColorSpace object.
	 */
	public static ColorSpace getInstance(int colorspace)
	{
		switch(colorspace)
		{
			case CS_sRGB:
				if (cs_sRGB == null)
				{
					cs_sRGB = new ICC_ColorSpace(new ICC_ProfileStub(CS_sRGB));
				}
				return cs_sRGB;
			case CS_CIEXYZ:
				if (cs_CIEXYZ == null)
				{
					cs_CIEXYZ = new ICC_ColorSpace(new ICC_ProfileStub(CS_CIEXYZ));
				}
				return cs_CIEXYZ;
			case CS_GRAY:
				if (cs_Gray == null)
				{
					cs_Gray = new ICC_ColorSpace(new ICC_ProfileStub(CS_GRAY));
				}
				return cs_Gray;
		}
		// Unknown argument passed
		throw new IllegalArgumentException("Not a predefined colorspace");
	}
}
