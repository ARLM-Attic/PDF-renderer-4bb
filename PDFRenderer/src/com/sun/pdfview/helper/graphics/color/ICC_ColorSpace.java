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

import littlecms.internal.lcms2;

/**
 * Partial implementation of java.awt.color.ICC_ColorSpace.
 */
public class ICC_ColorSpace extends ColorSpace
{
	/**
	 * According to ICC specification (from http://www.color.org)
	 * "For the CIEXYZ encoding, each component (X, Y, and Z)
	 * is encoded as a u1Fixed15Number".
	 * This means that max value for this encoding is 1 + (32767/32768)
	 */
	private static final float MAX_XYZ = 1f + (32767f / 32768f);
	private static final float MAX_SHORT = 65535f;
	private static final float INV_MAX_SHORT = 1f / MAX_SHORT;
	private static final float SHORT2XYZ_FACTOR = MAX_XYZ / MAX_SHORT;
	private static final float XYZ2SHORT_FACTOR = MAX_SHORT / MAX_XYZ;
	
	private ICC_Profile profile = null;
	private float[] minValues = null;
	private float[] maxValues = null;
	
	private Object[] converterCache;
	
	//Scaling helper values
	private float[] channelMulipliers = null;
	//private float[] invChannelMulipliers = null; //Used for "from" conventions
	
	private static final ICC_Profile sRGBProfile = ((ICC_ColorSpace)ColorSpace.getInstance(CS_sRGB)).getProfile();
	
	/**
	 * Constructs a new ICC_ColorSpace from an ICC_Profile object.
	 * @param pf The specified ICC_Profile object.
	 * @throws IllegalArgumentException If profile is inappropriate for representing a ColorSpace.
	 */
	public ICC_ColorSpace(ICC_Profile pf)
	{
		super(pf.getColorSpaceType(), pf.getNumComponents());
		
		switch (pf.getProfileClass())
		{
			case ICC_Profile.CLASS_COLORSPACECONVERSION:
			case ICC_Profile.CLASS_DISPLAY:
			case ICC_Profile.CLASS_OUTPUT:
			case ICC_Profile.CLASS_INPUT:
				break; // OK, it is color conversion profile
			default:
				throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COLOR_COLORSPACE_INVALID_CLASS));
		}
		
		profile = pf;
		converterCache = new Object[1];
		fillMinMaxValues();
	}
	
	private void fillMinMaxValues()
	{
        int n = getNumComponents();
        maxValues = new float[n];
        minValues = new float[n];
        channelMulipliers = new float[n];
        //invChannelMulipliers = new float[n];
        switch (getType())
        {
        	/*
            case ColorSpace.TYPE_XYZ:
                minValues[0] = 0;
                minValues[1] = 0;
                minValues[2] = 0;
                maxValues[0] = MAX_XYZ;
                maxValues[1] = MAX_XYZ;
                maxValues[2] = MAX_XYZ;
                break;
             */
            case ColorSpace.TYPE_Lab:
                minValues[0] = 0;
                minValues[1] = -128;
                minValues[2] = -128;
                maxValues[0] = 100;
                maxValues[1] = 127;
                maxValues[2] = 127;
                break;
            default:
                for(int i = 0; i < n; i++)
                {
                    minValues[i] = 0;
                    maxValues[i] = 1;
                }
                break;
        }
        
        //Setup scaling values
        for(int i = 0; i < n; i++)
        {
        	float dif = maxValues[i] - minValues[i];
        	channelMulipliers[i] = MAX_SHORT / dif;
        	//invChannelMulipliers[i] = dif / MAX_SHORT;
        }
    }
	
	public ICC_Profile getProfile()
	{
        if (profile instanceof ICC_ProfileStub)
        {
            profile = ((ICC_ProfileStub)profile).loadProfile();
        }
        
        return profile;
    }
	
	public float[] fromCIEXYZ(float[] colorvalue)
	{
		System.out.println("PDF ALERT---ICC_ColorSpace.fromCIEXYZ is called but not implemented.");
		throw new UnsupportedOperationException("To Implement");
	}
	
	public float[] fromRGB(float[] rgbvalue)
	{
		System.out.println("PDF ALERT---ICC_ColorSpace.fromRGB is called but not implemented.");
		throw new UnsupportedOperationException("To Implement");
	}
	
	public float[] toCIEXYZ(float[] colorvalue)
	{
		System.out.println("PDF ALERT---ICC_ColorSpace.toCIEXYZ is called but not implemented.");
		throw new UnsupportedOperationException("To Implement");
	}
	
	public float[] toRGB(float[] colorvalue)
	{
        short[] data = new short[getNumComponents()];
        
        scaleColor(colorvalue, data);
        
        short[] converted = ICC_Converter.convertColors(new ICC_Profile[]{this.getProfile(), sRGBProfile}, data, converterCache, 0); //Could probably cache ICC_Profile array, maybe later
        
        // unscale to sRGB
        float[] res = new float[3];
        
        res[0] = ((converted[0] & 0xFFFF)) * INV_MAX_SHORT;
        res[1] = ((converted[1] & 0xFFFF)) * INV_MAX_SHORT;
        res[2] = ((converted[2] & 0xFFFF)) * INV_MAX_SHORT;
        
        return res;
	}
	
	//To take the place of ColorScaler
	
	private void scaleColor(float[] pixelData, short[] chanData)
	{
		int n = getNumComponents();
        for (int chan = 0; chan < n; chan++)
        {
            chanData[chan] = (short)((pixelData[chan] - minValues[chan]) * channelMulipliers[chan] + 0.5f);
        }
	}
	
	/* Used for "from" conventions
	private void unscaleColor(float[] pixelData, short[] chanData)
	{
		int n = getNumComponents();
		for (int chan = 0; chan < n; chan++)
		{
	        pixelData[chan] = (chanData[chan] & 0xFFFF) * invChannelMulipliers[chan] + minValues[chan];
	    }
	}
	*/
	
	private static class ICC_Converter
	{
		public static short[] convertColors(ICC_Profile[] profiles, short[] src, Object[] converterCache, int cacheIndex)
		{
			int numProfiles = profiles.length;
			int[] renderingIntents = new int[numProfiles];
			// Default is perceptual
	        int currRenderingIntent = ICC_Profile.icPerceptual;
	        
	        // render as colorimetric for output device
	        if (profiles[0].getProfileClass() == ICC_Profile.CLASS_OUTPUT)
	        {
	            currRenderingIntent = ICC_Profile.icRelativeColorimetric;
	        }
	        
	        // get the transforms from each profile
	        for (int i = 0; i < numProfiles; i++)
	        {
	            // first or last profile cannot be abstract
	            // if profile is abstract, the only possible way is
	            // use AToB0Tag (perceptual), see ICC spec
	            if (i != 0 && i != numProfiles - 1 && profiles[i].getProfileClass() == ICC_Profile.CLASS_ABSTRACT)
	            {
	                currRenderingIntent = ICC_Profile.icPerceptual;
	            }
	            
	            renderingIntents[i] = currRenderingIntent;
	            // use current rendering intent
	            // to select LUT from the next profile (chaining)
	            currRenderingIntent = profiles[i].getRenderingIntent();
	        }
			
			return convertColors(profiles, renderingIntents, src, converterCache, cacheIndex);
		}
		
		public static short[] convertColors(ICC_Profile[] profiles, int[] intents, short[] src, Object[] converterCache, int cacheIndex)
		{
			//Get/Setup converter (basically the cmsHTRANSFORM)
			lcms2.cmsHTRANSFORM converter = null;
			int inputLength = lcms2.cmsMAXCHANNELS; //For safety use the max possible channels.
			int outputLength = lcms2.cmsMAXCHANNELS;
			if(converterCache != null && converterCache.length > 0 && converterCache[cacheIndex] instanceof Object[])
			{
				Object[] cache = (Object[])converterCache[cacheIndex];
				if(cache.length >= 3 && cache[0] instanceof Integer && cache[1] instanceof Integer && cache[2] instanceof lcms2.cmsHTRANSFORM)
				{
					inputLength = ((Integer)cache[0]).intValue();
					outputLength = ((Integer)cache[1]).intValue();
					converter = (lcms2.cmsHTRANSFORM)cache[2]; //Hopefully there the intent and profiles used to create this are the same as those in use now
				}
			}
			if(converter == null)
			{
				converter = setupConverter(profiles, intents);
				inputLength = profiles[0].getNumComponents();
				outputLength = profiles[profiles.length - 1].getNumComponents();
				if(converterCache != null && converterCache.length > 0)
				{
					//Cache so it can be used for later use
					converterCache[cacheIndex] = new Object[]{new Integer(inputLength), new Integer(outputLength), converter};
				}
			}
			
			if(src.length < inputLength)
			{
				throw new IllegalStateException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COLOR_COLORSPACE_NOT_ENOUGH_CHANNELS));
			}
			
			short[] dst = new short[outputLength];
			
			lcms2.cmsDoTransform(converter, src, dst, 1);
			
			return dst;
		}

		
		private static lcms2.cmsHTRANSFORM setupConverter(ICC_Profile[] profiles, int[] intents)
		{
			int nProfiles = profiles.length;
			
			if (nProfiles <= 0 || nProfiles > 255)
			{
				throw new IllegalArgumentException("Wrong number of profiles. 1..255 expected, " + nProfiles + " found.");
			}
			
			lcms2.cmsHPROFILE[] iccProfiles = new lcms2.cmsHPROFILE[nProfiles];
			
			for (int i = 0; i < nProfiles; i++)
			{
				lcms2.cmsHPROFILE profile = iccProfiles[i] = profiles[i].profile;
				
				//Cache the headers for use in other parts of the setup
				int deviceClass = lcms2.cmsGetDeviceClass(profile);
				if (deviceClass == ICC_Profile.icSigNamedColorClass || deviceClass == ICC_Profile.icSigLinkClass)
				{
					return null; // Unsupported named color and device link profiles
			    }
			}
			
			return lcms2.cmsCreateExtendedTransform(null, nProfiles, iccProfiles, new boolean[nProfiles], intents, new double[nProfiles], null, 0, 
					(2 << lcms2.BYTES_SHIFT_VALUE) | lcms2.CHANNELS_SH(lcms2.cmsChannelsOf(lcms2.cmsGetColorSpace(iccProfiles[0]))), 
					(2 << lcms2.BYTES_SHIFT_VALUE) | lcms2.CHANNELS_SH(lcms2.cmsChannelsOf(lcms2.cmsGetColorSpace(iccProfiles[nProfiles - 1]))), 
					lcms2.cmsFLAGS_FORCE_CLUT | lcms2.cmsFLAGS_CLUT_PRE_LINEARIZATION);
		}
	}
}
