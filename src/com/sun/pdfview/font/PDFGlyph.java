/*
 * File: PDFGlyph.java
 * Version: 1.3
 * Initial Creation: May 15, 2010 11:41:08 AM
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

import net.rim.device.api.math.Matrix4f;

import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFShapeCmd;
import com.sun.pdfview.helper.XYPointFloat;
import com.sun.pdfview.helper.graphics.Geometry;

/**
 * A single glyph in a stream of PDF text, which knows how to write itself
 * onto a PDF command stream
 */
public class PDFGlyph
{
	/** the character code of this glyph */
    private char src;
    
    /** the name of this glyph */
    private String name;
    
    /** the advance from this glyph */
    private XYPointFloat advance;
    
    /** the shape represented by this glyph (for all fonts but type 3) */
    private Geometry shape;
    
    /** the PDFPage storing this glyph's commands (for type 3 fonts) */
    private PDFPage page;
    
    /** Creates a new instance of PDFGlyph based on a shape */
    public PDFGlyph(char src, String name, Geometry shape, XYPointFloat advance)
    {
        this.shape = shape;
        this.advance = advance;
        this.src = src;
        this.name = name;
    }
    
    /** Creates a new instance of PDFGlyph based on a page */
    public PDFGlyph(char src, String name, PDFPage page, XYPointFloat advance)
    {
        this.page = page;
        this.advance = advance;
        this.src = src;
        this.name = name;
    }
    
    /** Get the character code of this glyph */
    public char getChar()
    {
        return src;
    }
    
    /** Get the name of this glyph */
    public String getName()
    {
        return name;
    }
    
    /** Get the shape of this glyph */
    public Geometry getShape()
    {
        return shape;
    }
    
    /** Get the PDFPage for a type3 font glyph */
    public PDFPage getPage()
    {
        return page;
    }
    
    /** Add commands for this glyph to a page */
    public XYPointFloat addCommands(PDFPage cmds, Matrix4f transform, int mode)
    {
        if(shape != null)
        {
        	Geometry outline = (Geometry)shape.createTransformedShape(transform);
            cmds.addCommand(new PDFShapeCmd(outline, mode));
        }
        else if(page != null)
        {
            cmds.addCommands(page, transform);
        }
    
        return advance;
    }

    public String toString()
    {
        StringBuffer str = new StringBuffer ();
        str.append(name);
        return str.toString();
    }
}
