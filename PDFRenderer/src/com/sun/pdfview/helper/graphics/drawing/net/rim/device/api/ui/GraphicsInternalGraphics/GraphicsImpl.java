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
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Graphics;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFGraphics;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.Paint;
import com.sun.pdfview.helper.graphics.PaintGenerator;
import com.sun.pdfview.helper.graphics.TranslatedBitmap;

/**
 * PDFgraphics implementation of Graphics.
 */
public class GraphicsImpl extends PDFGraphics
{
	private AffineTransform trans;
	private BasicStroke stroke;
	private Paint background, foreground;
	
	private Graphics destination;
	
	public GraphicsImpl()
	{
		trans = new AffineTransform();
		stroke = new BasicStroke();
		background = Paint.getInstance(Color.WHITE);
		foreground = Paint.getInstance(Color.BLACK);
	}
	
	protected void setDrawingDevice(Object device)
	{
		if(device == null)
		{
			throw new NullPointerException();
		}
		destination = (Graphics)device;
	}
	
	public void clear(int x, int y, int width, int height)
	{
		PaintGenerator gen = background.createGenerator(null);
		TranslatedBitmap btm = gen.getBitmap(x, y, width, height);
		destination.drawBitmap(x, y, width, height, btm.getBitmap(), 0, 0);
	}
	
	public void draw(Geometry s)
	{
		//TODO
	}
	
	public boolean drawImage(Bitmap img, AffineTransform xform)
	{
		//TODO
		return true;
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
		return this.trans;
	}
	
	public void setBackgroundColor(int c)
	{
		this.background = Paint.getInstance(c);
	}
	
	public void setClip(Geometry s, boolean direct)
	{
		//TODO
	}
	
	public void setColor(int c)
	{
		setPaint(Paint.getInstance(c));
	}
	
	public void setComposite(Composite comp)
	{
		//TODO
	}
	
	public void setPaint(Paint paint)
	{
		this.foreground = paint;
		if(this.foreground == null)
		{
			this.foreground = Paint.getInstance(Color.BLACK);
		}
	}
	
	public void setRenderingHint(int hintKey, int hintValue)
	{
		//TODO
	}
	
	public void setStroke(BasicStroke s)
	{
		this.stroke = s;
		if(this.stroke == null)
		{
			this.stroke = new BasicStroke();
		}
	}
	
	public void setTransform(AffineTransform Tx, boolean direct)
	{
		if(Tx == null)
		{
			Tx = new AffineTransform();
		}
		if(direct)
		{
			this.trans = Tx;
		}
		else
		{
			this.trans.concatenate(Tx);
		}
	}
	
	public void translate(int x, int y)
	{
		this.trans.translate(x, y);
	}
}
