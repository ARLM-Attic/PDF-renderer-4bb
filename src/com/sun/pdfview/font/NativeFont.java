/*
 * File: NativeFont.java
 * Version: 1.4
 * Initial Creation: May 16, 2010 4:15:42 PM
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.math.Matrix4f;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.FontFamily;
import net.rim.device.api.ui.FontManager;
import net.rim.device.api.ui.Ui;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.font.ttf.CMap;
import com.sun.pdfview.font.ttf.CMapFormat0;
import com.sun.pdfview.font.ttf.CMapFormat4;
import com.sun.pdfview.font.ttf.CmapTable;
import com.sun.pdfview.font.ttf.HeadTable;
import com.sun.pdfview.font.ttf.HmtxTable;
import com.sun.pdfview.font.ttf.NameTable;
import com.sun.pdfview.font.ttf.PostTable;
import com.sun.pdfview.font.ttf.TrueTypeFont;
import com.sun.pdfview.font.ttf.TrueTypeTable;
import com.sun.pdfview.helper.PDFUtil;
import com.sun.pdfview.helper.graphics.Geometry;

/**
 * a font object derived from a true type font.
 *
 * @author Mike Wessler
 */
public class NativeFont extends OutlineFont 
{
	/** Control characters to filter out of the underlying font */
    protected static final char[] controlChars = {0x9, 0xa, 0xd};
    
    /** the ids of our favorite CMaps */
    protected static final short[] mapIDs = {
        3, 1, /* Microsoft Unicode */
        0, 0, /* unicode default */
        0, 3, /* unicode 2.0 map */
        1, 0 /* macintosh */};
    
    /** the actual font in use */
    private Font f;
    
    /** the cmap table from a TrueType font */
    private CmapTable cmapTable;
    
    /** the post table from a TrueType font */
    private PostTable postTable;
    
    /** the number of font units in one em */
    private int unitsPerEm;
    
    /** the hmtx table from the TrueType font */
    private HmtxTable hmtxTable;
    
    /**
     * create a new NativeFont object based on a description of the
     * font from the PDF file.  If the description happens to contain
     * an in-line true-type font file (under key "FontFile2"), use the
     * true type font.  Otherwise, parse the description for key information
     * and use that to generate an appropriate font.
     */
    public NativeFont (String baseFont, PDFObject fontObj, PDFFontDescriptor descriptor) throws IOException
    {
        super (baseFont, fontObj, descriptor);
        
        String fontName = descriptor.getFontName();
        
        PDFObject ttf = descriptor.getFontFile2();
        if (ttf != null)
        {
            byte[] fontdata = ttf.getStream();
            
            try
            {
                setFont(fontdata);
            }
            catch (Exception ffe)
            {
                throw new PDFParseException ("Font format exception: " + ffe);
            }
        }
        else
        {
            int flags = descriptor.getFlags();
            int style = ((flags & PDFFontDescriptor.FORCEBOLD) != 0) ? Font.BOLD : Font.PLAIN;
            
            if (fontName.indexOf("Bold") > 0)
            {
                style |= Font.BOLD;
            }
            if (descriptor.getItalicAngle() != 0)
            {
                style |= Font.ITALIC;
            }
            FontFamily family = null;
            try
            {
	            if ((flags & PDFFontDescriptor.FIXED_PITCH) != 0) // fixed width
	            {
	            	family = FontFamily.forName("BBSerifFixed"); //BlackBerry equivalent of "Monospaced"
	            }
	            else if ((flags & PDFFontDescriptor.SERIF) != 0) // serif font
	            {
	            	family = FontFamily.forName("BBAlpha Serif"); //BlackBerry equivalent of "Serif"
	            }
	            else
	            {
	            	family = FontFamily.forName("BBSansSerif"); //BlackBerry equivalent of "Sans-serif"
	            }
            }
            catch(ClassNotFoundException cnfe)
            {
            	//Not going to happen
            }
            setFont(family.getFont(style, 1, Ui.UNITS_pt));
        }
    }
    
    /**
     * Get a glyph outline by name
     *
     * @param name the name of the desired glyph
     * @return the glyph outline, or null if unavailable
     */
    protected Geometry getOutline (String name, float width)
    {
        if (postTable != null && cmapTable != null)
        {
            // map this character name to a glyph ID
            short glyphID = postTable.getGlyphNameIndex(name);
            
            if (glyphID == 0)
            {
                // no glyph -- try by index
                return null;
            }
            
            // the mapped character
            char mappedChar = 0;
            
            int len = mapIDs.length;
            for (int i = 0; i < len; i += 2)
            {
                CMap map = cmapTable.getCMap(mapIDs[i], mapIDs[i + 1]);
                if (map != null)
                {
                    mappedChar = map.reverseMap (glyphID);
                    
                    // we found a character
                    if (mappedChar != 0)
                    {
                        break;
                    }
                }
            }
            
            return getOutline(mappedChar, width);
        }
        
        // no maps found, hope the font can deal
        return null;
    }
    
    /**
     * Get a glyph outline by character code
     *
     * Note this method must always return an outline 
     *
     * @param src the character code of the desired glyph
     * @return the glyph outline
     */
    protected Geometry getOutline(char src, float width)
    {
        // some true type fonts put characters in the undefined
        // region of Unicode instead of as normal characters.
        if (!(f.getAdvance(src) > 0) && (f.getAdvance((char)(src + 0xf000)) > 0))
        {
            src += 0xf000;
        }
        // filter out control characters
        for (int i = 0; i < controlChars.length; i++)
        {
            if (controlChars[i] == src)
            {
                src = (char)(0xf000 | src);
                break;
            }
        }
        
        //TODO: Options for handling this, have a special function in the Path class that allows images so this can draw the glyph or to implement a basic OCR system. Leaning towards special image but if it is possible to get it fast enough go OCR because it allows easier scaling.
        Geometry gv = PDFUtil.Font_createGlyphVector(f, src);
        Geometry gp = new Geometry(gv);
        
        // this should be gv.getGlyphMetrics(0).getAdvance(), but that is
        // broken on the Mac, so we need to read the advance from the
        // hmtx table in the font
        CMap map = cmapTable.getCMap(mapIDs[0], mapIDs[1]);
        int glyphID = map.map (src);
        float advance = (float)hmtxTable.getAdvance(glyphID) / (float)unitsPerEm;
        
        float widthfactor = width / advance;
        Matrix4f scale = new Matrix4f();
        Matrix4f.createScale(widthfactor, -1, 1, scale);
        gp.transform(scale);
        
        return gp;
    }
    
    /**
     * Set the font
     *
     * @param f the font to use
     */
    protected void setFont(Font f)
    {
    	this.f = f;
        
        /*Can't get tables from native Font object, skip it
        
        // if it's an OpenType font, parse the relevant tables to get
        // glyph name to code mappings
        if (f instanceof OpenType)
        {
            OpenType ot = (OpenType)f;
            
            byte[] cmapData = ot.getFontTable(OpenType.TAG_CMAP);
            byte[] postData = ot.getFontTable(OpenType.TAG_POST);
            
            TrueTypeFont ttf = new TrueTypeFont(0x10000);
            
            cmapTable = (CmapTable)TrueTypeTable.createTable(ttf, "cmap", ByteBuffer.wrap(cmapData));
            ttf.addTable("cmap", cmapTable);
            
            postTable = (PostTable)TrueTypeTable.createTable(ttf, "post", ByteBuffer.wrap(postData));
            ttf.addTable ("post", postTable);
        }
        */
    }
    
    /**
     * Set the font
     *
     * @param fontdata the font data as a byte array
     */
    protected void setFont (byte[] fontdata) throws IOException
    {
    	//TODO: Update test
        // System.out.println("Loading " + getBaseFont());
        // FileOutputStream fos = new FileOutputStream("/tmp/" + getBaseFont() + ".ttf");
        // fos.write(fontdata);
        // fos.close();
    	
        try
        {
            // read the true type information
            TrueTypeFont ttf = TrueTypeFont.parseFont(fontdata);
            
            // System.out.println(ttf.toString());
            
            // get the cmap, post, and hmtx tables for later use
            cmapTable = (CmapTable)ttf.getTable("cmap");
            postTable = (PostTable)ttf.getTable("post");
            hmtxTable = (HmtxTable)ttf.getTable("hmtx");
            
            // read the units per em from the head table
            HeadTable headTable = (HeadTable)ttf.getTable("head");
            unitsPerEm = headTable.getUnitsPerEm();
            
            /* Find out if we have the right info in our name table.
             * This is a hack because Java can only deal with fonts that
             * have a Microsoft encoded name in their name table (PlatformID 3).
             * We'll 'adjust' the font to add it if not, and take our chances
             * with our parsing, since it wasn't going to work anyway.
             */
            NameTable nameTable = null;
            
            try
            {
                nameTable = (NameTable)ttf.getTable("name");
            }
            catch (Exception ex)
            {
                System.out.println ("Error reading name table for font " + getBaseFont() + ".  Repairing!");
            }
            
            boolean nameFixed = fixNameTable(ttf, nameTable);
            
            /* Figure out if we need to hack the CMap table.  This might
             * be the case if we use characters that Java considers control
             * characters (0x9, 0xa and 0xd), that have to be re-mapped
             */
            boolean cmapFixed = fixCMapTable(ttf, cmapTable);
            
            // use the parsed font instead of the original
            if (nameFixed || cmapFixed)
            {
                // System.out.println("Using fixed font!");
                // System.out.println(ttf.toString());
                fontdata = ttf.writeFont();
                
                //try
                //{
                //	String path = PDFUtil.ERROR_DATA_PATH + getBaseFont() + ".fix";
                //	PDFUtil.ensurePath(path);
                //	FileConnection con = (FileConnection)Connector.open(path, Connector.READ);
                //	OutputStream fos2 = con.openOutputStream();
                //	fos2.write(fontdata);
                //	fos2.close();
                //	con.close();
                //}
                //catch(Exception e)
                //{
                //}
            }
        }
        catch (Exception ex)
        {
            System.out.println("Error parsing font : " + getBaseFont());
            ex.printStackTrace ();
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(fontdata);
        if(FontManager.getInstance().load(bais, getBaseFont(), FontManager.APPLICATION_FONT) == FontManager.SUCCESS)
        {
        	try
	        {
				f = FontFamily.forName(getBaseFont()).getFont(Font.PLAIN, 1);
			}
	        catch (ClassNotFoundException e)
	        {
			}
        }
        bais.close ();
    }
    
    /**
     * Fix a broken font name table for a TrueType font.  Some fonts do not
     * have Microsoft-specific name information, but Java won't work without
     * it (grrr.).  This method takes a font and adds the Microsoft data into
     * it.
     *
     * @param ttf the font
     * @param name the font's name table
     * @return true if the table was fixed, or false if it was left as is
     */
    private boolean fixNameTable (TrueTypeFont ttf, NameTable name)
    {
        // if we didn't find the table, or there was an exception,
        // just create a new one
        if (name == null)
        {
            name = (NameTable)TrueTypeTable.createTable(ttf, "name");
            ttf.addTable ("name", name);
        }
        
        // first, figure out some info about the font
        String fName = this.getBaseFont ();
        String style = "Regular";
        
        if (fName.indexOf ("Italic") > -1 || fName.indexOf ("italic") > -1)
        {
            style = "Italic";
        }
        else if (fName.indexOf ("Bold") > -1 || fName.indexOf ("bold") > -1)
        {
            style = "Bold";
        }
        
        if (fName.indexOf ('-') > -1)
        {
            fName = fName.substring(0, fName.indexOf ('-'));
        }
        
        short platID = NameTable.PLATFORMID_MICROSOFT;
        short encID = 1;
        short langID = 1033;
        
        short[] nameIDs = {
            NameTable.NAMEID_COPYRIGHT,
            NameTable.NAMEID_FAMILY,
            NameTable.NAMEID_SUBFAMILY,
            NameTable.NAMEID_SUBFAMILY_UNIQUE,
            NameTable.NAMEID_FULL_NAME,
            NameTable.NAMEID_VERSION,
            NameTable.NAMEID_POSTSCRIPT_NAME,
            NameTable.NAMEID_TRADEMARK
        };
        
        String[] defaultValues = {
            "No copyright",
            fName,
            style,
            fName + " " + style,
            fName + " " + style,
            "1.0 (Fake)",
            fName,
            "No Trademark"
        };
        
        boolean changed = false;
        
        //Fixed number of name IDs
        for (int i = 0; i < 8; i++)
        {
            if (name.getRecord (platID, encID, langID, nameIDs[i]) == null)
            {
                name.addRecord(platID, encID, langID, nameIDs[i], defaultValues[i]);
                changed = true;
            }
        }
        
        return changed;
    }
    
    /**
     * Fix the CMap table.  This can be necessary if characters are mapped to
     * control characters (0x9, 0xa, 0xd) Java will not render them, even 
     * though they are valid.
     *
     * Also, Java tends to not like it when there is only a Format 0 CMap,
     * which happens frequently when included Format 4 CMaps are broken.
     * Since PDF prefers the Format 0 map, while Java prefers the Format 4 map,
     * it is generally necessary to re-write the Format 0 map as a Format 4 map
     * to make most PDFs work.
     *
     * @param ttf the font
     * @param cmap the CMap table
     * @return true if the font was changed, or false if it was left as-is
     */
    private boolean fixCMapTable (TrueTypeFont ttf, CmapTable cmap)
    {
        CMapFormat4 fourMap = null;
        CMapFormat0 zeroMap = null;
        
        int len = mapIDs.length;
        for (int i = 0; i < len; i += 2)
        {
            CMap map = cmapTable.getCMap(mapIDs[i], mapIDs[i + 1]);
            if (map != null)
            {
                if (fourMap == null && map instanceof CMapFormat4)
                {
                    fourMap = (CMapFormat4)map;
                }
                else if (zeroMap == null && map instanceof CMapFormat0)
                {
                    zeroMap = (CMapFormat0)map;
                }
            }
        }
        
        // if there were no maps, we could have problems.  Just try creating
        // an identity map
        if (zeroMap == null && fourMap == null)
        {
            fourMap = (CMapFormat4)CMap.createMap ((short)4, (short)0);
            fourMap.addSegment((short)getFirstChar(), (short)getLastChar(), (short)0);
        }
        
        // create our map based on the type 0 map, since PDF seems
        // to prefer a type 0 map (Java prefers a unicode map)
        if (zeroMap != null)
        {
            fourMap = (CMapFormat4)CMap.createMap((short)4, (short)0);
            
            // add the mappings from 0 to null and 1 to notdef
            fourMap.addSegment((short)0, (short)1, (short)0);
            
            len = getLastChar();
            for (int i = getFirstChar(); i <= len; i++)
            {
                short value = (short)(zeroMap.map((byte)i) & 0xff);
                if (value != 0)
                {
                    fourMap.addSegment((short)i, (short)i, (short)(value - i));
                }
            }
        }
        
        // now that we have a type four map, remap control characters
        len = controlChars.length;
        for (int i = 0; i < len; i++)
        {
            short idx = (short)(0xf000 | controlChars[i]);
            short value = (short)fourMap.map (controlChars[i]);
            
            fourMap.addSegment (idx, idx, (short)(value - idx));
        }
        
        // create a whole new table with just our map
        cmap = (CmapTable)TrueTypeTable.createTable(ttf, "cmap");
        cmap.addCMap ((short)3, (short)1, fourMap);
        
        // replace the table in the font
        ttf.addTable ("cmap", cmap);
        
        // change the stored table
        cmapTable = cmap;
        
        return true;
    }
}
