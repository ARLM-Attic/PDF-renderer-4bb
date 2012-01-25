//#preprocessor

/*
 * File: GraphicsImpl.java
 * Version: 1.0
 * Initial Creation: Jun 28, 2010 11:22:29 AM
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

import net.rim.device.api.system.Bitmap;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFGraphics;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.Paint;

/**
 * PDFgraphics implementation of Graphics. This is simply a header for multiple graphics systems which can be OS specific, device specific, or just backups in case of failures.
 */
public class GraphicsImpl extends PDFGraphics
{
	//Leave as a "internal" type so that only sub-PDFGraphics can be used/uses it.
	interface InnerAccess
	{
		public void setDrawingDeviceIn(Object device);
		
		public void setClipIn(Geometry s, boolean direct);
		
		public void setTransformIn(AffineTransform Tx, boolean direct);
	}
	
	private static boolean[] allowAccess;
	
	static
	{
		//TODO
	}
	
	private PDFGraphics subGraphics;
	
	public GraphicsImpl()
	{
		//Determine the sub-GraphicsImpl to use.
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0
		//OpenVG
		this.subGraphics = GraphicsImplOpenVG.tryAndGet();
//#endif
//#ifndef BlackBerrySDK4.5.0
		//SVG
		if(this.subGraphics == null)
		{
			//TODO: Try to create GraphicsImplSVG
		}
//#endif
		
		//Last case Graphics use
		if(this.subGraphics == null)
		{
			this.subGraphics = new GraphicsImplNative();
		}
	}
	
	public boolean hasExtraProperties()
	{
		//TODO
		return false;
	}
	
	public boolean isValid()
	{
		//TODO
		return true;
	}
	
	public boolean setProperty(String propertyName, Object value)
	{
		//TODO
		return false;
	}
	
	public Object getProperty(String propertyName)
	{
		//TODO
		return null;
	}
	
	public String[] getSupportedProperties()
	{
		//TODO
		return null;
	}
	
	//Some sub-graphics types need to be cleaned up, this facilitates that necessary cleanup.
	protected void onFinished()
	{
		PDFGraphics.finishGraphics(this.subGraphics); //The PDFGraphics was not made with the PDFGraphics function but will still perform the proper operations on it.
	}
	
	protected void setDrawingDevice(Object device)
	{
		((InnerAccess)this.subGraphics).setDrawingDeviceIn(device);
	}
	
	public void clear(int x, int y, int width, int height)
	{
		this.subGraphics.clear(x, y, width, height);
	}
	
	public void draw(Geometry s)
	{
		this.subGraphics.draw(s);
	}
	
	public boolean drawImage(Bitmap img, AffineTransform xform)
	{
		return this.subGraphics.drawImage(img, xform);
	}
	
	public void fill(Geometry s)
	{
		this.subGraphics.fill(s);
	}
	
	public Geometry getClip()
	{
		return this.subGraphics.getClip();
	}
	
	public AffineTransform getTransform()
	{
		return this.subGraphics.getTransform();
	}
	
	public void setBackgroundColor(int c)
	{
		this.subGraphics.setBackgroundColor(c);
	}
	
	protected void setClip(Geometry s, boolean direct)
	{
		((InnerAccess)this.subGraphics).setClipIn(s, direct);
	}
	
	public void setColor(int c)
	{
		this.subGraphics.setColor(c);
	}
	
	public void setComposite(Composite comp)
	{
		this.subGraphics.setComposite(comp);
	}
	
	public void setPaint(Paint paint)
	{
		this.subGraphics.setPaint(paint);
	}
	
	public void setRenderingHint(int hintKey, int hintValue)
	{
		this.subGraphics.setRenderingHint(hintKey, hintValue);
	}
	
	public void setStroke(BasicStroke s)
	{
		this.subGraphics.setStroke(s);
	}
	
	protected void setTransform(AffineTransform Tx, boolean direct)
	{
		((InnerAccess)this.subGraphics).setTransformIn(Tx, direct);
	}
	
	public void translate(int x, int y)
	{
		this.subGraphics.translate(x, y);
	}
}
