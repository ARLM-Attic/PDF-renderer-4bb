/*
 * File: TranslatedBitmap.java
 * Version: 1.0
 * Initial Creation: May 23, 2010 5:08:07 PM
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

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.XYRect;

/**
 * Simple helper class to define a positioned Bitmap.
 * @author Vincent Simonetti
 */
public class TranslatedBitmap
{
	XYRect rect;
	Bitmap img;
	
	/**
	 * Create a new TranslatedBitmap.
	 * @param img The Bitmap to use.
	 */
	public TranslatedBitmap(Bitmap img)
	{
		this(img, 0, 0);
	}
	
	/**
	 * Create a new TranslatedBitmap.
	 * @param img The Bitmap to use.
	 * @param x The x position img should be located at.
	 * @param y The y position img should be located at.
	 */
	public TranslatedBitmap(Bitmap img, int x, int y)
	{
		this(img, x, y, img.getWidth(), img.getHeight());
	}
	
	/**
	 * Create a new TranslatedBitmap.
	 * @param img The Bitmap to use.
	 * @param x The x position img should be located at.
	 * @param y The y position img should be located at.
	 * @param w The width the Bitmap should be.
	 * @param h The height the Bitmap should be.
	 */
	public TranslatedBitmap(Bitmap img, int x, int y, int w, int h)
	{
		this.rect = new XYRect(x, y, w, h);
		this.img = img;
	}
	
	/** Get the X position of the Bitmap.*/
	public int getX()
	{
		return rect.x;
	}
	
	/** Get the Y position of the Bitmap.*/
	public int getY()
	{
		return rect.y;
	}
	
	/** Get the width of the Bitmap.*/
	public int getWidth()
	{
		return rect.width;
	}
	
	/** Get the height of the Bitmap.*/
	public int getHeight()
	{
		return rect.height;
	}
	
	/** Get the actual Bitmap.*/
	public Bitmap getBitmap()
	{
		return img;
	}
}
