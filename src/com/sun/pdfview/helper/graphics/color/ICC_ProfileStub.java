/*
 * File: ICC_ProfileStub.java
 * Version: 1.0
 * Initial Creation: May 23, 2010 9:31:16 PM
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

import com.sun.pdfview.helper.ColorSpace;

/**
 * Partial implementation of java.awt.color.ICC_ProfileStub.
 * @author Oleg V. Khaschansky, Vincent Simonetti
 */
public final class ICC_ProfileStub extends ICC_Profile
{
	int colorspace;
	
	/**
	 * Create a new ICC_ProfileStub.
	 * @param csSpecifier The type of stub to create.
	 */
	public ICC_ProfileStub(int csSpecifier)
	{
        switch (csSpecifier)
        {
            case ColorSpace.CS_sRGB:
            case ColorSpace.CS_CIEXYZ:
            case ColorSpace.CS_GRAY:
                break;
            default:
                throw new IllegalArgumentException("Invalid colorspace");
        }
        colorspace = csSpecifier;
    }
	
	public int getProfileClass()
	{
        return CLASS_COLORSPACECONVERSION;
    }
	
	 public int getNumComponents()
	 {
		 switch (colorspace)
		 {
            case ColorSpace.CS_sRGB:
            case ColorSpace.CS_CIEXYZ:
                return 3;
            case ColorSpace.CS_GRAY:
                return 1;
            default:
                throw new UnsupportedOperationException("Stub cannot perform this operation");
		 }
	 }
	 
	 public int getColorSpaceType()
	 {
		 switch (colorspace)
		 {
            case ColorSpace.CS_sRGB:
                return ColorSpace.TYPE_RGB;
            case ColorSpace.CS_CIEXYZ:
                return ColorSpace.TYPE_XYZ;
            case ColorSpace.CS_GRAY:
                return ColorSpace.TYPE_GRAY;
            default:
                throw new UnsupportedOperationException("Stub cannot perform this operation");
		 }
	 }
	 
	 public ICC_Profile loadProfile()
	 {
		 switch (colorspace)
		 {
		 	case ColorSpace.CS_sRGB:
		 		return ICC_Profile.getInstance(ColorSpace.CS_sRGB);
		 	case ColorSpace.CS_GRAY:
		 		return ICC_Profile.getInstance(ColorSpace.CS_GRAY);
		 	case ColorSpace.CS_CIEXYZ:
		 		return ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ);
		 		/*
		 	case ColorSpace.CS_LINEAR_RGB:
		 		return ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB);
		 	case ColorSpace.CS_PYCC:
		 		return ICC_Profile.getInstance(ColorSpace.CS_PYCC);
		 		*/
		 	default:
		 		throw new UnsupportedOperationException("Stub cannot perform this operation");
		 }
	 }
}
