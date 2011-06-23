//#preprocessor

/*
 * File: CMapFormat0.java
 * Version: 1.2
 * Initial Creation: May 19, 2010 5:38:11 PM
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
 *
 * @author  jkaplan
 */
public class CMapFormat0 extends CMap
{
	/**
     * The glyph index array
     */
    private byte[] glyphIndex;
    
    /** Creates a new instance of CMapFormat0 */
    protected CMapFormat0(short language)
    {
        super((short)0, language);
        
        byte[] initialIndex = new byte[256];
        for (int i = 0; i < 256; i++)
        {
            initialIndex[i] = (byte) i;
        }
        setMap(initialIndex);
    }
    
    /**
     * Get the length of this table
     */
    public short getLength()
    {
        return (short)262;
    }
    
    /** 
     * Map from a byte
     */
    public byte map(byte src)
    {
        int i = 0xff & src;
        
        return glyphIndex[i];
    }
    
    /**
     * Cannot map from short
     */
    public char map(char src)
    {
        if (src  < 0 || src > 255)
        {
            // out of range
            return (char)0;
        }
        
        return (char)(map((byte)src) & 0xff);
    }
    
    /**
     * Get the src code which maps to the given glyphID
     */
    public char reverseMap(short glyphID)
    {
    	int len = glyphIndex.length;
        for (int i = 0; i < len; i++)
        {
            if ((glyphIndex[i] & 0xff) == glyphID)
            {
                return (char)i;
            }
        }
        
        return (char)0;
    }
    
    /**
     * Set the entire map
     */
    public void setMap(byte[] glyphIndex)
    {
        if (glyphIndex.length != 256)
        {
            throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.FONT_TTF_CMAP0_BAD_GLYPH_MAP_SIZE));
        }
        
        this.glyphIndex = glyphIndex;
    }
    
    /**
     * Set a single mapping entry
     */
    public void setMap(byte src, byte dest)
    {
        int i = 0xff & src;
        
        glyphIndex[i] = dest;
    }
    
    /**
     * Get the whole map
     */
    protected byte[] getMap()
    {
        return glyphIndex;
    }
    
    /**
     * Get the data in this map as a ByteBuffer
     */
    public ByteBuffer getData()
    {
        ByteBuffer buf = ByteBuffer.allocateDirect(262);
        
        buf.putShort(getFormat());
        buf.putShort(getLength());
        buf.putShort(getLanguage());
        buf.put(getMap());
        
        // reset the position to the beginning of the buffer
        buf.flip();
        
        return buf;
    }
    
    /** 
     * Read the map in from a byte buffer
     */
    public void setData(int length, ByteBuffer data)
    {
        if (length != 262)
        {
            throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.FONT_TTF_CMAP0_BAD_LEN));
        }
        
        if (data.remaining() != 256)
        {
            throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.FONT_TTF_CMAP0_WRONG_DATA_AMOUNT));
        }
        
        byte[] map = new byte[256];
        data.get(map);
        
        setMap(map);
    }
}
