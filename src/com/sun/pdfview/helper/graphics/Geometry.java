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

import com.sun.pdfview.helper.XYPointFloat;
import com.sun.pdfview.helper.XYRectFloat;

import net.rim.device.api.math.Matrix4f;

/**
 * Represents one of more geometric paths.
 * @author Vincent Simonetti
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
	 * Enumeration through the elements of the path.
	 */
	public static class Enumeration
	{
		private Geometry g;
		private Matrix4f mat;
		private int index;
		
		private Enumeration(Geometry g, Matrix4f mat)
		{
			this.g = g;
			this.mat = mat;
			this.index = 0;
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
			//TODO
			return true;
		}
		
		/**
		 * Moves the iterator to the next segment of the path forwards along the primary direction of traversal as long as there are more points in that direction.
		 */
		public void next()
		{
			//TODO
		}
		
		/**
		 * Returns the coordinates and type of the current path segment in the iteration.
		 * @param coords An array that holds the data returned from this method.
		 * @return The path-segment type of the current path segment.
		 */
		public int currentSegment(float[] coords)
		{
			if(coords == null)
			{
				return -1;
			}
			//TODO
			return -1;
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
		//TODO
	}
	
	/**
	 * Constructs a new Geometry object from an arbitrary Geometry object.
	 * @param geom The Geometry object to add.
	 */
	public Geometry(Geometry geom)
	{
		//TODO
	}
	
	/**
	 * Constructs a new Geometry object from a box.
	 * @param bbox The box to create the Geometry object from.
	 */
	public Geometry(XYRectFloat bbox)
	{
		this();
		moveTo(bbox.x, bbox.y);
		lineTo(bbox.X2(), bbox.y);
		lineTo(bbox.X2(), bbox.Y2());
		lineTo(bbox.x, bbox.Y2());
		closePath();
	}

	/**
	 * Returns a Enumeration object that iterates along the boundary of this Geometry and provides access to the geometry of the outline of this Geometry.
	 * @param at An Matrix4f.
	 * @return a new Enumeration that iterates along the boundary of this Shape and provides access to the geometry of this Geometry's outline.
	 */
	public Enumeration getPathIterator(Matrix4f at)
	{
		//TODO
		return null;
	}
	
	/**
	 * Returns the bounding box of the path.
	 * @return A XYRectFloat object that bounds the current path.
	 */
	public XYRectFloat getBounds2D()
	{
		//TODO
		return null;
	}
	
	/**
	 * Returns the fill style winding rule.
	 * @return An integer representing the current winding rule.
	 */
	public int getWindingRule()
	{
		//TODO
		return -1;
	}
	
	/**
	 * Sets the winding rule for this path to the specified value.
	 * @param rule An integer representing the specified winding rule.
	 * @throws IllegalArgumentException If rule is not either WIND_EVEN_ODD or WIND_NON_ZERO.
	 */
	public void setWindingRule(int rule)
	{
		//TODO
	}
	
	/**
	 * Returns the coordinates most recently added to the end of the path as a XYPointFloat object.
	 * @return A XYPointFloat object containing the ending coordinates of the path or null if there are no points in the path.
	 */
	public XYPointFloat getCurrentPoint()
	{
		//TODO
		return null;
	}
	
	/**
	 * Transforms the geometry of this path using the specified Matrix4f.
	 * @param at The Matrix4f used to transform the area.
	 */
	public void transform(Matrix4f at)
	{
		//TODO
	}
	
	/**
	 * Appends the geometry of the specified Geometry object to the path, possibly connecting the new geometry to the existing path segments with a line segment.
	 * @param g The Geometry whose geometry is appended to this path.
	 * @param connect A boolean to control whether or not to turn an initial moveTo segment into a lineTo segment to connect the new geometry to the existing path.
	 */
	public void append(Geometry g, boolean connect)
	{
		//TODO
	}
	
	/**
	 * Adds a point to the path by moving to the specified coordinates.
	 */
	public void moveTo(float x, float y)
	{
		//TODO
	}
	
	/**
	 * Adds a curved segment, defined by two new points, to the path by drawing a Quadratic curve that intersects both the current coordinates and the coordinates (x2, y2), 
	 * using the specified point (x1, y1) as a quadratic parametric control point.
	 */
	public void quadTo(float x1, float y1, float x2, float y2)
	{
		//TODO
	}
	
	/**
	 * Adds a point to the path by drawing a straight line from the current coordinates to the new specified coordinates.
	 */
	public void lineTo(float x, float y)
	{
		//TODO
	}
	
	/**
	 * Adds a curved segment, defined by three new points, to the path by drawing a Bézier curve that intersects both the current coordinates and the coordinates (x3, y3), 
	 * using the specified points (x1, y1) and (x2, y2) as Bézier control points.
	 */
	public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
	{
		//TODO
	}
	
	/**
	 * Closes the current subpath by drawing a straight line back to the coordinates of the last moveTo. If the path is already closed then this method has no effect.
	 */
	public void closePath()
	{
		//TODO
	}
	
	/**
	 * Returns a new transformed Geometry.
	 * @param at The Matrix4f used to transform a new Geometry.
	 * @return A new Geometry, transformed with the specified Matrix4f.
	 */
	public Geometry createTransformedShape(Matrix4f at)
	{
		//TODO
		return null;
	}
}
