/*
 * File: PageField.java
 * Version: 1.0
 * Initial Creation: Jun 5, 2010 4:24:24 PM
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
package com.sun.pdfview;

import net.rim.device.api.math.Fixed32;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.system.PNGEncodedImage;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.TouchEvent;
import net.rim.device.api.ui.XYRect;

/**
 * Represents a page from a PDF.
 */
public class PageField extends Field
{
	private static final int BORDER = 3;
	private static final int PG_BORDER = ThumbnailPopup.THUMB_BUFFER;
	
	private int num, maxWidth, maxHeight;
	
	private Bitmap image;
	private PageTracker tracker;
	
	public PageField(PageTracker tracker)
	{
		this(null, tracker);
	}
	
	public PageField(Bitmap img, PageTracker tracker)
	{
		this(img, Display.getWidth(), Display.getHeight(), tracker);
	}
	
	public PageField(Bitmap img, int maxWidth, int maxHeight, PageTracker tracker)
	{
		super(Field.FOCUSABLE);
		
		this.tracker = tracker;
		this.num = tracker.getNext();
		
		if(maxWidth <= 0)
		{
			maxWidth = Display.getWidth();
		}
		if(maxHeight <= 0)
		{
			maxHeight = Display.getHeight();
		}
		
		setPage(img);
		setMax(maxWidth, maxHeight, false);
		
		//Top, right, bottom, left
		setMargin((PG_BORDER * 2), BORDER, (PG_BORDER * 2) + BORDER, 0);
	}
	
	public void setPage(Bitmap img)
	{
		if(img != null)
		{
			if(img.getWidth() > maxWidth || img.getHeight() > maxHeight)
			{
				//Need to resize the image. Just so this can will work on 4.6 and greater use EncodedImage.
				EncodedImage eimg = PNGEncodedImage.encode(img); //PNG images are looseless.
				int width = img.getWidth();
				int height = img.getHeight();
				
				//First figure out which value is greater over the MAX_* so that it can be scaled and keep the aspect ratio
				int max = Math.max(width -  maxWidth, height - maxHeight);
				int scale;
				if(max == (width -  maxWidth))
				{
					//Width is greater
					scale = Fixed32.toFP(width) / maxWidth;
				}
				else
				{
					//Height is greater
					scale = Fixed32.toFP(height) / maxHeight;
				}
				
				eimg.scaleImage32(scale, scale);
				img = eimg.getBitmap();
			}
		}
		this.image = img;
		invalidate();
	}
	
	/**
	 * Requires event lock
	 */
	public void setMax(int width, int height)
	{
		setMax(width, height, true);
	}
	
	private void setMax(int width, int height, boolean updateLayout)
	{
		this.maxWidth = width;
		this.maxHeight = height;
		if(this.image != null && (this.image.getWidth() > this.maxWidth || this.image.getHeight() > this.maxHeight))
		{
			setPage(this.image);
		}
		if(updateLayout)
		{
			this.updateLayout();
		}
	}
	
	public int getMaxWidth()
	{
		return this.maxWidth;
	}
	
	public int getMaxHeight()
	{
		return this.maxHeight;
	}
	
	public Bitmap getPage()
	{
		return this.image;
	}
	
	public int getPreferredHeight()
	{
		return this.image == null ? maxWidth : this.image.getHeight();
	}
	
	public int getPreferredWidth()
	{
		return this.image == null ? maxWidth : this.image.getWidth();
	}
	
	protected void layout(int width, int height)
	{
		setExtent(Math.min(width, getPreferredWidth()), Math.min(height, getPreferredHeight()));
	}
	
	protected boolean touchEvent(TouchEvent message)
	{
		switch(message.getEvent())
		{
			case TouchEvent.CLICK:
				synchronized(tracker)
				{
					tracker.click(this.num);
				}
				return true;
		}
		return super.touchEvent(message);
	}
	
	protected void paint(Graphics graphics)
	{
		XYRect clip = getContentRect();
		clip.width -= BORDER;
		clip.height -= BORDER;
		
		int prevCol = graphics.getColor();
		int prevAlpha = graphics.getGlobalAlpha();
		
		if(this.image != null)
		{
			//Draw shadow
			graphics.setColor(Color.WHITESMOKE);
			graphics.setGlobalAlpha(60);
			graphics.fillRect(BORDER, BORDER, clip.width, clip.height);
			
			//Draw image
			graphics.setGlobalAlpha(255);
			graphics.drawBitmap(0, 0, clip.width, clip.height, this.image, 0, 0);
			
			if(tracker.getSelected() == num)
			{
				//Draw focus
				graphics.setColor(Color.RED);
				graphics.setGlobalAlpha(140);
				graphics.fillRect(0, 0, clip.width, BORDER);
				graphics.fillRect(clip.width - BORDER, BORDER, BORDER, clip.height - BORDER);
				graphics.fillRect(0, clip.height - BORDER, clip.width - BORDER, BORDER);
				graphics.fillRect(0, BORDER, BORDER, clip.height - (BORDER * 2));
			}
		}
		else
		{
			graphics.setColor(Color.GRAY);
			graphics.fillRect(0, 0, clip.width, clip.height);
		}
		
		//Reset values
		graphics.setGlobalAlpha(prevAlpha);
		graphics.setColor(prevCol);
	}
	
	protected void drawFocus(Graphics graphics, boolean on)
	{
		//Don't want to draw focus
	}
}
