/*
 * File: Resources.java
 * Version: 1.0
 * Initial Creation: Jun 30, 2010 2:22:37 PM
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

import java.io.InputStream;

/**
 * Resource implementation.
 * @author Vincent Simonetti
 */
public interface Resources
{
	public InputStream getStream(String name);
	
	public String getName();
	
	public String getString(long ID);
	
	public String[] getStringArray(long ID);
	
	public String getFormattedString(long ID, Object[] args);
}
