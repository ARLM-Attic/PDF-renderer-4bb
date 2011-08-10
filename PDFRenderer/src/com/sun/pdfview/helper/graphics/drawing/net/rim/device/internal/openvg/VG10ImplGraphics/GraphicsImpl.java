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

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

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

/**
 * PDFgraphics implementation of VG10.
 */
public class GraphicsImpl extends PDFGraphics
{
	protected VG10 destination;
	
	protected float[] tmpMatrix;
	private int fillPaint;
	private int patternFillImage;
	protected int error;
	protected float blendAlpha;
	protected float blendAlphaScale;
	
	protected int mask;
	protected Geometry clipObj; //It would be preferred to simply get the Clip from VG but there doesn't appear to be a way to get the clip
	
	private Runnable bindHandler;
	protected Runnable releaseHandler;
	private boolean contextLost;
	
	private static final int PATTERN_IMG_SIZE = 256;
	
	public GraphicsImpl()
	{
		this.fillPaint = VG10.VG_INVALID_HANDLE;
		this.error = VG10.VG_NO_ERROR;
		this.blendAlpha = 1;
		this.blendAlphaScale = 1;
	}
	
	protected void onFinished()
	{
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
		if(this.mask != VG10.VG_INVALID_HANDLE)
		{
			this.destination.vgDestroyImage(this.mask);
			this.mask = VG10.VG_INVALID_HANDLE;
		}
		this.bindHandler = null;
		this.releaseHandler = null;
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
		else if(propertyName.equals("VG") && contextLost)
		{
			contextLost = false;
			this.destination = (VG10)value;
			this.destination.vgSeti(VG10.VG_MASKING, VG10.VG_TRUE);
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
	private final void setDefaults()
	{
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
				this.destination.vgClear(x, y, width, height);
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
	
	public void draw(Geometry s)
	{
		if(isValid())
		{
			bind();
			
			//TODO
			
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
	
	public void fill(Geometry s)
	{
		if(isValid())
		{
			bind();
			
			//TODO
			
			this.releaseHandler.run();
		}
	}
	
	protected final int generatePath(Geometry geo)
	{
		//TODO
		return VG10.VG_INVALID_HANDLE;
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
			tmpMatrix[4] = tmpMatrix[0]; //tmpMatrix = {A, R, G, B, A, ...}
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
		//XXX Outdated, see VG11 impl for up to date version
		
		if(setMask)
		{
			if(org == null)
			{
				//We don't need the mask anymore
				if(this.mask != VG10.VG_INVALID_HANDLE)
				{
					this.destination.vgDestroyImage(this.mask);
					this.mask = VG10.VG_INVALID_HANDLE;
				}
				
				//Query to get the width and height of the current drawing surface
				EGL10 egl = (EGL10)EGLContext.getEGL();
				EGLDisplay cDisp = egl.eglGetCurrentDisplay();
				EGLSurface cSurf = egl.eglGetCurrentSurface(EGL10.EGL_DRAW);
				int[] values = new int[2];
				egl.eglQuerySurface(cDisp, cSurf, EGL10.EGL_HEIGHT, values);
				values[1] = values[0];
				egl.eglQuerySurface(cDisp, cSurf, EGL10.EGL_WIDTH, values);
				
				//Now clear the mask itself, this way that when the mask is recreated (if) that it won't have old data in it
				this.destination.vgMask(VG10.VG_INVALID_HANDLE, VG10.VG_FILL_MASK, 0, 0, values[0], values[1]);
			}
			else
			{
				//TODO: Set mask
			}
		}
		else
		{
			//TODO: Append mask
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
				TranslatedBitmap img = gen.getBitmap(0, 0, PATTERN_IMG_SIZE, PATTERN_IMG_SIZE); //Is there a better way to determine the size of the paint so that it can be used more efficiently?
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
				case PDFGraphics.KEY_ANTIALIASING:
					switch(hintValue)
					{
						case PDFGraphics.VALUE_ANTIALIAS_ON:
						case PDFGraphics.VALUE_ANTIALIAS_OFF:
							//TODO: Would this be VG_RENDERING_QUALITY?
							break;
						default:
							return;
					}
					break;
				case PDFGraphics.KEY_INTERPOLATION:
					switch(hintValue)
					{
						case PDFGraphics.VALUE_INTERPOLATION_BICUBIC:
						case PDFGraphics.VALUE_INTERPOLATION_BILINEAR:
						case PDFGraphics.VALUE_INTERPOLATION_NEAREST_NEIGHBOR:
							//TODO
							break;
						default:
							return;
					}
					break;
				case PDFGraphics.KEY_ALPHA_INTERPOLATION:
					switch(hintValue)
					{
						case PDFGraphics.VALUE_ALPHA_INTERPOLATION_QUALITY:
						case PDFGraphics.VALUE_ALPHA_INTERPOLATION_SPEED:
							//TODO
							break;
						default:
							return;
					}
					break;
			}
			
			this.releaseHandler.run();
		}
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