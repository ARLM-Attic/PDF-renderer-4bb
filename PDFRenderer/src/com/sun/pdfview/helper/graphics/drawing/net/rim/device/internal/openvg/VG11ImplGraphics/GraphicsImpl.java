//#preprocessor

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0

/*
 * File: GraphicsImpl.java
 * Version: 1.0
 * Initial Creation: Aug 7, 2011 7:44:37 PM
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
package com.sun.pdfview.helper.graphics.drawing.net.rim.device.internal.openvg.VG11ImplGraphics;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import net.rim.device.api.openvg.VG10;
import net.rim.device.api.openvg.VG11;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.graphics.Geometry;

/**
 * PDFgraphics implementation of VG11.
 */
public final class GraphicsImpl extends com.sun.pdfview.helper.graphics.drawing.net.rim.device.internal.openvg.VG10ImplGraphics.GraphicsImpl
{
	//setTransform: ignore the glyph matrix (VG_MATRIX_GLYPH_USER_TO_SURFACE if needed later). All glyphs are direct geometry, no font/glyph code needed.
	
	protected void freeMask()
	{
		if(super.mask != VG10.VG_INVALID_HANDLE)
		{
			((VG11)super.destination).vgDestroyMaskLayer(this.mask);
			this.mask = VG10.VG_INVALID_HANDLE;
		}
	}
	
	protected void applyMask(boolean setMask, boolean alphaAdjust, Geometry org)
	{
		//The mathematical operations available in OpenVG are not suited to handle masks with a global alpha.
		//This means that we need to do the inefficient method of masking in order to accomplish tasks
		
		VG11 maskVg = (VG11)super.destination;
		
		//Query to get the width and height of the current drawing surface
		EGL10 egl = (EGL10)EGLContext.getEGL();
		EGLDisplay cDisp = egl.eglGetCurrentDisplay();
		EGLSurface cSurf = egl.eglGetCurrentSurface(EGL10.EGL_DRAW);
		int[] values = new int[2];
		egl.eglQuerySurface(cDisp, cSurf, EGL10.EGL_HEIGHT, values);
		values[1] = values[0];
		egl.eglQuerySurface(cDisp, cSurf, EGL10.EGL_WIDTH, values);
		
		//If the mask needs to be updated (resize, lost context, etc.), free the old mask and remake it
		if(super.updateMask)
		{
			this.freeMask();
			super.updateMask = false;
		}
		
		//Create and set the mask layer to the blendAlpha
		if(super.mask == VG10.VG_INVALID_HANDLE)
		{
			super.mask = maskVg.vgCreateMaskLayer(values[0], values[1]);
			if(super.mask == VG10.VG_INVALID_HANDLE)
			{
				super.error = VG10.VG_OUT_OF_MEMORY_ERROR;
			}
		}
		if(super.mask != VG10.VG_INVALID_HANDLE)
		{
			maskVg.vgFillMaskLayer(super.mask, 0, 0, values[0], values[1], Math.max(Math.min(super.blendAlpha, 1), 0));
		}
		
		//Clear the mask
		maskVg.vgMask(VG10.VG_INVALID_HANDLE, VG10.VG_CLEAR_MASK, 0, 0, values[0], values[1]);
		
		//Set the mask
		super.generatePath(org);
		if(super.path != VG10.VG_INVALID_HANDLE)
		{
			maskVg.vgRenderToMask(super.path, VG10.VG_FILL_PATH, VG10.VG_SET_MASK);
		}
		super.finishPath();
		
		//Modify the mask so that it takes blendAlpha into account
		if(super.mask != VG10.VG_INVALID_HANDLE)
		{
			maskVg.vgMask(super.mask, VG10.VG_INTERSECT_MASK, 0, 0, values[0], values[1]);
		}
		
		/* OLD CODE-Kept because it was close to what the goal of the masking operations were to achieve with better efficiency.
		if(alphaAdjust)
		{
			if(super.blendAlphaScale != 1) //We only want to change if something has actually changed
			{
				//Create the mask layer if it doesn't exist
				if(super.mask == VG10.VG_INVALID_HANDLE)
				{
					super.mask = maskVg.vgCreateMaskLayer(values[0], values[1]);
					if(super.mask == VG10.VG_INVALID_HANDLE)
					{
						super.error = VG10.VG_OUT_OF_MEMORY_ERROR;
					}
				}
				
				if(super.mask != VG10.VG_INVALID_HANDLE)
				{
					if(super.blendAlphaScale < 1)
					{
						//Set the mask layer to just the blendAlpha
						maskVg.vgFillMaskLayer(super.mask, 0, 0, values[0], values[1], Math.max(Math.min(super.blendAlphaScale, 1), 0));
						
						//Update the mask
						maskVg.vgMask(super.mask, VG10.VG_INTERSECT_MASK, 0, 0, values[0], values[1]);
					}
					else
					{
						//TODO: Figure out how to scale up since masks cannot have values greater then 1.
					}
				}
			}
		}
		else
		{
			if(setMask)
			{
				if(org == null)
				{
					//Create the mask layer if it doesn't exist
					if(super.mask == VG10.VG_INVALID_HANDLE)
					{
						super.mask = maskVg.vgCreateMaskLayer(values[0], values[1]);
						if(super.mask == VG10.VG_INVALID_HANDLE)
						{
							super.error = VG10.VG_OUT_OF_MEMORY_ERROR;
						}
					}
					
					if(super.mask != VG10.VG_INVALID_HANDLE)
					{
						//Set the mask layer to just the blendAlpha
						maskVg.vgFillMaskLayer(super.mask, 0, 0, values[0], values[1], Math.max(Math.min(super.blendAlpha, 1), 0));
						
						//Apply the mask
						maskVg.vgMask(super.mask, VG10.VG_SET_MASK, 0, 0, values[0], values[1]);
					}
				}
				else
				{
					//Create and set the mask layer to the blendAlpha
					if(super.mask == VG10.VG_INVALID_HANDLE)
					{
						super.mask = maskVg.vgCreateMaskLayer(values[0], values[1]);
						if(super.mask != VG10.VG_INVALID_HANDLE)
						{
							maskVg.vgFillMaskLayer(super.mask, 0, 0, values[0], values[1], Math.max(Math.min(super.blendAlpha, 1), 0));
						}
						else
						{
							super.error = VG10.VG_OUT_OF_MEMORY_ERROR;
						}
					}
					
					//Clear the mask
					maskVg.vgMask(VG10.VG_INVALID_HANDLE, VG10.VG_CLEAR_MASK, 0, 0, values[0], values[1]);
					
					//Set the mask
					super.generatePath(org);
					if(super.path != VG10.VG_INVALID_HANDLE)
					{
						maskVg.vgRenderToMask(super.path, VG10.VG_FILL_PATH, VG10.VG_SET_MASK);
					}
					super.finishPath();
					
					//Modify the mask so that it takes blendAlpha into account
					if(super.mask != VG10.VG_INVALID_HANDLE)
					{
						maskVg.vgMask(super.mask, VG10.VG_INTERSECT_MASK, 0, 0, values[0], values[1]);
					}
				}
			}
			else
			{
				//Append the mask
				//TODO
			}
		}
		*/
	}
}

//#endif
