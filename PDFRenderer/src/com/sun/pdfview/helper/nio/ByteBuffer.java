//#preprocessor

//#implicit BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1

/*
 * File: ByteBuffer.java
 * Version: 1.0
 * Initial Creation: Feb 26, 2011 12:49:21 PM
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
 * A skimmed down version of the ByteBuffer class for use in versions lower then 5.0. Based off J2SE java.nio.ByteBuffer class but no source code used for it.
 * @author Vincent Simonetti
 */
public abstract class ByteBuffer extends Buffer
{
	byte[] data;
	int offset;
	
	ByteBuffer(byte[] data)
	{
		this(data, 0, data.length);
	}
	
	ByteBuffer(byte[] data, int offset, int capLimit)
	{
		this.data = data;
		this.offset = offset;
		this.limit = this.capacity = capLimit;
	}
	
	/**
	 * Allocates a new direct byte buffer.
	 * @param capacity The new buffer's capacity, in bytes.
	 * @return The new byte buffer.
	 */
	public static ByteBuffer allocateDirect(int capacity)
	{
		if(capacity < 0)
		{
			throw new IllegalArgumentException();
		}
		return new ByteBufferImpl(new byte[capacity]);
	}
	
	/**
	 * Wraps a byte array into a buffer.
	 * @param array The array that will back this buffer.
	 * @return The new byte buffer.
	 */
	public static ByteBuffer wrap(byte[] array)
	{
		return new ByteBufferImpl(array);
	}
	
	/**
	 * Creates a view of this byte buffer as a short buffer.
	 * @return A new short buffer.
	 */
	public abstract ShortBuffer asShortBuffer();
	
	/**
	 * Writes the given byte into this buffer at the current position, and then increments the position.
	 * @param b The byte to be written.
	 * @return This buffer.
	 */
	public abstract ByteBuffer put(byte b);
	
	/**
	 * Writes the given byte into this buffer at the given index.
	 * @param index The index at which the byte will be written.
	 * @param b The byte value to be written.
	 * @return This buffer.
	 */
	public abstract ByteBuffer put(int index, byte b);
	
	/**
	 * This method transfers the bytes remaining in the given source buffer into this buffer.
	 * @param src The source buffer from which bytes are to be read; must not be this buffer.
	 * @return This buffer.
	 */
	public ByteBuffer put(ByteBuffer src)
	{
		while (src.hasRemaining())
		{
			this.put(src.get());
		}
		return this;
	}
	
	/**
	 * This method transfers the entire content of the given source byte array into this buffer.
	 * @return This buffer.
	 */
	public final ByteBuffer put(byte[] src)
	{
		return this.put(src, 0, src.length);
	}
	
	/**
	 * Relative bulk put method
	 * @param src The array from which bytes are to be read.
	 * @param offset The offset within the array of the first byte to be read; must be non-negative and no larger than array.length.
	 * @param length The number of bytes to be read from the given array; must be non-negative and no larger than array.length - offset.
	 * @return This buffer.
	 */
	public ByteBuffer put(byte[] src, int offset, int length)
	{
		for (int i = offset; i < offset + length; i++)
		{
			this.put(src[i]);
		}
		return this;
	}
	
	/**
	 * Relative put method for writing a short value.
	 * @param value The short value to be written.
	 * @return This buffer.
	 */
	public abstract ByteBuffer putShort(short value);
	
	/**
	 * Relative put method for writing an int value.
	 * @param value The int value to be written.
	 * @return This buffer
	 */
	public abstract ByteBuffer putInt(int value);
	
	/**
	 * Absolute put method for writing an int value.
	 * @param index The index at which the bytes will be written.
	 * @param value The int value to be written.
	 * @return This buffer.
	 */
	public abstract ByteBuffer putInt(int index, int value);
	
	/**
	 * Relative get method. Reads the byte at this buffer's current position, and then increments the position.
	 * @return The byte at the buffer's current position.
	 */
	public abstract byte get();
	
	/**
	 * Absolute get method. Reads the byte at the given index.
	 * @param index The index from which the byte will be read.
	 * @return The byte at the given index.
	 */
	public abstract byte get(int index);
	
	/**
	 * Relative bulk get method.
	 * @return This buffer.
	 */
	public ByteBuffer get(byte[] dst)
	{
		return get(dst, 0, dst.length);
	}
	
	/**
	 * This method transfers bytes from this buffer into the given destination array.
	 * @param dst The array into which bytes are to be written.
	 * @param offset The offset within the array of the first byte to be written; must be non-negative and no larger than dst.length.
	 * @param length The maximum number of bytes to be written to the given array; must be non-negative and no larger than dst.length - offset.
	 * @return This buffer.
	 */
	public ByteBuffer get(byte[] dst, int offset, int length)
	{
		for (int i = offset; i < offset + length; i++)
		{
			dst[i] = this.get();
		}
		return this;
	}
	
	/**
	 * Relative get method for reading a short value.
	 * @return The short value at the buffer's current position
	 */
	public abstract short getShort();
	
	/**
	 * Absolute get method for reading a short value.
	 * @param index The index from which the bytes will be read
	 * @return The short value at the given index
	 */
	public abstract short getShort(int index);
	
	/**
	 * Relative get method for reading an int value.
	 * @return The int value at the buffer's current position
	 */
	public abstract int getInt();
	
	/**
	 * Returns the byte array that backs this buffer.
	 * @return The array that backs this buffer.
	 */
	public final byte[] array()
	{
		if(!hasArray())
		{
			throw new UnsupportedOperationException();
		}
		return this.data;
	}
	
	/**
	 * Tells whether or not this buffer is backed by an accessible byte array.
	 * @return true if, and only if, this buffer is backed by an array and is not read-only.
	 */
	public final boolean hasArray()
	{
		return true;
	}
	
	/**
	 * Returns the offset within this buffer's backing array of the first element of the buffer.
	 * @return The offset within this buffer's array of the first element of the buffer.
	 */
	public final int arrayOffset()
	{
		if(!hasArray())
		{
			throw new UnsupportedOperationException();
		}
		return this.offset;
	}
	
	/**
	 * Creates a new byte buffer whose content is a shared subsequence of this buffer's content.
	 * @return The new byte buffer.
	 */
	public abstract ByteBuffer slice();
	
	private static class ByteBufferImpl extends ByteBuffer
	{
		private ShortBufferImpl sBuffer;
		
		public ByteBufferImpl(byte[] data)
		{
			super(data);
		}
		
		public ByteBufferImpl(byte[] data, int offset, int capLimit)
		{
			super(data, offset, capLimit);
		}
		
		public ShortBuffer asShortBuffer()
		{
			if(this.sBuffer == null)
			{
				this.sBuffer = new ShortBufferImpl(this);
			}
			return this.sBuffer;
		}
		
		public ByteBuffer put(byte b)
		{
			if(this.position > this.limit)
			{
				throw new BufferOverflowException();
			}
			unCheckedPut(this.position++, b);
			return this;
		}
		
		public ByteBuffer put(int index, byte b)
		{
			if(index < 0 || index > this.limit)
			{
				throw new IndexOutOfBoundsException();
			}
			unCheckedPut(index, b);
			return this;
		}
		
		public void unCheckedPut(int index, byte b)
		{
			this.data[this.offset + index] = b;
		}
		
		public ByteBuffer putShort(short value)
		{
			if(this.position + 1 > this.limit)
			{
				throw new BufferOverflowException();
			}
			return putShort(2 - (this.position += 2), value);
		}
		
		public ByteBuffer putShort(int index, short value)
		{
			if(index < 0 || index + 1 > this.limit)
			{
				throw new IndexOutOfBoundsException();
			}
			this.unCheckedPut(index, (byte)(value >> 8));
			this.unCheckedPut(index + 1, (byte)value);
			return this;
		}
		
		public ByteBuffer putInt(int value)
		{
			if(this.position + 3 > this.limit)
			{
				throw new BufferOverflowException();
			}
			return putInt(4 - (this.position += 4), value);
		}
		
		public ByteBuffer putInt(int index, int value)
		{
			if(index < 0 || index + 3 > this.limit)
			{
				throw new IndexOutOfBoundsException();
			}
			this.unCheckedPut(index, (byte)(value >> 24));
			this.unCheckedPut(index + 1, (byte)(value >> 16));
			this.unCheckedPut(index + 2, (byte)(value >> 8));
			this.unCheckedPut(index + 3, (byte)value);
			return this;
		}
		
		public byte get()
		{
			if(this.position > this.limit)
			{
				throw new BufferUnderflowException();
			}
			return this.unCheckedGet(this.position++);
		}
		
		public byte get(int index)
		{
			if(index < 0 || index > this.limit)
			{
				throw new IndexOutOfBoundsException();
			}
			return this.unCheckedGet(index);
		}
		
		public byte unCheckedGet(int index)
		{
			return this.data[this.offset + index];
		}
		
		public short getShort()
		{
			if(this.position + 1 > this.limit)
			{
				throw new BufferUnderflowException();
			}
			return this.getShort(2 - (this.position += 2));
		}
		
		public short getShort(int index)
		{
			if(index < 0 || index + 1 > this.limit)
			{
				throw new IndexOutOfBoundsException();
			}
			return (short)(((this.unCheckedGet(index) & 0xFF) << 8) | (this.unCheckedGet(index + 1) & 0xFF));
		}
		
		public int getInt()
		{
			if(this.position + 3 > this.limit)
			{
				throw new BufferUnderflowException();
			}
			return this.getInt(4 - (this.position += 4));
		}
		
		public int getInt(int index)
		{
			if(index < 0 || index + 3 > this.limit)
			{
				throw new IndexOutOfBoundsException();
			}
			return (((this.unCheckedGet(index) & 0xFF) << 24) | ((this.unCheckedGet(index + 1) & 0xFF) << 16) | ((this.unCheckedGet(index + 2) & 0xFF) << 8) | (this.unCheckedGet(index + 3) & 0xFF));
		}
		
		public ByteBuffer slice()
		{
			return new ByteBufferImpl(this.data, this.offset + this.position, this.capacity - this.position);
		}
	}
	
	private static class ShortBufferImpl extends ShortBuffer
	{
		private ByteBufferImpl byteBuffer;
		
		public ShortBufferImpl(ByteBufferImpl buffer)
		{
			this.byteBuffer = buffer;
		}
		
		public ShortBuffer put(short s)
		{
			this.byteBuffer.putShort(s);
			return this;
		}
		
		public short get()
		{
			return this.byteBuffer.getShort();
		}
	}
}
