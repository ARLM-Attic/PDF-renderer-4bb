/*
 * File: XYPointFloat.java
 * Version: 1.0
 * Initial Creation: May 11, 2010 3:50:16 PM
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

/**
 * Simple rectangle like {@link net.rim.device.api.ui.XYPoint XYPoint} only it store floating point values.
 * @author Vincent Simonetti
 */
public class XYPointFloat
{
	/** Distance of point along horizontal axis.*/
	public float x;
	/** Distance of point along vertical axis.*/
	public float y;
	
	/**
	 * Constructs an origin point.
	 */
	public XYPointFloat()
	{
		this(0, 0);
	}
	
	/**
	 * Constructs a specified point.
	 * @param x Horizontal distance from origin.
	 * @param y Vertical distance from origin.
	 */
	public XYPointFloat(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Returns a string representation of the object.
	 * @return A string representation of the object.
	 */
	public String toString()
	{
		StringBuffer buffer = new StringBuffer("{X = ");
		buffer.append(this.x);
		buffer.append(", Y = ");
		buffer.append(this.y);
		buffer.append('}');
		return buffer.toString();
	}
}
