/*
 * File: Identity8BitCharsetEncoder.java
 * Version: 1.0
 * Initial Creation: May 9, 2010 8:33:17 PM
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
package com.sun.pdfview;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import com.sun.pdfview.helper.CharsetEncoder;

/**
 * A {@link CharsetEncoder} that attempts to write out the lower 8 bits
 * of any character. Characters &gt;= 256 in value are regarded
 * as unmappable.
 *
 * @author Luke Kirby
 */
public class Identity8BitCharsetEncoder extends CharsetEncoder
{
	public Identity8BitCharsetEncoder()
	{
        super(null, 1, 1);
    }
	
    protected int encodeLoop(ShortBuffer in, ByteBuffer out)
    {
        while (in.remaining() > 0)
        {
            if (out.remaining() < 1)
            {
                return CharsetEncoder.RESULT_OVERFLOW;
            }
            final short c = in.get();
            if (c >= 0 && c < 256)
            {
                out.put((byte)c);
            }
            else
            {
                return CharsetEncoder.resultUnmappableForLength(1);
            }
        }
        return CharsetEncoder.RESULT_UNDERFLOW;
    }
    
    public boolean isLegalReplacement(byte[] repl)
    {
        // avoid referencing the non-existent character set
        return true;
    }
}
