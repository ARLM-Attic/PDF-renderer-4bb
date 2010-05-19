/*
 * File: CIDFontType2.java
 * Version: 1.5
 * Initial Creation: May 16, 2010 12:57:04 PM
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
package com.sun.pdfview.font;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import com.sun.pdfview.PDFObject;

/**
 * a font object derived from a CID font.
 *
 * @author Jonathan Kaplan
 */
public class CIDFontType2 extends TTFFont
{
	/**
     * The width of each glyph from the DW and W arrays
     */
    private Hashtable widths = null;
    /**
     * The vertical width of each glyph from the DW2 and W2 arrays
     */
    private Hashtable widthsVertical = null;
    
    /**
     * the default width
     */
    private int defaultWidth = 1000;
    /**
     * the default vertical width
     */
    private int defaultWidthVertical = 1000;
    /** the CIDtoGID map, if any */
    private ByteBuffer cidToGidMap;
    
    /**
     * create a new CIDFontType2 object based on the name of a built-in font
     * and the font descriptor
     * @param baseName the name of the font, from the PDF file
     * @param fontObj a dictionary that contains the DW (defaultWidth) and
     * W (width) parameters
     * @param descriptor a descriptor for the font
     */
    public CIDFontType2(String baseName, PDFObject fontObj, PDFFontDescriptor descriptor) throws IOException
    {
        super(baseName, fontObj, descriptor);
        
        parseWidths(fontObj);
        
        // read the CIDSystemInfo dictionary (required)
        PDFObject systemInfoObj = fontObj.getDictRef("CIDSystemInfo");
        // read the cid to gid map (optional)
        PDFObject mapObj = fontObj.getDictRef("CIDToGIDMap");
        
        // only read the map if it is a stream (if it is a name, it
        // is "Identity" and can be ignored
        if (mapObj != null && (mapObj.getType() == PDFObject.STREAM))
        {
            cidToGidMap = mapObj.getStreamBuffer();
        }
    }
    
    /** Parse the Widths array and DW object */
    private void parseWidths(PDFObject fontObj) throws IOException
    {
        // read the default width (otpional)
        PDFObject defaultWidthObj = fontObj.getDictRef("DW");
        if (defaultWidthObj != null)
        {
            defaultWidth = defaultWidthObj.getIntValue();
        }
        
        int entryIdx = 0;
        int first = 0;
        int last = 0;
        PDFObject[] widthArray;
        
        // read the widths table 
        PDFObject widthObj = fontObj.getDictRef("W");
        int len;
        if (widthObj != null)
        {
            // initialize the widths array
            widths = new Hashtable();
            
            // parse the width array
            widthArray = widthObj.getArray();
            
            /* an entry can be in one of two forms:
             *   <startIndex> <endIndex> <value> or
             *   <startIndex> [ array of values ]
             * we use the entryIdx to differentitate between them
             */
            len = widthArray.length;
            for (int i = 0; i < len; i++)
            {
                if (entryIdx == 0)
                {
                    // first value in an entry.  Just store it
                    first = widthArray[i].getIntValue();
                }
                else if (entryIdx == 1)
                {
                    // second value -- is it an int or array?
                    if (widthArray[i].getType() == PDFObject.ARRAY)
                    {
                        // add all the entries in the array to the width array
                        PDFObject[] entries = widthArray[i].getArray();
                        int len2 = entries.length;
                        for (int c = 0; c < len2; c++)
                        {
                            Character key = new Character((char)(c + first));
                            
                            // value is width / default width
                            float value = entries[c].getIntValue();
                            widths.put(key, new Float(value));
                        }
                        // all done
                        entryIdx = -1;
                    }
                    else
                    {
                        last = widthArray[i].getIntValue();
                    }
                }
                else
                {
                    // third value.  Set a range
                    int value = widthArray[i].getIntValue();
                    
                    // set the range
                    for (int c = first; c <= last; c++)
                    {
                        widths.put(new Character((char)c), new Float(value));
                    }
                    
                    // all done
                    entryIdx = -1;
                }
                
                entryIdx++;
            }
        }
        
        // read the optional vertical default width
        defaultWidthObj = fontObj.getDictRef("DW2");
        if (defaultWidthObj != null)
        {
            defaultWidthVertical = defaultWidthObj.getIntValue();
        }
        
        // read the vertical widths table
        widthObj = fontObj.getDictRef("W2");
        if (widthObj != null)
        {
            // initialize the widths array
            widthsVertical = new Hashtable();
            
            // parse the width2 array
            widthArray = widthObj.getArray();
            
            /* an entry can be in one of two forms:
             *   <startIndex> <endIndex> <value> or
             *   <startIndex> [ array of values ]
             * we use the entryIdx to differentitate between them
             */
            entryIdx = 0;
            first = 0;
            last = 0;
            
            len = widthArray.length;
            for (int i = 0; i < len; i++)
            {
                if (entryIdx == 0)
                {
                    // first value in an entry.  Just store it
                    first = widthArray[i].getIntValue();
                }
                else if (entryIdx == 1)
                {
                    // second value -- is it an int or array?
                    if (widthArray[i].getType() == PDFObject.ARRAY)
                    {
                        // add all the entries in the array to the width array
                        PDFObject[] entries = widthArray[i].getArray();
                        int len2 = entries.length;
                        for (int c = 0; c < len; c++)
                        {
                            Character key = new Character((char)(c + first));
                            
                            // value is width / default width
                            float value = entries[c].getIntValue();
                            widthsVertical.put(key, new Float(value));
                        }
                        // all done
                        entryIdx = -1;
                    }
                    else
                    {
                        last = widthArray[i].getIntValue();
                    }
                }
                else
                {
                    // third value.  Set a range
                    int value = widthArray[i].getIntValue();
                    
                    // set the range
                    for (int c = first; c <= last; c++)
                    {
                        widthsVertical.put(new Character((char)c), new Float(value));
                    }
                    
                    // all done
                    entryIdx = -1;
                }
                
                entryIdx++;
            }
        }
    }
    
    /** Get the default width in text space */
    public int getDefaultWidth()
    {
        return defaultWidth;
    }
    
    /** Get the width of a given character */
    public float getWidth(char code, String name)
    {
        if (widths == null)
        {
            return 1f;
        }
        Float w = (Float)widths.get(new Character(code));
        if (w == null)
        {
            return 1f;
        }
        
        return w.floatValue() / getDefaultWidth();
    }
    
    /** Get the default vertical width in text space */
    public int getDefaultWidthVertical()
    {
        return defaultWidthVertical;
    }
    
    /** Get the vertical width of a given character */
    public float getWidthVertical(char code, String name)
    {
        if (widthsVertical == null)
        {
            return 1f;
        }
        Float w = (Float)widthsVertical.get(new Character(code));
        if (w == null)
        {
            return 1f;
        }
        
        return w.floatValue() / getDefaultWidth();
    }
    
    /**
     * Get the outline of a character given the character code.  We
     * interpose here in order to avoid using the CMap of the font in
     * a CID mapped font.
     */
    protected synchronized GeneralPath getOutline(char src, float width)
    {
        int glyphId = (int)(src & 0xffff);
        
        // check if there is a cidToGidMap
        if (cidToGidMap != null)
        {
            // read the map
            glyphId = (char)cidToGidMap.getShort(glyphId * 2);
        }
        
        // call getOutline on the glyphId
        return getOutline(glyphId, width);
    }
}
