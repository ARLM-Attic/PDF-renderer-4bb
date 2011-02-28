//#preprocessor

/*
 * File: ShortBuffer.java
 * Version: 1.0
 * Initial Creation: Feb 26, 2011 12:52:06 PM
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

//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
package com.sun.pdfview.helper.nio;

/**
 * A skimmed down version of the ShortBuffer class for use in versions lower then 5.0. Based off J2SE java.nio.ShortBuffer class but no source code used for it.
 * @author Vincent Simonetti
 */
public abstract class ShortBuffer extends Buffer
{
	/**
	 * Writes the given short into this buffer at the current position, and then increments the position.
	 * @param s The short to be written.
	 * @return This buffer.
	 */
	public abstract ShortBuffer put(short s);
	
	/**
	 * Relative get method. Reads the short at this buffer's current position, and then increments the position.
	 * @return The short at the buffer's current position.
	 */
	public abstract short get();
}
//#endif
