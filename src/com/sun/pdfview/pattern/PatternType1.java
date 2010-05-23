/*
 * File: PatternType1.java
 * Version: 1.3
 * Initial Creation: May 14, 2010 7:24:30 AM
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
package com.sun.pdfview.pattern;

import java.io.IOException;
import java.util.Hashtable;

import net.rim.device.api.math.Matrix4f;
import net.rim.device.api.math.Vector3f;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.XYRect;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.PDFParser;
import com.sun.pdfview.PDFRenderer;
import com.sun.pdfview.helper.ColorSpace;
import com.sun.pdfview.helper.PDFGraphics;
import com.sun.pdfview.helper.PDFUtil;
import com.sun.pdfview.helper.XYPointFloat;
import com.sun.pdfview.helper.XYRectFloat;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.Paint;
import com.sun.pdfview.helper.graphics.PaintGenerator;
import com.sun.pdfview.helper.graphics.TranslatedBitmap;

/**
 * A type 1 (tiling) pattern
 */
public class PatternType1 extends PDFPattern
{
	/** paint types */
    public static final int PAINT_COLORED = 1;
    public static final int PAINT_UNCOLORED = 2;
    
    /** tiling types */
    public static final int TILE_CONSTANT = 1;
    public static final int TILE_NODISTORT = 2;
    public static final int TILE_FASTER = 3;
    
    /** the resources used by the image we will tile */
    private Hashtable resources;
    
    /** the paint type (colored or uncolored) */
    private int paintType;
    
    /** the tiling type (constant, no distort or faster) */
    private int tilingType;
    
    /** the bounding box of the tile, in tile space */
    private XYRectFloat bbox;
    
    /** the horiztonal tile spacing, in tile space */
    private int xStep;
    
    /** the vertical spacing, in tile space */
    private int yStep;
    
    /** the stream data */
    private byte[] data;
    
    /** Creates a new instance of PatternType1 */
    public PatternType1()
    {
        super(1);    
    }
	
    /**
     * Parse the pattern from the PDFObject
     *
     * Note the resources passed in are ignored...
     */
    protected void parse(PDFObject patternObj, Hashtable rsrc) throws IOException
    {
        data = patternObj.getStream();
        
        resources = patternObj.getDictRef("Resources").getDictionary();
        paintType = patternObj.getDictRef("PaintType").getIntValue();
        tilingType = patternObj.getDictRef("TilingType").getIntValue();
        
        PDFObject bboxObj = patternObj.getDictRef("BBox");
        bbox = new XYRectFloat(bboxObj.getAt(0).getFloatValue(),
                               bboxObj.getAt(1).getFloatValue(),
                               bboxObj.getAt(2).getFloatValue(),
                               bboxObj.getAt(3).getFloatValue());
        
        xStep = patternObj.getDictRef("XStep").getIntValue();
        yStep = patternObj.getDictRef("YStep").getIntValue();
    }
    
    /** 
     * Create a PDFPaint from this pattern and set of components.  
     * This creates a buffered image of this pattern using
     * the given paint, then uses that image to create the correct 
     * TexturePaint to use in the PDFPaint.
     *
     * @param basePaint the base paint to use, or null if not needed
     */
    public PDFPaint getPaint(PDFPaint basePaint)
    {
        // create the outline of the pattern in user space by creating
        // a box with width xstep and height ystep.  Transform that
        // box using the pattern's matrix to get the user space
        // bounding box
        XYRectFloat anchor = new XYRectFloat(getBBox().x,
                                             getBBox().y,
                                             getXStep(),
                                             getYStep());
        //anchor = getTransform().createTransformedShape(anchor).getBounds2D();
        
        // now create a page bounded by the pattern's user space size
        final PDFPage page = new PDFPage(getBBox(), 0);
        
        // set the base paint if there is one
        if (basePaint != null)
        {
            page.addFillPaint(basePaint);
            page.addStrokePaint(basePaint);
        }
        
        // undo the page's transform to user space
        /*
        Matrix4f xform = new Matrix4f(PDFUtil.affine2TransformMatrix(new float[]{1, 0, 0, -1, 0, getYStep()}));
        				//new Matrix4f(PDFUtil.affine2TransformMatrix(new float[]{1, 0, 0, -1, 0, getBBox().getHeight()}));
        page.addXform(xform);
        */
        
        // now parse the pattern contents
        PDFParser prc = new PDFParser(page, data, getResources());
        prc.go(true);
        
        int width = (int)getBBox().width;
        int height = (int)getBBox().height;
        
        // get actual image
        Paint paint = new Paint()
        {
            public PaintGenerator createGenerator(Matrix4f xform) 
            {
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                
                //Maybe to reduce memory usage, don't create a new Geometry object and instead create a helper function that will transform the Box and directly return the bounds.
                XYRectFloat devBBox = new Geometry(getBBox()).createTransformedShape(xform).getBounds2D();
                
                Vector3f steps = new Vector3f(getXStep(), getYStep(), 0);
                Matrix4f xformNoTrans = new Matrix4f(xform);
                //Get rid of translation. In J2SE's AffineMatrix the difference between transform and deltaTransform is deltaTransform doesn't use the translation component.
                xformNoTrans.set(0, 3, 0);
                xformNoTrans.set(1, 3, 0);
                xformNoTrans.set(2, 3, 0);
                xformNoTrans.transformPoint(steps);
                
                int width = (int)Math.ceil(devBBox.width);
                int height = (int)Math.ceil(devBBox.height);
                
                Bitmap img = page.getImage(width, height, null, false, true);
                
                return new Type1PaintContext(cs, devBBox, steps.x, steps.y, img);
            }
            
            public int getTransparency()
            {
                return Paint.TRANSPARENCY_TRANSLUCENT;
            }

			public int getColor()
			{
				return Color.BLACK;
			}
        };
        
        return new TilingPatternPaint(paint, this);
    }
    
    /** get the associated resources */
    public Hashtable getResources()
    {
        return resources;
    }
    
    /** get the paint type */
    public int getPaintType()
    {
        return paintType;
    }
    
    /** get the tiling type */
    public int getTilingType()
    {
        return tilingType;
    }
    
    /** get the bounding box */
    public XYRectFloat getBBox()
    {
        return bbox;
    }
    
    /** get the x step */
    public int getXStep()
    {
        return xStep;
    }
    
    /** get the y step */
    public int getYStep()
    {
        return yStep;
    }
    
    /** 
     * This class overrides PDFPaint to paint in the pattern coordinate space
     */
    class TilingPatternPaint extends PDFPaint
    {
    	/** the pattern to paint */
        private PatternType1 pattern;
        
        /** Create a tiling pattern paint */
        public TilingPatternPaint(Paint paint, PatternType1 pattern)
        {
            super(paint);
            
            this.pattern = pattern;
        }
        
        /**
         * fill a path with the paint, and record the dirty area.
         * @param state the current graphics state
         * @param g the graphics into which to draw
         * @param s the path to fill
         * @param drawn a Rectangle2D into which the dirty area (area drawn)
         * will be added.
         */
        public XYRectFloat fill(PDFRenderer state, PDFGraphics g, Geometry s)
        {
        	// first transform s into device space
            Matrix4f at = g.getTransform();
            Geometry xformed = s.createTransformedShape(at);
            
            // push the graphics state so we can restore it
            state.push();
            
            // set the transform to be the inital transform concatentated
            // with the pattern matrix
            state.setTransform(state.getInitialTransform());
            state.transform(pattern.getTransform());
            
            // now figure out where the shape should be
            Matrix4f mat = state.getTransform();
            if(!mat.invert(at))
            {
            	// oh well (?)
            }
            xformed.transform(at);
            
            // set the paint and draw the xformed shape
            g.setComposite(Composite.getInstance(Composite.SRC_OVER));
            g.setPaint(getPaint());
            g.fill(xformed);
            
            // restore the graphics state
            state.pop();
            
            // return the area changed
            return s.createTransformedShape(g.getTransform()).getBounds2D();
        }
    }
    
    /** 
     * A simple paint context that uses an existing raster in device
     * space to generate pixels
     */
    class Type1PaintContext extends PaintGenerator
    {
        /** the color space */
        private ColorSpace colorSpace;
        
        /** the anchor box */
        private XYRectFloat bbox;
        
        /** the x offset */
        private float xstep;
        
        /** the y offset */
        private float ystep;
        
        /** the image data, as a raster in device coordinates */
        private Bitmap data;
        
        /**
         * Create a paint context
         */
        Type1PaintContext(ColorSpace colorSpace, XYRectFloat bbox, float xstep, float ystep, Bitmap data) 
        {
            //this.colorSpace = colorSpace;
        	this.colorSpace = null;
            this.bbox = bbox;
            this.xstep = xstep;
            this.ystep = ystep;
            this.data = data;
        }
        
        public void dispose()
        {
        	//colorSpace = null;
            bbox = null;
            data = null;
        }
        
        public ColorSpace getColorSpace()
        {
            return colorSpace;
        }
        
        public TranslatedBitmap getBitmap(int x, int y, int w, int h)
        {
            //ColorSpace cs = getColorSpace();
            
            //int numComponents = cs.getNumComponents(); //This is not needed because the whole pixel is stored 0xAARRGGBB
            
            // all the data, plus alpha channel
            int[] imgData = new int[w * h /* * (numComponents + 1)*/];
            
            // the x and y step, as ints	
            int useXStep = (int)Math.abs(Math.ceil(xstep));
            int useYStep = (int)Math.abs(Math.ceil(ystep));
            
            // a completely transparent pixel (alpha of 0)
            int[] emptyPixel = new int[/*numComponents + */1];
            int[] usePixel = new int[/*numComponents + */1];
            
            int width = data.getWidth();
            int height = data.getHeight();
            
            // for each device coordinate
            for (int j = 0; j < h; j++)
            {
                for (int i = 0; i < w; i ++)
                {
                    // figure out what pixel we are at relative to the image
                    int xloc = (x + i) - (int)Math.ceil(bbox.x);
                    int yloc = (y + j) - (int)Math.ceil(bbox.y);
                    
                    xloc %= useXStep;
                    yloc %= useYStep;
                    
                    if (xloc < 0)
                    {
                        xloc = useXStep + xloc;
                    }
                    if (yloc < 0)
                    {
                        yloc = useYStep + yloc;
                    }
                    
                    int[] pixel = emptyPixel;
                    
                    // check if we are inside the image
                    if (xloc < width && yloc < height)
                    {
                    	data.getARGB(usePixel, 0, width, xloc, yloc, 1, 1);
                    }
                    
                    int base = (j * w + i)/* * (numComponents + 1)*/;
                    int len = pixel.length;
                    for (int c = 0; c < len; c++)
                    {
                        imgData[base + c] = pixel[c];
                    }
                }
            }
            
            Bitmap raster = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, w, h);
            raster.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
            raster.setARGB(imgData, 0, w, 0, 0, w, h);
            
            TranslatedBitmap child = new TranslatedBitmap(raster, x, y);
            
            return child;
        }
    }
}
