//#preprocessor

/*
 * File: CCITTFaxDecode.java
 * Version: 1.0
 * Initial Creation: May 12, 2010 4:15:31 PM
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
package com.sun.pdfview.decode;

import java.io.IOException;
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.ByteBuffer;
//#else
import com.sun.pdfview.helper.nio.ByteBuffer;
//#endif

import com.sun.pdfview.PDFObject;

public class CCITTFaxDecode
{
	protected static ByteBuffer decode(PDFObject dict, ByteBuffer buf, PDFObject params) throws IOException
	{
		byte[] bytes = new byte[buf.remaining()];
	    buf.get(bytes, 0, bytes.length);
		return ByteBuffer.wrap(decode(dict, bytes));
	}
	
	protected static byte[] decode(PDFObject dict, byte[] source) throws IOException
	{
		int width = 1728;
		PDFObject widthDef = dict.getDictRef("Width");
		if (widthDef == null)
		{
			widthDef = dict.getDictRef("W");
		}
		if (widthDef != null)
		{
			width = widthDef.getIntValue();
		}
		int height = 0;
		PDFObject heightDef = dict.getDictRef("Height");
		if (heightDef == null)
		{
			heightDef = dict.getDictRef("H");
		}
		if (heightDef != null)
		{
			height = heightDef.getIntValue();
		}
		
		//
		int columns = getOptionFieldInt(dict, "Columns", width);
		int rows = getOptionFieldInt(dict, "Rows", height);
		int k = getOptionFieldInt(dict, "K", 0);
		int size = rows * ((columns + 7) >> 3);
		byte[] destination = new byte[size];
		
		boolean align = getOptionFieldBoolean(dict, "EncodedByteAlign", false);
		
		CCITTFaxDecoder decoder = new CCITTFaxDecoder(1, columns, rows);
		decoder.setAlign(align);
		if (k == 0)
		{
			decoder.decodeT41D(destination, source, 0, rows);
		}
		else if (k > 0)
		{
			decoder.decodeT42D(destination, source, 0, rows);
		}
		else if (k < 0) 
		{
			decoder.decodeT6(destination, source, 0, rows);
		}
		if (!getOptionFieldBoolean(dict, "BlackIs1", false))
		{
			int len = destination.length;
			for (int i = 0; i < len; i++)
			{
				// bitwise not
				destination[i] = (byte) ~destination[i];
			}
		}
		
		return destination;
	}
	
	public static int getOptionFieldInt(PDFObject dict, String name, int defaultValue) throws IOException
	{
		PDFObject dictParams =  dict.getDictRef("DecodeParms");
		
		if (dictParams == null)
		{
			return defaultValue;
		}
		PDFObject value = dictParams.getDictRef(name);
		if (value == null)
		{
			return defaultValue;
		}
		return value.getIntValue();
	}

	public static boolean getOptionFieldBoolean(PDFObject dict, String name, boolean defaultValue) throws IOException
	{
		PDFObject dictParams =  dict.getDictRef("DecodeParms");
		
		if (dictParams == null)
		{
			return defaultValue;
		}
		PDFObject value = dictParams.getDictRef(name);
		if (value == null)
		{
			return defaultValue;
		}
		return value.getBooleanValue();
	}
}
