/*
 * File: PDFPattern.java
 * Version: 1.3
 * Initial Creation: May 14, 2010 7:09:43 AM
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
import java.util.Hashtable;

import net.rim.device.api.math.Matrix4f;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.PDFParseException;

/**
 * The abstract superclass of all PDF Pattern types
 */
public abstract class PDFPattern
{
	/** the pattern type (1 or 2) */
    private int type;
    
    /** the matrix to transform from pattern space to PDF space */
    private Matrix4f xform;
    
    /** Creates a new instance of PDFPattern */
    protected PDFPattern(int type) 
    {
        this.type = type;
    }
    
    /**
     * Read a pattern from the given pattern stream
     */
    public static PDFPattern getPattern(PDFObject patternObj, Hashtable resources) throws IOException
    {
        // see if the pattern is already cached
        PDFPattern pattern = (PDFPattern)patternObj.getCache();
        if (pattern != null)
        {
            return pattern;
        }
        
        // get the pattern type
        int type = patternObj.getDictRef("PatternType").getIntValue();
         
        // read the pattern transform matrix
        PDFObject matrix = patternObj.getDictRef("Matrix");
        Matrix4f xform = null;
        if (matrix == null)
        {
            xform = new Matrix4f();
        }
        else
        {
            float[] elts = new float[6];
            for (int i = 0; i < 6; i++)
            {
                elts[i] = ((PDFObject)matrix.getAt(i)).getFloatValue();
            }
            
            xform = new Matrix4f(elts);
        }
        
        switch (type)
        {
            case 1:
                pattern = new PatternType1();
                break;
            default:
                throw new PDFParseException("Unknown pattern type " + type);
        }
        
        // set the transform
        pattern.setTransform(xform);
        
        // parse the pattern-specific data
        pattern.parse(patternObj, resources);
        
        // set the cache
        patternObj.setCache(pattern);
        
        return pattern;
    }
    
    /**
     * Get the type of this pattern
     */
    public int getPatternType()
    {
        return type;
    }
    
    /**
     * Get the transform associated with this pattern
     */
    public Matrix4f getTransform()
    {
        return xform;
    }
    
    /**
     * Set the transform associated with this pattern
     */
    protected void setTransform(Matrix4f xform)
    {
        this.xform = xform;
    }
    
    /**
     * Parse the pattern-specific information from the pdf object
     *
     * @param patternObj the pdfobject with data for this pattern
     */
    protected abstract void parse(PDFObject patternObj, Hashtable resources) throws IOException;
    
    /**
     * Returns paint that represents the selected pattern
     *
     * @param basePaint the background paint color, or null for none
     */
    public abstract PDFPaint getPaint(PDFPaint basePaint);
}
