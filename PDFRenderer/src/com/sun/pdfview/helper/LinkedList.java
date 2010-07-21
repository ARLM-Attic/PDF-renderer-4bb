/*
 * File: LinkedList.java
 * Version: 1.0
 * Initial Creation: May 11, 2010 9:53:06 AM
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

import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Simplistic linked list based off a Vector.
 * @author Vincent Simonetti
 */
public class LinkedList extends Vector
{
	/**
	 * Adds the specified object at the beginning of this {@code LinkedList}.
	 * @param object The object to add.
	 */
	public void addFirst(Object object)
	{
		if(this.elementCount == 0)
		{
			this.addElement(object);
		}
		else
		{
			this.insertElementAt(object, 0);
		}
	}
	
	/**
	 * Removes the last object from this {@code LinkedList}.
	 * @return the removed object.
	 * @throws NoSuchElementException if this {@code LinkedList} is empty.
	 */
	public Object removeLast()
	{
		if(this.elementCount > 0)
		{
			Object ret = this.lastElement();
			this.removeElementAt(this.elementCount - 1);
			return ret;
		}
		throw new NoSuchElementException();
	}
	
	/**
	 * Removes the first object from this {@code LinkedList}.
	 * @return the removed object.
	 * @throws NoSuchElementException if this {@code LinkedList} is empty.
	 */
	public Object removeFirst()
	{
		if(this.elementCount > 0)
		{
			Object ret = this.firstElement();
			this.removeElementAt(0);
			return ret;
		}
		throw new NoSuchElementException();
	}
}
