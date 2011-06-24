//#preprocessor

/*
 * File: DCTDecode.java
 * Version: 1.3
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
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.ByteBuffer;
//#else
import com.sun.pdfview.helper.nio.ByteBuffer;
//#endif

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.Graphics;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.colorspace.PDFColorSpace;
import com.sun.pdfview.helper.PDFUtil;

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
     * of the image to create the array.  If this were to be used
     * against image objects we'd end up wasting a lot of work, because
     * we'd be generating a buffered image here, writing out the bytes,
     * and then generating a buffered image again from those bytes in the
     * PDFImage class.
     * <p>
     * Luckily, the image processing has been optimised to detect
     * DCT decodes at the end of filters, in which case it avoids
     * running the stream through this filter, and just directly
     * generates a BufferedImage from the DCT encoded byte stream.
     * As such, this decode will be invoked only if there's been
     * some very unusual employment of filters in the PDF - e.g.,
     * DCTDecode applied to non-image data, or if DCTDecode is not at
     * the end of a Filter dictionary entry. This is permissible but
     * unlikely to occur in practice.
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
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
        int type = Bitmap.ROWWISE_16BIT_COLOR;
        if (numComponents == 1)
        {
            type = Bitmap.ROWWISE_MONOCHROME;
        }
        else if (numComponents == 4)
        {
        	type = -type;
        }
//#else
        int type = Bitmap.ROWWISE_32BIT_XRGB8888;
        if (numComponents == 4)
        {
        	type = Bitmap.ROWWISE_32BIT_ARGB8888;
        }
//#endif
        
        // create a buffered image
        int width = img.getWidth();
        int height = img.getHeight();
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
        Bitmap bimg = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, width, height);
        if (numComponents == 4)
        {
        	bimg.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
        }
//#else
        Bitmap bimg = new Bitmap(type, width, height);
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1
        Graphics bg = Graphics.create(bimg);
//#else
        Graphics bg = new Graphics(bimg);
//#endif
        
        // draw the image onto it
        bg.drawImage(0, 0, width, height, img, 0, 0, 0);
        
        // read back the data
    	int len;
    	int[] data = new int[len = width * height];
    	bimg.getARGB(data, 0, 0, 0, 0, width, height);
    	
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
    	byte[] output = new byte[len * ((type == Bitmap.ROWWISE_MONOCHROME) ? 1 : (type == Bitmap.ROWWISE_16BIT_COLOR ? 3 : 4))];
//#else
    	PDFUtil.assert(numComponents == 1 || numComponents == 3 || numComponents == 4, "numComponents == 1 || numComponents == 3 || numComponents == 4");
    	byte[] output = new byte[len * numComponents]; //Unless an odd image type that doesn't use 1, 3, or 4 components this shouldn't have a problem
//#endif
    	
    	// -Might not necessarily apply to BlackBerry
    	// incidentally, there's a bit of an optimization we could apply here,
        // if we weren't pretty confident that this isn't actually going to
        // be called, anyway. Namely, if we just use JAI to read in the data
        // the underlying data buffer seems to typically be byte[] based,
        // and probably already in the desired arrangement (and if not, that
        // could be engineered by supplying our own sample model). As it is,
        // we won't bother, since this code is most likely not going
        // to be used.
    	
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
    	switch(type)
//#else
    	switch(numComponents)
//#endif
    	{
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
	    	case Bitmap.ROWWISE_16BIT_COLOR:
//#else
	    	case 3:
//#endif
	    		for (int i = 0; i < len; i++)
	        	{
	            	output[i * 3] = (byte)(data[i] >> 16);
	                output[i * 3 + 1] = (byte)(data[i] >> 8);
	                output[i * 3 + 2] = (byte)(data[i]);
	        	}
	    		break;
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
	    	case -Bitmap.ROWWISE_16BIT_COLOR:
//#else
	    	case 4:
//#endif
	    		for (int i = 0; i < len; i++)
	        	{
	        		output[i * 4] = (byte)(data[i] >> 24);
	                output[i * 4 + 1] = (byte)(data[i] >> 16);
	                output[i * 4 + 2] = (byte)(data[i] >> 8);
	                output[i * 4 + 3] = (byte)(data[i]);
	        	}
	    		break;
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
	    	case Bitmap.ROWWISE_MONOCHROME:
//#else
	    	case 1:
//#endif
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
