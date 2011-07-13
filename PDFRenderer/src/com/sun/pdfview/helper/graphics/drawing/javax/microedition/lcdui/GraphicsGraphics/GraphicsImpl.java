//#preprocessor

/*
 * File: GraphicsImpl.java
 * Version: 1.0
 * Initial Creation: Jun 26, 2011 9:34:22 AM
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
package com.sun.pdfview.helper.graphics.drawing.javax.microedition.lcdui.GraphicsGraphics;

import javax.microedition.lcdui.Graphics;

import net.rim.device.api.system.Bitmap;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFGraphics;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.Paint;

/**
 * PDFgraphics implementation of Graphics.
 */
public final class GraphicsImpl extends PDFGraphics
{
	private PDFGraphics gDest;
	private net.rim.device.api.ui.Graphics g;
	private Bitmap bmp;
	private int[] buffer;
	private int width, height;
	
	private Graphics destination;
	
	protected void onFinished()
	{
		PDFGraphics.finishGraphics(gDest);
	}
	
	protected void setDrawingDevice(Object device)
	{
		if(device == null)
		{
			throw new NullPointerException();
		}
		this.destination = (Graphics)device;
		
		this.width = this.destination.getClipWidth();
		this.height = this.destination.getClipHeight();
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
		this.bmp = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, this.width, this.height);
		this.bmp.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
//#else
		this.bmp = new Bitmap(Bitmap.ROWWISE_32BIT_ARGB8888, this.width, this.height); //Make with alpha
//#endif
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1
		this.g = new net.rim.device.api.ui.Graphics(this.bmp);
//#else
		this.g = net.rim.device.api.ui.Graphics.create(this.bmp);
//#endif
		this.buffer = new int[this.width * this.height];
		
		this.gDest = PDFGraphics.createGraphics(this.g);
	}
	
	private void commit()
	{
		//A more efficient way to do this would be to just copy over the differences. But in order to do that requires 5.0 and higher's locateDifference function. 
		//In order to use that we need 2 Bitmaps, which would need copying to do a comparison. We might as well have a whole bitmap copy over.
		
		this.bmp.getARGB(this.buffer, 0, this.width, 0, 0, this.width, this.height);
		this.destination.drawRGB(this.buffer, 0, this.width, 0, 0, this.width, this.height, false); //The reason for not processing alpha is that we already have the final graphics, so simply write that.
	}
	
	public void clear(int x, int y, int width, int height)
	{
		gDest.clear(x, y, width, height);
		commit();
	}
	
	public void draw(Geometry s)
	{
		gDest.draw(s);
		commit();
	}
	
	public boolean drawImage(Bitmap img, AffineTransform xform)
	{
		boolean result = gDest.drawImage(img, xform);
		if(result)
		{
			commit();
		}
		return result;
	}
	
	public void fill(Geometry s)
	{
		gDest.fill(s);
		commit();
	}
	
	public Geometry getClip()
	{
		return gDest.getClip();
	}
	
	public AffineTransform getTransform()
	{
		return gDest.getTransform();
	}
	
	public void setBackgroundColor(int c)
	{
		gDest.setBackgroundColor(c);
	}
	
	protected void setClip(Geometry s, boolean direct)
	{
		if(direct)
		{
			gDest.setClip(s);
		}
		else
		{
			gDest.clip(s);
		}
	}
	
	public void setColor(int c)
	{
		gDest.setColor(c);
	}
	
	public void setComposite(Composite comp)
	{
		gDest.setComposite(comp);
	}
	
	public void setPaint(Paint paint)
	{
		gDest.setPaint(paint);
	}
	
	public void setRenderingHint(int hintKey, int hintValue)
	{
		gDest.setRenderingHint(hintKey, hintValue);
	}
	
	public void setStroke(BasicStroke s)
	{
		gDest.setStroke(s);
	}
	
	protected void setTransform(AffineTransform Tx, boolean direct)
	{
		if(direct)
		{
			gDest.setTransform(Tx);
		}
		else
		{
			gDest.transform(Tx);
		}
	}
	
	public void translate(int x, int y)
	{
		gDest.translate(x, y);
	}
}
