//#preprocessor

/*
 * File: ShaderType2.java
 * Version: 1.2
 * Initial Creation: May 14, 2010 11:50:42 AM
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
package com.sun.pdfview.pattern;

import java.io.IOException;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Color;
//#ifndef BlackBerrySDK4.5.0
import net.rim.device.api.util.MathUtilities;
//#endif

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.function.PDFFunction;
import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.ColorSpace;
import com.sun.pdfview.helper.XYPointFloat;
import com.sun.pdfview.helper.graphics.Paint;
import com.sun.pdfview.helper.graphics.PaintGenerator;
import com.sun.pdfview.helper.graphics.TranslatedBitmap;

/**
 * A shader that performs axial shader based on a function.
 */
public class ShaderType2 extends PDFShader
{
	/** the start of the axis */
    private XYPointFloat axisStart;
    
    /** the end of the axis */
    private XYPointFloat axisEnd;
    
    /** the domain minimum */
    private float minT = 0f;
    
    /** the domain maximum */
    private float maxT = 1f;
    
    /** whether to extend the start of the axis */
    private boolean extendStart = false;
    
    /** whether to extend the end of the axis */
    private boolean extendEnd = false;
    
    /** functions, as an array of either 1 or n functions */
    private PDFFunction[] functions;
    
    /** Creates a new instance of ShaderType2 */
    public ShaderType2()
    {
        super(2);
    }
    
    /** 
     * Parse the shader-specific data
     */
    public void parse(PDFObject shaderObj) throws IOException
    {
        // read the axis coordinates (required)
        PDFObject coordsObj = shaderObj.getDictRef("Coords");
        if (coordsObj == null)
        {
            throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.PATTERN_SHADERTYPE2_NO_COORDINATES));
        }
        PDFObject[] coords = coordsObj.getArray();
        XYPointFloat start = new XYPointFloat(coords[0].getFloatValue(), coords[1].getFloatValue());
        XYPointFloat end   = new XYPointFloat(coords[2].getFloatValue(), coords[3].getFloatValue());
        setAxisStart(start);
        setAxisEnd(end);
        
        // read the domain (optional)
        PDFObject domainObj = shaderObj.getDictRef("Domain");
        if (domainObj != null)
        {
            PDFObject[] domain = domainObj.getArray();
            setMinT(domain[0].getFloatValue());
            setMaxT(domain[1].getFloatValue());
        }
        
        // read the functions (required)
        PDFObject functionObj = shaderObj.getDictRef("Function");
        if (functionObj == null)
        {
            throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.PATTERN_SHADERTYPE2_NO_SHADER_FUNCTION));
        }
        PDFObject[] functionArray = functionObj.getArray();
        int len;
        PDFFunction[] functions = new PDFFunction[len = functionArray.length];
        for (int i = 0; i < len; i++)
        {
            functions[i] = PDFFunction.getFunction(functionArray[i]);
        }
        setFunctions(functions);
        
        // read the extend array (optional)
        PDFObject extendObj = shaderObj.getDictRef("Extend");
        if (extendObj != null)
        {
            PDFObject[] extendArray = extendObj.getArray();
            setExtendStart(extendArray[0].getBooleanValue());
            setExtendEnd(extendArray[1].getBooleanValue());
        }
    }
    
    /**
     * Create a paint that paints this pattern
     */
    public PDFPaint getPaint()
    {
        return PDFPaint.getPaint(new Type2Paint());
    }
    
    /** 
     * Get the start of the axis
     */
    public XYPointFloat getAxisStart()
    {
        return axisStart;
    }
    
    /**
     * Set the start of the axis
     */
    protected void setAxisStart(XYPointFloat axisStart)
    {
        this.axisStart = axisStart;
    }
    
    /** 
     * Get the end of the axis
     */
    public XYPointFloat getAxisEnd()
    {
        return axisEnd;
    }
    
    /**
     * Set the start of the axis
     */
    protected void setAxisEnd(XYPointFloat axisEnd)
    {
        this.axisEnd = axisEnd;
    }
    
    /** 
     * Get the domain minimum
     */
    public float getMinT()
    {
        return minT;
    }
    
    /**
     * Set the domain minimum
     */
    protected void setMinT(float minT)
    {
        this.minT = minT;
    }
    
    /** 
     * Get the domain maximum
     */
    public float getMaxT()
    {
        return maxT;
    }
    
    /**
     * Set the domain maximum
     */
    protected void setMaxT(float maxT)
    {
        this.maxT = maxT;
    }
    
    /**
     * Get whether to extend the start of the axis
     */
    public boolean getExtendStart()
    {
        return extendStart;
    }
    
    /**
     * Set whether to extend the start of the axis
     */
    protected void setExtendStart(boolean extendStart)
    {
        this.extendStart = extendStart;
    }
    
    /**
     * Get whether to extend the end of the axis
     */
    public boolean getExtendEnd()
    {
        return extendEnd;
    }
    
    /**
     * Set whether to extend the end of the axis
     */
    protected void setExtendEnd(boolean extendEnd)
    {
        this.extendEnd = extendEnd;
    }
    
    /**
     * Get the functions associated with this shader
     */
    public PDFFunction[] getFunctions()
    {
        return functions;
    }
    
    /**
     * Set the functions associated with this shader
     */
    protected void setFunctions(PDFFunction[] functions)
    {
        this.functions = functions;
    }
    
    /**
     * A subclass of paint that uses this shader to generate a paint
     */
    class Type2Paint extends Paint
    {
        public Type2Paint()
        {
        }
        
        /** create a paint context */
        public PaintGenerator createGenerator(AffineTransform xform) 
        {
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            
            XYPointFloat devStart = getAxisStart();
            xform.transformPoint(devStart, devStart);
            
            XYPointFloat devEnd = getAxisEnd();
            xform.transformPoint(devEnd, devEnd);
          
            return new Type2PaintContext(cs, devStart, devEnd);
        }
        
        public int getTransparency()
        {
            return Paint.TRANSPARENCY_TRANSLUCENT;
        }

        public int getColor()
		{
			return Color.BLACK;
		}
    }
    
    /** 
     * A simple paint context that uses an existing raster in device
     * space to generate pixels
     */
    class Type2PaintContext extends PaintGenerator
    {
        /** the color space */
        private ColorSpace colorSpace;
        
        /** the start of the axis */
        private XYPointFloat start;
        
        /** the end of the axis */
        private XYPointFloat end;
        
        /**
         * Create a paint context
         */
        Type2PaintContext(ColorSpace colorSpace, XYPointFloat start, XYPointFloat end)
        {
            this.colorSpace = colorSpace;
            this.start = start;
            this.end = end;
        }
        
        public void dispose()
        {
        	colorSpace = null;
        }
        
        public ColorSpace getColorSpace()
        {
            return colorSpace;
        }
        
        public TranslatedBitmap getBitmap(int x, int y, int w, int h)
        {
            ColorSpace cs = getColorSpace();
            
            PDFFunction[] functions = getFunctions();
            int numComponents = cs.getNumComponents();
            
            float x0 = start.x;
            float x1 = end.x;
            float y0 = start.y;
            float y1 = end.y;
            
            float[] inputs = new float[1];
            float[] outputs = new float[numComponents];
            
            // all the data, plus alpha channel
            byte[] data = new byte[w * h * (numComponents + 1)];
            
            // for each device coordinate
            for (int j = 0; j < h; j++)
            {
                for (int i = 0; i < w + 8; i += 8)
                {
                    // find t for that user coordinate
                    float xp = getXPrime(i + x, j + y, x0, y0, x1, y1);
                    float t = getT(xp);
                    
                    // calculate the pixel values at t
                    inputs[0] = t;
                    if (functions.length == 1)
                    {
                        functions[0].calculate(inputs, 0, outputs, 0);
                    }
                    else
                    {
                    	int len = functions.length;
                        for (int c = 0; c < len; c++)
                        {
                            functions[c].calculate(inputs, 0, outputs, c);
                        }
                    }
                    
                    for (int q = i; q < i + 8 && q < w; q++)
                    {
                        int base = (j * w + q) * (numComponents + 1);
                        for (int c = 0; c < numComponents; c++)
                        {
                            data[base + c] = (byte)(outputs[c] * 255);
                        }
                        data[base + numComponents] = (byte)255; 
                    }
                }
            }
            
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
            Bitmap raster = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, w, h);
            raster.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
//#else
            Bitmap raster = new Bitmap(Bitmap.ROWWISE_32BIT_ARGB8888, w, h);
//#endif
            //Convert the data it compatible image data
            int len;
            int com = numComponents + 1;
            int[] imgData = new int[len = (w * h)];
            for(int i = 0, b = 0; i < len; i++)
            {
            	//TODO: need to make sure that the format is correct
            	for(int c = 0; c < com; c++)
            	{
            		imgData[i] |= data[b + c] << ((numComponents - c) * 8);
            	}
            }
            raster.setARGB(imgData, 0, w, 0, 0, w, h);
            
            TranslatedBitmap child = new TranslatedBitmap(raster, x, y);
            
            return child;
        }
        
        /**
         * x' = (x1 - x0) * (x - x0) + (y1 - y0) * (y - y0)
         *      -------------------------------------------
         *               (x1 - x0)^2 + (y1 - y0)^2
         */
        private float getXPrime(float x, float y, float x0, float y0, float x1, float y1)
        {
//#ifdef BlackBerrySDK4.5.0
        	double tp = (((x1 - x0) * (x - x0)) + ((y1 - y0) * (y - y0))) / (littlecms.internal.helper.Utility.pow(x1 - x0, 2) + littlecms.internal.helper.Utility.pow(y1 - y0, 2));
//#else
            double tp = (((x1 - x0) * (x - x0)) + ((y1 - y0) * (y - y0))) / (MathUtilities.pow(x1 - x0, 2) + MathUtilities.pow(y1 - y0, 2));
//#endif
            
            return (float)tp;
        }
        
        /**
         * t = t0 + (t1 - t0) x x'
         */
        private float getT(float xp)
        {
            float t0 = getMinT();
            float t1 = getMaxT();
            
            if (xp < 0)
            {
                return t0;
            }
            else if (xp > 1)
            {
                return t1;
            }
            else
            {
                return t0 + ((t1 - t0) * xp);
            }
        }
    }
}
