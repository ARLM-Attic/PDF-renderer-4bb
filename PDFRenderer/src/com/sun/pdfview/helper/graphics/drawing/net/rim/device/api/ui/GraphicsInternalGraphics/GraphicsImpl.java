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
	private Geometry clip;
	private int antiA, interp, alpha;
	private Composite comp;
	
	private Graphics destination;
	
	private Bitmap tmpBmp;
	private Graphics tmpG;
	
	public GraphicsImpl()
	{
		trans = new AffineTransform();
		stroke = new BasicStroke();
		background = Paint.getInstance(Color.WHITE);
		foreground = Paint.getInstance(Color.BLACK);
		comp = Composite.getInstance(Composite.SRC);
	}
	
	protected void setDrawingDevice(Object device)
	{
		if(device == null)
		{
			throw new NullPointerException();
		}
		this.destination = (Graphics)device;
		
		setRenderingHint(PDFGraphics.KEY_ANTIALIASING, PDFGraphics.VALUE_ANTIALIAS_DEFAULT);
		setRenderingHint(PDFGraphics.KEY_INTERPOLATION, PDFGraphics.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		setRenderingHint(PDFGraphics.KEY_ALPHA_INTERPOLATION, PDFGraphics.VALUE_ALPHA_INTERPOLATION_DEFAULT);
	}
	
	private void createMaskGraphics(int w, int h)
	{
		int ow = 0;
		int oh = 0;
		int bw = 0;
		int bh = 0;
		if(tmpBmp != null)
		{
			ow = bw = tmpBmp.getWidth();
			oh = bh = tmpBmp.getHeight();
		}
		bw = Math.max(bw, w);
		bh = Math.max(bh, h);
		if(bw != ow || bh != oh)
		{
			tmpBmp = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, bw, bh);
			tmpBmp.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP); //Make alpha
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1
			tmpG = Graphics.create(tmpBmp);
//#else
			tmpG = new Graphics(tmpBmp);
//#endif
			tmpG.setBackgroundColor(Color.BLACK);
			tmpG.setColor(Color.BLACK);
		}
		if(tmpG != null)
		{
			//Clear the image
			tmpG.setGlobalAlpha(0);
			tmpG.clear();
			tmpG.setGlobalAlpha(255);
		}
	}
	
	public void clear(int x, int y, int width, int height)
	{
		PaintGenerator gen = background.createGenerator(this.trans);
		TranslatedBitmap btm = gen.getBitmap(x, y, width, height);
		gen.dispose();
		destination.drawBitmap(x, y, width, height, btm.getBitmap(), 0, 0);
	}
	
	public void draw(Geometry s)
	{
		Geometry sg = stroke.createStrokedGeometry(s);
		Geometry.Enumeration en = sg.getPathEnumerator(this.trans);
		float[] coords = new float[6];
		float sx = 0;
		float sy = 0;
		while(!en.isDone())
		{
			switch(en.currentSegment(coords))
			{
			/*
				case Geometry.Enumeration.SEG_MOVETO:
					sx = coords[0];
					sy = coords[1];
					break;
				case Geometry.Enumeration.SEG_CLOSE:
				case Geometry.Enumeration.SEG_LINETO:
					drawLine(sx, sy, sx = coords[0], sy = coords[1]);
					break;
				case Geometry.Enumeration.SEG_CUBICTO:
				case Geometry.Enumeration.SEG_QUADTO:
					//TODO
					break;
			*/
			}
			en.next();
		}
	}
	
	/*
	private void drawLine(float sx, float sy, float ex, float ey)
	{
		//Calculate size and "adjusted" line start and end
		int[] xP = new int[4];
		int[] yP = new int[4];
		int width = 0;
		int height = 0;
		int x = 0;
		int y = 0;
		//TODO: Calculate line "rectangle" and determine width, height, and the origin point for it all
		createMaskGraphics(width, height);
		
		//Push the drawing context that will be where the line is actually shown
		this.destination.pushContext(x, y, width, height, 0, 0); //TODO: Need to figure out how to handle Geometry "clip"
		
		//Draw line
		tmpG.translate(-x, -y);
		tmpG.drawFilledPath(xP, yP, null, null); //It doesn't matter here but other path drawing need to take into account that drawFilledPath uses a even-odd drawing rule.
		tmpG.translate(x, y);
		
		//Generate drawing surface, mask surface to get result
		PaintGenerator gen = foreground.createGenerator(this.trans);
		TranslatedBitmap btm = gen.getBitmap(x, y, width, height);
		gen.dispose();
		Graphics tg;
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1
		tg = Graphics.create(btm.getBitmap());
//#else
		tg = new Graphics(btm.getBitmap());
//#endif
		tg.rop(Graphics.ROP2_DSa, x, y, width, height, tmpBmp, 0, 0);
		
		//Draw masked surface
		this.destination.drawBitmap(x, y, width, height, btm.getBitmap(), 0, 0);
		
		//Pop drawing context
		this.destination.popContext();
	}
	*/
	
	public boolean drawImage(Bitmap img, AffineTransform xform)
	{
		//TODO
		return true;
	}
	
	public void fill(Geometry s)
	{
		Geometry sg = stroke.createStrokedGeometry(s);
		Geometry.Enumeration en = sg.getPathEnumerator(this.trans);
		float[] coords = new float[6];
		while(!en.isDone())
		{
			switch(en.currentSegment(coords))
			{
			//TODO
			}
			en.next();
		}
	}
	
	public Geometry getClip()
	{
		return this.clip;
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
		if(s == null)
		{
			this.clip = null;
		}
		else if(direct)
		{
			this.clip = s;
		}
		else
		{
			//TODO
		}
	}
	
	public void setColor(int c)
	{
		setPaint(Paint.getInstance(c));
	}
	
	public void setComposite(Composite comp)
	{
		this.comp = comp;
	}
	
	public void setPaint(Paint paint)
	{
		if(paint != null)
		{
			this.foreground = paint;
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
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_AALINES, true);
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_AAPOLYGONS, true);
						break;
					case PDFGraphics.VALUE_ANTIALIAS_OFF:
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_AALINES, false);
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_AAPOLYGONS, false);
						break;
					default:
						return;
				}
				this.antiA = hintValue;
				break;
			case PDFGraphics.KEY_INTERPOLATION:
				switch(hintValue)
				{
					case PDFGraphics.VALUE_INTERPOLATION_BICUBIC:
					case PDFGraphics.VALUE_INTERPOLATION_BILINEAR:
					case PDFGraphics.VALUE_INTERPOLATION_NEAREST_NEIGHBOR:
						break;
					default:
						return;
				}
				//TODO
				this.interp = hintValue;
				break;
			case PDFGraphics.KEY_ALPHA_INTERPOLATION:
				switch(hintValue)
				{
					case PDFGraphics.VALUE_ALPHA_INTERPOLATION_QUALITY:
					case PDFGraphics.VALUE_ALPHA_INTERPOLATION_SPEED:
						break;
					default:
						return;
				}
				//TODO
				this.alpha = hintValue;
				break;
		}
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
