/*
 * File: PDFGraphics.java
 * Version: 1.0
 * Initial Creation: May 12, 2010 3:40:42 PM
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
package com.sun.pdfview.helper;

import net.rim.device.api.math.Matrix4f;

/**
 * Helper class to abstract Graphics drawing away from native classes. This way if the developer is using {@link javax.microedition.lcdui.Graphics J2ME Graphics},
 * {@link net.rim.device.api.ui.Graphics BlackBerry Graphics}, or even {@link javax.microedition.khronos.opengles.GL OpenGL} the graphics drawing will be the same.
 */
public abstract class PDFGraphics
{
	/**
	 * Create a new PDFGraphics device.
	 * @param drawingDevice The graphics device to draw with.
	 * @return The PDFGraphics that will draw to that graphics device or null if that device is not supported.
	 */
	public static PDFGraphics createGraphics(Object drawingDevice)
	{
		System.err.println("Graphics creation is not supported yet.");
		return null;
		
		//TODO: Testing needs to be done to determine what types the drawing objects will return and how to create the new drawing item. Should be dynamic so if someone creates their own drawing device it can still work.
	}
	
	/**
	 * Returns a copy of the current <code>Transform</code> in the <code>PDFGraphics</code> context.
	 * @return The current <code>Matrix4f</code> in the <code>PDFGraphics</code> context.
	 */
	public abstract Matrix4f getTransform();
	
	/**
	 * Sets the <code>Paint</code> attribute for the <code>PDFGraphics</code> context.
	 * @param paint The <code>Paint</code> object to be used to generate color during the rendering process, or null.
	 */
	public abstract void setPaint(Paint paint);
	
	/**
	 * Fills the interior of a <code>Shape</code> using the settings of the <code>PDFGraphics</code> context.
	 * @param s The Shape to be filled.
	 */
	public abstract void fill(Shape s);
}
