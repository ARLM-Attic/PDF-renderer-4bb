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

/**
 * Defines how raster data should be composited.
 * @author Vincent Simonetti
 */
public class Composite
{
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
	 * @param alpha The alpha value for the Composite.
	 * @return The specified Composite.
	 */
	public static Composite getInstance(int type, float alpha)
	{
		if(alpha == 1)
		{
			return getInstance(type);
		}
		switch(type)
		{
			case SRC:
			case SRC_OVER:
				//TODO
				return null;
			default:
				throw new IllegalArgumentException("Unknown type");
		}
	}
	
	/**
	 * Get a new Composite based on the specified type.
	 * @param type The type of Composite to get.
	 * @return The specified Composite.
	 */
	public static Composite getInstance(int type)
	{
		switch(type)
		{
			case SRC:
			case SRC_OVER:
				//TODO
				return null;
			default:
				throw new IllegalArgumentException("Unknown type");
		}
	}
}
