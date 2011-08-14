//#preprocessor

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0

/*
 * File: GraphicsImpl.java
 * Version: 1.0
 * Initial Creation: Aug 7, 2011 7:36:16 PM
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
package com.sun.pdfview.helper.graphics.drawing.net.rim.device.internal.openvg.VG10ImplGraphics;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import net.rim.device.api.opengles.GLUtils;
import net.rim.device.api.openvg.VG10;
import net.rim.device.api.openvg.VGUtils;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Color;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFGraphics;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.GfxUtil;
import com.sun.pdfview.helper.graphics.Paint;
import com.sun.pdfview.helper.graphics.PaintGenerator;
import com.sun.pdfview.helper.graphics.TranslatedBitmap;
import com.sun.pdfview.helper.graphics.Geometry.Enumeration;

/**
 * PDFgraphics implementation of VG10.
 */
public class GraphicsImpl extends PDFGraphics
{
	protected VG10 destination;
	
	protected float[] tmpMatrix;
	private int fillPaint;
	private int patternFillImage;
	protected int path;
	protected int error;
	protected float blendAlpha;
	protected float blendAlphaScale;
	
	protected int mask;
	protected Geometry clipObj; //It would be preferred to simply get the Clip from VG but there doesn't appear to be a way to get the clip
	protected boolean updateMask;
	
	private Runnable bindHandler;
	protected Runnable releaseHandler;
	private boolean contextLost;
	private int aaHint, alphaInterpHint;
	
	public GraphicsImpl()
	{
		this.fillPaint = VG10.VG_INVALID_HANDLE;
		this.error = VG10.VG_NO_ERROR;
		this.blendAlpha = 1;
		this.blendAlphaScale = 1;
		this.updateMask = false;
	}
	
	protected final void onFinished()
	{
		if(this.path != VG10.VG_INVALID_HANDLE)
		{
			this.destination.vgDestroyPath(this.path);
			this.path = VG10.VG_INVALID_HANDLE;
		}
		if(this.fillPaint != VG10.VG_INVALID_HANDLE)
		{
			this.destination.vgDestroyPaint(this.fillPaint);
			this.fillPaint = VG10.VG_INVALID_HANDLE;
		}
		if(this.patternFillImage != VG10.VG_INVALID_HANDLE)
		{
			this.destination.vgDestroyImage(this.patternFillImage);
			this.patternFillImage = VG10.VG_INVALID_HANDLE;
		}
		freeMask();
		this.bindHandler = null;
		this.releaseHandler = null;
	}
	
	protected void freeMask()
	{
		if(this.mask != VG10.VG_INVALID_HANDLE)
		{
			this.destination.vgDestroyImage(this.mask);
			this.mask = VG10.VG_INVALID_HANDLE;
		}
	}
	
	public final boolean hasExtraProperties()
	{
		return true;
	}
	
	public final boolean isValid()
	{
		return this.bindHandler != null && this.releaseHandler != null;
	}
	
	public final boolean setProperty(String propertyName, Object value)
	{
		if(propertyName.equals("BIND_HANDLER"))
		{
			this.bindHandler = (Runnable)value;
			if(isValid())
			{
				setDefaults();
			}
			return true;
		}
		else if(propertyName.equals("RELEASE_HANDLER"))
		{
			this.releaseHandler = (Runnable)value;
			if(isValid())
			{
				setDefaults();
			}
			return true;
		}
		else if(propertyName.equals("VG") && this.contextLost)
		{
			this.contextLost = false;
			this.destination = (VG10)value;
			this.updateMask = true;
			//this.destination.vgSeti(VG10.VG_MASKING, VG10.VG_TRUE); //It's assumed that loss of context would not mean that parameters would be erased.
			return true;
		}
		return false;
	}
	
	public final Object getProperty(String propertyName)
	{
		if(propertyName.equals("BIND_HANDLER"))
		{
			return this.bindHandler;
		}
		else if(propertyName.equals("RELEASE_HANDLER"))
		{
			return this.releaseHandler;
		}
		/*
		else if(propertyName.equals("VG"))
		{
			return this.destination;
		}
		*/
		return null;
	}
	
	public final String[] getSupportedProperties()
	{
		return new String[]{"BIND_HANDLER", "RELEASE_HANDLER", "VG"};
	}
	
	//Set default rendering hints
	private void setDefaults()
	{
		this.aaHint = PDFGraphics.VALUE_ANTIALIAS_ON;
		this.alphaInterpHint = PDFGraphics.VALUE_ALPHA_INTERPOLATION_QUALITY;
		
		setRenderingHint(PDFGraphics.KEY_ANTIALIASING, PDFGraphics.VALUE_ANTIALIAS_DEFAULT);
		setRenderingHint(PDFGraphics.KEY_INTERPOLATION, PDFGraphics.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		setRenderingHint(PDFGraphics.KEY_ALPHA_INTERPOLATION, PDFGraphics.VALUE_ALPHA_INTERPOLATION_DEFAULT);
	}
	
	protected final void setDrawingDevice(Object device)
	{
		if(device == null)
		{
			throw new NullPointerException();
		}
		this.destination = (VG10)device; //Everything is based of VG10 so it can be used as the base.
		this.destination.vgSeti(VG10.VG_MASKING, VG10.VG_TRUE);
	}
	
	public final void clear(int x, int y, int width, int height)
	{
		if(isValid())
		{
			if(width >= 0 && height >= 0)
			{
				bind();
				
				this.destination.vgSeti(VG10.VG_MASKING, VG10.VG_FALSE);
				this.destination.vgClear(x, y, width, height); //OpenVG Specification 1.1 says that vgClear is affected by scissoring and masking but not clipping.
				this.destination.vgSeti(VG10.VG_MASKING, VG10.VG_TRUE);
				
				this.releaseHandler.run();
			}
		}
	}
	
	protected final void bind()
	{
		this.contextLost = true; //Do this in case context is lost and needs to be reset
		this.bindHandler.run();
		this.contextLost = false;
	}
	
	public final void draw(Geometry s)
	{
		if(isValid())
		{
			bind();
			
			generatePath(s);
			if(this.path != VG10.VG_INVALID_HANDLE)
			{
				draw(this.path, VG10.VG_STROKE_PATH, s.getWindingRule() == Geometry.WIND_EVEN_ODD ? VG10.VG_EVEN_ODD : VG10.VG_NON_ZERO);
			}
			finishPath();
			
			this.releaseHandler.run();
		}
	}
	
	public final boolean drawImage(Bitmap img, AffineTransform xform)
	{
		boolean drawn = false;
		if(isValid())
		{
			bind();
			
			//First we need to copy over the geometry matrix
			this.destination.vgSeti(VG10.VG_MATRIX_MODE, VG10.VG_MATRIX_PATH_USER_TO_SURFACE);
			if(tmpMatrix == null)
			{
				tmpMatrix = new float[9];
			}
			this.destination.vgGetMatrix(tmpMatrix, 0);
			
			//Now write it to the image matrix
			this.destination.vgSeti(VG10.VG_MATRIX_MODE, VG10.VG_MATRIX_IMAGE_USER_TO_SURFACE); //Images
			this.destination.vgLoadMatrix(tmpMatrix, 0);
			
			//Apply the image space-to-user space transform matrix
			xform.getArray(tmpMatrix);
			this.destination.vgMultMatrix(tmpMatrix, 0);
			
			//Determine if the image matrix is invertible, if not then the image isn't drawn (not the most efficient but saves memory if the image isn't invertible)
			this.destination.vgGetMatrix(tmpMatrix, 0);
			if(new AffineTransform(tmpMatrix).isInvertable())
			{
				//Create the image
				int image = VGUtils.vgCreateImage(this.destination, img, false, this.destination.vgGeti(VG10.VG_IMAGE_QUALITY));
				if(image != VG10.VG_INVALID_HANDLE)
				{
					//Draw the image
					this.destination.vgDrawImage(image);
					drawn = true;
					
					//Cleanup
					this.destination.vgDestroyImage(image);
				}
			}
			
			this.releaseHandler.run();
		}
		return drawn;
	}
	
	public final void fill(Geometry s)
	{
		if(isValid())
		{
			bind();
			
			generatePath(s);
			if(this.path != VG10.VG_INVALID_HANDLE)
			{
				draw(this.path, VG10.VG_FILL_PATH | VG10.VG_STROKE_PATH, s.getWindingRule() == Geometry.WIND_EVEN_ODD ? VG10.VG_EVEN_ODD : VG10.VG_NON_ZERO);
			}
			finishPath();
			
			this.releaseHandler.run();
		}
	}
	
	protected final void generatePath(Geometry geo)
	{
		Geometry.Enumeration en = geo.getPathEnumerator(null);
		float[] coords = new float[6];
		
		//First count everything
		int cmdCount = 0;
		int coordCount = 0;
		while (!en.isDone())
		{
            switch (en.currentSegment(coords))
            {
	            case Enumeration.SEG_CUBICTO:
	            	coordCount += 2; //6 coords
	            case Enumeration.SEG_QUADTO:
	            	coordCount += 2; //4 coords
	            case Enumeration.SEG_MOVETO:
	            case Enumeration.SEG_LINETO:
	            	coordCount += 2; //2 coords
	            case Enumeration.SEG_CLOSE:
	            	cmdCount++;
	                break;
                default:
                	cmdCount = -1;
                	break;
            }
            if(cmdCount == -1)
            {
            	break; //Error
            }
            en.next();
        }
		
		if(cmdCount > 0 && coordCount > 0)
		{
			if(this.path == VG10.VG_INVALID_HANDLE)
			{
				this.path = this.destination.vgCreatePath(VG10.VG_PATH_FORMAT_STANDARD, VG10.VG_PATH_DATATYPE_F, 1, 0, cmdCount, coordCount, VG10.VG_PATH_CAPABILITY_APPEND_TO);
			}
			
			if(this.path != VG10.VG_INVALID_HANDLE) //We don't want to do extra work if we can't create the path
			{
				ByteBuffer cmdBuffer = ByteBuffer.allocateDirect(cmdCount); //Always use native Buffers. Do this because OpenVG is supported 6.0 and higher, NIO was introduced in 5.0.
				FloatBuffer coordBuffer = ByteBuffer.allocateDirect(coordCount * 4).asFloatBuffer();
				
				en = geo.getPathEnumerator(null);
				while (!en.isDone())
				{
					switch (en.currentSegment(coords))
		            {
			            case Enumeration.SEG_MOVETO:
			            	cmdBuffer.put(VG10.VG_MOVE_TO_ABS);
			            	coordBuffer.put(coords, 0, 2);
			            	break;
			            case Enumeration.SEG_LINETO:
			            	cmdBuffer.put(VG10.VG_LINE_TO_ABS);
			            	coordBuffer.put(coords, 0, 2);
			                break;
			            case Enumeration.SEG_QUADTO:
			            	cmdBuffer.put(VG10.VG_QUAD_TO_ABS);
			            	coordBuffer.put(coords, 0, 4);
			                break;
			            case Enumeration.SEG_CUBICTO:
			            	cmdBuffer.put(VG10.VG_CUBIC_TO_ABS);
			            	coordBuffer.put(coords, 0, 6);
			                break;
			            case Enumeration.SEG_CLOSE:
			            	cmdBuffer.put(VG10.VG_CLOSE_PATH);
			                break;
		            }
		            en.next();
		        }
				cmdBuffer.flip();
				coordBuffer.flip();
				
				this.destination.vgAppendPathData(this.path, cmdBuffer, coordBuffer);
				
				if((this.error = this.destination.vgGetError()) != VG10.VG_NO_ERROR)
				{
					//An error occurred, destroy the path and recreate it
					this.destination.vgDestroyPath(this.path);
					this.path = this.destination.vgCreatePath(VG10.VG_PATH_FORMAT_STANDARD, VG10.VG_PATH_DATATYPE_F, 1, 0, cmdCount, coordCount, VG10.VG_PATH_CAPABILITY_APPEND_TO);
					
					if(this.path != VG10.VG_INVALID_HANDLE)
					{
						this.destination.vgAppendPathData(this.path, cmdBuffer, coordBuffer);
					}
					
					//If this doesn't work then it won't work and should be ignored
				}
				
				GLUtils.freeBuffer(cmdBuffer);
				GLUtils.freeBuffer(coordBuffer);
				
				if(this.path != VG10.VG_INVALID_HANDLE)
				{
					this.destination.vgRemovePathCapabilities(this.path, VG10.VG_PATH_CAPABILITY_APPEND_TO); //Make the path read-only
				}
			}
		}
	}
	
	protected final void finishPath()
	{
		if(this.path != VG10.VG_INVALID_HANDLE)
		{
			this.destination.vgClearPath(this.path, VG10.VG_PATH_CAPABILITY_APPEND_TO);
		}
	}
	
	protected final void draw(int path, int paintModes, int fillMode)
	{
		this.destination.vgSeti(VG10.VG_FILL_RULE, fillMode);
		if((paintModes & VG10.VG_FILL_PATH) != 0)
		{
			this.destination.vgSetPaint(this.fillPaint, VG10.VG_FILL_PATH);
		}
		if((paintModes & VG10.VG_STROKE_PATH) != 0)
		{
			this.destination.vgSetPaint(this.fillPaint, VG10.VG_STROKE_PATH);
		}
		this.destination.vgDrawPath(path, paintModes);
	}
	
	public final Geometry getClip()
	{
		return this.clipObj;
	}
	
	public final AffineTransform getTransform()
	{
		if(tmpMatrix == null)
		{
			tmpMatrix = new float[9];
		}
		if(isValid())
		{
			bind();
			
			this.destination.vgSeti(VG10.VG_MATRIX_MODE, VG10.VG_MATRIX_PATH_USER_TO_SURFACE);
			this.destination.vgGetMatrix(tmpMatrix, 0);
			
			this.releaseHandler.run();
		}
		return new AffineTransform(tmpMatrix);
	}
	
	public final void setBackgroundColor(int c)
	{
		if(isValid())
		{
			bind();
			
			if(tmpMatrix == null)
			{
				tmpMatrix = new float[9];
			}
			GfxUtil.getColorAsFloat(c, tmpMatrix); //tmpMatrix = {A, R, G, B, ...}
			//Color format not in correct order, move it around
			//tmpMatrix[4] = tmpMatrix[0]; //tmpMatrix = {A, R, G, B, A, ...}
			tmpMatrix[4] = 1; //Though using the alpha from the color would be preferred, it doesn't always include alpha.
			this.destination.vgSetfv(VG10.VG_CLEAR_COLOR, 4, tmpMatrix, 1); //tmpMatrix = {A, |R, G, B, A|, ...}
			
			this.releaseHandler.run();
		}
	}
	
	protected final void setClip(Geometry s, boolean direct)
	{
		if(isValid())
		{
			bind();
			
			//VG 1.0 can do masking by using images. Instead of creating a "mask" (a la 1.1 >), you create an image, draw to the image, then apply that as a mask.
			boolean set = false;
			Geometry mod = null;
			if(s == null)
			{
				this.clipObj = null;
				set = true;
			}
			else
			{
				//Get the paint matrix
				this.destination.vgSeti(VG10.VG_MATRIX_MODE, VG10.VG_MATRIX_FILL_PAINT_TO_USER);
				this.destination.vgGetMatrix(tmpMatrix, 0);
				
				//Modify the clip
				mod = s.createTransformedShape(new AffineTransform(tmpMatrix));
				if(direct || this.clipObj == null)
				{
					this.clipObj = mod;
					set = true;
				}
				else
				{
					this.clipObj.append(mod, false);
				}
			}
			applyMask(set, false, s);
			
			this.releaseHandler.run();
		}
	}
	
	protected void applyMask(boolean setMask, boolean alphaAdjust, Geometry org)
	{
		//If the mask needs to be updated (resize, lost context, etc.), free the old mask and remake it
		if(this.updateMask)
		{
			this.freeMask();
			this.updateMask = false;
		}
		
		//Create and set the mask layer to the blendAlpha
		if(this.mask == VG10.VG_INVALID_HANDLE)
		{
			//Query to get the width and height of the current drawing surface
			EGL10 egl = (EGL10)EGLContext.getEGL();
			EGLDisplay cDisp = egl.eglGetCurrentDisplay();
			EGLSurface cSurf = egl.eglGetCurrentSurface(EGL10.EGL_DRAW);
			int[] values = new int[2];
			egl.eglQuerySurface(cDisp, cSurf, EGL10.EGL_HEIGHT, values);
			values[1] = values[0];
			egl.eglQuerySurface(cDisp, cSurf, EGL10.EGL_WIDTH, values);
			
			this.mask = this.destination.vgCreateImage(VG10.VG_sL_8, values[0], values[1], this.destination.vgGeti(VG10.VG_IMAGE_QUALITY));
			if(this.mask == VG10.VG_INVALID_HANDLE)
			{
				this.error = VG10.VG_OUT_OF_MEMORY_ERROR;
			}
		}
		
		if(this.mask != VG10.VG_INVALID_HANDLE)
		{
			//Not any way efficient but the best way to do the equivalent of vgRenderToMask.
			//This is because there doesn't seem to be a way to set an image to be the drawing surface unless a full VG object was created for the image and the path was drawn to it.
			
			int w = this.destination.vgGetParameteri(this.mask, VG10.VG_IMAGE_WIDTH);
			int h = this.destination.vgGetParameteri(this.mask, VG10.VG_IMAGE_HEIGHT);
			
			//Backup the clear-color
			float[] clearColor = new float[8];
			this.destination.vgGetfv(VG10.VG_CLEAR_COLOR, 4, clearColor, 0); //Get previous clear-color
			
			//Clear the mask
			if(this.clipObj != null)
			{
				this.destination.vgMask(VG10.VG_INVALID_HANDLE, VG10.VG_CLEAR_MASK, 0, 0, w, h);
				
				//Set the mask
				generatePath(this.clipObj);
				if(path != VG10.VG_INVALID_HANDLE)
				{
					//Create a temp image to save the current drawing surface
					int surfaceBackup = this.destination.vgCreateImage(VG10.VG_sARGB_8888, w, h, VG10.VG_IMAGE_QUALITY_BETTER);
					if(surfaceBackup != VG10.VG_INVALID_HANDLE)
					{
						//Copy drawing surface to temp image
						this.destination.vgGetPixels(surfaceBackup, 0, 0, 0, 0, w, h);
						
						//Clear the surface
						clearColor[4] = clearColor[5] = clearColor[6] = clearColor[7] = 0; //Black (no drawing)
						this.destination.vgSetfv(VG10.VG_CLEAR_COLOR, 4, clearColor, 4);
						this.destination.vgClear(0, 0, w, h);
						
						//Create temp paint objects
						clearColor[4] = clearColor[5] = clearColor[6] = clearColor[7] = 1; //White (draw everything)
						int maskPaint = this.destination.vgCreatePaint();
						if(maskPaint != VG10.VG_INVALID_HANDLE)
						{
							this.destination.vgSetParameteri(maskPaint, VG10.VG_PAINT_TYPE, VG10.VG_PAINT_TYPE_COLOR);
							this.destination.vgSetParameterfv(maskPaint, VG10.VG_PAINT_COLOR, 4, clearColor, 4);
							
							//Draw mask to drawing surface
							draw(this.path, VG10.VG_FILL_PATH, this.clipObj.getWindingRule() == Geometry.WIND_EVEN_ODD ? VG10.VG_EVEN_ODD : VG10.VG_NON_ZERO);
							
							//Copy drawing surface to mask
							this.destination.vgGetPixels(this.mask, 0, 0, 0, 0, w, h);
							
							//Draw mask
							this.destination.vgMask(this.mask, VG10.VG_SET_MASK, 0, 0, w, h);
						}
						
						//Restore the drawing (make sure masking is off first)
						this.destination.vgSeti(VG10.VG_MASKING, VG10.VG_FALSE);
						this.destination.vgSetPixels(0, 0, surfaceBackup, 0, 0, w, h);
						this.destination.vgSeti(VG10.VG_MASKING, VG10.VG_TRUE);
						
						//Clear resources
						this.destination.vgDestroyImage(surfaceBackup);
					}
				}
				finishPath();
			}
			else
			{
				this.destination.vgMask(VG10.VG_INVALID_HANDLE, VG10.VG_FILL_MASK, 0, 0, w, h); //This sets everything to be drawn
			}
			
			//Need to temporarily set the clear color in order to set the mask
			clearColor[4] = clearColor[5] = clearColor[6] = clearColor[7] = Math.max(Math.min(this.blendAlpha, 1), 0);
			this.destination.vgSetfv(VG10.VG_CLEAR_COLOR, 4, clearColor, 4); //Set the "new" clear-color
			this.destination.vgClearImage(this.mask, 0, 0, w, h); //Clear the mask
			
			//Restore previous clear-color
			this.destination.vgSetfv(VG10.VG_CLEAR_COLOR, 4, clearColor, 0);
			
			//Modify the mask so that it takes blendAlpha into account
			this.destination.vgMask(this.mask, VG10.VG_INTERSECT_MASK, 0, 0, w, h);
		}
	}
	
	public final void setColor(int c)
	{
		if(isValid())
		{
			bind();
			
			if(this.error == VG10.VG_NO_ERROR)
			{
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
				if(this.fillPaint != VG10.VG_INVALID_HANDLE)
				{
					if(this.destination.vgGetParameteri(this.fillPaint, VG10.VG_PAINT_TYPE) != VG10.VG_PAINT_TYPE_COLOR)
					{
						this.destination.vgSetParameteri(this.fillPaint, VG10.VG_PAINT_TYPE, VG10.VG_PAINT_TYPE_COLOR);
					}
					this.destination.vgSetColor(this.fillPaint, c);
				}
				else
				{
					this.fillPaint = VGUtils.vgCreateColorPaint(this.destination, c);
				}
				/*
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0
				if(this.fillPaint == VG10.VG_INVALID_HANDLE)
				{
					this.fillPaint = this.destination.vgCreatePaint();
					if(this.fillPaint == VG10.VG_INVALID_HANDLE)
					{
						this.error = VG10.VG_OUT_OF_MEMORY_ERROR; //This is the only time something like this can happen
					}
					else
					{
						this.destination.vgSetParameteri(this.fillPaint, VG10.VG_PAINT_TYPE, VG10.VG_PAINT_TYPE_COLOR);
					}
				}
				if(this.error == VG10.VG_NO_ERROR)
				{
					if(this.destination.vgGetParameteri(this.fillPaint, VG10.VG_PAINT_TYPE) != VG10.VG_PAINT_TYPE_COLOR)
					{
						this.destination.vgSetParameteri(this.fillPaint, VG10.VG_PAINT_TYPE, VG10.VG_PAINT_TYPE_COLOR);
					}
					this.destination.vgSetColor(this.fillPaint, c);
				}
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
				*/
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0
			}
			
			this.releaseHandler.run();
		}
	}
	
	public final void setComposite(Composite comp)
	{
		if(isValid())
		{
			bind();
			
			if(GfxUtil.isCompositeInternal(comp))
			{
				int blend = this.destination.vgGeti(VG10.VG_BLEND_MODE);
				switch(GfxUtil.compositeType(comp))
				{
					case Composite.SRC:
						blend = VG10.VG_BLEND_SRC;
						break;
					case Composite.SRC_OVER:
						blend = VG10.VG_BLEND_SRC_OVER;
						break;
				}
				this.destination.vgSeti(VG10.VG_BLEND_MODE, blend);
				float blendA = GfxUtil.compositeSrcAlphaF(comp);
				if(blendA != this.blendAlpha)
				{
					this.blendAlphaScale = blendA / this.blendAlpha;
					this.blendAlpha = blendA;
					applyMask(true, true, clipObj);
				}
			}
			else
			{
				//Not sure what to do here just yet
				throw new UnsupportedOperationException("How do we handle non-internal Composites?");
			}
			
			this.releaseHandler.run();
		}
	}
	
	public final void setPaint(Paint paint)
	{
		if(paint != null && isValid())
		{
			bind();
			
			if(GfxUtil.isPaintInternal(paint))
			{
				setColor(paint.getColor()); //Internal Paint object is always a solid color
			}
			else
			{
				//Get width and height
				EGL10 egl = (EGL10)EGLContext.getEGL();
				EGLDisplay cDisp = egl.eglGetCurrentDisplay();
				EGLSurface cSurf = egl.eglGetCurrentSurface(EGL10.EGL_DRAW);
				int[] values = new int[2];
				egl.eglQuerySurface(cDisp, cSurf, EGL10.EGL_HEIGHT, values);
				values[1] = values[0];
				egl.eglQuerySurface(cDisp, cSurf, EGL10.EGL_WIDTH, values);
				
				if(this.patternFillImage != VG10.VG_INVALID_HANDLE)
				{
					this.destination.vgDestroyImage(this.patternFillImage);
					this.patternFillImage = VG10.VG_INVALID_HANDLE;
				}
				
				//Create the paint object and set it to a pattern type
				if(this.fillPaint == VG10.VG_INVALID_HANDLE)
				{
					this.fillPaint = this.destination.vgCreatePaint();
					if(this.fillPaint == VG10.VG_INVALID_HANDLE)
					{
						this.error = VG10.VG_OUT_OF_MEMORY_ERROR; //This is the only time something like this can happen
					}
					else
					{
						this.destination.vgSetParameteri(this.fillPaint, VG10.VG_PAINT_TYPE, VG10.VG_PAINT_TYPE_PATTERN);
					}
				}
				else if(this.destination.vgGetParameteri(this.fillPaint, VG10.VG_PAINT_TYPE) != VG10.VG_PAINT_TYPE_PATTERN)
				{
					this.destination.vgSetParameteri(this.fillPaint, VG10.VG_PAINT_TYPE, VG10.VG_PAINT_TYPE_PATTERN);
				}
				
				//Get the paint matrix
				this.destination.vgSeti(VG10.VG_MATRIX_MODE, VG10.VG_MATRIX_FILL_PAINT_TO_USER);
				this.destination.vgGetMatrix(tmpMatrix, 0);
				
				//Get the image
				PaintGenerator gen = paint.createGenerator(new AffineTransform(tmpMatrix));
				TranslatedBitmap img = gen.getBitmap(0, 0, values[0], values[1]);
				gen.dispose();
				
				//Set the paint image
				this.patternFillImage = VGUtils.vgCreateImage(this.destination, img.getBitmap(), false, this.destination.vgGeti(VG10.VG_IMAGE_QUALITY));
				if(this.patternFillImage != VG10.VG_INVALID_HANDLE)
				{
					this.destination.vgPaintPattern(this.fillPaint, this.patternFillImage);
				}
				else
				{
					//Great... it failed. Ok, revert to a solid black color
					this.destination.vgSetParameteri(this.fillPaint, VG10.VG_PAINT_TYPE, VG10.VG_PAINT_TYPE_COLOR);
					this.destination.vgSetColor(this.fillPaint, Color.BLACK);
				}
			}
			
			this.releaseHandler.run();
		}
	}
	
	public void setRenderingHint(int hintKey, int hintValue)
	{
		if(isValid())
		{
			bind();
			
			switch(hintKey)
			{
				/* Combined quality setting
				 * (KEY_ANTIALIASING = VALUE_ANTIALIAS_ON) & (KEY_ALPHA_INTERPOLATION = VALUE_ALPHA_INTERPOLATION_QUALITY) == VG_RENDERING_QUALITY_BETTER
				 * (KEY_ANTIALIASING = VALUE_ANTIALIAS_OFF) & (KEY_ALPHA_INTERPOLATION = VALUE_ALPHA_INTERPOLATION_QUALITY) == VG_RENDERING_QUALITY_FASTER
				 * (KEY_ANTIALIASING = VALUE_ANTIALIAS_ON) & (KEY_ALPHA_INTERPOLATION = VALUE_ALPHA_INTERPOLATION_SPEED) == VG_RENDERING_QUALITY_FASTER
				 * (KEY_ANTIALIASING = VALUE_ANTIALIAS_OFF) & (KEY_ALPHA_INTERPOLATION = VALUE_ALPHA_INTERPOLATION_SPEED) == VG_RENDERING_QUALITY_NONANTIALIASED
				 */
				case PDFGraphics.KEY_ANTIALIASING:
					switch(hintValue)
					{
						case PDFGraphics.VALUE_ANTIALIAS_ON:
						case PDFGraphics.VALUE_ANTIALIAS_OFF:
							this.aaHint = hintValue;
							break;
					}
					setRenderingQuaility();
					break;
				case PDFGraphics.KEY_ALPHA_INTERPOLATION:
					switch(hintValue)
					{
						case PDFGraphics.VALUE_ALPHA_INTERPOLATION_QUALITY:
						case PDFGraphics.VALUE_ALPHA_INTERPOLATION_SPEED:
							this.alphaInterpHint = hintValue;
							break;
					}
					setRenderingQuaility();
					break;
					
				//Dedicated quality function
				case PDFGraphics.KEY_INTERPOLATION:
					switch(hintValue)
					{
						case PDFGraphics.VALUE_INTERPOLATION_BICUBIC:
							this.destination.vgSeti(VG10.VG_IMAGE_QUALITY, VG10.VG_IMAGE_QUALITY_BETTER);
							break;
						case PDFGraphics.VALUE_INTERPOLATION_BILINEAR:
							this.destination.vgSeti(VG10.VG_IMAGE_QUALITY, VG10.VG_IMAGE_QUALITY_FASTER);
							break;
						case PDFGraphics.VALUE_INTERPOLATION_NEAREST_NEIGHBOR:
							this.destination.vgSeti(VG10.VG_IMAGE_QUALITY, VG10.VG_IMAGE_QUALITY_NONANTIALIASED);
							break;
					}
					break;
			}
			
			this.releaseHandler.run();
		}
	}
	
	private void setRenderingQuaility()
	{
		int quality;
		if(this.aaHint == PDFGraphics.VALUE_ANTIALIAS_ON && this.alphaInterpHint == PDFGraphics.VALUE_ALPHA_INTERPOLATION_QUALITY)
		{
			quality = VG10.VG_RENDERING_QUALITY_BETTER;
		}
		else if(this.aaHint == PDFGraphics.VALUE_ANTIALIAS_OFF && this.alphaInterpHint == PDFGraphics.VALUE_ALPHA_INTERPOLATION_SPEED)
		{
			quality = VG10.VG_RENDERING_QUALITY_NONANTIALIASED;
		}
		else
		{
			quality = VG10.VG_RENDERING_QUALITY_FASTER;
		}
		this.destination.vgSeti(VG10.VG_RENDERING_QUALITY, quality);
	}
	
	public final void setStroke(BasicStroke s)
	{
		if(isValid())
		{
			bind();
			
			if(this.error == VG10.VG_NO_ERROR)
			{
				int cap = this.destination.vgGeti(VG10.VG_STROKE_CAP_STYLE);
				switch(s.getEndCap())
				{
					case BasicStroke.CAP_BUTT:
						cap = VG10.VG_CAP_BUTT;
						break;
					case BasicStroke.CAP_ROUND:
						cap = VG10.VG_CAP_ROUND;
						break;
					case BasicStroke.CAP_SQUARE:
						cap = VG10.VG_CAP_SQUARE;
						break;
				}
				this.destination.vgSeti(VG10.VG_STROKE_CAP_STYLE, cap);
				
				int join = this.destination.vgGeti(VG10.VG_STROKE_JOIN_STYLE);
				switch(s.getLineJoin())
				{
					case BasicStroke.JOIN_BEVEL:
						join = VG10.VG_JOIN_BEVEL;
						break;
					case BasicStroke.JOIN_MITER:
						join = VG10.VG_JOIN_MITER;
						break;
					case BasicStroke.JOIN_ROUND:
						join = VG10.VG_JOIN_ROUND;
						break;
				}
				this.destination.vgSeti(VG10.VG_STROKE_JOIN_STYLE, join);
				
				this.destination.vgSetf(VG10.VG_STROKE_LINE_WIDTH, s.getLineWidth());
				this.error = this.destination.vgGetError();
				
				if(this.error == VG10.VG_NO_ERROR)
				{
					this.destination.vgSetf(VG10.VG_STROKE_MITER_LIMIT, s.getMiterLimit());
					this.error = this.destination.vgGetError();
					
					if(this.error == VG10.VG_NO_ERROR)
					{
						this.destination.vgSetf(VG10.VG_STROKE_DASH_PHASE, s.getDashPhase());
						this.error = this.destination.vgGetError();
						
						if(this.error == VG10.VG_NO_ERROR)
						{
							float[] dash = s.getDashArray();
							if(dash == null)
							{
								dash = new float[0];
							}
							this.destination.vgSetfv(VG10.VG_STROKE_DASH_PATTERN, dash.length, dash, 0);
							this.error = this.destination.vgGetError();
						}
					}
				}
			}
			
			this.releaseHandler.run();
		}
	}
	
	protected final void setTransform(AffineTransform Tx, boolean direct)
	{
		if(isValid())
		{
			bind();
			
			if(direct)
			{
				this.destination.vgSeti(VG10.VG_MATRIX_MODE, VG10.VG_MATRIX_PATH_USER_TO_SURFACE); //Geometry
				if(Tx == null)
				{
					this.destination.vgLoadIdentity();
				}
				else
				{
					if(tmpMatrix == null)
					{
						tmpMatrix = new float[9];
					}
					Tx.getArray(tmpMatrix);
					this.destination.vgLoadMatrix(tmpMatrix, 0);
				}
				this.destination.vgSeti(VG10.VG_MATRIX_MODE, VG10.VG_MATRIX_FILL_PAINT_TO_USER); //Fill paint
				if(Tx == null)
				{
					this.destination.vgLoadIdentity();
				}
				else
				{
					//Don't need to worry about getting the matrix data or creating the array, done already in previous else statement
					this.destination.vgLoadMatrix(tmpMatrix, 0);
				}
				//setTransform: ignore the stroke matrix (VG_MATRIX_STROKE_PAINT_TO_USER if needed later)
			}
			else if(Tx != null) //We only want to do this if Tx doesn't equal null. Null means identity, which we don't need to do an operation with.
			{
				if(tmpMatrix == null)
				{
					tmpMatrix = new float[9];
				}
				Tx.getArray(tmpMatrix);
				this.destination.vgMultMatrix(tmpMatrix, 0);
			}
			
			this.releaseHandler.run();
		}
	}
	
	public final void translate(int x, int y)
	{
		if(isValid())
		{
			bind();
			
			this.destination.vgTranslate(x, y);
			
			this.releaseHandler.run();
		}
	}
}

//#endif
