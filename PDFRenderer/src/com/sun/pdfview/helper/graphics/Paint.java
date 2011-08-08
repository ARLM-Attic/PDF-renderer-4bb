//#preprocessor

/*
 * File: Paint.java
 * Version: 1.0
 * Initial Creation: May 23, 2010 9:48:04 AM
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

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.ColorSpace;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Graphics;

/**
 * Defines a fill pattern for drawing. Modified version of java.awt.Paint combined with java.awt.Transparency.
 * @author Vincent Simonetti
 */
public abstract class Paint
{
	/**
	 * Represents image data that is guaranteed to be either completely opaque, with an alpha value of 1.0, or completely transparent, with an alpha value of 0.0.
	 */
	public static final int TRANSPARENCY_BITMASK = 1;
	/**
	 * Represents image data that is guaranteed to be completely opaque, meaning that all pixels have an alpha value of 1.0.
	 */
	public static final int TRANSPARENCY_OPAQUE = 2;
	/**
	 * Represents image data that contains or might contain arbitrary alpha values between and including 0.0 and 1.0.
	 */
	public static final int TRANSPARENCY_TRANSLUCENT = 3;
	
	/**
	 * Get a solid color instance of Paint.
	 * @param color The color to use.
	 * @return A solid color instance of paint.
	 */
	public static Paint getInstance(final int color)
	{
		return new Paint()
		{
			public PaintGenerator createGenerator(AffineTransform xform)
			{
				return new PaintGenerator()
				{
					public ColorSpace getColorSpace()
					{
						return null;
					}
					
					public TranslatedBitmap getBitmap(int x, int y, int w, int h)
					{
						Bitmap bmp = new Bitmap(w, h);
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1
						Graphics g = new Graphics(bmp);
//#else
						Graphics g = Graphics.create(bmp);
//#endif
						g.setBackgroundColor(color);
						g.clear();
						return new TranslatedBitmap(bmp, x, y);
					}
					
					public void dispose()
					{
					}
				};
			}
			
			public int getTransparency()
			{
				return Paint.TRANSPARENCY_OPAQUE;
			}

			public int getColor()
			{
				return color;
			}
			
			boolean isInternal()
			{
				return true;
			}
		};
	}
	
	/**
	 * Create a PaintGenerator, this is similar to a PaintContext but the Generator will simply produce a Bitmap that will be drawn with.
	 * @param xform The AffineTransform from user space into device space. Could be null.
	 * @return The created PaintGenerator.
	 */
	public abstract PaintGenerator createGenerator(AffineTransform xform);
	
	/**
	 * Returns the type of this Paint's transparency.
	 * @return The field type of this transparency, which is either TRANSPARENCY_OPAQUE, TRANSPARENCY_BITMASK or TRANSPARENCY_TRANSLUCENT.
	 */
	public abstract int getTransparency();
	
	/**
	 * Get the Paint object's default color.
	 */
	public abstract int getColor();
	
	boolean isInternal()
	{
		return false;
	}
}
