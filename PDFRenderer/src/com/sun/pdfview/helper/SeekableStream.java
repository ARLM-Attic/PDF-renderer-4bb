/*
 * File: SeekableStream.java
 * Version: 1.0
 * Initial Creation: Some time in the past.
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import net.rim.device.api.io.Seekable;

/**
 * Basic seekable input stream.
 */
public class SeekableStream extends InputStream implements Seekable
{
	private int pos;
	private byte[] data;
	
	public SeekableStream(byte[] data)
	{
		this.data = data;
	}
	
	public int available() throws IOException
	{
		if(data == null)
		{
			throw new IOException("Stream closed");
		}
		return data.length - pos;
	}
	
	public boolean markSupported()
	{
		return false;
	}
	
	public long skip(long n) throws IOException
	{
		if(data == null)
		{
			throw new IOException("Stream closed");
		}
		if(n < 0)
		{
			return this.pos;
		}
		return (this.pos += n);
	}
	
	public int read() throws IOException
	{
		if(data == null)
		{
			throw new IOException("Stream closed");
		}
		if(pos == data.length)
		{
			pos++;
			return -1;
		}
		else if(pos > data.length)
		{
			throw new EOFException();
		}
		return data[pos++] & 0xFF;
	}
	
	public int read(byte[] b, int off, int len) throws IOException
	{
		if(data == null)
		{
			throw new IOException("Stream closed");
		}
		if(b == null)
		{
			throw new NullPointerException();
		}
		if(off < 0)
		{
			throw new IndexOutOfBoundsException("off");
		}
		if((off + len) > b.length)
		{
			throw new IndexOutOfBoundsException("len");
		}
		if(pos == data.length)
		{
			pos++;
			return -1;
		}
		else if(pos > data.length)
		{
			throw new EOFException();
		}
		len = Math.min(data.length - pos, len);
		System.arraycopy(data, pos, b, off, len);
		pos += len;
		return len;
	}
	
	public long getPosition() throws IOException
	{
		if(data == null)
		{
			throw new IOException("Stream closed");
		}
		return this.pos;
	}
	
	public void setPosition(long position) throws IOException
	{
		if(data == null)
		{
			throw new IOException("Stream closed");
		}
		if(position < 0 && position >= data.length)
		{
			throw new IOException("Position is outside bounds of data.");
		}
		this.pos = (int)position;
	}
	
	public void close() throws IOException
	{
		this.data = null;
	}
}
