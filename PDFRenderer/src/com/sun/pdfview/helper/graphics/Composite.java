//#preprocessor

/*
 * File: Composite.java
 * Version: 1.0
 * Initial Creation: May 22, 2010 11:01:18 AM
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

import net.rim.device.api.ui.Graphics;
//#ifndef BlackBerrySDK4.5.0
import net.rim.device.api.util.MathUtilities;
//#else
import com.sun.pdfview.helper.PDFUtil;
//#endif

/**
 * Defines how raster data should be composited.
 * @author Vincent Simonetti
 */
public abstract class Composite
{
	/* AlphaComposite to BlackBerry Graphics ROP
	 * 
	 * CLEAR ---- ROP2_0
	 * SRC ------ ??
	 * DST ------ ROP2_D
	 * SRC_OVER - ROP2_S
	 * DST_OVER - ??
	 * SRC_IN --- ROP2_DSa
	 * DST_IN --- ??
	 * SRC_OUT -- ??
	 * DST_OUT -- ??
	 * SRC_ATOP - ??
	 * DST_ATOP - ??
	 * XOR ------ ROP2_DSx
	 */
	
	/**
	 * The source is copied to the destination.
	 */
	public static final int SRC = 0; //Normal graphics operation with GlobalAlpha == 255,1f
	/**
	 * The source is composited over the destination.
	 */
	public static final int SRC_OVER = 1; //Normal graphics operation with GlobalAlpha <= 255,1f
	
	/**
	 * Get a new Composite based on the specified type.
	 * @param type The type of Composite to get.
	 * @param alpha The alpha value for the Composite, 0f-1f.
	 * @return The specified Composite.
	 */
	public static Composite getInstance(int type, float alpha)
	{
		if(alpha < 0 || alpha > 1)
		{
			throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COMPOSITE_ALPHA_RANGE_FLOAT));
		}
//#ifdef BlackBerrySDK4.5.0
		return getInstance(type, Math.min(Math.max((float)PDFUtil.round(alpha * 255), 0), 255)); //Use Math.min because it might round up and Math.min just to round it down.
//#else
		return getInstance(type, Math.min(Math.max(MathUtilities.round(alpha * 255), 0), 255)); //Use Math.min because it might round up and Math.min just to round it down.
//#endif
	}
	
	/**
	 * Get a new Composite based on the specified type.
	 * @param type The type of Composite to get.
	 * @param alpha The alpha value for the Composite, 0-255.
	 * @return The specified Composite.
	 */
	public static Composite getInstance(int type, int alpha)
	{
		if(alpha < 0 || alpha > 255)
		{
			throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COMPOSITE_ALPHA_RANGE_BYTE));
		}
		switch(type)
		{
			case SRC:
			case SRC_OVER:
				return new DefaultComposite(type, alpha);
			default:
				throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_COMPOSITE_UNK_TYPE));
		}
	}
	
	/**
	 * Get a new Composite based on the specified type.
	 * @param type The type of Composite to get.
	 * @return The specified Composite.
	 */
	public static Composite getInstance(int type)
	{
		return getInstance(type, 255);
	}
	
	/**
	 * Composes the two source TranslatedBitmap objects and places the result in the destination TranslatedBitmap.
	 * @param src The first source for the compositing operation.
	 * @param dstIn The second source for the compositing operation.
	 * @param dstOut The TranslatedBitmap into which the result of the operation is stored.
	 */
	public abstract void composite(TranslatedBitmap src, TranslatedBitmap dstIn, TranslatedBitmap dstOut);
	
	private static class DefaultComposite extends Composite
	{
		private int type, srcAlpha/*, dstAlpha*/;
		
		public DefaultComposite(int type)
		{
			this(type, 255);
		}
		
		public DefaultComposite(int type, int srcAlpha)
		{
			this(type, srcAlpha, 255);
		}
		
		public DefaultComposite(int type, int srcAlpha, int dstAlpha)
		{
			this.type = type;
			this.srcAlpha = srcAlpha;
			//this.dstAlpha = dstAlpha;
		}
		
		public void composite(TranslatedBitmap src, TranslatedBitmap dstIn, TranslatedBitmap dstOut)
		{
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1
			Graphics dstOutG = new Graphics(dstOut.getBitmap());
//#else
			Graphics dstOutG = Graphics.create(dstOut.getBitmap());
//#endif
			dstOutG.translate(dstOut.getX(), dstOut.getY());
			
			//Since not all code is implemented just do the bare minimum
			if(this.type == SRC_OVER && dstIn != null)
			{
				dstOutG.drawBitmap(dstIn.getX(), dstIn.getY(), dstIn.getWidth(), dstIn.getHeight(), dstIn.getBitmap(), 0, 0);
			}
			if(src != null)
			{
				if(srcAlpha != 255)
				{
					dstOutG.setGlobalAlpha(srcAlpha);
				}
				dstOutG.drawBitmap(src.getX(), src.getY(), src.getWidth(), src.getHeight(), src.getBitmap(), 0, 0);
			}
		}
		
		//If more types are added then create a "processing" type that will simply do the math and an "executor" that will read the pixels and alpha and send it to
		//the processor. The results will be set in the Bitmap.
		
		//Look at AlphaComposite documentation for equations
	}
}
