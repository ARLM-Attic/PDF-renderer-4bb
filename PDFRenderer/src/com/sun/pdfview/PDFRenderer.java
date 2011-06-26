//#preprocessor

/*
 * File: PDFRenderer.java
 * Version: 1.8
 * Initial Creation: May 14, 2010 3:28:59 PM
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.Stack;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.XYRect;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFGraphics;
import com.sun.pdfview.helper.PDFUtil;
import com.sun.pdfview.helper.XYPointFloat;
import com.sun.pdfview.helper.XYRectFloat;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;

import com.sun.pdfview.i18n.ResourcesResource;

/**
 * This class turns a set of PDF Commands from a PDF page into an image.  It
 * encapsulates the state of drawing in terms of stroke, fill, transform,
 * etc., as well as pushing and popping these states.
 *
 * When the run method is called, this class goes through all remaining commands
 * in the PDF Page and draws them to its buffered image.  It then updates any
 * ImageConsumers with the drawn data.
 */
public class PDFRenderer extends BaseWatchable implements Runnable
{
	/** the page we were generate from */
    private PDFPage page;
    /** where we are in the page's command list */
    private int currentCommand;
    /** a weak reference to the image we render into.  For the image
     * to remain available, some other code must retain a strong reference to it.
     */
    private WeakReference imageRef;
    /** the graphics object for use within an iteration.  Note this must be
     * set to null at the end of each iteration, or the image will not be
     * collected
     */
    private PDFGraphics g;
    /** the current graphics state */
    private GraphicsState state;
    /** the stack of push()ed graphics states */
    private Stack stack;
    /** the total region of this image that has been written to */
    private XYRectFloat globalDirtyRegion;
    /** the last shape we drew (to check for overlaps) */
    private Geometry lastShape;
    /** the info about the image, if we need to recreate it */
    private ImageInfo imageinfo;
    /** the next time the image should be notified about updates */
    //private long then = 0;
    /** the sum of all the individual dirty regions since the last update */
    private XYRectFloat unupdatedRegion;
    /** how long (in milliseconds) to wait between image updates */
    public static final long UPDATE_DURATION = 200;
    public static final float NOPHASE = -1000;
    public static final float NOWIDTH = -1000;
    public static final float NOLIMIT = -1000;
    public static final int NOCAP = -1000;
    public static final float[] NODASH = null;
    public static final int NOJOIN = -1000;
	
    /**
     * create a new PDFGraphics state
     * @param page the current page
     * @param imageinfo the parameters of the image to render
     */
    public PDFRenderer(PDFPage page, ImageInfo imageinfo, Bitmap bi)
    {
        super();
        
        this.page = page;
        this.imageinfo = imageinfo;
        this.imageRef = new WeakReference(bi);
    }
    
    /**
     * create a new PDFGraphics state, given a Graphics2D. This version
     * will <b>not</b> create an image, and you will get a NullPointerException
     * if you attempt to call getImage().
     * @param page the current page
     * @param g the PDFGraphics object to use for drawing
     * @param imgbounds the bounds of the image into which to fit the page
     * @param clip the portion of the page to draw, in page space, or null
     * if the whole page should be drawn
     * @param bgColor the color to draw the background of the image, or
     * null for no color (0 alpha value)
     */
    public PDFRenderer(PDFPage page, PDFGraphics g, XYRect imgbounds, XYRectFloat clip, int bgColor)
    {
    	this(page, g, new ImageInfo(imgbounds.width, imgbounds.height, clip, bgColor));
    	
        g.translate(imgbounds.x, imgbounds.y);
        //System.out.println("Translating by " + imgbounds.x + "," + imgbounds.y);
    }
    
    /**
     * create a new PDFGraphics state, given a Graphics2D. This version
     * will <b>not</b> create an image, and you will get a NullPointerException
     * if you attempt to call getImage().
     * @param page the current page
     * @param g the PDFGraphics object to use for drawing
     * @param imageinfo the parameters of the image to render
     */
    public PDFRenderer(PDFPage page, PDFGraphics g, ImageInfo imageinfo)
    {
        super();
        
        this.page = page;
        this.g = g;
        this.imageinfo = imageinfo;
    }
    
    /**
     * Set up the graphics transform to match the clip region
     * to the image size.
     */
    private void setupRendering(PDFGraphics g)
    {
        g.setRenderingHint(PDFGraphics.KEY_ANTIALIASING, PDFGraphics.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(PDFGraphics.KEY_INTERPOLATION, PDFGraphics.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(PDFGraphics.KEY_ALPHA_INTERPOLATION, PDFGraphics.VALUE_ALPHA_INTERPOLATION_QUALITY);
        
        //Default background color is black (0x00000000)
        if (imageinfo.bgColor != 0)
        {
            g.setBackgroundColor(imageinfo.bgColor);
            g.clear(0, 0, imageinfo.width, imageinfo.height);
        }
        
        g.setColor(Color.BLACK);
        
        // set the initial clip and transform on the graphics
        AffineTransform at = getInitialTransform();
        g.transform(at);
        
        // set up the initial graphics state
        state = new GraphicsState();
        state.cliprgn = null;
        state.stroke = new BasicStroke();
        state.strokePaint = PDFPaint.getColorPaint(Color.BLACK);
        state.fillPaint = state.strokePaint;
        state.fillAlpha = Composite.getInstance(Composite.SRC);
        state.strokeAlpha = Composite.getInstance(Composite.SRC);
        state.xform = g.getTransform();
        
        // initialize the stack
        stack = new Stack();
        
        // initialize the current command
        currentCommand = 0;
    }
    
    /**
     * push the current graphics state onto the stack.  Continue working
     * with the current object; calling pop() restores the state of this
     * object to its state when push() was called.
     */
    public void push()
    {
        state.cliprgn = g.getClip();
        stack.push(state);
        
        state = (GraphicsState)state.clone();
    }
    
    /**
     * restore the state of this object to what it was when the previous
     * push() was called.
     */
    public void pop()
    {
        state = (GraphicsState)stack.pop();
        
        setTransform(state.xform);
        setClip(state.cliprgn);
    }
    
    /**
     * draw an outline using the current stroke and draw paint
     * @param s the path to stroke
     * @return a XYRectFloat to which the current region being
     * drawn will be added.  May also be null, in which case no dirty
     * region will be recorded.
     */
    public XYRectFloat stroke(Geometry s)
    {
        g.setComposite(state.strokeAlpha);
        s = new Geometry(autoAdjustStrokeWidth(g, state.stroke).createStrokedGeometry(s));
        return state.strokePaint.fill(this, g, s);
    }
    
    /**
     * auto adjust the stroke width, according to 6.5.4, which presumes that
     * the device characteristics (an image) require a single pixel wide
     * line, even if the width is set to less. We determine the scaling to
     * see if we would produce a line that was too small, and if so, scale
     * it up to produce a graphics line of 1 pixel, or so. This matches our
     * output with Adobe Reader.
     * 
     * @param g
     * @param bs
     * @return
     */
    private BasicStroke autoAdjustStrokeWidth(PDFGraphics g, BasicStroke bs)
    {
    	AffineTransform bt = new AffineTransform(g.getTransform());
    	XYPointFloat scale = new XYPointFloat();
        bt.getScale(scale);
        float width = bs.getLineWidth() * scale.x;
        BasicStroke stroke = bs;
        if (width < 1f)
        {
            if (scale.x > 0.01)
            {
                width = 1.0f / scale.x;
            }
            else
            {
                // prevent division by a really small number
                width = 1.0f;
            }
            stroke = new BasicStroke(width,
                    bs.getEndCap(),
                    bs.getLineJoin(),
                    bs.getMiterLimit(),
                    bs.getDashArray(),
                    bs.getDashPhase());
        }
        return stroke;
    }
    
    /**
     * draw an outline.
     * @param p the path to draw
     * @param bs the stroke with which to draw the path
     */
    public void draw(Geometry p, BasicStroke bs)
    {
        g.setComposite(state.fillAlpha);
        g.setPaint(state.fillPaint.getPaint());
        g.setStroke(autoAdjustStrokeWidth(g, bs));
        g.draw(p);
    }

    /**
     * fill an outline using the current fill paint
     * @param s the path to fill
     */
    public XYRectFloat fill(Geometry s)
    {
        g.setComposite(state.fillAlpha);
        return state.fillPaint.fill(this, g, s);
    }
    
    /**
     * draw an image.
     * @param image the image to draw
     */
    public XYRectFloat drawImage(PDFImage image)
    {
    	AffineTransform at = new AffineTransform(
        		1f / image.getWidth(), 0,
                0, -1f / image.getHeight(),
                0, 1);

        Bitmap bi = image.getImage();
        if (image.isImageMask())
        {
            bi = getMaskedImage(bi);
        }
        
        /*
        final Bitmap bmp = bi;
        final net.rim.device.api.ui.container.MainScreen frame = new net.rim.device.api.ui.container.MainScreen()
        {
        	protected void onUiEngineAttached(boolean attached)
        	{
        		this.add(new net.rim.device.api.ui.component.BitmapField(bmp));
        		
        		super.onUiEngineAttached(attached);
        	}
        };
        net.rim.device.api.ui.UiApplication.getUiApplication().invokeLater(new Runnable(){
        	public void run()
        	{
        		net.rim.device.api.ui.UiApplication.getUiApplication().pushScreen(frame);
        	}
        });
        */
        
        g.setComposite(Composite.getInstance(Composite.SRC_OVER));
        if (!g.drawImage(bi, at))
        {
            System.out.println("Image not completed!");
        }
        
        // get the total transform that was executed
        AffineTransform bt = new AffineTransform(g.getTransform());
        bt.concatenate(at);
        
        //Original got the origin of the bitmap, it will always be 0 so save the trouble.
        float[] points = new float[]{
            0, 0, bi.getWidth(), bi.getHeight()
        };
        bt.transform(points, 0, points, 0, 2);
        
        return new XYRectFloat(points[0], points[1],
                points[2] - points[0],
                points[3] - points[1]);

    }
    
    /**
     * add the path to the current clip.  The new clip will be the intersection
     * of the old clip and given path.
     */
    public void clip(Geometry s)
    {
        g.clip(s);
    }
    
    /**
     * set the clip to be the given shape.  The current clip is not taken
     * into account.
     */
    private void setClip(Geometry s)
    {
        state.cliprgn = s;
        g.setClip(null);
        g.clip(s);
    }
    
    /**
     * get the current affine transform
     */
    public AffineTransform getTransform()
    {
        return state.xform;
    }
    
    /**
     * concatenate the given transform with the current transform
     */
    public void transform(AffineTransform at)
    {
    	state.xform.concatenate(at);
        g.setTransform(state.xform);
    }

    /**
     * replace the current transform with the given one.
     */
    public void setTransform(AffineTransform at)
    {
        state.xform = at;
        g.setTransform(state.xform);
    }
    
    /**
     * get the initial transform from page space to Java space
     */
    public AffineTransform getInitialTransform()
    {
        return page.getInitialTransform(imageinfo.width, imageinfo.height, imageinfo.clip);
    }
    
    /**
     * Set some or all aspects of the current stroke.
     * @param w the width of the stroke, or NOWIDTH to leave it unchanged
     * @param cap the end cap style, or NOCAP to leave it unchanged
     * @param join the join style, or NOJOIN to leave it unchanged
     * @param limit the miter limit, or NOLIMIT to leave it unchanged
     * @param phase the phase of the dash array, or NOPHASE to leave it
     * unchanged
     * @param ary the dash array, or null to leave it unchanged.  phase
     * and ary must both be valid, or phase must be NOPHASE while ary is null.
     */
    public void setStrokeParts(float w, int cap, int join, float limit, float[] ary, float phase)
    {
        if (w == NOWIDTH)
        {
            w = state.stroke.getLineWidth();
        }
        if (cap == NOCAP)
        {
            cap = state.stroke.getEndCap();
        }
        if (join == NOJOIN)
        {
            join = state.stroke.getLineJoin();
        }
        if (limit == NOLIMIT)
        {
            limit = state.stroke.getMiterLimit();
        }
        if (phase == NOPHASE)
        {
            ary = state.stroke.getDashArray();
            phase = state.stroke.getDashPhase();
        }
        if (ary != null && ary.length == 0)
        {
            ary = null;
        }
        if (phase == NOPHASE)
        {
            state.stroke = new BasicStroke(w, cap, join, limit);
        }
        else
        {
            state.stroke = new BasicStroke(w, cap, join, limit, ary, phase);
        }
    }
    
    /**
     * get the current stroke as a BasicStroke
     */
    public BasicStroke getStroke()
    {
        return state.stroke;
    }
    
    /**
     * set the current stroke as a BasicStroke
     */
    public void setStroke(BasicStroke bs)
    {
        state.stroke = bs;
    }
    
    /**
     * set the stroke color
     */
    public void setStrokePaint(PDFPaint paint)
    {
        state.strokePaint = paint;
    }
    
    /**
     * set the fill color
     */
    public void setFillPaint(PDFPaint paint)
    {
        state.fillPaint = paint;
    }
    
    /**
     * set the stroke alpha
     */
    public void setStrokeAlpha(float alpha)
    {
        state.strokeAlpha = Composite.getInstance(Composite.SRC_OVER, alpha);
    }
    
    /**
     * set the stroke alpha
     */
    public void setFillAlpha(float alpha)
    {
        state.fillAlpha = Composite.getInstance(Composite.SRC_OVER, alpha);
    }
    
    /**
     * Set the last shape drawn
     */
    public void setLastShape(Geometry shape)
    {
        this.lastShape = shape;
    }
    
    /**
     * Get the last shape drawn
     */
    public Geometry getLastShape()
    {
        return lastShape;
    }
    
    /**
     * Setup rendering.  Called before iteration begins
     */
    public void setup()
    {
        PDFGraphics graphics = null;
        
        if (imageRef != null)
        {
        	Bitmap bi = (Bitmap)imageRef.get();
            if (bi != null)
            {
                graphics = PDFGraphics.createGraphics(bi);
            }
        }
        else
        {
            graphics = g;
        }
        
        if (graphics != null)
        {
            setupRendering(graphics);
        }
    }
    
    /**
     * Draws the next command in the PDFPage to the buffered image.
     * The image will be notified about changes no less than every
     * UPDATE_DURATION milliseconds.
     *
     * @return <ul><li>Watchable.RUNNING when there are commands to be processed
     *             <li>Watchable.NEEDS_DATA when there are no commands to be
     *                 processed, but the page is not yet complete
     *             <li>Watchable.COMPLETED when the page is done and all
     *                 the commands have been processed
     *             <li>Watchable.STOPPED if the image we are rendering into
     *                 has gone away
     *         </ul>
     */
    public int iterate() throws Exception
    {
        // make sure we have a page to render
        if (page == null)
        {
            return Watchable.COMPLETED;
        }
        
        // check if this renderer is based on a weak reference to a graphics
        // object.  If it is, and the graphics is no longer valid, then just quit
        Bitmap bi = null;
        if (imageRef != null)
        {
            bi = (Bitmap)imageRef.get();
            if (bi == null)
            {
                System.out.println("Image went away.  Stopping");
                return Watchable.STOPPED;
            }
            
            g = PDFGraphics.createGraphics(bi);
        }
        
        // check if there are any commands to parse.  If there aren't,
        // just return, but check if we'return really finished or not
        if (currentCommand >= page.getCommandCount())
        {
            if (page.isFinished())
            {
                return Watchable.COMPLETED;
            }
            else
            {
                return Watchable.NEEDS_DATA;
            }
        }
        
        // find the current command
        PDFCmd cmd = page.getCommand(currentCommand++);
        if (cmd == null)
        {
            // uh oh.  Synchronization problem!
            throw new PDFParseException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getString(ResourcesResource.RENDERER_CMD_NOT_FOUND));
        }
        
        // execute the command
        XYRectFloat dirtyRegion = cmd.execute(this);
        
        // append to the global dirty region
        globalDirtyRegion = addDirtyRegion(dirtyRegion, globalDirtyRegion);
        unupdatedRegion = addDirtyRegion(dirtyRegion, unupdatedRegion);
        
        /* No observers so no need to do this.
        long now = System.currentTimeMillis();
        if (now > then || rendererFinished())
        {
            // now tell any observers, so they can repaint
            notifyObservers(bi, unupdatedRegion);
            unupdatedRegion = null;
            then = now + UPDATE_DURATION;
        }
        */
        
        // if we are based on a reference to a graphics, don't hold on to it
        // since that will prevent the image from being collected.
        if (imageRef != null)
        {
            g = null;
        }
        
        // if we need to stop, it will be caught at the start of the next
        // iteration.
        return Watchable.RUNNING;
    }
    
    /**
     * Called when iteration has stopped
     */
    public void cleanup()
    {
        page = null;
        state = null;
        stack = null;
        globalDirtyRegion = null;
        lastShape = null;
        
        // keep around the image ref and image info for use in late addObserver() call
        //EDIT: No observers are used so there is no need
        if(imageRef != null)
        {
	        imageRef.clear();
	        imageRef = null;
        }
    }
    
    /**
     * Append a rectangle to the total dirty region of this shape
     */
    private XYRectFloat addDirtyRegion(XYRectFloat region, XYRectFloat glob)
    {
        if (region == null)
        {
            return glob;
        }
        else if (glob == null)
        {
            return region;
        }
        else
        {
            PDFUtil.union(glob, region, glob);
            return glob;
        }
    }

    /**
     * Determine if we are finished
     */
    private boolean rendererFinished()
    {
        if (page == null)
        {
            return true;
        }
        
        return (page.isFinished() && currentCommand == page.getCommandCount());
    }
    
    /**
     * Convert an image mask into an image by painting over any pixels
     * that have a value in the image with the current paint
     */
    private Bitmap getMaskedImage(Bitmap bi)
    {
        // get the color of the current paint
        int col = state.fillPaint.getPaint().getColor();
        
        // format as 8 bits each of ARGB
        int paintColor = PDFUtil.Color_getAlpha(col) << 24;
        paintColor |= PDFUtil.Color_getRed(col) << 16;
        paintColor |= PDFUtil.Color_getGreen(col) << 8;
        paintColor |= PDFUtil.Color_getBlue(col);
        
        // transparent (alpha = 1)
        int noColor = 0;
        
        // get the coordinates of the source image
        int startX = 0;
        int startY = 0;
        int width = bi.getWidth();
        int height = bi.getHeight();
        
        // create a destination image of the same size
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
        Bitmap dstImage = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, width, height);
        dstImage.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
//#else
        Bitmap dstImage = new Bitmap(Bitmap.ROWWISE_32BIT_ARGB8888, width, height);
//#endif
        
        // copy the pixels row by row
        for (int i = 0; i < height; i++)
        {
            int[] srcPixels = new int[width];
            int[] dstPixels = new int[width];
            
            // read a row of pixels from the source
            bi.getARGB(srcPixels, 0, height, startX, startY + i, width, 1);
            
            // figure out which ones should get painted
            for (int j = 0; j < width; j++)
            {
                if (srcPixels[j] == 0xff000000)
                {
                    dstPixels[j] = paintColor;
                }
                else
                {
                    dstPixels[j] = noColor;
                }
            }
            
            // write the destination image
            dstImage.setARGB(dstPixels, 0, height, startX, startY + i, width, 1);
        }
        
        return dstImage;
    }
    
    class GraphicsState
    {
        /** the clip region */
    	Geometry cliprgn;
        /** the current stroke */
        BasicStroke stroke;
        /** the current paint for drawing strokes */
        PDFPaint strokePaint;
        /** the current paint for filling shapes */
        PDFPaint fillPaint;
        /** the current compositing alpha for stroking */
        Composite strokeAlpha;
        /** the current compositing alpha for filling */
        Composite fillAlpha;
        /** the current transform */
        AffineTransform xform;
        
        /** Clone this Graphics state.
         *
         * Note that cliprgn is not cloned.  It must be set manually from
         * the current graphics object's clip
         */
        public Object clone()
        {
            GraphicsState cState = new GraphicsState();
            cState.cliprgn = null;
            
            // copy immutable fields
            cState.strokePaint = strokePaint;
            cState.fillPaint = fillPaint;
            cState.strokeAlpha = strokeAlpha;
            cState.fillAlpha = fillAlpha;
            
            // clone mutable fields
            cState.stroke = new BasicStroke(stroke.getLineWidth(),
                    stroke.getEndCap(),
                    stroke.getLineJoin(),
                    stroke.getMiterLimit(),
                    stroke.getDashArray(),
                    stroke.getDashPhase());
            cState.xform = new AffineTransform(xform);
            
            return cState;
        }
    }
}
