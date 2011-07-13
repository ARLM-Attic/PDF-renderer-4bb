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
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.XYRect;

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
	private interface RenderEx
	{
		public void render(XYRect bounds);
	}
	
	private AffineTransform trans;
	private BasicStroke stroke;
	private Paint background, foreground;
	private Geometry clip;
	private int antiA, interp, alpha;
	private Composite comp;
	
	private Graphics destination;
	
	private Bitmap tmpBmp;
	private Graphics tmpG;
	
	private XYRect clipBounds;
	private Bitmap clipBmp;
	
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
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
			tmpBmp = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, bw, bh);
			tmpBmp.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP); //Make alpha
//#else
			tmpBmp = new Bitmap(Bitmap.ROWWISE_32BIT_ARGB8888, bw, bh); //Make with alpha
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1
			tmpG = Graphics.create(tmpBmp);
//#else
			tmpG = new Graphics(tmpBmp);
//#endif
			tmpG.setBackgroundColor(Color.BLACK);
			tmpG.setColor(Color.WHITE);
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
		//Draw uses Stroke
		Geometry sg = stroke.createStrokedGeometry(s);
		Geometry.Enumeration en = sg.getPathEnumerator(this.trans);
		float[] coords = new float[6];
		//float sx = 0;
		//float sy = 0;
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
	
	public boolean drawImage(Bitmap img, AffineTransform xform)
	{
		if(img != null)
		{
			//Create "final" transform
			xform = new AffineTransform(this.trans);
			xform.concatenate(xform);
			//TODO: Transform image
			if(this.clip != null)
			{
				//TODO: Clip image (look at renderGraphics)
			}
			//TODO: Composite the image
		}
		return true;
	}
	
	public void fill(Geometry s)
	{
		//Fill doesn't seem to use Stroke
		Geometry.Enumeration en = s.getPathEnumerator(this.trans);
		
		//XXX Temp
		XYRect bounds = new XYRect();
		final Object[] path = generatePath(en, bounds); //Generate the path to fill
		if(path != null)
		{
			final RenderEx render = new RenderEx()
			{
				public void render(XYRect bounds)
				{
					//Draw path
					tmpG.translate(-bounds.x, -bounds.y);
					tmpG.drawFilledPath((int[])path[0], (int[])path[1], (byte[])path[2], (int[])path[3]);
					tmpG.translate(bounds.x, bounds.y);
				}
			};
			renderGraphics(bounds, render);
		}
	}
	
	private static Object[] generatePath(Geometry.Enumeration en, XYRect rect)
	{
		int[] xP = null;
		int[] yP = null;
		byte[] types = null;
		int[] offsets = null;
		
		int xs, ys, ts, os;
		xs = ys = ts = os = 0;
		
		int fx = 0;
		int fy = 0;
		int tx, ty;
		boolean moveCalled = false;
		
		float[] coords = new float[6];
		while(!en.isDone())
		{
			switch(en.currentSegment(coords))
			{
				case Geometry.Enumeration.SEG_MOVETO:
					if(moveCalled)
					{
						return null;
					}
					moveCalled = true;
					fx = rect.width = rect.x = (int)Math.floor(coords[0]);
					fy = rect.height = rect.y = (int)Math.floor(coords[1]);
					xP = ensureSize(xP, ++xs);
					yP = ensureSize(yP, ++ys);
					xP[xs - 1] = fx;
					yP[ys - 1] = fy;
					break;
				case Geometry.Enumeration.SEG_LINETO:
					tx = (int)Math.floor(coords[0]);
					ty = (int)Math.floor(coords[1]);
					if(tx < rect.x)
					{
						rect.x = tx;
					}
					else if(tx > rect.width)
					{
						rect.width = tx;
					}
					if(ty < rect.y)
					{
						rect.y = ty;
					}
					else if(ty > rect.height)
					{
						rect.height = ty;
					}
					xP = ensureSize(xP, ++xs);
					yP = ensureSize(yP, ++ys);
					xP[xs - 1] = tx;
					yP[ys - 1] = ty;
					break;
				case Geometry.Enumeration.SEG_CLOSE:
					xP = ensureSize(xP, ++xs);
					yP = ensureSize(yP, ++ys);
					xP[xs - 1] = fx;
					yP[ys - 1] = fy;
					break;
				//TODO
			}
			en.next();
		}
		rect.width = Math.abs(rect.width - rect.x);
		rect.height = Math.abs(rect.height - rect.y);
		
		if(rect.width == 0 || rect.height == 0)
		{
			return null;
		}
		return new Object[]{xP, yP, types, offsets};
	}
	
	private static int[] ensureSize(int[] dat, int size)
	{
		if(dat == null)
		{
			return new int[size];
		}
		if(dat.length < size)
		{
			int[] n = new int[size];
			System.arraycopy(dat, 0, n, 0, dat.length);
			return n;
		}
		return dat;
	}
	
	private void renderGraphics(XYRect bounds, RenderEx render)
	{
		//Generate mask graphics to use for rendering
		createMaskGraphics(bounds.width, bounds.height);
		
		//Generate drawing surface
		PaintGenerator gen = this.foreground.createGenerator(this.trans);
		TranslatedBitmap btm = gen.getBitmap(bounds.x, bounds.y, bounds.width, bounds.height);
		gen.dispose();
		
		//Perform render operation to create mask
		render.render(bounds);
		
		//Mask drawing surface to get result
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1
		Graphics tg = Graphics.create(btm.getBitmap());
//#else
		Graphics tg = new Graphics(btm.getBitmap());
//#endif
		if(this.clip != null)
		{
			tg.rop(Graphics.ROP2_DSa, this.clipBounds.x, this.clipBounds.y, this.clipBounds.width, this.clipBounds.height, this.clipBmp, 0, 0); //Apply clip
		}
		tg.rop(Graphics.ROP2_DSa, 0, 0, bounds.width, bounds.height, this.tmpBmp, 0, 0);
		
		//Push the drawing context that will be where the render is actually shown
		this.destination.pushContext(bounds.x, bounds.y, bounds.width, bounds.height, 0, 0);
		
		//Draw masked surface
		this.destination.drawBitmap(bounds, btm.getBitmap(), 0, 0);
		
		//Pop drawing context
		this.destination.popContext();
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
	
	protected void setClip(Geometry s, boolean direct)
	{
		if(s == null)
		{
			this.clip = null;
			this.clipBmp = null;
			this.clipBounds = null;
		}
		else
		{
			s = s.createTransformedShape(this.trans);
			if(direct || this.clip == null)
			{
				this.clip = s;
			}
			else
			{
				this.clip.append(s, false);
			}
			
			if(this.clipBounds == null)
			{
				this.clipBounds = new XYRect();
			}
			
			//This isn't efficient but since the clip can completely change, it's not a major concern
			
			//Save graphics state
			AffineTransform tmpTrans = this.trans;
			this.trans = null;
			Paint tmpForeground = this.foreground;
			setColor(Color.WHITE);
			Bitmap tmpTmpBmp = this.tmpBmp;
			this.tmpBmp = null;
			Graphics tmpTmpG = this.tmpG;
			this.tmpG = null;
			Graphics tmpDestination = this.destination;
			
			//Generate bounds
			com.sun.pdfview.helper.XYRectFloat b = this.clip.getBounds2D();
			this.clipBounds.x = (int)Math.floor(b.x);
			this.clipBounds.y = (int)Math.floor(b.y);
			this.clipBounds.width = (int)Math.floor(b.width);
			this.clipBounds.height = (int)Math.floor(b.height);
			
			//Generate destination
			createMaskGraphics(this.clipBounds.width, this.clipBounds.height);
			this.destination = this.tmpG;
			this.tmpG = null;
			this.clipBmp = this.tmpBmp;
			this.tmpBmp = null;
			
			//Draw clip
			s = this.clip;
			this.clip = null;
			fill(s);
			this.clip = s;
			
			//Restore graphics state
			this.foreground = tmpForeground;
			this.trans = tmpTrans;
			this.tmpBmp = tmpTmpBmp;
			this.tmpG = tmpTmpG;
			this.destination = tmpDestination;
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
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_AALINES, true);
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_AAPOLYGONS, true);
//#else
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_ANTIALIASED, true);
//#endif
						break;
					case PDFGraphics.VALUE_ANTIALIAS_OFF:
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_AALINES, false);
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_AAPOLYGONS, false);
//#else
						this.destination.setDrawingStyle(Graphics.DRAWSTYLE_ANTIALIASED, false);
//#endif
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
				//TODO: Add support for interpolation
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
				//TODO: Add support for alpha interpolation
				this.alpha = hintValue;
				break;
		}
	}
	
	public void setStroke(BasicStroke s)
	{
		if(s == null)
		{
			throw new NullPointerException();
		}
		this.stroke = s;
	}
	
	protected void setTransform(AffineTransform Tx, boolean direct)
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
