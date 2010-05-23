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

import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.Paint;

import net.rim.device.api.math.Matrix4f;
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
	
	/**
	 * Create a new PDFGraphics device.
	 * @param drawingDevice The graphics device to draw with.
	 * @return The PDFGraphics that will draw to that graphics device or null if that device is not supported.
	 */
	public static PDFGraphics createGraphics(Object drawingDevice)
	{
		System.err.println("Graphics creation is not supported yet.");
		return null;
		
		//TYPES: Bitmap, RIM Graphics, J2ME Graphics
		//TODO: Testing needs to be done to determine what types the drawing objects will return and how to create the new drawing item. Should be dynamic so if someone creates their own drawing device it can still work.
	}
	
	/**
	 * Returns a copy of the current <code>Transform</code> in the <code>PDFGraphics</code> context.
	 * @return The current <code>Matrix4f</code> in the <code>PDFGraphics</code> context.
	 */
	public abstract Matrix4f getTransform();
	
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
	 * Composes an Matrix4f object with the Transform in this PDFGraphics according to the rule last-specified-first-applied.
	 * @param Tx The Matrix4f object to be composed with the current Transform.
	 */
	public void transform(Matrix4f Tx)
	{
		setTransform(Tx, false);
	}
	
	/**
	 * Overwrites the Transform in the PDFGraphics context.
	 * @param Tx The Matrix4f that was retrieved from the getTransform method.
	 */
	public void setTransform(Matrix4f Tx)
	{
		setTransform(Tx, true);
	}
	
	/**
	 * Sets the Transform in the PDFGraphics context.
	 * @param Tx The Matrix4f to set to the device's matrix.
	 * @param direct true if the matrix will replace the device's matrix, false if it should be combined with the previous matrix.
	 */
	public abstract void setTransform(Matrix4f Tx, boolean direct);
	
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
	public abstract boolean drawImage(Bitmap img, Matrix4f xform);
}
