/*
 * File: TTFFont.java
 * Version: 1.11
 * Initial Creation: May 16, 2010 1:08:36 PM
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
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.io.IOUtilities;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.font.ttf.AdobeGlyphList;
import com.sun.pdfview.font.ttf.CMap;
import com.sun.pdfview.font.ttf.CmapTable;
import com.sun.pdfview.font.ttf.Glyf;
import com.sun.pdfview.font.ttf.GlyfCompound;
import com.sun.pdfview.font.ttf.GlyfSimple;
import com.sun.pdfview.font.ttf.GlyfTable;
import com.sun.pdfview.font.ttf.HeadTable;
import com.sun.pdfview.font.ttf.HmtxTable;
import com.sun.pdfview.font.ttf.PostTable;
import com.sun.pdfview.font.ttf.TrueTypeFont;
import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFUtil;
import com.sun.pdfview.helper.graphics.Geometry;

/**
 * A true-type font
 */
public class TTFFont extends OutlineFont
{
	/** the truetype font itself */
    private TrueTypeFont font;
    /** the number of units per em in the font */
    private float unitsPerEm;
    
    public TTFFont (String baseFont, PDFObject fontObj, PDFFontDescriptor descriptor) throws IOException
    {
    	this(baseFont, fontObj, descriptor, null);
    }
    
    /**
     * create a new TrueTypeFont object based on a description of the
     * font from the PDF file.  If the description happens to contain
     * an in-line true-type font file (under key "FontFile2"), use the
     * true type font.  Otherwise, parse the description for key information
     * and use that to generate an appropriate font.
     */
    public TTFFont(String baseFont, PDFObject fontObj, PDFFontDescriptor descriptor, String fontFile) throws IOException
    {
        super(baseFont, fontObj, descriptor);
        
        String fontName = descriptor.getFontName();
        PDFObject ttfObj = descriptor.getFontFile2();
        
        // try
        // {
        //    byte[] fontData = ttfObj.getStream();
        //    String path = PDFUtil.ERROR_DATA_PATH + fontName + ".ttf";
        //    PDFUtil.ensurePath(path);
        //    javax.microedition.io.file.FileConnection file = (javax.microedition.io.file.FileConnection)javax.microedition.io.Connector.open(path, javax.microedition.io.Connector.WRITE);
        //    if(!file.exists())
        //    {
        // 	   file.create();
        //    }
        //    java.io.OutputStream fis = file.openOutputStream();
        //    fis.write(fontData);
        //    fis.flush();
        //    fis.close();
        //    file.close();
        // }
        // catch (Exception ex)
        // {
        //    ex.printStackTrace();
        // }
        if (ttfObj != null || fontFile != null)
        {
        	if (ttfObj != null)
        	{
        		font = TrueTypeFont.parseFont(ttfObj.getStreamBuffer());
        	}
        	else
        	{
        		final FileConnection fcon = (FileConnection)Connector.open(fontFile, Connector.READ);
        		InputStream in = null;
        		try
        		{
        			in = fcon.openInputStream();
        			font = TrueTypeFont.parseFont(IOUtilities.streamToBytes(in));
        		}
        		finally
        		{
        			try
        			{
        				in.close();
        			}
        			catch(Exception e)
        			{
        			}
        			try
        			{
        				fcon.close();
        			}
        			catch(Exception e)
        			{
        			}
        		}
        	}
            // read the units per em from the head table
            HeadTable head = (HeadTable)font.getTable("head");
            unitsPerEm = head.getUnitsPerEm();
        }
        else
        {
            font = null;
        }
//        System.out.println ("TTFFont: ttfObj: " + ttfObj + ", fontName: " + fontName);
    }
    
    public Vector getNames()
    {
        return font.getNames();
    }
    
    /**
     * Get the outline of a character given the character code
     */
    protected synchronized Geometry getOutline(char src, float width)
    {
        // find the cmaps
        CmapTable cmap = (CmapTable)font.getTable("cmap");
        
        // if there are no cmaps, this is (hopefully) a cid-mapped font,
        // so just trust the value we were given for src
        if (cmap == null)
        {
            return getOutline((int) src, width);
        }
        
        CMap[] maps = cmap.getCMaps();
        
        // try the maps in order
        int len = maps.length;
        for (int i = 0; i < len; i++)
        {
            int idx = maps[i].map(src);
            if (idx != 0)
            {
                return getOutline(idx, width);
            }
        }
        
        // not found, return the empty glyph
        return getOutline(0, width);
    }
    
    /**
     * lookup the outline using the CMAPs, as specified in 32000-1:2008,
     * 9.6.6.4, when an Encoding is specified.
     * 
     * @param val
     * @param width
     * @return GeneralPath
     */
    protected synchronized Geometry getOutlineFromCMaps(char val, float width)
    {
        // find the cmaps
        CmapTable cmap = (CmapTable)font.getTable("cmap");
        
        if (cmap == null)
        {
            return null;
        }
        
        // try maps in required order of (3, 1), (1, 0)
        CMap map = cmap.getCMap((short)3, (short)1);
        if (map == null)
        {
            map = cmap.getCMap((short) 1, (short) 0);
        }
        int idx = map.map(val);
        if (idx != 0)
        {
            return getOutline(idx, width);
        }
        
        return null;
    }
    
    /**
     * Get the outline of a character given the character name
     */
    protected synchronized Geometry getOutline(String name, float width)
    {
        int idx;
        PostTable post = (PostTable)font.getTable("post");
        if (post != null)
        {
        	idx = post.getGlyphNameIndex(name);
        	if (idx != 0)
        	{
        		return getOutline(idx, width);
        	}
        	return null;
        }
        
        Integer res = AdobeGlyphList.getGlyphNameIndex(name);
        if(res != null)
        {
        	idx = res.intValue();
        	return getOutlineFromCMaps((char)idx, width);
        }
        return null;
    }
    
    /**
     * Get the outline of a character given the glyph id
     */
    protected synchronized Geometry getOutline(int glyphId, float width)
    {
        // find the glyph itself
        GlyfTable glyf = (GlyfTable) font.getTable("glyf");
        Glyf g = glyf.getGlyph(glyphId);
        
        Geometry gp = null;
        if (g instanceof GlyfSimple)
        {
            gp = renderSimpleGlyph((GlyfSimple)g);
        }
        else if (g instanceof GlyfCompound)
        {
            gp = renderCompoundGlyph(glyf, (GlyfCompound) g);
        }
        else
        {
            gp = new Geometry();
        }
        
        // calculate the advance
        HmtxTable hmtx = (HmtxTable)font.getTable("hmtx");
        float advance = (float) hmtx.getAdvance(glyphId) / (float) unitsPerEm;
        
        // scale the glyph to match the desired advance
        float widthfactor = width / advance;
        
        // the base transform scales the glyph to 1x1
        AffineTransform at = AffineTransform.createScale(1 / unitsPerEm, 1 / unitsPerEm);
        at.concatenate(AffineTransform.createScale(widthfactor, 1));
        
        gp.transform(at);
        
        return gp;
    }
    
    /**
     * Render a simple glyf
     */
    protected Geometry renderSimpleGlyph(GlyfSimple g)
    {
        // the current contour
        int curContour = 0;
        
        // the render state
        RenderState rs = new RenderState();
        rs.gp = new Geometry();
        
        int len = g.getNumPoints();
        for (int i = 0; i < len; i++)
        {
            PointRec rec = new PointRec(g, i);
            
            if (rec.onCurve)
            {
                addOnCurvePoint(rec, rs);
            }
            else
            {
                addOffCurvePoint(rec, rs);
            }
            
            // see if we just ended a contour
            if (i == g.getContourEndPoint(curContour))
            {
                curContour++;
                
                if (rs.firstOff != null)
                {
                    addOffCurvePoint(rs.firstOff, rs);
                }
                
                if (rs.firstOn != null)
                {
                    addOnCurvePoint(rs.firstOn, rs);
                }
                
                rs.firstOn = null;
                rs.firstOff = null;
                rs.prevOff = null;
            }
        }
        
        return rs.gp;
    }
    
    /**
     * Render a compound glyf
     */
    protected Geometry renderCompoundGlyph(GlyfTable glyf, GlyfCompound g)
    {
    	Geometry gp = new Geometry();
        
        int len = g.getNumComponents();
        for (int i = 0; i < len; i++)
        {
            // find and render the component glyf
        	Glyf gl = glyf.getGlyph (g.getGlyphIndex (i));
            Geometry path = null;
            if (gl instanceof GlyfSimple)
            {
                path = renderSimpleGlyph ((GlyfSimple)gl);
            }
            else if (gl instanceof GlyfCompound)
            {
                path = renderCompoundGlyph (glyf, (GlyfCompound)gl);
            }
            else
            {
                throw new RuntimeException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.FONT_TTFFONT_UNSUPPORTED_GLYPH, new Object[]{gl.getClass().getName()}));
            }
            
            // multiply the translations by units per em
            float[] matrix = g.getTransform(i);
            
            // transform the path
            path.transform(new AffineTransform(matrix));
            
            // add it to the global path
            gp.append(path, false);
        }
        
        return gp;
    }
    
    /** add a point on the curve */
    private void addOnCurvePoint(PointRec rec, RenderState rs)
    {
        // if the point is on the curve, either move to it,
        // or draw a line from the previous point
        if (rs.firstOn == null)
        {
            rs.firstOn = rec;
            rs.gp.moveTo(rec.x, rec.y);
        }
        else if (rs.prevOff != null)
        {
            rs.gp.quadTo(rs.prevOff.x, rs.prevOff.y, rec.x, rec.y);
            rs.prevOff = null;
        }
        else
        {
            rs.gp.lineTo(rec.x, rec.y);
        }
    }
    
    /** add a point off the curve */
    private void addOffCurvePoint(PointRec rec, RenderState rs)
    {
        if (rs.prevOff != null)
        {
            PointRec oc = new PointRec((rec.x + rs.prevOff.x) / 2, (rec.y + rs.prevOff.y) / 2, true);
            addOnCurvePoint(oc, rs);
        }
        else if (rs.firstOn == null)
        {
            rs.firstOff = rec;
        }
        rs.prevOff = rec;
    }
    
    class RenderState
    {
        // the shape itself
    	Geometry gp;
        // the first off and on-curve points in the current segment
        PointRec firstOn;
        PointRec firstOff;
        // the previous off and on-curve points in the current segment
        PointRec prevOff;
    }
    
    /** a point on the stack of points */
    class PointRec
    {
        int x;
        int y;
        boolean onCurve;
        
        public PointRec(int x, int y, boolean onCurve) 
        {
            this.x = x;
            this.y = y;
            this.onCurve = onCurve;
        }
        
        public PointRec(GlyfSimple g, int idx)
        {
            x = g.getXCoord(idx);
            y = g.getYCoord(idx);
            onCurve = g.onCurve(idx);
        }
    }
}
