/*
 * File: XYRectFloat.java
 * Version: 1.0
 * Initial Creation: May 6, 2010 6:05:40 PM
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
package com.sun.pdfview;

/**
 * Simple rectangle like XYRect only it store floating point values.
 * @author Vincent Simonetti
 */
public class XYRectFloat
{
	//Only has what is necessary
	
	/** Height of rect. */
	public float height;
	/** Width of rect. */
	public float width;
	/** Horizontal position of top left point. */
	public float x;
	/** Vertical position of top left point. */
	public float y;
	
	/**
	 * Constructs a new XYRectFloat instance and sets all fields to 0.
	 */
	public XYRectFloat()
	{
		this(0, 0, 0, 0);
	}
	
	/**
	 * Constructs a specified rectangle.
	 * @param x Horizontal location of rectangle's origin.
	 * @param y Vertical location of rectangle's origin.
	 * @param width Distance in pixels to the right edge of the rectangle.
	 * @param height Distance in pixels to the bottom edge of the rectangle.
	 */
	public XYRectFloat(float x, float y, float width, float height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
}
