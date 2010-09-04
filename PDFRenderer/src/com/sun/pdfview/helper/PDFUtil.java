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
package com.sun.pdfview.helper;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

import com.sun.pdfview.helper.graphics.Geometry;

import net.rim.device.api.system.RuntimeStore;
import net.rim.device.api.ui.Font;
import net.rim.device.api.util.CharacterUtilities;
import net.rim.device.api.util.LongVector;

/**
 * Simple utilities to support performing operations that are not standard on BlackBerry.
 * @author Vincent Simonetti
 */
public class PDFUtil
{
	/* BUFFER_BIG_ENDIAN == false
	private static boolean BUFFER_BIG_ENDIAN;
	
	static
	{
		ByteBuffer buf = ByteBuffer.allocateDirect(4);
		buf.putInt(0x11000022);
		buf.rewind();
		byte val = buf.get();
		BUFFER_BIG_ENDIAN = val == 0x11;
		buf = null;
	}
	*/
	
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
	
	/**
	 * Create a BlackBerry compatible color.
	 * @param r The red component on a 0 - 1 scale.
	 * @param g The green component on a 0 - 1 scale.
	 * @param b The blue component on a 0 - 1 scale.
	 * @return The BlackBerry compatible color in 0x00RRGGBB format.
	 */
	public static int createColor(float r, float g, float b)
	{
		return createColor(0, r, g, b);
	}
	
	/**
	 * Create a BlackBerry compatible color.
	 * @param a The alpha component on a 0 - 1 scale.
	 * @param r The red component on a 0 - 1 scale.
	 * @param g The green component on a 0 - 1 scale.
	 * @param b The blue component on a 0 - 1 scale.
	 * @return The BlackBerry compatible color in 0xAARRGGBB format.
	 */
	public static int createColor(float a, float r, float g, float b)
	{
		int ai = (int)(a * 255);
		int ri = (int)(r * 255);
		int gi = (int)(g * 255);
		int bi = (int)(b * 255);
		return (ai << 24) | (ri << 16) | (gi << 8) | bi;
	}
	
	/**
	 * Returns the red component in the range 0-255 in the default sRGB space.
	 * @param color The color to use.
	 * @return The red component.
	 */
	public static int Color_getRed(int color)
	{
		return (color >> 16) & 0xFF;
	}
	
	/**
	 * Returns the green component in the range 0-255 in the default sRGB space.
	 * @param color The color to use.
	 * @return The green component.
	 */
	public static int Color_getGreen(int color)
	{
		return (color >> 8) & 0xFF;
	}
	
	/**
	 * Returns the blue component in the range 0-255 in the default sRGB space.
	 * @param color The color to use.
	 * @return The blue component.
	 */
	public static int Color_getBlue(int color)
	{
		return color & 0xFF;
	}
	
	/**
	 * Returns the alpha component in the range 0-255 in the default sRGB space.
	 * @param color The color to use.
	 * @return The alpha component.
	 */
	public static int Color_getAlpha(int color)
	{
		return (color >> 24) & 0xFF;
	}
	
	/*
	/**
	 * Convert a 3x3 affine matrix from J2SE format to a transformation matrix compatible with Matrix4f.
	 * @param mat The 3x2 matrix to convert.
	 * @return The converted 4x4 matrix.
	 * /
	public static float[] affine2TransformMatrix(float[] mat)
	{
		float[] result = new float[4*4];
		affine2TransformMatrix(mat, result);
		return result;
	}
	
	/**
	 * Convert a 3x3 affine matrix from J2SE format to a transformation matrix compatible with Matrix4f.
	 * @param mat The 3x2 matrix to convert.
	 * @param dest The converted 4x4 matrix.
	 * /
	public static void affine2TransformMatrix(float[] mat, float[] dest)
	{
		/*
		 * Affine:
		 * 0, 1, 2
		 * 3, 4, 5
		 * ?, ?, ?
		 * 
		 * Transform:
		 * 0, 2, ?, 4
		 * 1, 3, ?, 5
		 * ?, ?, ?, ?
		 * ?, ?, ?, ?
		 * /
		dest[0] = mat[0];
		dest[1] = mat[2];
		dest[3] = mat[4];
		dest[4] = mat[1];
		dest[5] = mat[3];
		dest[7] = mat[5];
	}
	*/
	
	/**
	 * Unions the pair of source XYRectFloat objects and puts the result into the specified destination XYRectFloat object.
	 * @param src1 the first of a pair of XYRectFloat objects to be combined with each other
	 * @param src2 the second of a pair of XYRectFloat objects to be combined with each other
	 * @param dst the XYRectFloat that holds the results of the union of src1 and src2
	 */
	public static void union(XYRectFloat src1, XYRectFloat src2, XYRectFloat dst)
	{
		float x1 = Math.min(src1.x, src2.x);
		float y1 = Math.min(src1.y, src2.y);
		float x2 = Math.max(src1.X2(), src2.X2());
		float y2 = Math.max(src1.Y2(), src2.Y2());
		dst.x = x1;
		dst.y = y1;
		dst.width = x2 - x1;
		dst.height = y2 - y1;
	}
	
	/**
	 * Indicates whether the specified character is a whitespace character in Java.
	 * @param c The character to check.
	 * @return {@code true} if the supplied {@code c} is a whitespace character in Java; {@code false} otherwise.
	 */
	public static boolean Character_isWhiteSpace(char c)
	{
		//From Java source code
		// Optimized case for ASCII
		if ((c >= 0x1c && c <= 0x20) || (c >= 0x9 && c <= 0xd))
		{
			return true;
		}
		if (c == 0x1680)
		{
			return true;
		}
		if (c < 0x2000 || c == 0x2007)
		{
			return false;
		}
		return c <= 0x200b || c == 0x2028 || c == 0x2029 || c == 0x3000;
	}
	
	public static String ERROR_DATA_PATH = "file:///store/appdata/tmp/";
	
	/**
     * Ensure the specified path exists (do not create file).
     */
    public static void ensurePath(String path) throws IOException
    {
    	if(!path.endsWith("/"))
    	{
    		path = path.substring(0, path.lastIndexOf('/') + 1);
    	}
    	//TODO: Make sure the path up to the file exists so no errors occur
	}
    
    /**
     * Relative get method for reading a long value.
     * @param buffer The ByteBuffer to reader the long from.
     * @return The long value at the buffer's current position.
     */
    public static final long ByteBuffer_getLong(ByteBuffer buffer)
    {
    	int position = buffer.position();
    	int newPosition = position + 8;
    	if (newPosition > buffer.limit())
    	{
    		throw new BufferUnderflowException();
    	}
    	long result = ByteBuffer_loadLong(buffer, position);
    	position = newPosition;
    	return result;
    }
    
    /**
     * Relative put method for writing a long value  (optional operation).
     * @param buffer The buffer to put a long in.
     * @param value The long value to be written.
     * @return The buffer.
     */
    public static ByteBuffer ByteBuffer_putLong(ByteBuffer buffer, long value)
    {
    	int position = buffer.position();
    	int newPosition = position + 8;
    	if (newPosition > buffer.limit())
    	{
    		throw new BufferOverflowException();
    	}
    	ByteBuffer_store(buffer, position, value);
    	buffer.position(newPosition);
    	return buffer;
    }
    
    private static final void ByteBuffer_store(ByteBuffer buffer, int index, long value)
    {
    	int baseOffset = index;  //this is basically absolute since no "internal" fields are used.
    	/*
    	if (BUFFER_BIG_ENDIAN)
    	{
    		for (int i = 7; i >= 0; i--)
    		{
    			buffer.put(baseOffset + i, (byte)(value & 0xFF));
    			value = value >> 8;
    		}
    	}
    	else
    	{
    	*/
    		for (int i = 0; i <= 7; i++)
    		{
    			buffer.put(baseOffset + i, (byte)(value & 0xFF));
    			value = value >> 8;
    		}
    	//}
    }
    
    private static final long ByteBuffer_loadLong(ByteBuffer buffer, int index)
    {
    	int baseOffset = index; //this is basically absolute since no "internal" fields are used.
    	long bytes = 0;
    	/*
    	if (BUFFER_BIG_ENDIAN)
    	{
    		for (int i = 0; i < 8; i++)
    		{
    			bytes = bytes << 8;
    			bytes = bytes | (buffer.get(baseOffset + i) & 0xFF);
    		}
    	}
    	else
    	{
    	*/
    		for (int i = 7; i >= 0; i--)
    		{
    			bytes = bytes << 8;
    			bytes = bytes | (buffer.get(baseOffset + i) & 0xFF);
    		}
    	//}
    	return bytes;
    }
    
    /**
     * Creates a Geometry by mapping characters to glyphs one-to-one based on the Unicode cmap in a Font.
     * @param f The font to get the Geometry object from.
     * @param charecter The character to get the Geometry object from.
     * @return A new Geometry created with the specified character.
     */
    public static Geometry Font_createGlyphVector(Font f, char charecter)
    {
    	//TODO: Glyphs should be cached for speed in later operations.
    	return null;
    }
	
	private static boolean eq(Object a, Object b)
	{
		return a == null ? b == null : a.equals(b);
	}
	
	/**
	 * Returns a wrapper on the specified Vector which synchronizes all access to the Vector.
	 * @param vector the Vector to wrap in a synchronized Vector.
	 * @return a synchronized Vector.
	 */
	public static Vector synchronizedVector(Vector vector)
	{
		if (vector == null)
		{
			throw new NullPointerException();
		}
		return new SynchronizedVector(vector);
	}
	
	private static class SynchronizedVector extends Vector
	{
		final Vector list;
		final Object mutex;
		
		SynchronizedVector(Vector l)
		{
			this.list = l;
			this.mutex = this;
		}
		
		public void addElement(Object obj)
		{
			synchronized(mutex)
			{
				list.addElement(obj);
			}
		}
		
		public int capacity()
		{
			synchronized(mutex)
			{
				return list.capacity();
			}
		}
		
		public boolean contains(Object elem)
		{
			synchronized(mutex)
			{
				return list.contains(elem);
			}
		}
		
		public void copyInto(Object[] anArray)
		{
			synchronized(mutex)
			{
				list.copyInto(anArray);
			}
		}
		
		public Object elementAt(int index)
		{
			synchronized(mutex)
			{
				return list.elementAt(index);
			}
		}
		
		public Enumeration elements()
		{
			synchronized(mutex)
			{
				//Should this be a synchronized enumeration? The J2SE source code from Apache doesn't but if everything else is synced, why not this?
				return list.elements();
			}
		}
		
		public void ensureCapacity(int minCapacity)
		{
			synchronized(mutex)
			{
				list.ensureCapacity(minCapacity);
			}
		}
		
		public boolean equals(Object obj)
		{
			synchronized(mutex)
			{
				return list.equals(obj);
			}
		}
		
		public Object firstElement()
		{
			synchronized(mutex)
			{
				return list.firstElement();
			}
		}
		
		public int hashCode()
		{
			synchronized(mutex)
			{
				return list.hashCode();
			}
		}
		
		public int indexOf(Object elem)
		{
			synchronized(mutex)
			{
				return list.indexOf(elem);
			}
		}
		
		public int indexOf(Object elem, int index)
		{
			synchronized(mutex)
			{
				return list.indexOf(elem, index);
			}
		}
		
		public void insertElementAt(Object obj, int index)
		{
			synchronized(mutex)
			{
				list.insertElementAt(obj, index);
			}
		}
		
		public boolean isEmpty()
		{
			synchronized(mutex)
			{
				return list.isEmpty();
			}
		}
		
		public Object lastElement()
		{
			synchronized(mutex)
			{
				return list.lastElement();
			}
		}
		
		public int lastIndexOf(Object elem)
		{
			synchronized(mutex)
			{
				return list.lastIndexOf(elem);
			}
		}
		
		public int lastIndexOf(Object elem, int index)
		{
			synchronized(mutex)
			{
				return list.lastIndexOf(elem, index);
			}
		}
		
		public void removeAllElements()
		{
			synchronized(mutex)
			{
				list.removeAllElements();
			}
		}
		
		public boolean removeElement(Object obj)
		{
			synchronized(mutex)
			{
				return list.removeElement(obj);
			}
		}
		
		public void removeElementAt(int index)
		{
			synchronized(mutex)
			{
				list.removeElementAt(index);
			}
		}
		
		public void setElementAt(Object obj, int index)
		{
			synchronized(mutex)
			{
				list.setElementAt(obj, index);
			}
		}
		
		public void setSize(int newSize)
		{
			synchronized(mutex)
			{
				list.setSize(newSize);
			}
		}
		
		public int size()
		{
			synchronized(mutex)
			{
				return list.size();
			}
		}
		
		public String toString()
		{
			synchronized(mutex)
			{
				return list.toString();
			}
		}
		
		public void trimToSize()
		{
			synchronized(mutex)
			{
				list.trimToSize();
			}
		}
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
