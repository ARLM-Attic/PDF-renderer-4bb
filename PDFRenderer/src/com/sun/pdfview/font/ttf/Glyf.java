//#preprocessor

/*
 * File: Glyf.java
 * Version: 1.2
 * Initial Creation: May 19, 2010 2:34:46 PM
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
package com.sun.pdfview.font.ttf;

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.ByteBuffer;
//#else
import com.sun.pdfview.helper.nio.ByteBuffer;
//#endif

/**
 * A single glyph in a pdf font.  May be simple or compound via subclasses
 */
public class Glyf
{
	/** If true, the glyf is compound */
    private boolean isCompound;
    
    /** the number of contours */
    private short numContours;
    
    /** the minimum x value */
    private short minX;
    
    /** the minimum y value */
    private short minY;
    
    /** the maximum x value */
    private short maxX;
    
    /** the maximum y value */
    private short maxY;
    
    /** 
     * Creates a new instance of glyf 
     * Don't use this directly, use <code>Glyf.getGlyf()</code>
     */
    protected Glyf()
    {
    }
    
    /**
     * Get a map from the given data
     *
     * This method reads the format, data and length variables of
     * the map.
     */
    public static Glyf getGlyf(ByteBuffer data)
    {
        short numContours = data.getShort();
        
        Glyf g = null;
        if (numContours == 0)
        {
            // no glyph data
            g = new Glyf();
        }
        else if (numContours == -1)
        {
            // compound glyf
            g = new GlyfCompound();
        }
        else if (numContours > 0)
        {
            // simple glyf
            g = new GlyfSimple();
        }
        else
        {
            throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.FONT_TTF_UNK_GLYF, new Object[]{new Integer(numContours & 0xFFFF)}));
        }
        
        g.setNumContours(numContours);
        g.setMinX(data.getShort());
        g.setMinY(data.getShort());
        g.setMaxX(data.getShort());
        g.setMaxY(data.getShort());
        
        // do glyphtype-specific parsing
        g.setData(data);
        
        return g;
    }
    
    /**
     * Set the data for this glyf.  Do nothing, since a glyf with
     * no contours has no glyf data.
     */
    public void setData(ByteBuffer data)
    {
        return;
    }
    
    /**
     * Get the data in this glyf as a byte buffer.  Return the basic
     * glyf data only, since there is no specific data.  This method returns
     * the data un-flipped, so subclasses can simply append to the allocated
     * buffer.
     */
    public ByteBuffer getData()
    {
        ByteBuffer buf = ByteBuffer.allocateDirect(getLength());
        
        buf.putShort(getNumContours());
        buf.putShort(getMinX());
        buf.putShort(getMinY());
        buf.putShort(getMaxX());
        buf.putShort(getMaxY());
        
        // don't flip the buffer, since it may be used by subclasses
        return buf;
    }
    
    /**
     * Get the length of this glyf.  A glyf with no data has a length
     * of 10 (2 bytes each for 5 short values)
     */
    public short getLength()
    {
        return 10;
    }
    
    /**
     * Get whether this is a simple or compound glyf
     */
    public boolean isCompound()
    {
        return isCompound;
    }
    
    /** 
     * Set whether this is a simple or compound glyf
     */
    protected void setCompound(boolean isCompound)
    {
        this.isCompound = isCompound;
    }
    
    /**
     * Get the number of contours in this glyf
     */
    public short getNumContours()
    {
        return numContours;
    }
    
    /**
     * Set the number of contours in this glyf
     */
    protected void setNumContours(short numContours)
    {
        this.numContours = numContours;
    }
    
    /**
     * Get the minimum x in this glyf
     */
    public short getMinX()
    {
        return minX;
    }
    
    /**
     * Set the minimum X in this glyf
     */
    protected void setMinX(short minX)
    {
        this.minX = minX;
    }
    
    /**
     * Get the minimum y in this glyf
     */
    public short getMinY()
    {
        return minY;
    }
    
    /**
     * Set the minimum Y in this glyf
     */
    protected void setMinY(short minY)
    {
        this.minY = minY;
    }
    /**
     * Get the maximum x in this glyf
     */
    public short getMaxX()
    {
        return maxX;
    }
    
    /**
     * Set the maximum X in this glyf
     */
    protected void setMaxX(short maxX)
    {
        this.maxX = maxX;
    }
    
    /**
     * Get the maximum y in this glyf
     */
    public short getMaxY()
    {
        return maxY;
    }
    
    /**
     * Set the maximum Y in this glyf
     */
    protected void setMaxY(short maxY)
    {
        this.maxY = maxY;
    }
}
