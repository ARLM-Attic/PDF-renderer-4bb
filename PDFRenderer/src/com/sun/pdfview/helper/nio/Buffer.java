//#preprocessor

//#implicit BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1

/*
 * File: Buffer.java
 * Version: 1.0
 * Initial Creation: Feb 26, 2011 12:40:44 PM
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
package com.sun.pdfview.helper.nio;

/**
 * A container for data of a specific primitive type. Based off J2SE java.nio.Buffer class but no source code used for it.
 * @author Vincent Simonetti
 */
public abstract class Buffer
{
	int mark, position, limit, capacity;
	
	Buffer()
	{
		this.mark = -1;
	}
	
	/**
	 * Returns this buffer's limit.
	 * @return The limit of this buffer.
	 */
	public final int limit()
	{
		return this.limit;
	}
	
	/**
	 * Returns this buffer's position.
	 * @return The position of this buffer.
	 */
	public final int position()
	{
		return this.position;
	}
	
	/**
	 * Sets this buffer's position.
	 * @param newPosition The new position value; must be non-negative and no larger than the current limit.
	 * @return This buffer.
	 */
	public final Buffer position(int newPosition)
	{
		if(newPosition < 0 || newPosition > this.limit)
		{
			throw new IllegalArgumentException();
		}
		if(this.mark != -1 && this.mark > newPosition)
		{
			this.mark = -1;
		}
		this.position = newPosition;
		return this;
	}
	
	/**
	 * Returns the number of elements between the current position and the limit.
	 * @return The number of elements remaining in this buffer.
	 */
	public final int remaining()
	{
		return this.limit - this.position;
	}
	
	/**
	 * Flips this buffer.
	 * @return this buffer.
	 */
	public final Buffer flip()
	{
		this.limit = this.position;
		return rewind();
	}
	
	/**
	 * Returns this buffer's capacity.
	 * @return The capacity of this buffer.
	 */
	public final int capacity()
	{
		return this.capacity;
	}
	
	/**
	 * Sets this buffer's limit.
	 * @param newLimit the new limit value.
	 * @return this buffer.
	 */
	public final Buffer limit(int newLimit)
	{
		if(newLimit < 0 || newLimit > this.capacity)
		{
			throw new IllegalArgumentException();
		}
		if(this.position > newLimit)
		{
			this.position = newLimit;
		}
		if(this.mark != -1 && this.mark > newLimit)
		{
			this.mark = -1;
		}
		this.limit = newLimit;
		return this;
	}
	
	/**
	 * Rewinds this buffer.
	 * @return this buffer.
	 */
	public final Buffer rewind()
	{
		this.position = 0;
		this.mark = -1;
		return this;
	}
	
	/**
	 * Tells whether there are any elements between the current position and the limit.
	 * @return true if, and only if, there is at least one element remaining in this buffer.
	 */
	public final boolean hasRemaining()
	{
		return remaining() >= 1;
	}
	
	/**
	 * Clears this buffer.
	 * @return this buffer.
	 */
	public final Buffer clear()
	{
		this.limit = this.capacity;
		return rewind();
	}
}
