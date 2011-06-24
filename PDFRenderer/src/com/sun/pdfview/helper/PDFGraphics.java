/*
 * File: PDFGraphics.java
 * Version: 1.0
 * Initial Creation: May 12, 2010 3:40:42 PM
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
package com.sun.pdfview.helper;

import java.util.Vector;

import com.sun.pdfview.ResourceManager;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.Paint;

import net.rim.device.api.system.Bitmap;

/**
 * Helper class to abstract Graphics drawing away from native classes. This way if the developer is using {@link javax.microedition.lcdui.Graphics J2ME Graphics},
 * {@link net.rim.device.api.ui.Graphics BlackBerry Graphics}, or even {@link javax.microedition.khronos.opengles.GL OpenGL} the graphics drawing will be the same.
 * <p>
 * <b>Implementation notes:</b> Default background color and current color should be black.
 * @author Vincent Simonetti
 */
public abstract class PDFGraphics
{
	/** Antialiasing hint key.*/
	public static final int KEY_ANTIALIASING = 0xAAA;
	/** Interpolation hint key.*/
	public static final int KEY_INTERPOLATION = 0xEA;
	/** Alpha interpolation hint key.*/
	public static final int KEY_ALPHA_INTERPOLATION = 0xAAEA;
	
	/** Antialiasing hint values -- rendering is done with antialiasing.*/
	public static final int VALUE_ANTIALIAS_ON = 2;
	/** Antialiasing hint values -- rendering is done without antialiasing.*/
	public static final int VALUE_ANTIALIAS_OFF = 1;
	/** Antialiasing hint values -- rendering is done with the platform default antialiasing mode.*/
	public static final int VALUE_ANTIALIAS_DEFAULT = VALUE_ANTIALIAS_ON;
	
	/** Interpolation hint value -- INTERPOLATION_NEAREST_NEIGHBOR.*/
	public static final int VALUE_INTERPOLATION_NEAREST_NEIGHBOR = 1;
	/** Interpolation hint value -- INTERPOLATION_BILINEAR.*/
	public static final int VALUE_INTERPOLATION_BILINEAR = 2;
	/** Interpolation hint value -- INTERPOLATION_BICUBIC.*/
	public static final int VALUE_INTERPOLATION_BICUBIC = 3;
	
	/** Alpha interpolation hint value -- ALPHA_INTERPOLATION_QUALITY.*/
	public static final int VALUE_ALPHA_INTERPOLATION_QUALITY = 2;
	/** Alpha interpolation hint value -- ALPHA_INTERPOLATION_SPEED.*/
	public static final int VALUE_ALPHA_INTERPOLATION_SPEED = 1;
	/** Alpha interpolation hint value -- ALPHA_INTERPOLATION_DEFAULT.*/
	public static final int VALUE_ALPHA_INTERPOLATION_DEFAULT = VALUE_ALPHA_INTERPOLATION_SPEED;
	
	private class GraphicsObject
	{
		public PDFGraphics graphics;
		private SoftReference ref;
		
		public GraphicsObject(PDFGraphics graphics, Object drawingObject)
		{
			if(graphics == null || drawingObject == null)
			{
				throw new NullPointerException();
			}
			this.graphics = graphics;
			this.ref = new SoftReference(drawingObject);
		}
		
		public boolean equals(Object obj)
		{
			Object reference = ref.get();
			if(reference != null)
			{
				if(obj != null)
				{
					return reference == obj; //This way it compares pointers and an exact match can be made.
				}
			}
			return false;
		}
		
		public boolean isValid()
		{
			return ref.get() != null;
		}
	}
	
	private static final long GFX_OBJS_ID = 0x8A3FAFC98105BCAAL;
	
	/**
	 * Create a new PDFGraphics device.
	 * @param drawingDevice The graphics device to draw with.
	 * @return The PDFGraphics that will draw to that graphics device or null if that device is not supported.
	 */
	public synchronized static PDFGraphics createGraphics(Object drawingDevice)
	{
		PDFGraphics graphics = null;
		
		Vector graphicsObjects;
		Object obj;
		if((obj = ResourceManager.singletonStorageGet(GFX_OBJS_ID)) != null)
		{
			graphicsObjects = (Vector)obj;
			
			//First try to find an already created graphics object
			int c = graphicsObjects.size();
			Vector remove = null;
			for(int i = 0; i < c; i++)
			{
				GraphicsObject gfx = (GraphicsObject)graphicsObjects.elementAt(i);
				if(graphics == null && gfx.equals(drawingDevice))
				{
					//Found it
					graphics = gfx.graphics;
				}
				else if(!gfx.isValid())
				{
					if(remove == null)
					{
						remove = new Vector();
					}
					remove.addElement(gfx);
				}
			}
			if(remove != null)
			{
				//Remove any invalid graphics objects
				c = remove.size();
				for(int i = 0; i < c; i++)
				{
					graphicsObjects.removeElement(remove.elementAt(i));
				}
			}
		}
		else
		{
			graphicsObjects = new Vector();
			ResourceManager.singletonStorageSet(GFX_OBJS_ID, graphicsObjects);
		}
		
		if(graphics == null)
		{
			//Couldn't find a precreated graphics object, so try making one
			String drawingSystemName = "com.sun.pdfview.helper.graphics.drawing." + drawingDevice.getClass().getName() + "Graphics.GraphicsImpl";
			try
			{
				Class drawingSystem = Class.forName(drawingSystemName);
				PDFGraphics system = (PDFGraphics)drawingSystem.newInstance();
				system.setDrawingDevice(drawingDevice);
				graphicsObjects.addElement(system.new GraphicsObject(system, drawingDevice));
				graphics = system;
			}
			catch (Exception e)
			{
				//Couldn't find or create graphics system.
				System.out.println("Graphics creation is not supported for: " + drawingDevice.getClass() + '.');
			}
		}
		
		return graphics;
		
		//TYPES: Bitmap, RIM Graphics, J2ME Graphics
		//TODO: Testing needs to be done to determine what types the drawing objects will return and how to create the new drawing item. Should be dynamic so if someone creates their own drawing device it can still work.
		//Remember: http://supportforums.blackberry.com/t5/Java-Development/Rotate-and-scale-bitmaps/ta-p/492524 and Advanced Graphics stuff
	}
	
	/**
	 * Returns a copy of the current <code>Transform</code> in the <code>PDFGraphics</code> context.
	 * @return The current <code>AffineTransform</code> in the <code>PDFGraphics</code> context.
	 */
	public abstract AffineTransform getTransform();
	
	/**
	 * Gets the current clipping area.
	 * @return A Shape object representing the current clipping area, or null if no clip is set.
	 */
	public abstract Geometry getClip();
	
	/**
	 * Sets the Composite for the PDFGraphics context.
	 * @param comp the Composite object to be used for rendering.
	 */
	public abstract void setComposite(Composite comp);
	
	/**
	 * Sets the <code>Paint</code> attribute for the <code>PDFGraphics</code> context.
	 * @param paint The <code>Paint</code> object to be used to generate color during the rendering process, or null.
	 */
	public abstract void setPaint(Paint paint);
	
	/**
	 * Sets this graphics context's current color to the specified color.
	 * @param c The new rendering color.
	 */
	public abstract void setColor(int c);
	
	/**
	 * Sets this graphics context's current background color to the specified color.
	 * @param c The new background color.
	 */
	public abstract void setBackgroundColor(int c);
	
	/**
	 * Sets the value of a single preference for the rendering algorithms.
	 * @param hintKey The key of the hint to be set.
	 * @param hintValue The value indicating preferences for the specified hint category.
	 */
	public abstract void setRenderingHint(int hintKey, int hintValue);
	
	/**
	 * Sets the Stroke for the PDFGraphics context.
	 * @param s The Stroke object to be used to stroke a Shape during the rendering process.
	 */
	public abstract void setStroke(BasicStroke s);
	
	/**
	 * Intersects the current Clip with the interior of the specified Shape and sets the Clip to the resulting intersection.
	 * @param s The Shape to be intersected with the current Clip. If s is null, this method clears the current Clip.
	 */
	public void clip(Geometry s)
	{
		setClip(s, false);
	}
	
	/**
	 * Sets the current clipping area to an arbitrary clip shape.
	 * @param s The Shape to use to set the clip.
	 */
	public void setClip(Geometry s)
	{
		setClip(s, true);
	}
	
	/**
	 * Set the current clip.
	 * @param s The Shape to use to set the clip or null to clear it.
	 * @param direct true if the clip should set the device's actual clip, false if the clip area should be added (or if no clip exists, set) to the devices clip. If 
	 * <code>s</code> is null then the device clip should be cleared regardless of this flag.
	 */
	protected abstract void setClip(Geometry s, boolean direct);
	
	/**
	 * Composes an AffineTransform object with the Transform in this PDFGraphics according to the rule last-specified-first-applied.
	 * @param Tx The AffineTransform object to be composed with the current Transform.
	 */
	public void transform(AffineTransform Tx)
	{
		setTransform(Tx, false);
	}
	
	/**
	 * Overwrites the Transform in the PDFGraphics context.
	 * @param Tx The AffineTransform that was retrieved from the getTransform method.
	 */
	public void setTransform(AffineTransform Tx)
	{
		setTransform(Tx, true);
	}
	
	/**
	 * Sets the Transform in the PDFGraphics context.
	 * @param Tx The AffineTransform to set to the device's matrix.
	 * @param direct true if the matrix will replace the device's matrix, false if it should be combined with the previous matrix.
	 */
	public abstract void setTransform(AffineTransform Tx, boolean direct);
	
	/**
	 * Translates the origin of the PDFGraphics context to the point (x, y) in the current coordinate system.
	 * @param x The specified x coordinate.
	 * @param y The specified y coordinate.
	 */
	public abstract void translate(int x, int y);
	
	/**
	 * Fills the interior of a <code>Shape</code> using the settings of the <code>PDFGraphics</code> context.
	 * @param s The Shape to be filled.
	 */
	public abstract void fill(Geometry s);
	
	/**
	 * Clears a region to the current background color.
	 * @param x Left edge of the region.
	 * @param y Top edge of the region.
	 * @param width Width of the region.
	 * @param height Height of the region.
	 */
	public abstract void clear(int x, int y, int width, int height);
	
	/**
	 * Strokes the outline of a Shape using the settings of the current PDFGraphics context.
	 * @param s The Shape to be rendered.
	 */
	public abstract void draw(Geometry s);
	
	/**
	 * Renders an image, applying a transform from image space into user space before drawing.
	 * @param img The Bitmap to be rendered.
	 * @param xform The transformation from image space into user space.
	 * @return true if the Image is fully loaded and completely rendered; false if the Image is still being loaded.
	 */
	public abstract boolean drawImage(Bitmap img, AffineTransform xform);
	
	/**
	 * Sets the drawing device that everything will get drawn to.
	 * @param device The device to draw with.
	 */
	protected abstract void setDrawingDevice(Object device);
}
