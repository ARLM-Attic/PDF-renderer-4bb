/*
 * File: GfxUtil.java
 * Version: 1.0
 * Initial Creation: Aug 8, 2011 1:49:34 PM
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
package com.sun.pdfview.helper.graphics;

/**
 * Graphics utility to get values from the various Graphics helper classes within this namespace.
 */
public final class GfxUtil
{
	private GfxUtil()
	{
	}
	
	private static final float INV_FF = 1f / 255f;
	
	public static float[] getColorAsFloat(int ARGB)
	{
		float[] val = new float[4];
		getColorAsFloat(ARGB, val);
		return val;
	}
	
	public static void getColorAsFloat(int ARGB, float[] buf)
	{
		buf[0] = ((ARGB >> 24) & 0xFF) * INV_FF;
		buf[1] = ((ARGB >> 16) & 0xFF) * INV_FF;
		buf[2] = ((ARGB >> 8) & 0xFF) * INV_FF;
		buf[3] = (ARGB & 0xFF) * INV_FF;
	}
	
	public static boolean isPaintInternal(Paint paint)
	{
		return paint.isInternal();
	}
	
	public static boolean isCompositeInternal(Composite com)
	{
		return com instanceof Composite.DefaultComposite;
	}
	
	public static int compositeType(Composite com)
	{
		return ((Composite.DefaultComposite)com).type;
	}
	
	public static int compositeSrcAlpha(Composite com)
	{
		return ((Composite.DefaultComposite)com).srcAlpha;
	}
	
	public static float compositeSrcAlphaF(Composite com)
	{
		return compositeSrcAlpha(com) * INV_FF;
	}
}
