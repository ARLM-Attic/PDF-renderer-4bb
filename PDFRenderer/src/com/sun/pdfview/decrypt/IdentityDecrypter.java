//#preprocessor

/*
 * File: IdentityDecrypter.java
 * Version: 1.0
 * Initial Creation: May 6, 2010 6:23:44 PM
 *
 * Copyright 2008 Pirion Systems Pty Ltd, 139 Warry St,
 * Fortitude Valley, Queensland, Australia
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
package com.sun.pdfview.decrypt;

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.ByteBuffer;
//#else
import com.sun.pdfview.helper.nio.ByteBuffer;
//#endif

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;

/**
 * Performs identity decryption; that is, inputs aren't encrypted and
 * are returned right back.
 *
 * @author Luke Kirby
 */
public class IdentityDecrypter implements PDFDecrypter
{
	private static final long IDENTITY_DECRYPTOR_ID = 0x7977626E3448F098L;
	private static IdentityDecrypter INSTANCE;
	
    public ByteBuffer decryptBuffer(String cryptFilterName, PDFObject streamObj, ByteBuffer streamBuf) throws PDFParseException
    {
        if (cryptFilterName != null)
        {
            throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.DECRYPT_IDENTITY_NO_FILTER));
        }
        
        return streamBuf;
    }
    
    public String decryptString(int objNum, int objGen, String inputBasicString)
    {
        return inputBasicString;
    }
    
    public static IdentityDecrypter getInstance()
    {
    	if(INSTANCE == null)
    	{
    		INSTANCE = (IdentityDecrypter)com.sun.pdfview.ResourceManager.singletonStorageGet(IDENTITY_DECRYPTOR_ID);
    		if(INSTANCE == null)
    		{
    			INSTANCE = new IdentityDecrypter();
    			com.sun.pdfview.ResourceManager.singletonStorageSet(IDENTITY_DECRYPTOR_ID, INSTANCE);
    		}
    	}
        return INSTANCE;
    }
    
    public boolean isEncryptionPresent()
    {
        return false;
    }
    
    public boolean isEncryptionPresent(String cryptFilterName)
    {
        return false;
    }
    
    public boolean isOwnerAuthorised()
    {
        return false;
    }
}
