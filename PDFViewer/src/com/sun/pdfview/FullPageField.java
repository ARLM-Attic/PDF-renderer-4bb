/*
 * File: FullPageField.java
 * Version: 1.0
 * Initial Creation: Jun 16, 2010 1:14:08 PM
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

import com.sun.pdfview.ui.GestureField;

import net.rim.device.api.math.Fixed32;
import net.rim.device.api.math.VecMath;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.XYRect;

/**
 * Full screen view page viewer.
 */
public class FullPageField extends GestureField
{
	private static final int SHADOW_OFFSET = 5;
	
	private static final int MATRIX_SIZE = 9;
	private static final int DRAWING_MATRIX = 0;
	private static final int WORKING_MATRIX = DRAWING_MATRIX + MATRIX_SIZE;
	private int[] mat;
	
	private PDFPage page;
	private int curRenderWidth, curRenderHeight;
	private Bitmap drawnPage;
	private boolean needRerender;
	
	private int sW, sH;
	
	public FullPageField()
	{
		super.gestureProcessing = false; //Disable by default
		
		this.mat = new int[MATRIX_SIZE * 2]; //Two matrixes
		GestureField.matrixSetIdentity(mat, DRAWING_MATRIX);
	}
	
	protected void layout(int width, int height)
	{
		setExtent(sW = Math.min(width, Display.getWidth()), sH = Math.min(height, Display.getHeight()));
		sW = Fixed32.toFP(sW);
		sH = Fixed32.toFP(sH);
	}
	
	protected void paint(Graphics graphics)
	{
		int pcolor = graphics.getColor();
		int palpha = graphics.getGlobalAlpha();
		
		XYRect extent = this.getExtent();
		if(super.gestureProcessing) //Simple way to check if a PDF is in use
		{
			//Clear the background
			graphics.setColor(Color.GRAY);
			graphics.fillRect(0, 0, extent.width, extent.height);
			
			//Create the path
			int[] xPts = new int[]{0, 0, this.curRenderWidth, this.curRenderWidth};
			int[] yPts = new int[]{0, this.curRenderHeight, this.curRenderHeight, 0};
			
			//Transform points
			VecMath.transformPoints(mat, DRAWING_MATRIX, xPts, yPts, xPts, yPts);
			
			//Draw "shadow"
			graphics.setColor(Color.BLACK);
			graphics.setGlobalAlpha(75);
			graphics.translate(SHADOW_OFFSET, SHADOW_OFFSET); //Offset the shadow a bit
			graphics.drawFilledPath(xPts, yPts, null, null);
			graphics.translate(-SHADOW_OFFSET, -SHADOW_OFFSET);
			graphics.setGlobalAlpha(255);
			
			//Get UV points for texture
			int widthScale = Fixed32.div(Fixed32.toFP(this.curRenderWidth), Fixed32.toFP(extent.width));
			int heightScale = Fixed32.div(Fixed32.toFP(this.curRenderHeight), Fixed32.toFP(extent.height));
			int dux = Fixed32.div(this.mat[DRAWING_MATRIX], widthScale);
			int dvx = Fixed32.div(this.mat[DRAWING_MATRIX + 1], widthScale);
			int duy = Fixed32.div(this.mat[DRAWING_MATRIX + 3], heightScale);
			int dvy = Fixed32.div(this.mat[DRAWING_MATRIX + 4], heightScale);
			
			//Draw
			graphics.drawTexturedPath(xPts, yPts, null, null, 0, 0, dux, dvx, duy, dvy, this.drawnPage);
		}
		else
		{
			//No PDF to draw
			String text = "No page selected";
			
			graphics.setColor(Color.BLACK);
			graphics.drawText(text, 0, (extent.height >> 1) - (graphics.getFont().getHeight() >> 1), Graphics.HCENTER, extent.width);
		}
		
		graphics.setGlobalAlpha(palpha);
		graphics.setColor(pcolor);
	}
	
	protected boolean interactionMove(int x, int y)
	{
		GestureField.matrixSetTranslate(mat, WORKING_MATRIX, x, y);
		return applyMatrix(true);
	}
	
	protected boolean interactionScale(int x, int y)
	{
		needRerender = true;
		GestureField.matrixSetScale(mat, WORKING_MATRIX, x, y);
		sW = Fixed32.mul(sW, x);
		sH = Fixed32.mul(sH, y);
		return applyMatrix(false);
	}
	
	protected boolean interactionRotate(int rad)
	{
		GestureField.matrixSetRotate(mat, WORKING_MATRIX, rad);
		return applyMatrix(false);
	}
	
	protected boolean interactionContact(int contact, boolean down)
	{
		if(!down && this.getContactCount() == 0)
		{
			//TODO: When contacts are removed and page is too small (in other words it has been zoomed out to much) then start reseting operation to make the page full screen
		}
		return super.interactionContact(contact, down);
	}
	
	protected void interactionComplete()
	{
		if(this.needRerender)
		{
			//Make sure that if the page will be rescaled to refit the screen that this doesn't run... yet.
			if(this.page != null)
			{
				if(!this.page.isFinished())
				{
					this.page.stop(this.curRenderWidth, this.curRenderHeight, null);
					try
					{
						this.page.waitForFinish();
					}
					catch (InterruptedException e)
					{
					}
				}
				renderPDF();
			}
			this.needRerender = false;
		}
		else
		{
			invalidate();
		}
	}
	
	private void checkForPageAdjustment()
	{
		//TODO: If page is too small, enlarge. Rotated, rotate back to normal. Translated off screen, move back on screen. If smaller then screen, then call fitToField.
		//All should be animated.
	}
	
	public void fitToField()
	{
		//TODO: Animate the transformation of the page till it fits back in the field.
	}
	
	public void zoom(int scale)
	{
		//Scale should be in Fixed32 format, negative means zoom out, positive means zoom in. Values should be pixel wise, in other words a value of 2 means that one pixel at the current zoom will be equal to two pixels.
		//TODO
	}
	
	public synchronized void showPage(PDFPage page)
	{
		if(this.page != page)
		{
			if(this.page != null)
			{
				if(!this.page.isFinished())
				{
					this.page.stop(this.curRenderWidth, this.curRenderHeight, null);
					try
					{
						this.page.waitForFinish();
					}
					catch (InterruptedException e)
					{
					}
				}
			}
			this.page = page;
			GestureField.matrixSetIdentity(mat, DRAWING_MATRIX);
			renderPDF();
		}
	}
	
	private void renderPDF()
	{
		this.gestureProcessing = this.page != null;
		if(this.page != null)
		{
			Thread thread = new Thread(new Runnable()
			{
				public void run()
				{
					FullPageField fpf = FullPageField.this;
					//TODO: Reset transformation
					fpf.curRenderWidth = Fixed32.toInt(fpf.sW);
					fpf.curRenderHeight = Fixed32.toInt(fpf.sH);
					Bitmap bmp = fpf.page.getImage(fpf.curRenderWidth, fpf.curRenderHeight, null, true, true);
					if(fpf.page.isFinished())
					{
						fpf.drawnPage = bmp;
						fpf.invalidate();
					}
				}
			});
			thread.start();
		}
	}
	
	protected boolean interactionGesture(Gesture gesture)
	{
		switch(gesture.getEvent())
		{
			case Gesture.EVENT_PINCH:
				GestureField.matrixSetRotate(mat, WORKING_MATRIX, gesture.getGestureValue(Gesture.TYPE_PINCH_ROTATE));
				boolean process = applyMatrix(false); //TODO: Not sure if this should be concatenated or not, it might need to be so the origin isn't rotated.
				GestureField.matrixSetScale(mat, WORKING_MATRIX, gesture.getGestureValue(Gesture.TYPE_PINCH_SCALE_X), gesture.getGestureValue(Gesture.TYPE_PINCH_SCALE_Y));
				process |= applyMatrix(false);
				GestureField.matrixSetTranslate(mat, WORKING_MATRIX, gesture.getGestureValue(Gesture.TYPE_PINCH_TRANSLATE_X), gesture.getGestureValue(Gesture.TYPE_PINCH_TRANSLATE_Y));
				process |= applyMatrix(true);
				checkForPageAdjustment();
				return process;
			case Gesture.EVENT_HOVER:
				//TODO: Reset page to show the whole page
			case Gesture.EVENT_SWIPE:
				//TODO: Jump to border in respected direction. If at border then jump to next page.
			case Gesture.EVENT_DOUBLE_TAP:
				//TODO: Zoom in until cannot zoom any more
				break;
		}
		return super.interactionGesture(gesture);
	}
	
	private boolean applyMatrix(boolean concat)
	{
		if(!VecMath.isIdentity(mat, WORKING_MATRIX))
		{
			VecMath.multiply3x3(mat, concat ? WORKING_MATRIX : DRAWING_MATRIX, mat, concat ? DRAWING_MATRIX : WORKING_MATRIX, mat, DRAWING_MATRIX);
			return true; //By returning true it will call interactionComplete()...
		}
		return false; //...but if no processing occurred, why redraw the screen?
	}
}
