//#preprocessor

/*
 * File: PDFDecoder.java
 * Version: 1.6
 * Initial Creation: May 12, 2010 4:11:17 PM
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

import java.util.Vector;

import java.io.IOException;
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.ByteBuffer;
//#else
import com.sun.pdfview.helper.nio.ByteBuffer;
//#endif

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.decrypt.PDFDecrypterFactory;

/**
 * A PDF Decoder encapsulates all the methods of decoding a stream of bytes
 * based on all the various encoding methods.
 * <p>
 * You should use the decodeStream() method of this object rather than using
 * any of the decoders directly.
 */
public class PDFDecoder
{
	private static String FILTER_DCT = "DCT";
    private static String FILTER_DCTDECODE = "DCTDecode";
    public final static Vector DCT_FILTERS;
    
    static
    {
    	DCT_FILTERS = new Vector();
    	DCT_FILTERS.addElement(FILTER_DCT);
    	DCT_FILTERS.addElement(FILTER_DCTDECODE);
    }
	
	/** Creates a new instance of PDFDecoder */
    private PDFDecoder()
    {
    }
    
    public static boolean isLastFilter(PDFObject dict, Vector filters) throws IOException
    {
        PDFObject filter = dict.getDictRef("Filter");
        if (filter == null)
        {
            return false;
        }
        else if (filter.getType() == PDFObject.NAME)
        {
            return filters.contains(filter.getStringValue());
        }
        else
        {
            final PDFObject[] ary = filter.getArray();
            return filters.contains(ary[ary.length - 1].getStringValue());
        }
    }
    
	/**
     * Utility class for reading and storing the specification of
     * Filters on a stream
     */
    private static class FilterSpec
    {
        PDFObject[] ary;
        PDFObject[] params;
        
        private FilterSpec(PDFObject dict, PDFObject filter) throws IOException
        {
            if (filter.getType() == PDFObject.NAME)
            {
                ary = new PDFObject[1];
                ary[0] = filter;
                params = new PDFObject[1];
                params[0] = dict.getDictRef("DecodeParms");
            }
            else
            {
                ary = filter.getArray();
                PDFObject parmsobj = dict.getDictRef("DecodeParms");
                if (parmsobj != null)
                {
                    params = parmsobj.getArray();
                }
                else
                {
                    params = new PDFObject[ary.length];
                }
            }
        }
    }
            
    /**
     * decode a byte[] stream using the filters specified in the object's
     * dictionary (passed as argument 1).
     * @param dict the dictionary associated with the stream
     * @param streamBuf the data in the stream, as a byte buffer
     */
    public static ByteBuffer decodeStream(PDFObject dict, ByteBuffer streamBuf, Vector filterLimits) throws IOException
    {
        PDFObject filter = dict.getDictRef("Filter");
        if (filter == null)
        {
            // just apply default decryption
            return dict.getDecrypter().decryptBuffer(null, dict, streamBuf);
        }
        else
        {
            // apply filters
            FilterSpec spec = new FilterSpec(dict, filter);
            
            // determine whether default encryption applies or if there's a
            // specific Crypt filter; it must be the first filter according to
            // the errata for PDF1.7
            boolean specificCryptFilter = spec.ary.length != 0 && spec.ary[0].getStringValue().equals("Crypt");
            if (!specificCryptFilter)
            {
                // No Crypt filter, so should apply default decryption (if present!)
                streamBuf = dict.getDecrypter().decryptBuffer(null, dict, streamBuf);
            }
            
            int len = spec.ary.length;
            for (int i = 0; i < len; i++)
            {
                String enctype = spec.ary[i].getStringValue();
                if (filterLimits.contains(enctype))
                {
                    break;
                }
                if (enctype == null)
                {
                }
                else if (enctype.equals("FlateDecode") || enctype.equals("Fl")) 
                {
                    streamBuf = FlateDecode.decode(dict, streamBuf, spec.params[i]);
                }
                else if (enctype.equals("LZWDecode") || enctype.equals("LZW"))
                {
                    streamBuf = LZWDecode.decode(streamBuf, spec.params[i]);
                }
                else if (enctype.equals("ASCII85Decode") || enctype.equals("A85"))
                {
                    streamBuf = ASCII85Decode.decode(streamBuf, spec.params[i]);
                }
                else if (enctype.equals("ASCIIHexDecode") || enctype.equals("AHx"))
                {
                    streamBuf = ASCIIHexDecode.decode(streamBuf, spec.params[i]);
                }
                else if (enctype.equals("RunLengthDecode") || enctype.equals("RL"))
                {
                    streamBuf = RunLengthDecode.decode(streamBuf, spec.params[i]);
                }
                else if (enctype.equals(FILTER_DCTDECODE) || enctype.equals(FILTER_DCT))
                {
                    streamBuf = DCTDecode.decode(dict, streamBuf, spec.params[i]);
                }
                else if (enctype.equals("CCITTFaxDecode") || enctype.equals("CCF"))
                {
                    streamBuf = CCITTFaxDecode.decode(dict, streamBuf, spec.params[i]);
                }
                else if (enctype.equals("Crypt"))
                {
                    String cfName = getCryptFilterName(spec.params[i]);
                    streamBuf = dict.getDecrypter().decryptBuffer(cfName, null, streamBuf);
                }
                else
                {
                    throw new PDFParseException("Unknown coding method:" + spec.ary[i].getStringValue());
                }
            }
        }
        
        return streamBuf;
    }
    
    /**
     * The name of the Crypt filter to apply
     * @param param the parameters to the Crypt filter
     * @return the name of the crypt filter to apply
     * @throws IOException if there's a problem reading the objects
     */
    private static String getCryptFilterName(PDFObject param) throws IOException
    {
        String cfName = PDFDecrypterFactory.CF_IDENTITY;
        if (param != null)
        {
            final PDFObject nameObj = param.getDictRef("Name");
            if (nameObj != null && nameObj.getType() == PDFObject.NAME)
            {
                cfName = nameObj.getStringValue();
            }
        }
        return cfName;
    }
    
    /**
     * Determines whether a stream is encrypted or not; note that encodings
     * (e.g., Flate, LZW) are not considered encryptions.
     * @param dict the stream dictionary
     * @return whether the stream is encrypted
     * @throws IOException if the stream dictionary can't be read
     */
    public static boolean isEncrypted(PDFObject dict) throws IOException
    {
        PDFObject filter = dict.getDictRef("Filter");
        if (filter == null)
        {
            // just apply default decryption
            return dict.getDecrypter().isEncryptionPresent();
        }
        else
        {
            // apply filters
            FilterSpec spec = new FilterSpec(dict, filter);
            
            // determine whether default encryption applies or if there's a
            // specific Crypt filter; it must be the first filter according to
            // the errata for PDF1.7
            boolean specificCryptFilter = spec.ary.length != 0 && spec.ary[0].getStringValue().equals("Crypt");
            if (!specificCryptFilter)
            {
                // No Crypt filter, so we just need to refer to
                // the default decrypter
                return dict.getDecrypter().isEncryptionPresent();
            }
            else
            {
                String cfName = getCryptFilterName(spec.params[0]);
                // see whether the specified crypt filter really decrypts
                return dict.getDecrypter().isEncryptionPresent(cfName);
            }
        }
    }
}
