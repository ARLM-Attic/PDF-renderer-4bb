/*
 * File: IndexedColor.java
 * Version: 1.4
 * Initial Creation: May 12, 2010 9:15:29 PM
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

import java.io.IOException;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.helper.PDFUtil;

/**
 * A PDFColorSpace for an IndexedColor model
 *
 * @author Mike Wessler
 */
public class IndexedColor extends PDFColorSpace
{
	/**
     * r,g,and b components of the color table as a single array, for
     * Java's IndexColorModel */
    protected byte[] finalcolors;
    /** the color table */
    int[] table;
    /** size of the color table */
    int count;
    /** number of channels in the base Color Space (unused) */
    int nchannels = 1;
    
    /**
     * create a new IndexColor PDFColorSpace based on another PDFColorSpace,
     * a count of colors, and a stream of values.  Every consecutive n bytes
     * of the stream is interpreted as a color in the base ColorSpace, where
     * n is the number of components in that color space.
     *
     * @param base the color space in which the data is interpreted
     * @param count the number of colors in the table
     * @param stream a stream of bytes.  The number of bytes must be count*n,
     * where n is the number of components in the base colorspace.
     */
    public IndexedColor(PDFColorSpace base, int count, PDFObject stream) throws IOException
    {
        super(null);
        count++;
        this.count = count;
        byte[] data = stream.getStream();
        nchannels = base.getNumComponents();
        boolean offSized = (data.length / nchannels) < count;
        finalcolors = new byte[3 * count];
        table = new int[count];
        float comps[] = new float[nchannels];
        int loc = 0;
        int finalloc = 0;
        for (int i = 0; i < count; i++)
        {
        	int len = comps.length;
            for (int j = 0; j < len; j++)
            {
                if (loc < data.length)
                {
                    comps[j] = (((int)data[loc++]) & 0xff) / 255f;
                }
                else
                {
                    comps[j] = 1.0f;
                }
            }
            table[i] = base.getPaint(comps).getPaint().getColor();
            finalcolors[finalloc++] = (byte)PDFUtil.Color_getRed(table[i]);
            finalcolors[finalloc++] = (byte)PDFUtil.Color_getGreen(table[i]);
            finalcolors[finalloc++] = (byte)PDFUtil.Color_getBlue(table[i]);
        }
    }
    
    /**
     * create a new IndexColor PDFColorSpace based on a table of colors.  
     * 
     * @param table an array of colors
     */
    public IndexedColor(int[] table) throws IOException
    {
        super(null);
        
        this.count = table.length;
        this.table = table;
        
        finalcolors = new byte[3 * count];
        nchannels = 3;
        
        int loc = 0;
        
        for (int i = 0; i < count; i++)
        {
        	finalcolors[loc++] = (byte)PDFUtil.Color_getRed(table[i]);
            finalcolors[loc++] = (byte)PDFUtil.Color_getGreen(table[i]);
            finalcolors[loc++] = (byte)PDFUtil.Color_getBlue(table[i]);
        }
    }
    
    /**
     * Get the number of indices
     */
    public int getCount()
    {
        return count;
    }
    
    /**
     * Get the table of color components
     */
    public byte[] getColorComponents()
    {
        return finalcolors;
    }
    
    /**
     * get the number of components of this colorspace (1)
     */
    public int getNumComponents()
    {
        return 1;
    }
    
    /**
     * get the color represented by the index.
     * @param components an array of exactly one integer number whose
     * value is between 0 and the size of the color table - 1.
     */
    public PDFPaint getPaint(float[] components)
    {
        return PDFPaint.getColorPaint(table[(int)components[0]]);
    }
}
