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

import net.rim.device.api.util.Arrays;

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
            throw new IllegalArgumentException("Negative width");
        }
        if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE)
        {
            throw new IllegalArgumentException("Illegal cap");
        }
        if (join != JOIN_MITER && join != JOIN_ROUND && join != JOIN_BEVEL)
        {
            throw new IllegalArgumentException("Illegal join");
        }
        if (join == JOIN_MITER && miterLimit < 1.0f)
        {
            throw new IllegalArgumentException("miterLimit less than 1.0f");
        }
        if (dash != null)
        {
        	if (dashPhase < 0.0f)
            {
                throw new IllegalArgumentException("Negative dashPhase");
            }
            if (dash.length == 0)
            {
                throw new IllegalArgumentException("Zero dash length");
            }
            ZERO:
            {
            	int len = dash.length;
                for(int i = 0; i < len; i++)
                {
                    if (dash[i] < 0.0)
                    {
                        throw new IllegalArgumentException("Negative dash[" + i + "]");
                    }
                    if (dash[i] > 0.0)
                    {
                        break ZERO;
                    }
                }
                throw new IllegalArgumentException("All dash lengths zero");
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
     * Returns a Geometry whose interior defines the stroked outline of a specified Geometry.
     * @param geo The Geometry boundary be stroked.
     * @return The Geometry of the stroked outline.
     */
    public Geometry createStrokedGeometry(Geometry geo)
    {
    	//TODO
    	return null;
    }
}
