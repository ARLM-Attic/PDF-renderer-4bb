//#preprocessor

/*
 * File: BasicStroke.java
 * Version: 1.0
 * Initial Creation: May 22, 2010 10:19:08 AM
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Denis M. Kishenko
 */
package com.sun.pdfview.helper.graphics;

//#ifndef BlackBerrySDK4.5.0
import net.rim.device.api.util.MathUtilities;
//#else
import com.sun.pdfview.helper.PDFUtil;
//#endif

/**
 * Partial implementation of java.awt.BasicStroke.
 * @author Denis M. Kishenko, Vincent Simonetti
 */
public class BasicStroke
{
	/**
	 * Ends unclosed subpaths and dash segments with no added decoration.
	 */
	public static final int CAP_BUTT = 0;
	/**
	 * Ends unclosed subpaths and dash segments with a round decoration that has a radius equal to half of the width of the pen.
	 */
	public static final int CAP_ROUND = 1;
	/**
	 * Ends unclosed subpaths and dash segments with a square projection that extends beyond the end of the segment to a distance equal to half of the line width.
	 */
	public static final int CAP_SQUARE = 2;
	
	/**
	 * Joins path segments by extending their outside edges until they meet.
	 */
	public static final int JOIN_MITER = 0;
	/**
	 * Joins path segments by rounding off the corner at a radius of half the line width.
	 */
	public static final int JOIN_ROUND = 1;
	/**
	 * Joins path segments by connecting the outer corners of their wide outlines with a straight segment.
	 */
	public static final int JOIN_BEVEL = 2;
	
	/*
	 * Constants for calculating 
	 */
	static final int MAX_LEVEL = 20;        // Maximal deepness of curve subdivision
	static final float CURVE_DELTA = 2;  // Width tolerance
	static final float CORNER_ANGLE = 4; // Minimum corner angel
	static final float CORNER_ZERO = 0.01f; // Zero angle
	static final float CUBIC_ARC = 4f / 3f * ((float)Math.sqrt(2f) - 1f);
	static final float DIV_BY_2 = 1f / 2f;
	
	/**
     * Stroke width
     */
    float width;
    
    /**
     * Stroke cap type
     */
    int cap;
    
    /**
     * Stroke join type
     */
    int join;
    
    /**
     * Stroke miter limit
     */
    float miterLimit;
    
    /**
     * Stroke dashes array
     */
    float[] dash;
    
    /**
     * Stroke dash phase
     */
    float dashPhase;
    
    /**
     * The temporary pre-calculated values
     */
    float curveDelta;
    float cornerDelta;
    float zeroDelta;
    
    float w2;
    float fmx, fmy;
    float scx, scy, smx, smy;
    float mx, my, cx, cy;
    
    /**
     * The temporary indicators
     */
    boolean isMove;
    boolean isFirst;
    boolean checkMove;
    
    /**
     * The temporary and destination work paths
     */
    BufferedPath dst, lp, rp, sp;
    
    /**
     * Stroke dasher class 
     */
    Dasher dasher;
	
	/**
	 * Constructs a new BasicStroke with defaults for all attributes.
	 */
	public BasicStroke()
	{
		this(1, CAP_SQUARE, JOIN_MITER, 10, null, 0);
	}
	
	/**
	 * Constructs a new BasicStroke with the specified attributes.
	 * @param width The width of this BasicStroke. The width must be greater than or equal to 0.0f. If width is set to 0.0f, the stroke is rendered as the thinnest possible line for the target device and the antialias hint setting.
	 * @param cap The decoration of the ends of a BasicStroke.
	 * @param join The decoration applied where path segments meet.
	 * @param miterLimit The limit to trim the miter join. The miterlimit must be greater than or equal to 1.0f.
	 * @param dash The array representing the dashing pattern.
	 * @param dashPhase The offset to start the dashing pattern.
	 * @throws IllegalArgumentException If width is negative.
	 * @throws IllegalArgumentException If cap is not either CAP_BUTT, CAP_ROUND or CAP_SQUARE.
	 * @throws IllegalArgumentException If miterlimit is less than 1 and join is JOIN_MITER.
	 * @throws IllegalArgumentException If join is not either JOIN_ROUND, JOIN_BEVEL, or JOIN_MITER.
	 * @throws IllegalArgumentException If dash_phase is negative and dash is not null.
	 * @throws IllegalArgumentException If the length of dash is zero.
	 * @throws IllegalArgumentException If dash lengths are all zero.
	 */
	public BasicStroke(float width, int cap, int join, float miterLimit, float[] dash, float dashPhase)
	{
		if (width < 0.0f)
		{
            throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_STROKE_NEG_WIDTH));
        }
        if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE)
        {
            throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_STROKE_ILLEGAL_CAP));
        }
        if (join != JOIN_MITER && join != JOIN_ROUND && join != JOIN_BEVEL)
        {
            throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_STROKE_ILLEGAL_JOIN));
        }
        if (join == JOIN_MITER && miterLimit < 1.0f)
        {
            throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_STROKE_MITER_LIM_LS_1));
        }
        if (dash != null)
        {
        	if (dashPhase < 0.0f)
            {
                throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_STROKE_NEG_DASH_PHASE));
            }
            if (dash.length == 0)
            {
                throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_STROKE_0_DASH_LEN));
            }
            ZERO:
            {
            	int len = dash.length;
                for(int i = 0; i < len; i++)
                {
                    if (dash[i] < 0.0)
                    {
                        throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_STROKE_NEG_DASH) + i + ']');
                    }
                    if (dash[i] > 0.0)
                    {
                        break ZERO;
                    }
                }
                throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_STROKE_DASH_LEN_ZERO));
            }
        }
        this.width = width;
        this.cap = cap;
        this.join = join;
        this.miterLimit = miterLimit;
        this.dash = dash;
        this.dashPhase = dashPhase;
	}
	
	/**
	 * Constructs a solid BasicStroke with the specified attributes.
	 * @param width The width of the BasicStroke.
	 * @param cap The decoration of the ends of a BasicStroke.
	 * @param join The decoration applied where path segments meet.
	 * @param miterLimit The limit to trim the miter join.
	 * @throws IllegalArgumentException If width is negative.
	 * @throws IllegalArgumentException If cap is not either CAP_BUTT, CAP_ROUND or CAP_SQUARE.
	 * @throws IllegalArgumentException If miterlimit is less than 1 and join is JOIN_MITER.
	 * @throws IllegalArgumentException If join is not either JOIN_ROUND, JOIN_BEVEL, or JOIN_MITER.
	 */
	public BasicStroke(float width, int cap, int join, float miterLimit)
	{
        this(width, cap, join, miterLimit, null, 0);
    }
	
	/**
	 * Constructs a solid BasicStroke with the specified attributes.
	 * @param width The width of the BasicStroke.
	 * @param cap The decoration of the ends of a BasicStroke.
	 * @param join The decoration applied where path segments meet.
	 * @throws IllegalArgumentException If width is negative.
	 * @throws IllegalArgumentException If cap is not either CAP_BUTT, CAP_ROUND or CAP_SQUARE.
	 * @throws IllegalArgumentException If join is not either JOIN_ROUND, JOIN_BEVEL, or JOIN_MITER.
	 */
	public BasicStroke(float width, int cap, int join)
	{
        this(width, cap, join, 10, null, 0);
    }
	
	/**
	 * Returns the line width.
	 * @return The line width of this BasicStroke.
	 */
	public float getLineWidth()
	{
        return width;
    }
	
	/**
	 * Returns the end cap style.
	 * @return The end cap style of this BasicStroke as one of the static int values that define possible end cap styles.
	 */
    public int getEndCap()
    {
        return cap;
    }
    
    /**
     * Returns the line join style.
     * @return The line join style of the BasicStroke as one of the static int values that define possible line join styles.
     */
    public int getLineJoin()
    {
        return join;
    }
    
    /**
     * Returns the limit of miter joins.
     * @return The limit of miter joins of the BasicStroke.
     */
    public float getMiterLimit()
    {
        return miterLimit;
    }
    
    /**
     * Returns the array representing the lengths of the dash segments.
     * @return The dash array.
     */
    public float[] getDashArray()
    {
        return dash;
    }
    
    /**
     * Returns the current dash phase.
     * @return The dash phase as a float value.
     */
    public float getDashPhase()
    {
        return dashPhase;
    }
    
    /**
     * Tests if a specified object is equal to this BasicStroke by first testing if it is a BasicStroke and then comparing its width, join, cap, miter limit, dash, and 
     * dash phase attributes with those of this BasicStroke.
     * @param obj The specified object to compare to this BasicStroke.
     * @return true if the width, join, cap, miter limit, dash, and dash phase are the same for both objects; false otherwise.
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (obj instanceof BasicStroke)
        {
            BasicStroke bs = (BasicStroke)obj;
            if(bs.width == width &&
                bs.cap == cap &&
                bs.join == join &&
                bs.miterLimit == miterLimit &&
                bs.dashPhase == dashPhase &&
                bs.dash.length == dash.length)
            {
            	int len = dash.length;
            	for(int i = 0; i < len; i++)
            	{
            		if(bs.dash[i] != dash[i])
            		{
            			return false;
            		}
            	}
            	return true;
            }
        }
        return false;
    }
    
    /**
     * Calculates allowable curve derivation 
     */
    float getCurveDelta(float width)
    {
    	float a = width + CURVE_DELTA;
    	float cos = 1f - 2f * width * width / (a * a);
    	float sin = (float)Math.sqrt(1f - cos * cos);
        return Math.abs(sin / cos);
    }
    
    /**
     * Calculates value to detect small angle 
     */
    float getCornerDelta(float width)
    {
        return width * width * (float)Math.sin(Math.PI * CORNER_ANGLE / 180f);
    }
    
    /**
     * Calculates value to detect zero angle 
     */
    float getZeroDelta(float width)
    {
        return width * width * (float)Math.sin(Math.PI * CORNER_ZERO / 180f);
    }
    
    /**
     * Returns a Geometry whose interior defines the stroked outline of a specified Geometry.
     * @param geo The Geometry boundary be stroked.
     * @return The Geometry of the stroked outline.
     */
    public Geometry createStrokedGeometry(Geometry geo)
    {
    	w2 = width * DIV_BY_2;
        curveDelta = getCurveDelta(w2);
        cornerDelta = getCornerDelta(w2);
        zeroDelta = getZeroDelta(w2);
        
        dst = new BufferedPath();
        lp = new BufferedPath();
        rp = new BufferedPath();
        
        Geometry.Enumeration en = geo.getPathEnumerator(null);
        if (dash == null)
        {
            createSolidShape(en);
        }
        else
        {
            createDashedShape(en);
        }
        
    	return dst.createGeometry();
    }
    
    /**
     * Generates solid stroked shape without dash
     * @param p - the Geometry.Enumeration of source shape
     */
    void createSolidShape(Geometry.Enumeration p)
    {
        float[] coords = new float[6];
        mx = my = cx = cy = 0;
        isMove = false;
        isFirst = false;
        checkMove = true;
        boolean isClosed = true;
        
        while(!p.isDone())
        {
            switch(p.currentSegment(coords))
            {
	            case Geometry.Enumeration.SEG_MOVETO:
	                if (!isClosed)
	                {
	                    closeSolidShape();
	                }
	                rp.clean();
	                mx = cx = coords[0];
	                my = cy = coords[1];
	                isMove = true;
	                isClosed = false;
	                break;
	            case Geometry.Enumeration.SEG_LINETO:
	                addLine(cx, cy, cx = coords[0], cy = coords[1], true);
	                break;
	            case Geometry.Enumeration.SEG_QUADTO:
	                addQuad(cx, cy, coords[0], coords[1], cx = coords[2], cy = coords[3]);
	                break;
	            case Geometry.Enumeration.SEG_CUBICTO:
	                addCubic(cx, cy, coords[0], coords[1], coords[2], coords[3], cx = coords[4], cy = coords[5]);
	                break;
	            case Geometry.Enumeration.SEG_CLOSE:
	                addLine(cx, cy, mx, my, false);
	                addJoin(lp, mx, my, lp.xMove, lp.yMove, true);
	                addJoin(rp, mx, my, rp.xMove, rp.yMove, false);
	                lp.closePath();
	                rp.closePath();
	                lp.appendReverse(rp);
	                isClosed = true;
	                break;
            }
            p.next();
        }
        if (!isClosed)
        {
            closeSolidShape();
        }
        
        dst = lp;
    }
    
    /**
     * Closes solid shape path
     */
    void closeSolidShape()
    {
        addCap(lp, cx, cy, rp.xLast, rp.yLast);
        lp.combine(rp);
        addCap(lp, mx, my, lp.xMove, lp.yMove);
        lp.closePath();
    }
    
    /**
     * Generates dashed stroked shape
     * @param p - the Geometry.Enumeration of source shape
     */
    void createDashedShape(Geometry.Enumeration p)
    {
        float[] coords = new float[6];
        mx = my = cx = cy = 0;
        smx = smy = scx = scy = 0;
        isMove = false;
        checkMove = false;
        boolean isClosed = true;
        
        while(!p.isDone())
        {
            switch(p.currentSegment(coords))
            {
	            case Geometry.Enumeration.SEG_MOVETO:
	                if (!isClosed)
	                {
	                    closeDashedShape();
	                }
	                
	                dasher = new Dasher(dash, dashPhase);
	                lp.clean();
	                rp.clean();
	                sp = null;
	                isFirst = true;
	                isMove = true;
	                isClosed = false;
	                mx = cx = coords[0];
	                my = cy = coords[1];
	                break;
	            case Geometry.Enumeration.SEG_LINETO:
	                addDashLine(cx, cy, cx = coords[0], cy = coords[1]);
	                break;
	            case Geometry.Enumeration.SEG_QUADTO:
	                addDashQuad(cx, cy, coords[0], coords[1], cx = coords[2], cy = coords[3]);
	                break;
	            case Geometry.Enumeration.SEG_CUBICTO:
	                addDashCubic(cx, cy, coords[0], coords[1], coords[2], coords[3], cx = coords[4], cy = coords[5]);
	                break;
	            case Geometry.Enumeration.SEG_CLOSE:
	                addDashLine(cx, cy, cx = mx, cy = my);
	                
	                if (dasher.isConnected())
	                {
	                    // Connect current and head segments
	                    addJoin(lp, fmx, fmy, sp.xMove, sp.yMove, true);
	                    lp.join(sp);
	                    addJoin(lp, fmx, fmy, rp.xLast, rp.yLast, true);
	                    lp.combine(rp);
	                    addCap(lp, smx, smy, lp.xMove, lp.yMove);
	                    lp.closePath();
	                    dst.append(lp);
	                    sp = null;
	                }
	                else
	                {
	                    closeDashedShape();
	                }
	                
	                isClosed = true;
	                break;
            }
            p.next();
        }
        
        if (!isClosed)
        {
            closeDashedShape();
        }
    }
    
    /**
     * Closes dashed shape path
     */
    void closeDashedShape()
    {
        // Add head segment
        if (sp != null)
        {
            addCap(sp, fmx, fmy, sp.xMove, sp.yMove);
            sp.closePath();
            dst.append(sp);
        }
        if (lp.typeSize > 0)
        {
            // Close current segment
            if (!dasher.isClosed())
            {
                addCap(lp, scx, scy, rp.xLast, rp.yLast);
                lp.combine(rp);
                addCap(lp, smx, smy, lp.xMove, lp.yMove);
                lp.closePath();
            }
            dst.append(lp);
        }
    }
    
    /**
     * Adds cap to the work path 
     * @param p - the BufferedPath object of work path
     * @param x0 - the x coordinate of the source path 
     * @param y0 - the y coordinate on the source path
     * @param x2 - the x coordinate of the next point on the work path 
     * @param y2 - the y coordinate of the next point on the work path
     */
    void addCap(BufferedPath p, float x0, float y0, float x2, float y2)
    {
    	float x1 = p.xLast;
    	float y1 = p.yLast;
        float x10 = x1 - x0;
        float y10 = y1 - y0;
        float x20 = x2 - x0;
        float y20 = y2 - y0;
        
        switch(cap)
        {
	        case CAP_BUTT:
	            p.lineTo(x2, y2);
	            break;
	        case CAP_ROUND:
	        	float mx = x10 * CUBIC_ARC;
	        	float my = y10 * CUBIC_ARC;
	            
	        	float x3 = x0 + y10;
	        	float y3 = y0 - x10;
	            
	            x10 *= CUBIC_ARC;
	            y10 *= CUBIC_ARC;
	            x20 *= CUBIC_ARC;
	            y20 *= CUBIC_ARC;
	            
	            p.cubicTo(x1 + y10, y1 - x10, x3 + mx, y3 + my, x3, y3);
	            p.cubicTo(x3 - mx, y3 - my, x2 - y20, y2 + x20, x2, y2);
	            break;
	        case CAP_SQUARE:
	            p.lineTo(x1 + y10, y1 - x10);
	            p.lineTo(x2 - y20, y2 + x20);
	            p.lineTo(x2, y2);
	            break;
        }
    }
    
    /**
     * Adds bevel and miter join to the work path 
     * @param p - the BufferedPath object of work path
     * @param x0 - the x coordinate of the source path 
     * @param y0 - the y coordinate on the source path
     * @param x2 - the x coordinate of the next point on the work path 
     * @param y2 - the y coordinate of the next point on the work path
     * @param isLeft - the orientation of work path, true if work path lies to the left from source path, false otherwise 
     */
    void addJoin(BufferedPath p, float x0, float y0, float x2, float y2, boolean isLeft)
    {
    	float x1 = p.xLast;
    	float y1 = p.yLast;
    	float x10 = x1 - x0;
    	float y10 = y1 - y0;
    	float x20 = x2 - x0;
    	float y20 = y2 - y0;
    	float sin0 = x10 * y20 - y10 * x20;
        
        // Small corner
        if (-cornerDelta < sin0 && sin0 < cornerDelta)
        {
        	float cos0 = x10 * x20 + y10 * y20;
            if (cos0 > 0)
            {
                // if zero corner do nothing
                if (-zeroDelta > sin0 || sin0 > zeroDelta)
                {
                	float x3 = x0 + w2 * w2 * (y20 - y10) / sin0;
                	float y3 = y0 + w2 * w2 * (x10 - x20) / sin0;
                    p.setLast(x3, y3);
                }
                return;
            }
            // Zero corner
            if (-zeroDelta < sin0 && sin0 < zeroDelta)
            {
                p.lineTo(x2, y2);
            }
            return;
        }
        
        if (isLeft ^ (sin0 < 0))
        {
            // Twisted corner
            p.lineTo(x0, y0);
            p.lineTo(x2, y2);
        }
        else
        {
            switch(join)
            {
	            case JOIN_BEVEL:
	                p.lineTo(x2, y2);
	                break;
	            case JOIN_MITER:
	            	float s1 = x1 * x10 + y1 * y10;
	            	float s2 = x2 * x20 + y2 * y20;
	            	float x3 = (s1 * y20 - s2 * y10) / sin0;
	            	float y3 = (s2 * x10 - s1 * x20) / sin0;
	            	float x30 = x3 - x0;
	            	float y30 = y3 - y0;
	            	float miterLength = (float)Math.sqrt(x30 * x30 + y30 * y30);
	                if (miterLength < miterLimit * w2)
	                {
	                    p.lineTo(x3, y3);
	                }
	                p.lineTo(x2, y2);
	                break;
	            case JOIN_ROUND:
	                addRoundJoin(p, x0, y0, x2, y2, isLeft);
	                break;
            }
        }
    }
    
    /**
     * Adds round join to the work path 
     * @param p - the BufferedPath object of work path
     * @param x0 - the x coordinate of the source path 
     * @param y0 - the y coordinate on the source path
     * @param x2 - the x coordinate of the next point on the work path 
     * @param y2 - the y coordinate of the next point on the work path
     * @param isLeft - the orientation of work path, true if work path lies to the left from source path, false otherwise 
     */
    void addRoundJoin(BufferedPath p, float x0, float y0, float x2, float y2, boolean isLeft)
    {
    	float x1 = p.xLast;
    	float y1 = p.yLast;
    	float x10 = x1 - x0;
    	float y10 = y1 - y0;
    	float x20 = x2 - x0;
    	float y20 = y2 - y0;
        
    	float x30 = x10 + x20;
        float y30 = y10 + y20;
        
        float l30 = (float)Math.sqrt(x30 * x30 + y30 * y30);
        
        if (l30 < 1E-5f)
        {
            p.lineTo(x2, y2);
            return;
        }
        
        float w = w2 / l30;
        
        x30 *= w;
        y30 *= w;
        
        float x3 = x0 + x30;
        float y3 = y0 + y30;
        
        float cos = x10 * x20 + y10 * y20;
//#ifdef BlackBerrySDK4.5.0
        float a = (float)PDFUtil.acos(cos / (w2 * w2));
//#else
        float a = (float)MathUtilities.acos(cos / (w2 * w2));
//#endif
        if (cos >= 0.0)
        {
        	float k = 4f / 3f * (float)Math.tan(a / 4f);
            if (isLeft)
            {
                k = -k;
            }
            
            x10 *= k;
            y10 *= k;
            x20 *= k;
            y20 *= k;
            
            p.cubicTo(x1 - y10, y1 + x10, x2 + y20, y2 - x20, x2, y2);
        }
        else
        {
        	float k = 4f / 3f * (float)Math.tan(a / 8f);
            if (isLeft)
            {
                k = -k;
            }
            
            x10 *= k;
            y10 *= k;
            x20 *= k;
            y20 *= k;
            x30 *= k;
            y30 *= k;
            
            p.cubicTo(x1 - y10, y1 + x10, x3 + y30, y3 - x30, x3, y3);
            p.cubicTo(x3 - y30, y3 + x30, x2 + y20, y2 - x20, x2, y2);
        }
    }
    
    /**
     * Adds solid line segment to the work path
     * @param x1 - the x coordinate of the start line point
     * @param y1 - the y coordinate of the start line point
     * @param x2 - the x coordinate of the end line point
     * @param y2 - the y coordinate of the end line point
     * @param zero - if true it's allowable to add zero length line segment
     */
    void addLine(float x1, float y1, float x2, float y2, boolean zero)
    {
    	float dx = x2 - x1;
    	float dy = y2 - y1;
        
        if (dx == 0 && dy == 0)
        {
            if (!zero)
            {
                return;
            }
            dx = w2;
            dy = 0;
        }
        else
        {
        	float w = w2 / (float)Math.sqrt(dx * dx + dy * dy);
            dx *= w;
            dy *= w;
        }
        
        float lx1 = x1 - dy;
        float ly1 = y1 + dx;
        float rx1 = x1 + dy;
        float ry1 = y1 - dx;
        
        if (checkMove)
        {
            if (isMove)
            {
                isMove = false;
                lp.moveTo(lx1, ly1);
                rp.moveTo(rx1, ry1);
            }
            else
            {
                addJoin(lp, x1, y1, lx1, ly1, true);
                addJoin(rp, x1, y1, rx1, ry1, false);
            }
        }
        
        lp.lineTo(x2 - dy, y2 + dx);
        rp.lineTo(x2 + dy, y2 - dx);
    }
    
    /**
     * Adds solid quad segment to the work path
     * @param x1 - the x coordinate of the first control point
     * @param y1 - the y coordinate of the first control point
     * @param x2 - the x coordinate of the second control point
     * @param y2 - the y coordinate of the second control point
     * @param x3 - the x coordinate of the third control point
     * @param y3 - the y coordinate of the third control point
     */
    void addQuad(float x1, float y1, float x2, float y2, float x3, float y3)
    {
    	float x21 = x2 - x1;
    	float y21 = y2 - y1;
    	float x23 = x2 - x3;
    	float y23 = y2 - y3;
        
    	float l21 = (float)Math.sqrt(x21 * x21 + y21 * y21);
    	float l23 = (float)Math.sqrt(x23 * x23 + y23 * y23);
        
        if (l21 == 0 && l23 == 0)
        {
            addLine(x1, y1, x3, y3, false);
            return;
        }
        
        if (l21 == 0)
        {
            addLine(x2, y2, x3, y3, false);
            return;
        }
        
        if (l23 == 0)
        {
            addLine(x1, y1, x2, y2, false);
            return;
        }
        
        float w;
        w = w2 / l21;
        float mx1 = - y21 * w;
        float my1 =   x21 * w;
        w = w2 / l23;
        float mx3 =   y23 * w;
        float my3 = - x23 * w;
        
        float lx1 = x1 + mx1;
        float ly1 = y1 + my1;
        float rx1 = x1 - mx1;
        float ry1 = y1 - my1;
        
        if (checkMove)
        {
            if (isMove)
            {
                isMove = false;
                lp.moveTo(lx1, ly1);
                rp.moveTo(rx1, ry1);
            }
            else
            {
                addJoin(lp, x1, y1, lx1, ly1, true);
                addJoin(rp, x1, y1, rx1, ry1, false);
            }
        }
        
        if (x21 * y23 - y21 * x23 == 0)
        {
            // On line curve
            if (x21 * x23 + y21 * y23 > 0)
            {
                // Twisted curve
                if (l21 == l23)
                {
                	float px = x1 + (x21 + x23) / 4f;
                	float py = y1 + (y21 + y23) / 4f;
                    lp.lineTo(px + mx1, py + my1);
                    rp.lineTo(px - mx1, py - my1);
                    lp.lineTo(px - mx1, py - my1);
                    rp.lineTo(px + mx1, py + my1);
                    lp.lineTo(x3 - mx1, y3 - my1);
                    rp.lineTo(x3 + mx1, y3 + my1);
                }
                else
                {
                	float px1, py1;
                	float k = l21 / (l21 + l23);
                	float px = x1 + (x21 + x23) * k * k;
                	float py = y1 + (y21 + y23) * k * k;
                    px1 = (x1 + px) * DIV_BY_2;
                    py1 = (y1 + py) * DIV_BY_2;
                    lp.quadTo(px1 + mx1, py1 + my1, px + mx1, py + my1);
                    rp.quadTo(px1 - mx1, py1 - my1, px - mx1, py - my1);
                    lp.lineTo(px - mx1, py - my1);
                    rp.lineTo(px + mx1, py + my1);
                    px1 = (x3 + px) * DIV_BY_2;
                    py1 = (y3 + py) * DIV_BY_2;
                    lp.quadTo(px1 - mx1, py1 - my1, x3 - mx1, y3 - my1);
                    rp.quadTo(px1 + mx1, py1 + my1, x3 + mx1, y3 + my1);
                }
            }
            else
            {
                // Simple curve
                lp.quadTo(x2 + mx1, y2 + my1, x3 + mx3, y3 + my3);
                rp.quadTo(x2 - mx1, y2 - my1, x3 - mx3, y3 - my3);
            }
        }
        else
        {
            addSubQuad(x1, y1, x2, y2, x3, y3, 0);
        }
    }
    
    /**
     * Subdivides solid quad curve to make outline for source quad segment and adds it to work path
     * @param x1 - the x coordinate of the first control point
     * @param y1 - the y coordinate of the first control point
     * @param x2 - the x coordinate of the second control point
     * @param y2 - the y coordinate of the second control point
     * @param x3 - the x coordinate of the third control point
     * @param y3 - the y coordinate of the third control point
     * @param level - the maximum level of subdivision deepness
     */
    void addSubQuad(float x1, float y1, float x2, float y2, float x3, float y3, int level)
    {
    	float x21 = x2 - x1;
    	float y21 = y2 - y1;
    	float x23 = x2 - x3;
    	float y23 = y2 - y3;
    	
    	float cos = x21 * x23 + y21 * y23;
    	float sin = x21 * y23 - y21 * x23;
    	
        if (level < MAX_LEVEL && (cos >= 0.0 || (Math.abs(sin / cos) > curveDelta)))
        {
        	float c1x = (x2 + x1) * DIV_BY_2;
        	float c1y = (y2 + y1) * DIV_BY_2;
        	float c2x = (x2 + x3) * DIV_BY_2;
        	float c2y = (y2 + y3) * DIV_BY_2;
        	float c3x = (c1x + c2x) * DIV_BY_2;
        	float c3y = (c1y + c2y) * DIV_BY_2;
            addSubQuad(x1, y1, c1x, c1y, c3x, c3y, level + 1);
            addSubQuad(c3x, c3y, c2x, c2y, x3, y3, level + 1);
        }
        else
        {
        	float w;
        	float l21 = (float)Math.sqrt(x21 * x21 + y21 * y21);
        	float l23 = (float)Math.sqrt(x23 * x23 + y23 * y23);
            w = w2 / sin;
            float mx2 = (x21 * l23 + x23 * l21) * w;
            float my2 = (y21 * l23 + y23 * l21) * w;
            w = w2 / l23;
            float mx3 =   y23 * w;
            float my3 = - x23 * w;
            lp.quadTo(x2 + mx2, y2 + my2, x3 + mx3, y3 + my3);
            rp.quadTo(x2 - mx2, y2 - my2, x3 - mx3, y3 - my3);
        }
    }
    
    /**
     * Adds solid cubic segment to the work path
     * @param x1 - the x coordinate of the first control point
     * @param y1 - the y coordinate of the first control point
     * @param x2 - the x coordinate of the second control point
     * @param y2 - the y coordinate of the second control point
     * @param x3 - the x coordinate of the third control point
     * @param y3 - the y coordinate of the third control point
     * @param x4 - the x coordinate of the fours control point
     * @param y4 - the y coordinate of the fours control point
     */
    void addCubic(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4)
    {
    	float x12 = x1 - x2;
    	float y12 = y1 - y2;
    	float x23 = x2 - x3;
    	float y23 = y2 - y3;
    	float x34 = x3 - x4;
    	float y34 = y3 - y4;
        
    	float l12 = (float)Math.sqrt(x12 * x12 + y12 * y12);
    	float l23 = (float)Math.sqrt(x23 * x23 + y23 * y23);
    	float l34 = (float)Math.sqrt(x34 * x34 + y34 * y34);
        
        // All edges are zero
        if (l12 == 0 && l23 == 0 && l34 == 0)
        {
            addLine(x1, y1, x4, y4, false);
            return;
        }
        
        // One zero edge
        if (l12 == 0 && l23 == 0)
        {
            addLine(x3, y3, x4, y4, false);
            return;
        }
        
        if (l23 == 0 && l34 == 0)
        {
            addLine(x1, y1, x2, y2, false);
            return;
        }
        
        if (l12 == 0 && l34 == 0)
        {
            addLine(x2, y2, x3, y3, false);
            return;
        }
        
        float w, mx1, my1, mx4, my4;
        boolean onLine;
        
        if (l12 == 0)
        {
            w = w2 / l23;
            mx1 =   y23 * w;
            my1 = - x23 * w;
            w = w2 / l34;
            mx4 =   y34 * w;
            my4 = - x34 * w;
            onLine = - x23 * y34 + y23 * x34 == 0; // sin3
        }
        else if (l34 == 0)
        {
            w = w2 / l12;
            mx1 =   y12 * w;
            my1 = - x12 * w;
            w = w2 / l23;
            mx4 =   y23 * w;
            my4 = - x23 * w;
            onLine = - x12 * y23 + y12 * x23 == 0; // sin2
        }
        else
        {
            w = w2 / l12;
            mx1 =   y12 * w;
            my1 = - x12 * w;
            w = w2 / l34;
            mx4 =   y34 * w;
            my4 = - x34 * w;
            if (l23 == 0)
            {
                onLine = - x12 * y34 + y12 * x34 == 0;
            }
            else
            {
                onLine =
                    - x12 * y34 + y12 * x34 == 0 &&
                    - x12 * y23 + y12 * x23 == 0 && // sin2
                    - x23 * y34 + y23 * x34 == 0;   // sin3
            }
        }
        
        float lx1 = x1 + mx1;
        float ly1 = y1 + my1;
        float rx1 = x1 - mx1;
        float ry1 = y1 - my1;
        
        if (checkMove)
        {
            if (isMove)
            {
                isMove = false;
                lp.moveTo(lx1, ly1);
                rp.moveTo(rx1, ry1);
            }
            else
            {
                addJoin(lp, x1, y1, lx1, ly1, true);
                addJoin(rp, x1, y1, rx1, ry1, false);
            }
        }
        
        if (onLine)
        {
            if ((x1 == x2 && y1 < y2) || x1 < x2)
            {
                l12 = -l12;
            }
            if ((x2 == x3 && y2 < y3) || x2 < x3)
            {
                l23 = -l23;
            }
            if ((x3 == x4 && y3 < y4) || x3 < x4)
            {
                l34 = -l34;
            }
            float d = l23 * l23 - l12 * l34;
            float[] roots = new float[3];
            int rc = 0;
            if (d == 0)
            {
            	float t = (l12 - l23) / (l12 + l34 - l23 - l23);
                if (0 < t && t < 1)
                {
                    roots[rc++] = t;
                }
            }
            else if (d > 0)
            {
                d = (float)Math.sqrt(d);
                float z = l12 + l34 - l23 - l23;
                float t;
                t = (l12 - l23 + d) / z;
                if (0.0 < t && t < 1.0)
                {
                    roots[rc++] = t;
                }
                t = (l12 - l23 - d) / z;
                if (0.0 < t && t < 1.0)
                {
                    roots[rc++] = t;
                }
            }
            
            if (rc > 0)
            {
                // Sort roots
                if (rc == 2 && roots[0] > roots[1])
                {
                	float tmp = roots[0];
                    roots[0] = roots[1];
                    roots[1] = tmp;
                }
                roots[rc++] = 1;
                
                float ax = - x34 - x12 + x23 + x23;
                float ay = - y34 - y12 + y23 + y23;
                float bx = 3 * (- x23 + x12);
                float by = 3 * (- y23 + y12);
                float cx = 3 * (- x12);
                float cy = 3 * (- y12);
                float xPrev = x1;
                float yPrev = y1;
                for(int i = 0; i < rc; i++)
                {
                	float t = roots[i];
                	float px = t * (t * (t * ax + bx) + cx) + x1;
                	float py = t * (t * (t * ay + by) + cy) + y1;
                	float px1 = (xPrev + px) * DIV_BY_2;
                	float py1 = (yPrev + py) * DIV_BY_2;
                    lp.cubicTo(px1 + mx1, py1 + my1, px1 + mx1, py1 + my1, px + mx1, py + my1);
                    rp.cubicTo(px1 - mx1, py1 - my1, px1 - mx1, py1 - my1, px - mx1, py - my1);
                    if (i < rc - 1)
                    {
                        lp.lineTo(px - mx1, py - my1);
                        rp.lineTo(px + mx1, py + my1);
                    }
                    xPrev = px;
                    yPrev = py;
                    mx1 = - mx1;
                    my1 = - my1;
                }
            }
            else
            {
                lp.cubicTo(x2 + mx1, y2 + my1, x3 + mx4, y3 + my4, x4 + mx4, y4 + my4);
                rp.cubicTo(x2 - mx1, y2 - my1, x3 - mx4, y3 - my4, x4 - mx4, y4 - my4);
            }
        }
        else
        {
            addSubCubic(x1, y1, x2, y2, x3, y3, x4, y4, 0);
        }
    }
    
    /**
     * Subdivides solid cubic curve to make outline for source quad segment and adds it to work path
     * @param x1 - the x coordinate of the first control point
     * @param y1 - the y coordinate of the first control point
     * @param x2 - the x coordinate of the second control point
     * @param y2 - the y coordinate of the second control point
     * @param x3 - the x coordinate of the third control point
     * @param y3 - the y coordinate of the third control point
     * @param x4 - the x coordinate of the fours control point
     * @param y4 - the y coordinate of the fours control point
     * @param level - the maximum level of subdivision deepness
     */
    void addSubCubic(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int level)
    {
    	float x12 = x1 - x2;
    	float y12 = y1 - y2;
    	float x23 = x2 - x3;
    	float y23 = y2 - y3;
    	float x34 = x3 - x4;
    	float y34 = y3 - y4;
    	
    	float cos2 = - x12 * x23 - y12 * y23;
    	float cos3 = - x23 * x34 - y23 * y34;
    	float sin2 = - x12 * y23 + y12 * x23;
    	float sin3 = - x23 * y34 + y23 * x34;
    	float sin0 = - x12 * y34 + y12 * x34;
    	float cos0 = - x12 * x34 - y12 * y34;
    	
        if (level < MAX_LEVEL && (sin2 != 0 || sin3 != 0 || sin0 != 0) && (cos2 >= 0 || cos3 >= 0 || cos0 >= 0 ||
            (Math.abs(sin2 / cos2) > curveDelta) ||
            (Math.abs(sin3 / cos3) > curveDelta) ||
            (Math.abs(sin0 / cos0) > curveDelta)))
        {
        	float cx = (x2 + x3) * DIV_BY_2;
        	float cy = (y2 + y3) * DIV_BY_2;
        	float lx2 = (x2 + x1) * DIV_BY_2;
        	float ly2 = (y2 + y1) * DIV_BY_2;
        	float rx3 = (x3 + x4) * DIV_BY_2;
        	float ry3 = (y3 + y4) * DIV_BY_2;
        	float lx3 = (cx + lx2) * DIV_BY_2;
        	float ly3 = (cy + ly2) * DIV_BY_2;
        	float rx2 = (cx + rx3) * DIV_BY_2;
        	float ry2 = (cy + ry3) * DIV_BY_2;
            cx = (lx3 + rx2) * DIV_BY_2;
            cy = (ly3 + ry2) * DIV_BY_2;
            addSubCubic(x1, y1, lx2, ly2, lx3, ly3, cx, cy, level + 1);
            addSubCubic(cx, cy, rx2, ry2, rx3, ry3, x4, y4, level + 1);
        }
        else
        {
        	float w, mx1, my1, mx2, my2, mx3, my3, mx4, my4;
        	float l12 = (float)Math.sqrt(x12 * x12 + y12 * y12);
        	float l23 = (float)Math.sqrt(x23 * x23 + y23 * y23);
        	float l34 = (float)Math.sqrt(x34 * x34 + y34 * y34);
        	
            if (l12 == 0)
            {
                w = w2 / l23;
                mx1 =   y23 * w;
                my1 = - x23 * w;
                w = w2 / l34;
                mx4 =   y34 * w;
                my4 = - x34 * w;
            }
            else if (l34 == 0)
            {
                w = w2 / l12;
                mx1 =   y12 * w;
                my1 = - x12 * w;
                w = w2 / l23;
                mx4 =   y23 * w;
                my4 = - x23 * w;
            }
            else
            {
                // Common case
                w = w2 / l12;
                mx1 =   y12 * w;
                my1 = - x12 * w;
                w = w2 / l34;
                mx4 =   y34 * w;
                my4 = - x34 * w;
            }
            
            if (sin2 == 0)
            {
                mx2 = mx1;
                my2 = my1;
            }
            else
            {
                w = w2 / sin2;
                mx2 = -(x12 * l23 - x23 * l12) * w;
                my2 = -(y12 * l23 - y23 * l12) * w;
            }
            if (sin3 == 0)
            {
                mx3 = mx4;
                my3 = my4;
            }
            else
            {
                w = w2 / sin3;
                mx3 = -(x23 * l34 - x34 * l23) * w;
                my3 = -(y23 * l34 - y34 * l23) * w;
            }
            
            lp.cubicTo(x2 + mx2, y2 + my2, x3 + mx3, y3 + my3, x4 + mx4, y4 + my4);
            rp.cubicTo(x2 - mx2, y2 - my2, x3 - mx3, y3 - my3, x4 - mx4, y4 - my4);
        }
    }
    
    /**
     * Adds dashed line segment to the work path
     * @param x1 - the x coordinate of the start line point
     * @param y1 - the y coordinate of the start line point
     * @param x2 - the x coordinate of the end line point
     * @param y2 - the y coordinate of the end line point
     */
    void addDashLine(float x1, float y1, float x2, float y2)
    {
    	float x21 = x2 - x1;
    	float y21 = y2 - y1;
        
    	float l21 = (float)Math.sqrt(x21 * x21 + y21 * y21);
        
        if (l21 == 0)
        {
            return;
        }
        
        float px1, py1;
        px1 = py1 = 0;
        float w = w2 / l21;
        float mx = - y21 * w;
        float my =   x21 * w;
        
        dasher.init(new DashIterator.Line(l21));
        
        while(!dasher.eof())
        {
        	float t = dasher.getValue();
            scx = x1 + t * x21;
            scy = y1 + t * y21;
            
            if (dasher.isOpen())
            {
                px1 = scx;
                py1 = scy;
                float lx1 = px1 + mx;
                float ly1 = py1 + my;
                float rx1 = px1 - mx;
                float ry1 = py1 - my;
                if (isMove)
                {
                    isMove = false;
                    smx = px1;
                    smy = py1;
                    rp.clean();
                    lp.moveTo(lx1, ly1);
                    rp.moveTo(rx1, ry1);
                }
                else
                {
                    addJoin(lp, x1, y1, lx1, ly1, true);
                    addJoin(rp, x1, y1, rx1, ry1, false);
                }
            }
            else if (dasher.isContinue())
            {
            	float px2 = scx;
            	float py2 = scy;
                lp.lineTo(px2 + mx, py2 + my);
                rp.lineTo(px2 - mx, py2 - my);
                if (dasher.close)
                {
                    addCap(lp, px2, py2, rp.xLast, rp.yLast);
                    lp.combine(rp);
                    if (isFirst)
                    {
                        isFirst = false;
                        fmx = smx;
                        fmy = smy;
                        sp = lp;
                        lp = new BufferedPath();
                    }
                    else
                    {
                        addCap(lp, smx, smy, lp.xMove, lp.yMove);
                        lp.closePath();
                    }
                    isMove = true;
                }
            }
            dasher.next();
        }
    }
    
    /**
     * Adds dashed quad segment to the work path
     * @param x1 - the x coordinate of the first control point
     * @param y1 - the y coordinate of the first control point
     * @param x2 - the x coordinate of the second control point
     * @param y2 - the y coordinate of the second control point
     * @param x3 - the x coordinate of the third control point
     * @param y3 - the y coordinate of the third control point
     */
    void addDashQuad(float x1, float y1, float x2, float y2, float x3, float y3)
    {
    	float x21 = x2 - x1;
    	float y21 = y2 - y1;
    	float x23 = x2 - x3;
    	float y23 = y2 - y3;
    	
    	float l21 = (float)Math.sqrt(x21 * x21 + y21 * y21);
    	float l23 = (float)Math.sqrt(x23 * x23 + y23 * y23);
    	
        if (l21 == 0 && l23 == 0)
        {
            return;
        }
        
        if (l21 == 0)
        {
            addDashLine(x2, y2, x3, y3);
            return;
        }
        
        if (l23 == 0)
        {
            addDashLine(x1, y1, x2, y2);
            return;
        }
        
        float ax = x1 + x3 - x2 - x2;
        float ay = y1 + y3 - y2 - y2;
        float bx = x2 - x1;
        float by = y2 - y1;
        float cx = x1;
        float cy = y1;
        
        float px1, py1, dx1, dy1;
        px1 = py1 = dx1 = dy1 = 0;
        float prev = 0;
        
        dasher.init(new DashIterator.Quad(x1, y1, x2, y2, x3, y3));
        
        while(!dasher.eof())
        {
        	float t = dasher.getValue();
        	float dx = t * ax + bx;
        	float dy = t * ay + by;
            scx = t * (dx + bx) + cx; // t^2 * ax + 2.0 * t * bx + cx
            scy = t * (dy + by) + cy; // t^2 * ay + 2.0 * t * by + cy
            if (dasher.isOpen())
            {
                px1 = scx;
                py1 = scy;
                dx1 = dx;
                dy1 = dy;
                float w = w2 / (float)Math.sqrt(dx1 * dx1 + dy1 * dy1);
                float mx1 = - dy1 * w;
                float my1 =   dx1 * w;
                float lx1 = px1 + mx1;
                float ly1 = py1 + my1;
                float rx1 = px1 - mx1;
                float ry1 = py1 - my1;
                if (isMove)
                {
                    isMove = false;
                    smx = px1;
                    smy = py1;
                    rp.clean();
                    lp.moveTo(lx1, ly1);
                    rp.moveTo(rx1, ry1);
                }
                else
                {
                    addJoin(lp, x1, y1, lx1, ly1, true);
                    addJoin(rp, x1, y1, rx1, ry1, false);
                }
            }
            else if (dasher.isContinue())
            {
            	float px3 = scx;
            	float py3 = scy;
            	float sx = x2 - x23 * prev;
            	float sy = y2 - y23 * prev;
            	float t2 = (t - prev) / (1 - prev);
            	float px2 = px1 + (sx - px1) * t2;
            	float py2 = py1 + (sy - py1) * t2;
            	
                addQuad(px1, py1, px2, py2, px3, py3);
                if (dasher.isClosed())
                {
                    addCap(lp, px3, py3, rp.xLast, rp.yLast);
                    lp.combine(rp);
                    if (isFirst)
                    {
                        isFirst = false;
                        fmx = smx;
                        fmy = smy;
                        sp = lp;
                        lp = new BufferedPath();
                    }
                    else
                    {
                        addCap(lp, smx, smy, lp.xMove, lp.yMove);
                        lp.closePath();
                    }
                    isMove = true;
                }
            }
            
            prev = t;
            dasher.next();
        }
    }
    
    /**
     * Adds dashed cubic segment to the work path
     * @param x1 - the x coordinate of the first control point
     * @param y1 - the y coordinate of the first control point
     * @param x2 - the x coordinate of the second control point
     * @param y2 - the y coordinate of the second control point
     * @param x3 - the x coordinate of the third control point
     * @param y3 - the y coordinate of the third control point
     * @param x4 - the x coordinate of the fours control point
     * @param y4 - the y coordinate of the fours control point
     */
    void addDashCubic(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4)
    {
    	float x12 = x1 - x2;
    	float y12 = y1 - y2;
    	float x23 = x2 - x3;
    	float y23 = y2 - y3;
    	float x34 = x3 - x4;
    	float y34 = y3 - y4;
        
    	float l12 = (float)Math.sqrt(x12 * x12 + y12 * y12);
    	float l23 = (float)Math.sqrt(x23 * x23 + y23 * y23);
    	float l34 = (float)Math.sqrt(x34 * x34 + y34 * y34);
        
        // All edges are zero
        if (l12 == 0 && l23 == 0 && l34 == 0)
        {
            // NOTHING
            return;
        }
        
        // One zero edge
        if (l12 == 0 && l23 == 0)
        {
            addDashLine(x3, y3, x4, y4);
            return;
        }
        
        if (l23 == 0 && l34 == 0)
        {
            addDashLine(x1, y1, x2, y2);
            return;
        }
        
        if (l12 == 0 && l34 == 0)
        {
            addDashLine(x2, y2, x3, y3);
            return;
        }
        
        float ax = x4 - x1 + 3 * (x2 - x3);
        float ay = y4 - y1 + 3 * (y2 - y3);
        float bx = 3 * (x1 + x3 - x2 - x2);
        float by = 3 * (y1 + y3 - y2 - y2);
        float cx = 3 * (x2 - x1);
        float cy = 3 * (y2 - y1);
        float dx = x1;
        float dy = y1;
        
        float px1 = 0;
        float py1 = 0;
        float prev = 0;
        
        dasher.init(new DashIterator.Cubic(x1, y1, x2, y2, x3, y3, x4, y4));
        
        while(!dasher.eof())
        {
        	float t = dasher.getValue();
            scx = t * (t * (t * ax + bx) + cx) + dx;
            scy = t * (t * (t * ay + by) + cy) + dy;
            if (dasher.isOpen())
            {
                px1 = scx;
                py1 = scy;
                float dx1 = t * (t * (ax + ax + ax) + bx + bx) + cx;
                float dy1 = t * (t * (ay + ay + ay) + by + by) + cy;
                float w = w2 / (float)Math.sqrt(dx1 * dx1 + dy1 * dy1);
                float mx1 = - dy1 * w;
                float my1 =   dx1 * w;
                float lx1 = px1 + mx1;
                float ly1 = py1 + my1;
                float rx1 = px1 - mx1;
                float ry1 = py1 - my1;
                if (isMove)
                {
                    isMove = false;
                    smx = px1;
                    smy = py1;
                    rp.clean();
                    lp.moveTo(lx1, ly1);
                    rp.moveTo(rx1, ry1);
                }
                else
                {
                    addJoin(lp, x1, y1, lx1, ly1, true);
                    addJoin(rp, x1, y1, rx1, ry1, false);
                }
            }
            else if (dasher.isContinue())
            {
            	float sx1 = x2 - x23 * prev;
            	float sy1 = y2 - y23 * prev;
            	float sx2 = x3 - x34 * prev;
            	float sy2 = y3 - y34 * prev;
            	float sx3 = sx1 + (sx2 - sx1) * prev;
            	float sy3 = sy1 + (sy2 - sy1) * prev;
            	float t2 = (t - prev) / (1 - prev);
            	float sx4 = sx3 + (sx2 - sx3) * t2;
            	float sy4 = sy3 + (sy2 - sy3) * t2;

            	float px4 = scx;
            	float py4 = scy;
            	float px2 = px1 + (sx3 - px1) * t2;
            	float py2 = py1 + (sy3 - py1) * t2;
            	float px3 = px2 + (sx4 - px2) * t2;
            	float py3 = py2 + (sy4 - py2) * t2;

                addCubic(px1, py1, px2, py2, px3, py3, px4, py4);
                if (dasher.isClosed())
                {
                    addCap(lp, px4, py4, rp.xLast, rp.yLast);
                    lp.combine(rp);
                    if (isFirst)
                    {
                        isFirst = false;
                        fmx = smx;
                        fmy = smy;
                        sp = lp;
                        lp = new BufferedPath();
                    }
                    else
                    {
                        addCap(lp, smx, smy, lp.xMove, lp.yMove);
                        lp.closePath();
                    }
                    isMove = true;
                }
            }
            
            prev = t;
            dasher.next();
        }
    }
    
    /**
     *  Dasher class provides dashing for particular dash style
     */
    class Dasher
    {
    	float pos;
        boolean close, visible, first;
        float[] dash;
        float phase;
        int index;
        DashIterator iter;
        
        Dasher(float[] dash, float phase)
        {
            this.dash = dash;
            this.phase = phase;
            index = 0;
            pos = phase;
            visible = true;
            while (pos >= dash[index])
            {
                visible = !visible;
                pos -= dash[index];
                index = (index + 1) % dash.length;
            }            
            pos = -pos;
            first = visible;
        }
        
        void init(DashIterator iter)
        {
            this.iter = iter;
            close = true;
        }
        
        boolean isOpen()
        {
            return visible && pos < iter.length;
        }
        
        boolean isContinue()
        {
            return !visible && pos > 0;
        }
        
        boolean isClosed()
        {
            return close;
        }
        
        boolean isConnected()
        {
            return first && !close;
        }
        
        boolean eof()
        {
            if (!close)
            {
                pos -= iter.length;
                return true;
            }
            if (pos >= iter.length)
            {
                if (visible)
                {
                    pos -= iter.length;
                    return true;
                }
                close = pos == iter.length;
            }
            return false;
        }
        
        void next()
        {
            if (close)
            {
                pos += dash[index];
                index = (index + 1) % dash.length;
            }
            else
            {
                // Go back
                index = (index + dash.length - 1) % dash.length;
                pos -= dash[index];
            }
            visible = !visible;
        }
        
        float getValue()
        {
        	float t = iter.getNext(pos);
            return t < 0 ? 0 : (t > 1 ? 1 : t);
        }
    }
    
    /**
     * DashIterator class provides dashing for particular segment type  
     */
    static abstract class DashIterator
    {
        static final float FLATNESS = 1;
        
        static class Line extends DashIterator
        {
            Line(float len)
            {
                length = len;
            }
            
            float getNext(float dashPos)
            {
                return dashPos / length;
            }
        }
        
        static class Quad extends DashIterator
        {
            int valSize;
            int valPos;
            float curLen;
            float prevLen;
            float lastLen;
            float[] values;
            float step;
            
            Quad(float x1, float y1, float x2, float y2, float x3, float y3)
            {
            	float nx = x1 + x3 - x2 - x2;
            	float ny = y1 + y3 - y2 - y2;
                
                int n = (int)(1 + Math.sqrt(0.75f * (Math.abs(nx) + Math.abs(ny)) * FLATNESS));
                step = 1 / n;
                
                float ax = x1 + x3 - x2 - x2;
                float ay = y1 + y3 - y2 - y2;
                float bx = 2 * (x2 - x1);
                float by = 2 * (y2 - y1);
                
                float dx1 = step * (step * ax + bx);
                float dy1 = step * (step * ay + by);
                float dx2 = step * (step * ax * 2);
                float dy2 = step * (step * ay * 2);
                float vx = x1;
                float vy = y1;
                
                valSize = n;
                values = new float[valSize];
                float pvx = vx;
                float pvy = vy;
                length = 0;
                for(int i = 0; i < n; i++)
                {
                    vx += dx1;
                    vy += dy1;
                    dx1 += dx2;
                    dy1 += dy2;
                    double lx = vx - pvx;
                    double ly = vy - pvy;
                    values[i] = (float)Math.sqrt(lx * lx + ly * ly);
                    length += values[i];
                    pvx = vx;
                    pvy = vy;
                }
                
                valPos = 0;
                curLen = 0;
                prevLen = 0;
            }
            
            float getNext(float dashPos)
            {
            	float t = 2;
                while (curLen <= dashPos && valPos < valSize)
                {
                    prevLen = curLen;
                    curLen += lastLen = values[valPos++];
                }
                if (curLen > dashPos)
                {
                    t = (valPos - 1 + (dashPos - prevLen) / lastLen) * step;
                }
                return t;
            }
        }
        
        static class Cubic extends DashIterator
        {
            int valSize;
            int valPos;
            float curLen;
            float prevLen;
            float lastLen;
            float[] values;
            float step;
            
            Cubic(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4)
            {
            	float nx1 = x1 + x3 - x2 - x2;
            	float ny1 = y1 + y3 - y2 - y2;
            	float nx2 = x2 + x4 - x3 - x3;
            	float ny2 = y2 + y4 - y3 - y3;
                
            	float max = Math.max(Math.abs(nx1) + Math.abs(ny1), Math.abs(nx2) + Math.abs(ny2));
                int n = (int)(1 + Math.sqrt(0.75f * max) * FLATNESS);
                step = 1f / n;
                
                float ax = x4 - x1 + 3 * (x2 - x3);
                float ay = y4 - y1 + 3 * (y2 - y3);
                float bx = 3 * (x1 + x3 - x2 - x2);
                float by = 3 * (y1 + y3 - y2 - y2);
                float cx = 3 * (x2 - x1);
                float cy = 3 * (y2 - y1);
                
                float dx1 = step * (step * (step * ax + bx) + cx);
                float dy1 = step * (step * (step * ay + by) + cy);
                float dx2 = step * (step * (step * ax * 6 + bx * 2));
                float dy2 = step * (step * (step * ay * 6 + by * 2));
                float dx3 = step * (step * (step * ax * 6));
                float dy3 = step * (step * (step * ay * 6));
                float vx = x1;
                float vy = y1;
                
                valSize = n;
                values = new float[valSize];
                float pvx = vx;
                float pvy = vy;
                length = 0;
                for(int i = 0; i < n; i++)
                {
                    vx += dx1;
                    vy += dy1;
                    dx1 += dx2;
                    dy1 += dy2;
                    dx2 += dx3;
                    dy2 += dy3;
                    float lx = vx - pvx;
                    float ly = vy - pvy;
                    values[i] = (float)Math.sqrt(lx * lx + ly * ly);
                    length += values[i];
                    pvx = vx;
                    pvy = vy;
                }
                
                valPos = 0;
                curLen = 0;
                prevLen = 0;
            }
            
            float getNext(float dashPos)
            {
            	float t = 2;
                while (curLen <= dashPos && valPos < valSize)
                {
                    prevLen = curLen;
                    curLen += lastLen = values[valPos++];
                }
                if (curLen > dashPos)
                {
                    t = (valPos - 1 + (dashPos - prevLen) / lastLen) * step;
                }
                return t;
            }
        }
        
        float length;
        
        abstract float getNext(float dashPos);
    }
    
    /**
     * BufferedPath class provides work path storing and processing
     */
    static class BufferedPath
    {
        private static final int bufCapacity = 10;
        
        static int[] pointShift = {
                2,  // MOVETO
                2,  // LINETO
                4,  // QUADTO
                6,  // CUBICTO
                0}; // CLOSE
        
        byte[] types;
        float[] points;
        int typeSize;
        int pointSize;
        
        float xLast;
        float yLast;
        float xMove;
        float yMove;
        
        public BufferedPath()
        {
            types = new byte[bufCapacity];
            points = new float[bufCapacity * 2];
        }
        
        void checkBuf(int typeCount, int pointCount)
        {
            if (typeSize + typeCount > types.length)
            {
                byte[] tmp = new byte[typeSize + Math.max(bufCapacity, typeCount)];
                System.arraycopy(types, 0, tmp, 0, typeSize);
                types = tmp;
            }
            if (pointSize + pointCount > points.length)
            {
                float[] tmp = new float[pointSize + Math.max(bufCapacity * 2, pointCount)];
                System.arraycopy(points, 0, tmp, 0, pointSize);
                points = tmp;
            }
        }
        
        boolean isEmpty()
        {
            return typeSize == 0;
        }
        
        void clean()
        {
            typeSize = 0;
            pointSize = 0;
        }
        
        void moveTo(float x, float y)
        {
            checkBuf(1, 2);
            types[typeSize++] = Geometry.Enumeration.SEG_MOVETO;
            points[pointSize++] = xMove = x;
            points[pointSize++] = yMove = y;
        }
        
        void lineTo(float x, float y)
        {
            checkBuf(1, 2);
            types[typeSize++] = Geometry.Enumeration.SEG_LINETO;
            points[pointSize++] = xLast = x;
            points[pointSize++] = yLast = y;
        }
        
        void quadTo(float x1, float y1, float x2, float y2)
        {
            checkBuf(1, 4);
            types[typeSize++] = Geometry.Enumeration.SEG_QUADTO;
            points[pointSize++] = x1;
            points[pointSize++] = y1;
            points[pointSize++] = xLast = x2;
            points[pointSize++] = yLast = y2;
        }
        
        void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3)
        {
            checkBuf(1, 6);
            types[typeSize++] = Geometry.Enumeration.SEG_CUBICTO;
            points[pointSize++] = x1;
            points[pointSize++] = y1;
            points[pointSize++] = x2;
            points[pointSize++] = y2;
            points[pointSize++] = xLast = x3;
            points[pointSize++] = yLast = y3;
        }

        void closePath()
        {
            checkBuf(1, 0);
            types[typeSize++] = Geometry.Enumeration.SEG_CLOSE;
        }

        void setLast(float x, float y)
        {
            points[pointSize - 2] = xLast = x;
            points[pointSize - 1] = yLast = y;
        }
        
        void append(BufferedPath p)
        {
            checkBuf(p.typeSize, p.pointSize);
            System.arraycopy(p.points, 0, points, pointSize, p.pointSize);
            System.arraycopy(p.types, 0, types, typeSize, p.typeSize);
            pointSize += p.pointSize;
            typeSize += p.typeSize;
            xLast = points[pointSize - 2];
            yLast = points[pointSize - 1];
        }
        
        void appendReverse(BufferedPath p)
        {
            checkBuf(p.typeSize, p.pointSize);
            // Skip last point, because it's the first point of the second path
            for(int i = p.pointSize - 2; i >= 0; i -= 2)
            {
                points[pointSize++] = p.points[i + 0];
                points[pointSize++] = p.points[i + 1];
            }
            // Skip first type, because it's always MOVETO
            int closeIndex = 0;
            for(int i = p.typeSize - 1; i >= 0; i--)
            {
                byte type = p.types[i];
                if (type == Geometry.Enumeration.SEG_MOVETO)
                {
                    types[closeIndex] = Geometry.Enumeration.SEG_MOVETO;
                    types[typeSize++] = Geometry.Enumeration.SEG_CLOSE;
                }
                else
                {
                    if (type == Geometry.Enumeration.SEG_CLOSE)
                    {
                        closeIndex = typeSize;
                    }
                    types[typeSize++] = type;
                }
            }
            xLast = points[pointSize - 2];
            yLast = points[pointSize - 1];
        }
        
        void join(BufferedPath p)
        {
            // Skip MOVETO
            checkBuf(p.typeSize - 1, p.pointSize - 2);
            System.arraycopy(p.points, 2, points, pointSize, p.pointSize - 2);
            System.arraycopy(p.types, 1, types, typeSize, p.typeSize - 1);
            pointSize += p.pointSize - 2;
            typeSize += p.typeSize - 1;
            xLast = points[pointSize - 2];
            yLast = points[pointSize - 1];
        }
        
        void combine(BufferedPath p)
        {
            checkBuf(p.typeSize - 1, p.pointSize - 2);
            // Skip last point, because it's the first point of the second path
            for(int i = p.pointSize - 4; i >= 0; i -= 2)
            {
                points[pointSize++] = p.points[i + 0];
                points[pointSize++] = p.points[i + 1];
            }
            // Skip first type, because it's always MOVETO
            for(int i = p.typeSize - 1; i >= 1; i--)
            {
                types[typeSize++] = p.types[i];
            }
            xLast = points[pointSize - 2];
            yLast = points[pointSize - 1];
        }
        
        Geometry createGeometry()
        {
        	Geometry p = new Geometry();
            int j = 0;
            for(int i = 0; i < typeSize; i++)
            {
                int type = types[i];
                switch(type)
                {
	                case Geometry.Enumeration.SEG_MOVETO:
	                    p.moveTo(points[j], points[j + 1]);
	                    break;
	                case Geometry.Enumeration.SEG_LINETO:
	                    p.lineTo(points[j], points[j + 1]);
	                    break;
	                case Geometry.Enumeration.SEG_QUADTO:
	                    p.quadTo(points[j], points[j + 1], points[j + 2], points[j + 3]);
	                    break;
	                case Geometry.Enumeration.SEG_CUBICTO:
	                    p.curveTo(points[j], points[j + 1], points[j + 2], points[j + 3], points[j + 4], points[j + 5]);
	                    break;
	                case Geometry.Enumeration.SEG_CLOSE:
	                    p.closePath();
	                    break;
                }
                j += pointShift[type];
            }
            return p;
        }
    }
}
