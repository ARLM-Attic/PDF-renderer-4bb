/*
 * File: DCTDecode.java
 * Version: 1.2
 * Initial Creation: May 12, 2010 4:54:06 PM
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
package com.sun.pdfview.decode;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.Graphics;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.colorspace.PDFColorSpace;

/**
 * decode a DCT encoded array into a byte array.  This class uses Java's
 * built-in JPEG image class to do the decoding.
 *
 * @author Mike Wessler
 */
public class DCTDecode
{
	/**
     * decode an array of bytes in DCT format.
     * <p>
     * DCT is the format used by JPEG images, so this class simply
     * loads the DCT-format bytes as an image, then reads the bytes out
     * of the image to create the array.  Unfortunately, their most
     * likely use is to get turned BACK into an image, so this isn't
     * terribly efficient... but is is general... don't hit, please.
     * <p>
     * The DCT-encoded stream may have 1, 3 or 4 samples per pixel, depending
     * on the colorspace of the image.  In decoding, we look for the colorspace
     * in the stream object's dictionary to decide how to decode this image.
     * If no colorspace is present, we guess 3 samples per pixel.
     *
     * @param dict the stream dictionary
     * @param buf the DCT-encoded buffer
     * @param params the parameters to the decoder (ignored)
     * @return the decoded buffer
     */
    protected static ByteBuffer decode(PDFObject dict, ByteBuffer buf, PDFObject params) throws PDFParseException
    {
//    	System.out.println("DCTDecode image info: " + params);
        buf.rewind();
        
        // copy the data into a byte array required by createEncodedImage
        byte[] ary = new byte[buf.remaining()];
        buf.get(ary);
        
        // wait for the image to get drawn
        EncodedImage img = EncodedImage.createEncodedImage(ary, 0, ary.length);
        
        // the default components per pixel is 3
        int numComponents = 3;
        
        // see if we have a colorspace
        try
        {
            PDFObject csObj = dict.getDictRef("ColorSpace");
            if (csObj != null)
            {
                // we do, so get the number of components
                PDFColorSpace cs = PDFColorSpace.getColorSpace(csObj, null);
                numComponents = cs.getNumComponents();
            }
        }
        catch (IOException ioe)
        {
            // oh well
        }
        
        // figure out the type
        int type = Bitmap.ROWWISE_16BIT_COLOR;
        if (numComponents == 1)
        {
            type = Bitmap.ROWWISE_MONOCHROME;
        }
        else if (numComponents == 4)
        {
        	type = -type;
        }
        
        // create a buffered image
        int width = img.getWidth();
        int height = img.getHeight();
        Bitmap bimg = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, width, height);
        if (numComponents == 4)
        {
        	bimg.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
        }
        Graphics bg = Graphics.create(bimg);
        
        // draw the image onto it
        bg.drawImage(0, 0, width, height, img, 0, 0, 0);
        
        // read back the data
    	int len;
    	int[] data = new int[len = width * height];
    	bimg.getARGB(data, 0, 0, 0, 0, width, height);
    	
    	byte[] output = new byte[len * ((type == Bitmap.ROWWISE_MONOCHROME) ? 1 : (type == Bitmap.ROWWISE_16BIT_COLOR ? 3 : 4))];
    	switch(type)
    	{
	    	case Bitmap.ROWWISE_16BIT_COLOR:
	    		for (int i = 0; i < len; i++)
	        	{
	            	output[i * 3] = (byte)(data[i] >> 16);
	                output[i * 3 + 1] = (byte)(data[i] >> 8);
	                output[i * 3 + 2] = (byte)(data[i]);
	        	}
	    		break;
	    	case -Bitmap.ROWWISE_16BIT_COLOR:
	    		for (int i = 0; i < len; i++)
	        	{
	        		output[i * 4] = (byte)(data[i] >> 24);
	                output[i * 4 + 1] = (byte)(data[i] >> 16);
	                output[i * 4 + 2] = (byte)(data[i] >> 8);
	                output[i * 4 + 3] = (byte)(data[i]);
	        	}
	    		break;
	    	case Bitmap.ROWWISE_MONOCHROME:
	    		for (int i = 0; i < len; i++)
	        	{
	    			output[i] = (byte)((((data[i] >> 16) & 0xFF) + ((data[i] >> 8) & 0xFF) + (data[i] & 0xFF)) / 3);
	        	}
	    		break;
    	}
        
//        System.out.println("Translated data");
        return ByteBuffer.wrap(output);
    }
}
