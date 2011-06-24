//#preprocessor

/*
 * File: FlateDecode.java
 * Version: 1.4
 * Initial Creation: May 12, 2010 4:20:34 PM
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.ByteBuffer;
//#else
import com.sun.pdfview.helper.nio.ByteBuffer;
//#endif

import net.rim.device.api.compress.ZLibInputStream;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;

/**
 * decode a deFlated byte array
 * @author Mike Wessler
 */
public class FlateDecode
{
	/**
     * decode a byte buffer in Flate format.
     * <p>
     * Flate is a built-in Java algorithm.  It's part of the java.util.zip
     * package.
     *
     * @param buf the deflated input buffer
     * @param params parameters to the decoder (unused)
     * @return the decoded (inflated) bytes
     */
    public static ByteBuffer decode(PDFObject dict, ByteBuffer buf, PDFObject params) throws IOException
    {
        int bufSize = buf.remaining();
        
        // copy the data, since the array() method is not supported
        // on raf-based ByteBuffers
        byte[] data = new byte[bufSize];
        buf.get(data);
        
        // set the input to the inflater
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZLibInputStream inf = new ZLibInputStream(bais, false);
        
        // output to a byte-array output stream, since we don't
        // know how big the output will be
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] decomp = new byte[bufSize];
        //int loc = 0;
        int read = 0;
        
        try
        {
        	while(true) //Read until there is no data left
        	{
        		read = inf.read(decomp);
        		if (read <= 0)
        		{
        			if(baos.size() > 0)
        			{
        				break;
        			}
        			return ByteBuffer.allocateDirect(0);
        		}
        		baos.write(decomp, 0, read);
        	}
        }
        catch (IOException dfe)
        {
            throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.DECODE_FLATE_DATA_FORMAT_EXCEPTION, new Object[]{dfe.getMessage()}));
        }
        
        // return the output as a byte buffer
        ByteBuffer outBytes = ByteBuffer.wrap(baos.toByteArray());
        baos.close();
        bais.close();
        
        // undo a predictor algorithm, if any was used
        if (params != null && params.getDictionary().containsKey("Predictor"))
        {
            Predictor predictor = Predictor.getPredictor(params);
            if (predictor != null)
            {
                outBytes = predictor.unpredict(outBytes);
            }
        }
        
        return outBytes;
    }
}
