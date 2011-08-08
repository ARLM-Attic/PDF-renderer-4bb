//#preprocessor

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0

/*
 * File: GraphicsImplOpenVG.java
 * Version: 1.0
 * Initial Creation: Aug 7, 2011 7:52:43 PM
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
package com.sun.pdfview.helper.graphics.drawing.net.rim.device.api.ui.GraphicsInternalGraphics;

import net.rim.device.api.openvg.VG;
import net.rim.device.api.openvg.VGUtils;
import net.rim.device.api.system.Bitmap;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFGraphics;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.Paint;

/**
 * PDFgraphics implementation of Graphics, this is the native Graphics system only.
 */
final class GraphicsImplOpenVG extends PDFGraphics implements GraphicsImpl.InnerAccess
{
	public static PDFGraphics tryAndGet()
	{
		if(VGUtils.isSupported())
		{
			//TODO: Do a better check to make sure that OpenVG is actually supported and would work.
			return new GraphicsImplOpenVG();
		}
		return null;
	}
	
	protected void onFinished()
	{
		//TODO: dispose the "actual" PDFGraphics, then clean up the VG graphics
	}
	
	public void setDrawingDeviceIn(Object device)
	{
		setDrawingDevice(device);
	}
	
	protected void setDrawingDevice(Object device)
	{
		if(device == null)
		{
			throw new NullPointerException();
		}
		//TODO
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
	
	public void setClipIn(Geometry s, boolean direct)
	{
		setClip(s, direct);
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
	
	public void setTransformIn(AffineTransform Tx, boolean direct)
	{
		setTransform(Tx, direct);
	}
	
	protected void setTransform(AffineTransform Tx, boolean direct)
	{
		//TODO
	}
	
	public void translate(int x, int y)
	{
		//TODO
	}
}

//#endif
