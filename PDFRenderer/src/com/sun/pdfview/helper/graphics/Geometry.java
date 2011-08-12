/*
 * File: Geometry.java
 * Version: 1.0
 * Initial Creation: May 21, 2010 2:45:24 PM
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
package com.sun.pdfview.helper.graphics;

import java.util.NoSuchElementException;

import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.XYPointFloat;
import com.sun.pdfview.helper.XYRectFloat;

/**
 * Represents one of more geometric paths. Built off java.awt.geom.GeneralPath.
 */
public class Geometry
{
	/**
	 * An even-odd winding rule for determining the interior of a path.
	 */
	public static final int WIND_EVEN_ODD = 0;
	/**
	 * A non-zero winding rule for determining the interior of a path.
	 */
	public static final int WIND_NON_ZERO = 1;
	
	/**
     * The buffers size
     */
    private static final int BUFFER_SIZE = 10;
    
    /**
     * The buffers capacity
     */
    private static final int BUFFER_CAPACITY = 10;

    /**
     * The point's types buffer
     */
    byte[] types;
    
    /**
     * The points buffer
     */
    float[] points;
    
    /**
     * The point's type buffer size
     */
    int typeSize;
    
    /**
     * The points buffer size
     */
    int pointSize;
    
    /**
     * The path rule 
     */
    int rule;

    /**
     * The space amount in points buffer for different segmenet's types
     */
    static final int[] pointShift = {
            2,  // MOVETO
            2,  // LINETO
            4,  // QUADTO
            6,  // CUBICTO
            0}; // CLOSE
	
	/**
	 * Enumeration through the elements of the path.
	 */
	public static class Enumeration
	{
		private Geometry g;
		private AffineTransform mat;
		private int cordIndex, typeIndex;
		
		private Enumeration(Geometry g, AffineTransform mat)
		{
			this.g = g;
			this.mat = mat;
			this.cordIndex = 0;
			this.typeIndex = 0;
		}
		
		/**
		 * The segment type constant for a point that specifies the starting location for a new subpath.
		 */
		public static final int SEG_MOVETO = 0;
		/**
		 * The segment type constant for a point that specifies the end point of a line to be drawn from the most recently specified point.
		 */
		public static final int SEG_LINETO = 1;
		/**
		 * The segment type constant for the pair of points that specify a quadratic parametric curve to be drawn from the most recently specified point.
		 */
		public static final int SEG_QUADTO = 2;
		/**
		 * The segment type constant for the set of 3 points that specify a cubic parametric curve to be drawn from the most recently specified point.
		 */
		public static final int SEG_CUBICTO = 3;
		/**
		 * The segment type constant that specifies that the preceding subpath should be closed by appending a line segment back to the point corresponding to the most recent SEG_MOVETO.
		 */
		public static final int SEG_CLOSE = 4;
		/**
		 * The winding rule constant for specifying an even-odd rule for determining the interior of a path.
		 */
		public static final int WIND_EVEN_ODD = Geometry.WIND_EVEN_ODD;
		/**
		 * The winding rule constant for specifying a non-zero rule for determining the interior of a path.
		 */
		public static final int WIND_NON_ZERO = Geometry.WIND_NON_ZERO;
		
		/**
		 * Tests if the iteration is complete.
		 * @return true if all the segments have been read; false otherwise.
		 */
		public boolean isDone()
		{
			return typeIndex >= g.typeSize;
		}
		
		/**
		 * Moves the iterator to the next segment of the path forwards along the primary direction of traversal as long as there are more points in that direction.
		 */
		public void next()
		{
			typeIndex++;
		}
		
		/**
		 * Returns the coordinates and type of the current path segment in the iteration.
		 * @param coords An array that holds the data returned from this method.
		 * @return The path-segment type of the current path segment.
		 */
		public int currentSegment(float[] coords)
		{
			if (isDone())
			{
				throw new NoSuchElementException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_ENUMERATION_OUT_OF_BOUNDS));
			}
			int type = g.types[typeIndex];
			int count = Geometry.pointShift[type];
			System.arraycopy(g.points, cordIndex, coords, 0, count);
			if (mat != null)
			{
				mat.transform(coords, 0, coords, 0, count / 2);
			}
			cordIndex += count;
			return type;
		}
		
		/**
		 * Returns the winding rule for determining the interior of the path.
		 * @return The winding rule.
		 */
		public int getWindingRule()
		{
			return g.getWindingRule();
		}
	}
	
	/**
	 * Constructs a new Geometry object.
	 */
	public Geometry()
	{
		setWindingRule(WIND_NON_ZERO);
        types = new byte[BUFFER_SIZE];
        points = new float[BUFFER_SIZE * 2];
	}
	
	/**
	 * Constructs a new Geometry object from an arbitrary Geometry object.
	 * @param geom The Geometry object to add.
	 */
	public Geometry(Geometry geom)
	{
		this();
		append(geom, false);
	}
	
	/**
	 * Constructs a new Geometry object from a box.
	 * @param bbox The box to create the Geometry object from.
	 */
	public Geometry(XYRectFloat bbox)
	{
		this();
		moveTo((float)bbox.x, (float)bbox.y);
		lineTo((float)bbox.X2(), (float)bbox.y);
		lineTo((float)bbox.X2(), (float)bbox.Y2());
		lineTo((float)bbox.x, (float)bbox.Y2());
		closePath();
	}
	
	public final Object clone()
	{
		return new Geometry(this);
	}
	
	/**
     * Checks points and types buffer size to add pointCount points. If necessary realloc buffers to enlarge size.   
     * @param pointCount - the point count to be added in buffer
     */
    void checkBuf(int pointCount, boolean checkMove)
    {
        if (checkMove && typeSize == 0)
        {
            throw new IllegalStateException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_FIRST_SEG_MOVETO));
        }
        if (typeSize == types.length)
        {
            byte[] tmp = new byte[typeSize + BUFFER_CAPACITY];
            System.arraycopy(types, 0, tmp, 0, typeSize);
            types = tmp;
        }
        if (pointSize + pointCount > points.length)
        {
            float[] tmp = new float[pointSize + Math.max(BUFFER_CAPACITY * 2, pointCount)];
            System.arraycopy(points, 0, tmp, 0, pointSize);
            points = tmp;
        }
    }
	
	/**
	 * Returns a Enumeration object that enumerates along the boundary of this Geometry and provides access to the geometry of the outline of this Geometry.
	 * @param at An AffineTransform.
	 * @return a new Enumeration that iterates along the boundary of this Shape and provides access to the geometry of this Geometry's outline.
	 */
	public Enumeration getPathEnumerator(AffineTransform at)
	{
		return new Enumeration(this, at);
	}
	
	/**
	 * Returns the bounding box of the path.
	 * @return A XYRectFloat object that bounds the current path.
	 */
	public XYRectFloat getBounds2D()
	{
		float rx1, ry1, rx2, ry2;
        if (pointSize == 0)
        {
            rx1 = ry1 = rx2 = ry2 = 0.0f;
        }
        else
        {
            int i = pointSize - 1;
            ry1 = ry2 = points[i--];
            rx1 = rx2 = points[i--];
            while (i > 0)
            {
                float y = points[i--];
                float x = points[i--];
                if (x < rx1)
                {
                    rx1 = x;
                }
                else if (x > rx2)
                {
                	rx2 = x;
                }
                if (y < ry1)
                {
                    ry1 = y;
                }
                else if (y > ry2)
                {
                	ry2 = y;
                }
            }
        }
        return new XYRectFloat(rx1, ry1, rx2 - rx1, ry2 - ry1);
	}
	
	/**
	 * Returns the fill style winding rule.
	 * @return An integer representing the current winding rule.
	 */
	public int getWindingRule()
	{
		return this.rule;
	}
	
	/**
	 * Sets the winding rule for this path to the specified value.
	 * @param rule An integer representing the specified winding rule.
	 * @throws IllegalArgumentException If rule is not either WIND_EVEN_ODD or WIND_NON_ZERO.
	 */
	public void setWindingRule(int rule)
	{
		if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO)
		{
			throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.HELPER_GRAPHICS_INVALID_WINDING_RULE));
		}
		this.rule = rule;
	}
	
	/**
	 * Returns the coordinates most recently added to the end of the path as a XYPointFloat object.
	 * @return A XYPointFloat object containing the ending coordinates of the path or null if there are no points in the path.
	 */
	public XYPointFloat getCurrentPoint()
	{
		if (typeSize == 0)
		{
            return null;
        }
        int j = pointSize - 2;
        if (types[typeSize - 1] == Enumeration.SEG_CLOSE)
        {
            for (int i = typeSize - 2; i > 0; i--)
            {
                int type = types[i];
                if (type == Enumeration.SEG_MOVETO)
                {
                    break;
                }
                j -= pointShift[type];
            }
        }
        return new XYPointFloat(points[j], points[j + 1]);
	}
	
	/**
	 * Transforms the geometry of this path using the specified AffineTransform.
	 * @param at The AffineTransform used to transform the area.
	 */
	public void transform(AffineTransform at)
	{
		at.transform(points, 0, points, 0, pointSize / 2);
	}
	
	/**
	 * Appends the geometry of the specified Geometry object to the path, possibly connecting the new geometry to the existing path segments with a line segment.
	 * @param g The Geometry whose geometry is appended to this path.
	 * @param connect A boolean to control whether or not to turn an initial moveTo segment into a lineTo segment to connect the new geometry to the existing path.
	 */
	public void append(Geometry g, boolean connect)
	{
		Enumeration path = g.getPathEnumerator(null);
		float[] coords = new float[6];
		while (!path.isDone())
		{
            switch (path.currentSegment(coords))
            {
	            case Enumeration.SEG_MOVETO:
	                if (!connect || typeSize == 0)
	                {
	                    moveTo(coords[0], coords[1]);
	                    break;
	                }
	                if (types[typeSize - 1] != Enumeration.SEG_CLOSE &&
	                    points[pointSize - 2] == coords[0] &&
	                    points[pointSize - 1] == coords[1])
	                {
	                    break;
	                }
	            // NO BREAK;
	            case Enumeration.SEG_LINETO:
	                lineTo(coords[0], coords[1]);
	                break;
	            case Enumeration.SEG_QUADTO:
	                quadTo(coords[0], coords[1], coords[2], coords[3]);
	                break;
	            case Enumeration.SEG_CUBICTO:
	                curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
	                break;
	            case Enumeration.SEG_CLOSE:
	                closePath();
	                break;
            }
            path.next();
            connect = false;
        }
	}
	
	/**
	 * Adds a point to the path by moving to the specified coordinates.
	 */
	public void moveTo(float x, float y)
	{
		if (typeSize > 0 && types[typeSize - 1] == Enumeration.SEG_MOVETO)
		{
            points[pointSize - 2] = x;
            points[pointSize - 1] = y;
        }
		else
		{
            checkBuf(2, false);
            types[typeSize++] = Enumeration.SEG_MOVETO;
            points[pointSize++] = x;
            points[pointSize++] = y;
        }
	}
	
	/**
	 * Adds a curved segment, defined by two new points, to the path by drawing a Quadratic curve that intersects both the current coordinates and the coordinates (x2, y2), 
	 * using the specified point (x1, y1) as a quadratic parametric control point.
	 */
	public void quadTo(float x1, float y1, float x2, float y2)
	{
		checkBuf(4, true);
        types[typeSize++] = Enumeration.SEG_QUADTO;
        points[pointSize++] = x1;
        points[pointSize++] = y1;
        points[pointSize++] = x2;
        points[pointSize++] = y2;
	}
	
	/**
	 * Adds a point to the path by drawing a straight line from the current coordinates to the new specified coordinates.
	 */
	public void lineTo(float x, float y)
	{
		checkBuf(2, true);
        types[typeSize++] = Enumeration.SEG_LINETO;
        points[pointSize++] = x;
        points[pointSize++] = y;
	}
	
	/**
	 * Adds a curved segment, defined by three new points, to the path by drawing a Bézier curve that intersects both the current coordinates and the coordinates (x3, y3), 
	 * using the specified points (x1, y1) and (x2, y2) as Bézier control points.
	 */
	public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
	{
		checkBuf(6, true);
        types[typeSize++] = Enumeration.SEG_CUBICTO;
        points[pointSize++] = x1;
        points[pointSize++] = y1;
        points[pointSize++] = x2;
        points[pointSize++] = y2;
        points[pointSize++] = x3;
        points[pointSize++] = y3;
	}
	
	/**
	 * Closes the current subpath by drawing a straight line back to the coordinates of the last moveTo. If the path is already closed then this method has no effect.
	 */
	public void closePath()
	{
		if (typeSize == 0 || types[typeSize - 1] != Enumeration.SEG_CLOSE)
		{
            checkBuf(0, true);
            types[typeSize++] = Enumeration.SEG_CLOSE;
        }
	}
	
	/**
	 * Returns a new transformed Geometry.
	 * @param at The AffineTransform used to transform a new Geometry.
	 * @return A new Geometry, transformed with the specified AffineTransform.
	 */
	public Geometry createTransformedShape(AffineTransform at)
	{
		Geometry geo = new Geometry(this);
        if (at != null)
        {
        	geo.transform(at);
        }
        return geo;
	}
	
	/**
	 * Determine if the area that contains the specified point is filled or not.
	 * @param x The x-point to test.
	 * @param y The y-point to test.
	 * @return <code>true</code> if and only if the point is within the Geometry, and follows the winding-rule. <code>false</code> if otherwise.
	 */
	public boolean isFilled(float x, float y)
	{
		//From Insight Toolkit (ITK) on the Wikipedia article: http://en.wikipedia.org/wiki/Even-odd_rule
		if(this.typeSize < 3)
		{
			return false;
		}
		int numpoints = this.typeSize;
		int typePtr = 0;
		int pointPtr = 0;
		float node1x = 0;
		float node1y = 0;
		float node2x = 0;
		float node2y = 0;
		float fx = 0, fy = 0;
		int type, count;
		// If last point same as first, don't bother with it.
		if(types[numpoints - 1] == Enumeration.SEG_CLOSE)
		{
			numpoints--;
		}
		boolean oddNodes = false;
		for(int i = 0; i < numpoints; i++)
		{
			//Get end points
			type = this.types[typePtr++];
			count = Geometry.pointShift[type];
			switch(type)
			{
				case Enumeration.SEG_MOVETO:
				case Enumeration.SEG_LINETO:
					node1x = this.points[pointPtr + 0];
					node1y = this.points[pointPtr + 1];
					break;
				case Enumeration.SEG_QUADTO:
					node1x = this.points[pointPtr + 2];
					node1y = this.points[pointPtr + 3];
					break;
				case Enumeration.SEG_CUBICTO:
					node1x = this.points[pointPtr + 4];
					node1y = this.points[pointPtr + 5];
					break;
			}
			pointPtr += count;
			if(i == 0)
			{
				fx = node1x;
				fy = node1y;
			}
			if(i == numpoints - 1)
			{
				node2x = fx;
				node2y = fy;
			}
			else
			{
				type = this.types[typePtr++];
				count = Geometry.pointShift[type];
				switch(type)
				{
					case Enumeration.SEG_MOVETO:
					case Enumeration.SEG_LINETO:
						node2x = this.points[pointPtr + 0];
						node2y = this.points[pointPtr + 1];
						break;
					case Enumeration.SEG_QUADTO:
						node2x = this.points[pointPtr + 2];
						node2y = this.points[pointPtr + 3];
						break;
					case Enumeration.SEG_CUBICTO:
						node2x = this.points[pointPtr + 4];
						node2y = this.points[pointPtr + 5];
						break;
				}
				pointPtr += count;
			}
			//Check to see if point is within Geometry
			if((node1y < y && node2y >= y) || (node2y < y && node1y >= y))
			{
				if(node1x + (y - node1y) / (node2y - node1y) * (node2x - node1x) < x)
				{
					if(!(this.rule == WIND_NON_ZERO && oddNodes))
					{
						oddNodes = !oddNodes; //Only want it to change if winding rule is odd-even or is first time with non-zero
					}
				}
			}
		}
		return oddNodes;
	}
}
