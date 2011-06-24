/*
 * File: PDFPage.java
 * Version: 1.7
 * Initial Creation: May 14, 2010 7:01:04 PM
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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.XYDimension;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFUtil;
import com.sun.pdfview.helper.XYRectFloat;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Geometry;

import com.sun.pdfview.i18n.ResourcesResource;

/**
 * A PDFPage encapsulates the parsed commands required to render a
 * single page from a PDFFile.  The PDFPage is not itself drawable;
 * instead, create a PDFImage to display something on the screen.
 * <p>
 * This file also contains all of the PDFCmd commands that might
 * be a part of the command stream in a PDFPage.  They probably
 * should be inner classes of PDFPage instead of separate non-public
 * classes.
 *
 * @author Mike Wessler
 */
public class PDFPage
{
	/** the array of commands.  The length of this array will always
     * be greater than or equal to the actual number of commands. */
    private Vector commands;
    /** whether this page has been finished.  If true, there will be no
     * more commands added to the cmds list. */
    private boolean finished = false;
    /** the page number used to find this page */
    private int pageNumber;
    /**
     * the bounding box of the page, in page coordinates, straight from the page dictionary
     */
    private XYRectFloat pageDictBbox;
    
    /** the post-rotation bounding box in page points with the x,y co-ordinates at 0,0 */
    private XYRectFloat targetBbox;
    
    /** the rotation of this page, in degrees */
    private int rotation;
    /** a map from image info (width, height, clip) to a soft reference to the
    rendered image */
    private Cache cache;
    /** a map from image info to weak references to parsers that are active */
    private Hashtable renderers;

    /**
     * create a PDFPage with dimensions in bbox and rotation.
     */
    public PDFPage(XYRectFloat bbox, int rotation)
    {
        this(-1, bbox, rotation, null);
    }
    
    /**
     * create a PDFPage
     * @param pageNumber the page number
     * @param bbox the bounding box, specified in pre-rotation page co-ordinates
     * @param rotation the rotation to apply to the page; must be 0/90/180/270
     * @param cache a cache to use
     */
    public PDFPage(int pageNumber, XYRectFloat bbox, int rotation, Cache cache)
    {
        this.pageNumber = pageNumber;
        this.cache = cache;
        
        if (bbox == null)
        {
            bbox = new XYRectFloat(0, 0, 1, 1);
        }
        this.pageDictBbox = bbox;
        
        if (rotation < 0)
        {
            rotation += 360;
        }
        
        this.rotation = rotation;
        
        if (rotation == 0 || rotation == 180)
        {
            this.targetBbox = new XYRectFloat(0, 0, pageDictBbox.width, pageDictBbox.height);
        }
        else
        {
            this.targetBbox = new XYRectFloat(0, 0, pageDictBbox.height, pageDictBbox.width);
        }
        
        // initialize the cache of images and parsers
        renderers = PDFUtil.synchronizedTable(new Hashtable());
        
        // initialize the list of commands
        commands = PDFUtil.synchronizedVector(new Vector(250));
    }
    
    /**
     * Get the width and height of this image in the correct aspect ratio.
     * The image returned will have at least one of the width and
     * height values identical to those requested.  The other
     * dimension may be smaller, so as to keep the aspect ratio
     * the same as in the original page.
     *
     * @param width the maximum width of the image
     * @param height the maximum height of the image
     * @param clip the region in <b>page space co-ordinates</b> of the page to
     * display.  It may be null, in which the page crop/media box is used.
     */
    public XYDimension getUnstretchedSize(int width, int height, XYRectFloat clip)
    {
        if (clip == null)
        {
            clip = pageDictBbox;
        }
        
        final boolean swapDimensions = doesRotationSwapDimensions();
        final double srcHeight = swapDimensions ? clip.width : clip.height;
        final double srcWidth = swapDimensions ? clip.height : clip.width;
        double ratio = srcHeight / srcWidth;
        float askratio = (float)height / (float)width;
        if (askratio > ratio)
        {
            // asked for something too high
            height = (int)(width * ratio + 0.5f);
        }
        else
        {
            // asked for something too wide
            width = (int)(height / ratio + 0.5f);
        }
        
        return new XYDimension(width, height);
    }
    
    private boolean doesRotationSwapDimensions()
    {
        return getRotation() == 90 || getRotation() == 270;
    }
    
    /**
     * Get an image producer which can be used to draw the image
     * represented by this PDFPage.  The ImageProducer is guaranteed to
     * stay in sync with the PDFPage as commands are added to it.
     *
     * The image will contain the section of the page specified by the clip,
     * scaled to fit in the area given by width and height.
     *
     * @param width the width of the image to be produced
     * @param height the height of the image to be produced
     * @param clip the region in <b>page space</b> of the entire page to
     *        display
     * @return an Image that contains the PDF data
     */
    public Bitmap getImage(int width, int height, XYRectFloat clip)
    {
        return getImage(width, height, clip, true, false);
    }
    
    /**
     * Get an image producer which can be used to draw the image
     * represented by this PDFPage.  The ImageProducer is guaranteed to
     * stay in sync with the PDFPage as commands are added to it.
     *
     * The image will contain the section of the page specified by the clip,
     * scaled to fit in the area given by width and height.
     *
     * @param width the width of the image to be produced
     * @param height the height of the image to be produced
     * @param clip the region in <b>page space</b> of the entire page to
     *             display
     * @param drawbg if true, put a white background on the image.  If not,
     *        draw no color (alpha 0) for the background.
     * @param wait if true, do not return until this image is fully rendered.
     * @return an Image that contains the PDF data
     */
    public Bitmap getImage(int width, int height, XYRectFloat clip, boolean drawbg, boolean wait)
    {
        // see if we already have this image
    	Bitmap image = null;
        PDFRenderer renderer = null;
        ImageInfo info = new ImageInfo(width, height, clip, 0);
        
        if (cache != null)
        {
            image = cache.getImage(this, info);
            renderer = cache.getImageRenderer(this, info);
        }
        
        // not in the cache, so create it
        if (image == null)
        {
            if (drawbg)
            {
                info.bgColor = Color.WHITE;
            }
            
            image = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, info.width, info.height);
            image.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
            renderer = new PDFRenderer(this, info, image);
            
            if (cache != null)
            {
                cache.addImage(this, info, image, renderer);
            }
            
            renderers.put(info, new WeakReference(renderer));
        }
        
        // the renderer may be null if we are getting this image from the
        // cache and rendering has completed.
        if (renderer != null)
        {
            if (!renderer.isFinished())
            {
                renderer.go(wait);
            }
        }
        
        // return the image
        return image;
    }
    
    /**
     * get the page number used to lookup this page
     * @return the page number
     */
    public int getPageNumber()
    {
        return pageNumber;
    }
    
    /**
     * get the aspect ratio of the correctly oriented page.
     * @return the width/height aspect ratio of the page
     */
    public float getAspectRatio()
    {
        return getWidth() / getHeight();
    }
    
    /**
     * Get the original crop/media box of the page, in page units, before
     * any rotation and with clipping co-ordinates
     * @return the page box
     */
    public XYRectFloat getPageBox()
    {
        return pageDictBbox;
    }
    
    /**
     * get the post-rotation box placed at 0, 0 in page units
     */
    public XYRectFloat getBBox()
    {
        return targetBbox;
    }
    
    /**
     * get the width of this page, in page points, after rotation
     */
    public float getWidth()
    {
        return (float)targetBbox.width;
    }
    
    /**s
     * get the height of this page, in page points, after rotation
     */
    public float getHeight()
    {
        return (float)targetBbox.height;
    }
    
    /**
     * get the rotation of this image
     */
    public int getRotation()
    {
        return rotation;
    }
    
    /**
     * Get the initial transform to map from a specified clip rectangle in
     * pdf coordinates to an image of the specfied width and
     * height in device coordinates
     *
     * @param width the width of the target image
     * @param height the height of the target image
     * @param clip the desired clip rectangle to use in page co-ordinates;
     *  use <code>null</code> to draw the page crop/media box
     */
    public AffineTransform getInitialTransform(int width, int height, XYRectFloat clip)
    {
    	if (clip == null)
    	{
            clip = pageDictBbox;
        }
    	
        AffineTransform at;
        switch (getRotation())
        {
            case 0:
            	at = new AffineTransform(1, 0, 0, -1, 0, height);
                break;
            case 90:
            	at = new AffineTransform(0, 1, 1, 0, 0, 0);
                break;
            case 180:
            	at = new AffineTransform(-1, 0, 0, 1, width, 0);
                break;
            case 270:
            	at = new AffineTransform(0, -1, -1, 0, width, height);
                break;
            default:
                throw new IllegalArgumentException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getFormattedString(ResourcesResource.PAGE_NON_QUAD_ROT, new Object[]{new Integer(getRotation())}));
        }
        
        // now scale the image to be the size of the clip
        double scaleX = (doesRotationSwapDimensions() ? height : width) / clip.width;
        double scaleY = (doesRotationSwapDimensions() ? width: height) / clip.height;
        at.scale((float)scaleX, (float)scaleY);
        
        // create a transform that moves the top left corner of the clip region
        // (minX, minY) to (0,0) in the image
        at.translate((float)-clip.x, (float)-clip.y);
        
        return at;
    }
    
    /**
     * get the current number of commands for this page
     */
    public int getCommandCount()
    {
        return commands.size();
    }
    
    /**
     * get the command at a given index
     */
    public PDFCmd getCommand(int index)
    {
        return (PDFCmd)commands.elementAt(index);
    }
    
    /**
     * get all the commands in the current page
     */
    public Vector getCommands()
    {
        return commands;
    }
    
    /**
     * get all the commands in the current page starting at the given index
     */
    public Vector getCommands(int startIndex)
    {
        return getCommands(startIndex, getCommandCount());
    }
    
    /**
     * get the commands in the page within the given start and end indices
     */
    public Vector getCommands(int startIndex, int endIndex)
    {
    	//No such thing as sublist in J2ME, if this method is very wrong then it should be changed, until then this should be good enough
    	Vector v = new Vector(endIndex - startIndex - 1);
    	for(int i = startIndex; i < endIndex; i++)
    	{
    		v.addElement(commands.elementAt(i));
    	}
    	return PDFUtil.synchronizedVector(v);
    }

    /**
     * Add a single command to the page list.
     */
    public void addCommand(PDFCmd cmd)
    {
        synchronized(commands)
        {
            commands.addElement(cmd);
        }
        
        // notify any outstanding images
        updateImages();
    }
    
    /**
     * add a collection of commands to the page list.  This is probably
     * invoked as the result of an XObject 'do' command, or through a
     * type 3 font.
     */
    public void addCommands(PDFPage page)
    {
        addCommands(page, null);
    }
    
    /**
     * add a collection of commands to the page list.  This is probably
     * invoked as the result of an XObject 'do' command, or through a
     * type 3 font.
     * @param page the source of other commands.  It MUST be finished.
     * @param extra a transform to perform before adding the commands.
     * If null, no extra transform will be added.
     */
    public void addCommands(PDFPage page, AffineTransform extra)
    {
        synchronized(commands)
        {
            addPush();
            if (extra != null)
            {
                addXform(extra);
            }
            //addXform(page.getTransform());
            //No addAll function, do this manually unless later a helper function or native function gets added for this.
            for(Enumeration en = page.getCommands().elements(); en.hasMoreElements();)
            {
            	commands.addElement(en.nextElement());
            }
            addPop();
        }
        
        // notify any outstanding images
        updateImages();
    }
    
    /**
     * Clear all commands off the current page
     */
    public void clearCommands()
    {
        synchronized(commands)
        {
            commands.removeAllElements();
        }
        
        // notify any outstanding images
        updateImages();
    }
    
    /**
     * get whether parsing for this PDFPage has been completed and all
     * commands are in place.
     */
    public boolean isFinished()
    {
        return finished;
    }
    
    /**
     * wait for finish
     */
    public synchronized void waitForFinish() throws InterruptedException
    {
        if (!finished)
        {
            wait();
        }
    }
    
    /**
     * Stop the rendering of a particular image on this page
     */
    public void stop(int width, int height, XYRectFloat clip)
    {
        ImageInfo info = new ImageInfo(width, height, clip);
        
        synchronized(renderers)
        {
            // find our renderer
            WeakReference rendererRef = (WeakReference)renderers.get(info);
            if (rendererRef != null)
            {
                PDFRenderer renderer = (PDFRenderer)rendererRef.get();
                if (renderer != null)
                {
                    // stop it
                    renderer.stop();
                }
            }
        }
    }
    
    /**
     * The entire page is done.  This must only be invoked once.  All
     * observers will be notified.
     */
    public synchronized void finish()
    {
    	//System.out.println("Page finished!");
        finished = true;
        notifyAll();
        
        // notify any outstanding images
        updateImages();
    }
    
    /** push the graphics state */
    public void addPush()
    {
        addCommand(new PDFPushCmd());
    }
    
    /** pop the graphics state */
    public void addPop()
    {
        addCommand(new PDFPopCmd());
    }
    
    /** concatenate a transform to the graphics state */
    public void addXform(AffineTransform at)
    {
        //PDFXformCmd xc = lastXformCmd();
    	//xc.at.concatenate(at);
        addCommand(new PDFXformCmd(new AffineTransform(at)));
    }
    
    /**
     * set the stroke width
     * @param w the width of the stroke
     */
    public void addStrokeWidth(float w)
    {
        PDFChangeStrokeCmd sc = new PDFChangeStrokeCmd();
//        if (w == 0)
//        {
//        	w = 0.1f;
//        }
        sc.setWidth(w);
        addCommand(sc);
    }
    
    /**
     * set the end cap style
     * @param capstyle the cap style:  0 = BUTT, 1 = ROUND, 2 = SQUARE
     */
    public void addEndCap(int capstyle)
    {
        PDFChangeStrokeCmd sc = new PDFChangeStrokeCmd();
        
        int cap = BasicStroke.CAP_BUTT;
        switch (capstyle)
        {
            case 0:
                cap = BasicStroke.CAP_BUTT;
                break;
            case 1:
                cap = BasicStroke.CAP_ROUND;
                break;
            case 2:
                cap = BasicStroke.CAP_SQUARE;
                break;
        }
        sc.setEndCap(cap);
        
        addCommand(sc);
    }
    
    /**
     * set the line join style
     * @param joinstyle the join style: 0 = MITER, 1 = ROUND, 2 = BEVEL
     */
    public void addLineJoin(int joinstyle)
    {
        PDFChangeStrokeCmd sc = new PDFChangeStrokeCmd();
        
        int join = BasicStroke.JOIN_MITER;
        switch (joinstyle)
        {
            case 0:
                join = BasicStroke.JOIN_MITER;
                break;
            case 1:
                join = BasicStroke.JOIN_ROUND;
                break;
            case 2:
                join = BasicStroke.JOIN_BEVEL;
                break;
        }
        sc.setLineJoin(join);
        
        addCommand(sc);
    }
    
    /**
     * set the miter limit
     */
    public void addMiterLimit(float limit)
    {
        PDFChangeStrokeCmd sc = new PDFChangeStrokeCmd();
        
        sc.setMiterLimit(limit);
        
        addCommand(sc);
    }

    /**
     * set the dash style
     * @param dashary the array of on-off lengths
     * @param phase offset of the array at the start of the line drawing
     */
    public void addDash(float[] dashary, float phase)
    {
        PDFChangeStrokeCmd sc = new PDFChangeStrokeCmd();
        
        sc.setDash(dashary, phase);
        
        addCommand(sc);
    }
    
    /**
     * set the current path
     * @param path the path
     * @param style the style: PDFShapeCmd.STROKE, PDFShapeCmd.FILL,
     * PDFShapeCmd.BOTH, PDFShapeCmd.CLIP, or some combination.
     */
    public void addPath(Geometry path, int style)
    {
        addCommand(new PDFShapeCmd(path, style));
    }
    
    /**
     * set the fill paint
     */
    public void addFillPaint(PDFPaint p)
    {
        addCommand(new PDFFillPaintCmd(p));
    }
    
    /** set the stroke paint */
    public void addStrokePaint(PDFPaint p)
    {
        addCommand(new PDFStrokePaintCmd(p));
    }
    
    /**
     * set the fill alpha
     */
    public void addFillAlpha(float a)
    {
        addCommand(new PDFFillAlphaCmd(a));
    }
    
    /** set the stroke alpha */
    public void addStrokeAlpha(float a)
    {
        addCommand(new PDFStrokeAlphaCmd(a));
    }
    
    /**
     * draw an image
     * @param image the image to draw
     */
    public void addImage(PDFImage image)
    {
        addCommand(new PDFImageCmd(image));
    }
    
    /**
     * Notify all images we know about that a command has been added
     */
    public void updateImages()
    {
        for (Enumeration i = renderers.elements(); i.hasMoreElements();)
        {
            WeakReference ref = (WeakReference)i.nextElement();
            PDFRenderer renderer = (PDFRenderer)ref.get();
            
            if (renderer != null)
            {
                if (renderer.getStatus() == Watchable.NEEDS_DATA)
                {
                    // there are watchers.  Set the state to paused and
                    // let the watcher decide when to start.
                    renderer.setStatus(Watchable.PAUSED);
                }
            }
        }
    }
}

/**
 * draw an image
 */
class PDFImageCmd extends PDFCmd
{
    PDFImage image;
    
    public PDFImageCmd(PDFImage image)
    {
        this.image = image;
    }
    
    public XYRectFloat execute(PDFRenderer state)
    {
        return state.drawImage(image);
    }
}

/**
 * set the fill paint
 */
class PDFFillPaintCmd extends PDFCmd
{
    PDFPaint p;
    
    public PDFFillPaintCmd(PDFPaint p)
    {
        this.p = p;
    }
    
    public XYRectFloat execute(PDFRenderer state)
    {
        state.setFillPaint(p);
        return null;
    }
}

/**
 * set the stroke paint
 */
class PDFStrokePaintCmd extends PDFCmd
{
    PDFPaint p;
    
    public PDFStrokePaintCmd(PDFPaint p)
    {
        this.p = p;
    }
    
    public XYRectFloat execute(PDFRenderer state)
    {
        state.setStrokePaint(p);
        return null;
    }
}

/**
 * set the fill paint
 */
class PDFFillAlphaCmd extends PDFCmd
{
    float a;
    
    public PDFFillAlphaCmd(float a)
    {
        this.a = a;
    }
    
    public XYRectFloat execute(PDFRenderer state)
    {
        state.setFillAlpha(a);
        return null;
    }
}

/**
 * set the stroke paint
 */
class PDFStrokeAlphaCmd extends PDFCmd
{
    float a;
    
    public PDFStrokeAlphaCmd(float a)
    {
        this.a = a;
    }

    public XYRectFloat execute(PDFRenderer state)
    {
        state.setStrokeAlpha(a);
        return null;
    }
}

/**
 * push the graphics state
 */
class PDFPushCmd extends PDFCmd
{
    public XYRectFloat execute(PDFRenderer state)
    {
        state.push();
        return null;
    }
}

/**
 * pop the graphics state
 */
class PDFPopCmd extends PDFCmd
{
    public XYRectFloat execute(PDFRenderer state)
    {
        state.pop();
        return null;
    }
}

/**
 * concatenate a transform to the graphics state
 */
class PDFXformCmd extends PDFCmd
{
	AffineTransform at;
    
    public PDFXformCmd(AffineTransform at)
    {
        if (at == null)
        {
            throw new RuntimeException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getString(ResourcesResource.PAGE_NULL_TRANSFORM));
        }
        this.at = at;
    }
    
    public XYRectFloat execute(PDFRenderer state)
    {
        state.transform(at);
        return null;
    }
    
    public String toString(PDFRenderer state)
    {
        return "PDFXformCmd: " + at;
    }
    
    public String getDetails()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("PDFXformCommand: \n");
        buf.append(at.toString());
        
        return buf.toString();
    }
}

/**
 * change the stroke style
 */
class PDFChangeStrokeCmd extends PDFCmd
{
    float w, limit, phase;
    int cap, join;
    float[] ary;
    
    public PDFChangeStrokeCmd()
    {
        this.w = PDFRenderer.NOWIDTH;
        this.cap = PDFRenderer.NOCAP;
        this.join = PDFRenderer.NOJOIN;
        this.limit = PDFRenderer.NOLIMIT;
        this.ary = PDFRenderer.NODASH;
        this.phase = PDFRenderer.NOPHASE;
    }
    
    /**
     * set the width of the stroke. Rendering needs to account for a minimum
     * stroke width in creating the output.
     *
     * @param w float
     */
    public void setWidth(float w)
    {
        this.w = w;
    }
    
    public void setEndCap(int cap)
    {
        this.cap = cap;
    }
    
    public void setLineJoin(int join)
    {
        this.join = join;
    }
    
    public void setMiterLimit(float limit)
    {
        this.limit = limit;
    }
    
    public void setDash(float[] ary, float phase)
    {
        if (ary != null)
        {
            // make sure no pairs start with 0, since having no opaque
            // region doesn't make any sense.
        	int len = ary.length - 1;
            for (int i = 0; i < len; i += 2)
            {
                if (ary[i] == 0)
                {
                    /* Give a very small value, since 0 messes java up */
                    ary[i] = 0.00001f;
                    break;
                }
            }
        }
        this.ary = ary;
        this.phase = phase;
    }
    
    public XYRectFloat execute(PDFRenderer state)
    {
        state.setStrokeParts(w, cap, join, limit, ary, phase);
        return null;
    }
    
    public String toString(PDFRenderer state)
    {
        return "STROKE: w=" + w + " cap=" + cap + " join=" + join + " limit=" + limit + " ary=" + ary + " phase=" + phase;
    }
}
