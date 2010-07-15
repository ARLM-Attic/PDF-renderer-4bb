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
