/*
 * File: ImageInfo.java
 * Version: 1.3
 * Initial Creation: May 8, 2010 11:34:14 PM
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
package com.sun.pdfview;

import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.XYRect;

public class ImageInfo
{
	int width;
    int height;
    XYRect clip;
    int bgColor;
    
    public ImageInfo(int width, int height, XYRect clip)
    {
        this(width, height, clip, Color.WHITE);
    }
    
    public ImageInfo(int width, int height, XYRect clip, int bgColor)
    {
        this.width = width;
        this.height = height;
        this.clip = clip;
        this.bgColor = bgColor;
    }
    
    // a hashcode that uses width, height and clip to generate its number
    public int hashCode()
    {
        int code = (width ^ height << 16);
        
        if (clip != null)
        {
            code ^= ((int)clip.width | (int)clip.height) << 8;
            code ^= ((int)clip.x | (int)clip.y);
        }
        
        return code;
    }
    
    // an equals method that compares values
    public boolean equals(Object o)
    {
        if (!(o instanceof ImageInfo))
        {
            return false;
        }
        
        ImageInfo ii = (ImageInfo)o;
        
        if (width != ii.width || height != ii.height)
        {
            return false;
        }
        else if (clip != null && ii.clip != null)
        {
            return clip.equals(ii.clip);
        }
        else if (clip == null && ii.clip == null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
