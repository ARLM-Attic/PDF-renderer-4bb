/*
 * File: PDFUtil.java
 * Version: 1.0
 * Initial Creation: May 6, 2010 5:58:06 PM
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

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

import net.rim.device.api.util.CharacterUtilities;

/**
 * Simple utilities to support performing operations that are not standard on BlackBerry.
 * @author Vincent Simonetti
 */
public class PDFUtil
{
	/**
	 * Copies all of the mappings from the specified map to this map.
	 * These mappings will replace any mappings that this map had for
	 * any of the keys currently in the specified map.
	 * 
	 * @param dest the map to store the elements in
	 * @param source mappings to be stored in the map
	 * @throws NullPointerException if the specified map is null
	 */
	public static void Hashtable_putAll(Hashtable dest, Hashtable source)
	{
		int numKeysToBeAdded = source.size();
		if (numKeysToBeAdded == 0)
		{
			return;
		}
		
		//Original Sun code did a conservative resizing of the map, access to internal variables are not available so will let BlackBerry do the work for me.
		
		if(dest instanceof SynchronizedTable)
		{
			SynchronizedTable stable = (SynchronizedTable)dest;
			synchronized(stable.mutex)
			{
				for (Enumeration i = source.elements(), k = source.keys(); i.hasMoreElements();)
				{
					//If this blocks because the mutex is already synchronized then change to "stable.t.put"
					stable.put(k.nextElement(), i.nextElement());
				}
			}
		}
		else
		{
			for (Enumeration i = source.elements(), k = source.keys(); i.hasMoreElements();)
			{
				dest.put(k.nextElement(), i.nextElement());
			}
		}
	}
	
	/**
	 * Determines if the specified character is a letter or digit.
	 * @param ch the character to be tested.
	 * @return <code>true</code> if the character is a letter or digit; <code>false</code> otherwise.
	 */
	public static boolean Character_isLetterOrDigit(char ch)
	{
		return CharacterUtilities.isLetter(ch) || CharacterUtilities.isDigit(ch);
	}
	
	/**
	 * Simple assert
	 * @param assert The operation to make sure is true.
	 * @param function The function that is being checked.
	 */
	public static void assert(boolean assert, String function)
	{
		assert(assert, function, null);
	}
	
	/**
	 * Simple assert
	 * @param assert The operation to make sure is true.
	 * @param function The function that is being checked.
	 * @param message The message that the developer would like in the assert if invoked.
	 */
	public static void assert(boolean assert, String function, String message)
	{
		if(!assert)
		{
			throw new RuntimeException("Assert error: " + function + (message == null ? "" : ": " + message));
		}
	}
	
	/**
	 * Wrap a {@link String} in a Buffer.
	 * @param text The {@link String} to wrap.
	 * @return The Buffer wrapping the {@link String}.
	 */
	public static ShortBuffer wrapString(String text)
	{
		char[] dat = text.toCharArray();
		int len = dat.length;
		ShortBuffer buffer = ByteBuffer.allocateDirect(len * 2).asShortBuffer();
		for(int i = 0; i < dat.length; i++)
		{
			buffer.put((short)dat[i]);
		}
		return buffer;
	}
	
	private static boolean eq(Object a, Object b)
	{
		return a == null ? b == null : a.equals(b);
	}
	
	/**
	 * Returns a synchronized (thread-safe) table backed by the specified table.
	 * @param table The table to make synchronized.
	 * @return The synchronized table.
	 */
	public static Hashtable synchronizedTable(Hashtable table)
	{
		return new SynchronizedTable(table);
	}
	
	private static class SynchronizedTable extends Hashtable
	{
		private final Hashtable t; // Backing Map
		final Object mutex; // Object on which to synchronize
		
		SynchronizedTable(Hashtable table)
		{
			if(table == null)
			{
				throw new NullPointerException();
			}
			this.t = table;
			this.mutex = this;
		}
		
		SynchronizedTable(Hashtable table, Object mutex)
		{
			this.t = table;
			this.mutex = mutex;
		}
		
		public int size()
		{
			synchronized(this.mutex)
			{
				return this.t.size();
			}
		}
		
		public boolean isEmpty()
		{
			synchronized(this.mutex)
			{
				return this.t.isEmpty();
			}
		}
		
		public boolean containsKey(Object key)
		{
			synchronized(this.mutex)
			{
				return this.t.containsKey(key);
			}
		}
		
		public boolean contains(Object value)
		{
			synchronized(this.mutex)
			{
				return this.t.contains(value);
			}
		}
		
		public Object get(Object key)
		{
			synchronized(this.mutex)
			{
				return this.t.get(key);
			}
		}
		
		public Object put(Object key, Object value)
		{
			synchronized(this.mutex)
			{
				return this.t.put(key, value);
			}
		}
		
		public Object remove(Object key)
		{
			synchronized(this.mutex)
			{
				return this.t.remove(key);
			}
		}
		
		public void clear()
		{
			synchronized(this.mutex)
			{
				this.t.clear();
			}
		}
		
		public Enumeration elements()
		{
			synchronized(this.mutex)
			{
				return synchronizedEnumeration(this.t.elements(), this.mutex);
			}
		}
		
		public Enumeration keys()
		{
			synchronized(this.mutex)
			{
				return synchronizedEnumeration(this.t.keys(), this.mutex);
			}
		}
		
		public boolean equals(Object obj)
		{
			synchronized(this.mutex)
			{
				return this.t.equals(obj);
			}
		}
		
		public int hashCode()
		{
			synchronized(this.mutex)
			{
				return this.t.hashCode();
			}
		}
		
		public String toString()
		{
			synchronized(this.mutex)
			{
				return this.t.toString();
			}
		}
	}
	
	/**
	 * Returns an immutable Vector containing only the specified object.
	 * @param obj The sole object to be stored in the returned Vector.
	 * @return An immutable Vector containing only the specified object.
	 */
	public static Vector singletonVector(Object obj)
	{
		return new SingletonVector(obj);
	}
	
	private static class SingletonVector extends Vector
	{
		private final Object element;
		
		SingletonVector(Object obj)
		{
			this.element = obj;
		}
		
		public Enumeration elements()
		{
			return singletonEnumeration(this.element);
		}
		
		public int size()
		{
			return 1;
		}
		
		public boolean contains(Object obj)
		{
			return eq(obj, this.element);
		}
		
		public Object elementAt(int index)
		{
            if (index != 0)
            {
            	throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
            }
            return element;
         }
	}
	
	private static Enumeration synchronizedEnumeration(final Enumeration en, final Object mutex)
	{
		return new Enumeration()
		{
			public boolean hasMoreElements()
			{
				synchronized(mutex)
				{
					return en.hasMoreElements();
				}
			}
			
			public Object nextElement()
			{
				synchronized(mutex)
				{
					return en.nextElement();
				}
			}
		};
	}
	
	private static Enumeration singletonEnumeration(final Object obj)
	{
		return new Enumeration()
		{
			private boolean hasNext = true;
			
			public boolean hasMoreElements()
			{
				return hasNext;
			}
			
			public Object nextElement()
			{
				if(hasNext)
				{
					hasNext = false;
					return obj;
				}
				throw new NoSuchElementException();
			}
		};
	}
}
