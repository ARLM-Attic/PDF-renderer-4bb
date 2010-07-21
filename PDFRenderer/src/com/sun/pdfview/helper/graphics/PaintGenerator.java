/*
 * File: PaintGenerator.java
 * Version: 1.0
 * Initial Creation: May 23, 2010 10:21:17 AM
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
package com.sun.pdfview.helper.graphics;

import com.sun.pdfview.helper.ColorSpace;

/**
 * A modified version of java.awt.PaintContext. Used to make a more direct fill.
 */
public abstract class PaintGenerator
{
	/** Releases the resources allocated for the operation.*/
	public abstract void dispose();
	
	/**
	 * Returns the ColorSpace of the output.
	 * @return The ColorSpace that this PaintGenerator is using.
	 */
	public abstract ColorSpace getColorSpace();
	
	/**
	 * Returns a Bitmap containing the colors generated for the graphics operation.
	 * @param x The x coordinate of the area in device space for which colors are generated.
	 * @param y The y coordinate of the area in device space for which colors are generated.
	 * @param w The width of the area in device space.
	 * @param h The height of the area in device space.
	 * @return A Bitmap representing the specified rectangular area and containing the colors generated for the graphics operation.
	 */
	public abstract TranslatedBitmap getBitmap(int x, int y, int w, int h);
}
