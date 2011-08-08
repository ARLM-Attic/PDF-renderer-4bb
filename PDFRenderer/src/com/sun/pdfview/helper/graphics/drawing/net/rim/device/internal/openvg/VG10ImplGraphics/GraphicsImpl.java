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

import net.rim.device.api.openvg.VG10;
import net.rim.device.api.system.Bitmap;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFGraphics;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.GfxUtil;
import com.sun.pdfview.helper.graphics.Paint;

/**
 * PDFgraphics implementation of VG10.
 */
public class GraphicsImpl extends PDFGraphics
{
	protected VG10 destination;
	
	private float[] tmpMatrix;
	private int fillPaint;
	private int error;
	private int blendAlpha;
	
	public GraphicsImpl()
	{
		this.fillPaint = VG10.VG_INVALID_HANDLE;
		this.error = VG10.VG_NO_ERROR;
		this.blendAlpha = 255;
	}
	
	protected void onFinished()
	{
		if(this.fillPaint != VG10.VG_INVALID_HANDLE)
		{
			this.destination.vgDestroyPaint(this.fillPaint);
			this.fillPaint = VG10.VG_INVALID_HANDLE;
			this.error = this.destination.vgGetError();
		}
		//TODO
	}
	
	protected final void setDrawingDevice(Object device)
	{
		if(device == null)
		{
			throw new NullPointerException();
		}
		this.destination = (VG10)device; //Everything is based of VG10 so it can be used as the base.
		
		setRenderingHint(PDFGraphics.KEY_ANTIALIASING, PDFGraphics.VALUE_ANTIALIAS_DEFAULT);
		setRenderingHint(PDFGraphics.KEY_INTERPOLATION, PDFGraphics.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		setRenderingHint(PDFGraphics.KEY_ALPHA_INTERPOLATION, PDFGraphics.VALUE_ALPHA_INTERPOLATION_DEFAULT);
	}
	
	public final void clear(int x, int y, int width, int height)
	{
		if(width >= 0 && height >= 0)
		{
			int preValue = this.destination.vgGeti(VG10.VG_MASKING);
			this.destination.vgSeti(VG10.VG_MASKING, VG10.VG_FALSE);
			this.destination.vgClear(x, y, width, height);
			this.destination.vgSeti(VG10.VG_MASKING, preValue);
		}
	}
	
	public void draw(Geometry s)
	{
		//TODO
	}
	
	public boolean drawImage(Bitmap img, AffineTransform xform)
	{
		//TODO: Check all states, make sure no context is lost, etc.
		
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
		
		//TODO
		return false;
	}
	
	public void fill(Geometry s)
	{
		//TODO
	}
	
	public Geometry getClip()
	{
		//TODO
		return null;
	}
	
	public final AffineTransform getTransform()
	{
		if(tmpMatrix == null)
		{
			tmpMatrix = new float[9];
		}
		this.destination.vgSeti(VG10.VG_MATRIX_MODE, VG10.VG_MATRIX_PATH_USER_TO_SURFACE);
		this.destination.vgGetMatrix(tmpMatrix, 0);
		return new AffineTransform(tmpMatrix);
	}
	
	public final void setBackgroundColor(int c)
	{
		if(tmpMatrix == null)
		{
			tmpMatrix = new float[9];
		}
		GfxUtil.getColorAsFloat(c, tmpMatrix); //tmpMatrix = {A, R, G, B, ...}
		//Color format not in correct order, move it around
		tmpMatrix[4] = tmpMatrix[0]; //tmpMatrix = {A, R, G, B, A, ...}
		this.destination.vgSetfv(VG10.VG_CLEAR_COLOR, 4, tmpMatrix, 1); //tmpMatrix = {A, |R, G, B, A|, ...}
	}
	
	protected void setClip(Geometry s, boolean direct)
	{
		//TODO
	}
	
	public final void setColor(int c)
	{
		if(this.error == VG10.VG_NO_ERROR)
		{
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
				this.destination.vgSetColor(this.fillPaint, c);
			}
		}
	}
	
	public void setComposite(Composite comp)
	{
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
			this.blendAlpha = GfxUtil.compositeSrcAlpha(comp); //XXX See if there is a way to have VG handle this instead of making it a separate value... See if this can even be used.
		}
		else
		{
			//TODO: Not sure what to do here just yet
		}
	}
	
	public void setPaint(Paint paint)
	{
		if(GfxUtil.isPaintInternal(paint))
		{
			setColor(paint.getColor()); //Internal Paint object is always a solid color
		}
		else
		{
			//TODO: paint needs to be a pattern
		}
	}
	
	public void setRenderingHint(int hintKey, int hintValue)
	{
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
	}
	
	public final void setStroke(BasicStroke s)
	{
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
	}
	
	protected void setTransform(AffineTransform Tx, boolean direct)
	{
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
			//XXX This might not actually be needed
			this.destination.vgSeti(VG10.VG_MATRIX_MODE, VG10.VG_MATRIX_STROKE_PAINT_TO_USER); //Stroke paint
			if(Tx == null)
			{
				this.destination.vgLoadIdentity();
			}
			else
			{
				//Don't need to worry about getting the matrix data or creating the array, done already in previous else statement
				this.destination.vgLoadMatrix(tmpMatrix, 0);
			}
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
	}
	
	public final void translate(int x, int y)
	{
		this.destination.vgTranslate(x, y);
	}
}

//#endif
