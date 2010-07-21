/*
 * File: MaskColorSpace.java
 * Version: 1.3
 * Initial Creation: May 13, 2010 5:11:54 PM
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.sun.pdfview.colorspace;

import net.rim.device.api.util.MathUtilities;

import com.sun.pdfview.helper.ColorSpace;
import com.sun.pdfview.helper.PDFUtil;

import com.sun.pdfview.PDFPaint;

/**
 * A color space used to implement masks.  For now, the only type of mask
 * supported is one where the image pixels specify where to paint, and the
 * painting itself is done in a pre-specified PDF Paint.
 */
public class MaskColorSpace extends ColorSpace
{
	/** The paint to paint in.  Note this cannot be a pattern or gradient. */
    private PDFPaint paint;
    
    /** Creates a new instance of PaintColorSpace */
    public MaskColorSpace(PDFPaint paint)
    {
        super (TYPE_RGB, 1);
        
        this.paint = paint;
    }
    
    public float[] fromCIEXYZ(float[] colorvalue)
    {
        float x = colorvalue[0];
        float y = colorvalue[1];
        float z = colorvalue[2];
        
        float[] mask = new float[1];
        
        if (MathUtilities.round(x) > 0 || MathUtilities.round(y) > 0 || MathUtilities.round(z) > 0)
        {
            mask[0] = 1;
        }
        else
        {
            mask[0] = 0; 
        }
        
        return mask;
    }
    
    public float[] fromRGB(float[] rgbvalue)
    {
        float r = rgbvalue[0];
        float g = rgbvalue[1];
        float b = rgbvalue[2];
        
        float[] mask = new float[1];
        
        if (MathUtilities.round(r) > 0 || MathUtilities.round(g) > 0 || MathUtilities.round(b) > 0)
        {
            mask[0] = 1;
        }
        else
        {
            mask[0] = 0; 
        }
        
        return mask;
    }
    
    ColorSpace cie = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
    float[] prev1 = cie.fromRGB(toRGB(new float[] {1.0f}));
    float[] prev0 = cie.fromRGB(toRGB(new float[] {0.0f}));

    public float[] toCIEXYZ(float[] colorvalue)
    {
		if (colorvalue[0] == 1)
		{
		    return prev1;
		}
		else if (colorvalue[0] == 0)
		{
		    return prev0;
		}
		else
		{
//			System.out.println("MaskColorSpace converting: " + colorvalue[0]);
		    return cie.fromRGB(toRGB(colorvalue));
		}
    }
    
    public float[] toRGB(float[] colorvalue)
    {
    	int color = paint.getPaint().getColor();
    	return new float[]{PDFUtil.Color_getRed(color) / 255f, PDFUtil.Color_getGreen(color) / 255f, PDFUtil.Color_getBlue(color) / 255f};
    }
    
    public int getNumComponents()
    {
    	return 1;
    }
}
