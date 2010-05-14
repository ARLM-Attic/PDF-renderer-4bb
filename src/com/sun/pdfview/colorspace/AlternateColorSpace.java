/*
 * File: AlternateColorSpace.java
 * Version: 1.2
 * Initial Creation: May 12, 2010 9:11:54 PM
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

import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.function.PDFFunction;
import com.sun.pdfview.helper.ColorSpace;

public class AlternateColorSpace extends PDFColorSpace
{
	/** The alternate color space */
    private PDFColorSpace alternate;
    
    /** The function */
    private PDFFunction function;
    
    /** Creates a new instance of AlternateColorSpace */
    public AlternateColorSpace(PDFColorSpace alternate, PDFFunction function)
    {
        super(null);
        
        this.alternate = alternate;
        this.function = function;
    }
    
    /**
     * get the number of components expected in the getPaint command
     */
    public int getNumComponents()
    {
    	if (function != null)
    	{
    		return function.getNumInputs();
        }
    	else 
    	{
            return alternate.getNumComponents();
        }
    }
    
    /**
     * get the PDFPaint representing the color described by the
     * given color components
     * @param components the color components corresponding to the given
     * colorspace
     * @return a PDFPaint object representing the closest Color to the
     * given components.
     */
    public PDFPaint getPaint(float[] components)
    {
    	if (function != null)
    	{
            // translate values using function
            components = function.calculate(components);
        }
        
        return alternate.getPaint(components);
    }
    
    /**
     * get the original Java ColorSpace.
     */
    public ColorSpace getColorSpace()
    {
    	return alternate.getColorSpace();
    }
}
