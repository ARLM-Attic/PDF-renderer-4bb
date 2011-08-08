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
import com.sun.pdfview.helper.graphics.Paint;

/**
 * PDFgraphics implementation of VG10.
 */
public class GraphicsImpl extends PDFGraphics
{
	protected VG10 destination;
	
	private float[] tmpMatrix;
	
	protected void onFinished()
	{
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
	
	public void clear(int x, int y, int width, int height)
	{
		//TODO
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
	
	public AffineTransform getTransform()
	{
		//TODO
		return null;
	}
	
	public void setBackgroundColor(int c)
	{
		//TODO
	}
	
	protected void setClip(Geometry s, boolean direct)
	{
		//TODO
	}
	
	public void setColor(int c)
	{
		//TODO
	}
	
	public void setComposite(Composite comp)
	{
		//TODO
	}
	
	public void setPaint(Paint paint)
	{
		//TODO
	}
	
	public void setRenderingHint(int hintKey, int hintValue)
	{
		//TODO
	}
	
	public void setStroke(BasicStroke s)
	{
		//TODO
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
				//Don't need to work about getting the matrix data or creating the array, done already in previous else statement
				this.destination.vgLoadMatrix(tmpMatrix, 0);
			}
			//Ignore Stroke (VG_MATRIX_STROKE_PAINT_TO_USER if needed later)
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
	
	public void translate(int x, int y)
	{
		this.destination.vgTranslate(x, y);
	}
}

//#endif
