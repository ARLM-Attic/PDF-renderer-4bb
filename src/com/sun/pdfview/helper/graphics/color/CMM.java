/*
 * File: CMM.java
 * Version: 1.0
 * Initial Creation: May 20, 2010 9:45:31 PM
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
package com.sun.pdfview.helper.graphics.color;

import java.io.DataInputStream;
import java.io.IOException;

import com.sun.pdfview.helper.SeekableStream;

//Why is it called CMM? Should it be named ICC?
/**
 * Used to handle CMM processing.
 * <p>
 * based off Apache source located in their source tar in "working_classlib\modules\awt\src\main\native\lcmm\shared" and Little CMS Engine.
 * @author Vincent Simonetti
 */
public class CMM
{
	private Tag[] tags;
	private SeekableStream stream;
	
	private static final int MAX_TAG_COUNT = 100;
	
	/**
	 * Create a new ICC controller.
	 * @param data The data that defines a ICC.
	 */
	public CMM(byte[] data) throws IOException
	{
		this.stream = new SeekableStream(data);
		loadTags();
	}
	
	public void loadTags() throws IOException
	{
		stream.setPosition(ICC_Profile.headerSize);
		DataInputStream din = new DataInputStream(stream);
		
		int tagCount = din.readInt();
		if(tagCount > MAX_TAG_COUNT)
		{
			throw new IllegalArgumentException("Too many tags (" + tagCount + ")");
		}
		tags = new Tag[tagCount];
		for(int i = 0; i < tagCount; i++)
		{
			tags[i] = new Tag(din.readInt(), din.readInt(), din.readInt());
			if(tags[i].offset + tags[i].size > ICC_Profile.headerSize)
			{
				i--;
				continue;
			}
		}
	}
	
	private int findTag(int sig)
	{
		int len = this.tags.length;
		for (int i = 0; i < len; i++)
		{
			if (sig == this.tags[i].ID)
			{
				return i;
			}
		}
		return -1; // Not found
	}
	
	/**
	 * Get a tag size in bytes.
	 */
	public int cmmGetProfileElementSize(int tagSignature)
	{
		int size;
		if(tagSignature == ICC_Profile.icSigHead)
		{
			size = ICC_Profile.headerSize;
		}
		else
		{
			 int idx = findTag(tagSignature);
			 if(idx < 0)
			 {
				 size =  -1;
			 }
			 else
			 {
				 size = this.tags[idx].size;
			 }
		}
		
		if(size < 0)
		{
			throw new RuntimeException("Profile element not found");
		}
		return size;
	}
	
	/**
	 * Get tag data.
	 */
	public void cmmGetProfileElement(int tagSignature, byte[] data) throws IOException
	{
		int len = data.length;
		if(tagSignature == ICC_Profile.icSigHead)
		{
			len = Math.min(len, ICC_Profile.headerSize);
			
			this.stream.setPosition(0);
			this.stream.read(data, 0, len);
		}
		else
		{
			int idx = findTag(tagSignature);
			
			if(idx < 0)
			{
				throw new RuntimeException("Tagged profile element not found");
			}
			
			len = Math.min(len, this.tags[idx].size);
			
			this.stream.setPosition(this.tags[idx].offset);
			this.stream.read(data, 0, len);
		}
	}
	
	private static class Tag
	{
		public int ID;
		public int size;
		public int offset;
		
		public Tag(int ID, int offset, int size)
		{
			this.ID = ID;
			this.size = size;
			this.offset = offset;
		}
	}
}
