/*
 * File: AffineTransform.java
 * Version: 1.0
 * Initial Creation: Jun 25, 2010 9:12:26 PM
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Denis M. Kishenko
 */
package com.sun.pdfview.helper;

/**
 * Partial/modified implementation of java.awt.geom.AffineTransform.
 */
public class AffineTransform
{
	/** This constant indicates that the transform defined by this object is an identity transform.*/
	public static final int TYPE_IDENTITY = 0;
	/** This flag bit indicates that the transform defined by this object performs a translation in addition to the conversions indicated by other flag bits.*/
    public static final int TYPE_TRANSLATION = 1;
    /** This flag bit indicates that the transform defined by this object performs a uniform scale in addition to the conversions indicated by other flag bits.*/
    public static final int TYPE_UNIFORM_SCALE = 2;
    /** This flag bit indicates that the transform defined by this object performs a general scale in addition to the conversions indicated by other flag bits.*/
    public static final int TYPE_GENERAL_SCALE = 4;
    /**
     * This flag bit indicates that the transform defined by this object performs a quadrant rotation by some multiple of 90 degrees in addition to the conversions 
     * indicated by other flag bits.
     */
    public static final int TYPE_QUADRANT_ROTATION = 8;
    /**
     * This flag bit indicates that the transform defined by this object performs a rotation by an arbitrary angle in addition to the conversions indicated by other flag bits.
     */
    public static final int TYPE_GENERAL_ROTATION = 16;
    /** This constant indicates that the transform defined by this object performs an arbitrary conversion of the input coordinates.*/
    public static final int TYPE_GENERAL_TRANSFORM = 32;
    /**
     * This flag bit indicates that the transform defined by this object performs a mirror image flip about some axis which changes the normally right handed coordinate 
     * system into a left handed system in addition to the conversions indicated by other flag bits.
     */
    public static final int TYPE_FLIP = 64;
    /** This constant is a bit mask for any of the scale flag bits.*/
    public static final int TYPE_MASK_SCALE = TYPE_UNIFORM_SCALE | TYPE_GENERAL_SCALE;
    /** This constant is a bit mask for any of the rotation flag bits.*/
    public static final int TYPE_MASK_ROTATION = TYPE_QUADRANT_ROTATION | TYPE_GENERAL_ROTATION;

    /**
     * The <code>TYPE_UNKNOWN</code> is an initial type value
     */
    static final int TYPE_UNKNOWN = -1;
    
    /**
     * The min value equivalent to zero. If absolute value less then ZERO it considered as zero.  
     */
    static final float ZERO = 1E-10f;
	
	/**
     * The values of transformation matrix
     */
    float m00;
    float m10;
    float m01;
    float m11;
    float m02;
    float m12;

    /**
     * The transformation <code>type</code> 
     */
    transient int type;
    
    /**
     * Constructs a new AffineTransform representing the Identity transformation.
     */
    public AffineTransform()
    {
        type = TYPE_IDENTITY;
        m00 = m11 = 1;
        m10 = m01 = m02 = m12 = 0;
    }
    
    /**
     * Constructs a new AffineTransform that is a copy of the specified AffineTransform object.
     * @param t The AffineTransform object to copy.
     */
    public AffineTransform(AffineTransform t)
    {
        this.type = t.type;
        this.m00 = t.m00;
        this.m10 = t.m10;
        this.m01 = t.m01;
        this.m11 = t.m11;
        this.m02 = t.m02;
        this.m12 = t.m12;
    }
    
    /**
     * Constructs a new AffineTransform from 6 floating point values representing the 6 specifiable entries of the 3x3 transformation matrix.
     */
    public AffineTransform(float m00, float m10, float m01, float m11, float m02, float m12)
    {
        this.type = TYPE_UNKNOWN;
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        deriveMatrixType();
    }
    
    /**
     * Constructs a new AffineTransform from an array of floating point values representing either the 4 non-translation enries or the 6 specifiable entries of the 3x3 
     * transformation matrix.
     * @param matrix The float array containing the values to be set in the new AffineTransform object. The length of the array is assumed to be at least 4. If the length 
     * of the array is less than 6, only the first 4 values are taken. If the length of the array is greater than 6, the first 6 values are taken.
     */
    public AffineTransform(float[] matrix)
    {
        this.type = TYPE_UNKNOWN;
        m00 = matrix[0];
        m10 = matrix[1];
        m01 = matrix[2];
        m11 = matrix[3];
        if (matrix.length > 4)
        {
            m02 = matrix[4];
            m12 = matrix[5];
        }
        deriveMatrixType();
    }
    
    /*
     * Method returns type of affine transformation.
     * 
     * Transform matrix is
     *   m00 m01 m02
     *   m10 m11 m12
     * 
     * According analytic geometry new basis vectors are (m00, m01) and (m10, m11), 
     * translation vector is (m02, m12). Original basis vectors are (1, 0) and (0, 1). 
     * Type transformations classification:  
     *   TYPE_IDENTITY - new basis equals original one and zero translation
     *   TYPE_TRANSLATION - translation vector isn't zero  
     *   TYPE_UNIFORM_SCALE - vectors length of new basis equals
     *   TYPE_GENERAL_SCALE - vectors length of new basis doesn't equal 
     *   TYPE_FLIP - new basis vector orientation differ from original one
     *   TYPE_QUADRANT_ROTATION - new basis is rotated by 90, 180, 270, or 360 degrees     
     *   TYPE_GENERAL_ROTATION - new basis is rotated by arbitrary angle
     *   TYPE_GENERAL_TRANSFORM - transformation can't be inversed
     */
    public int getType()
    {
    	deriveMatrixType();
    	return this.type;
    }
    
    private void deriveMatrixType()
    {
    	if (type == TYPE_UNKNOWN)
    	{
    		this.type = 0;
    		
    		if (m00 * m01 + m10 * m11 != 0)
    		{
    			this.type |= TYPE_GENERAL_TRANSFORM;
    		}
    		
    		if (m02 != 0 || m12 != 0)
    		{
    			this.type |= TYPE_TRANSLATION;
    		}
    		else if (m00 == 1 && m11 == 1 && m01 == 0 && m10 == 0)
    		{
    			this.type = TYPE_IDENTITY;
    		}
    		
    		if (m00 * m11 - m01 * m10 < 0)
    		{
    			this.type |= TYPE_FLIP;
    		}
    		
    		float dx = m00 * m00 + m10 * m10;
    		float dy = m01 * m01 + m11 * m11;
    		if (dx != dy)
    		{
    			this.type |= TYPE_GENERAL_SCALE;
    		}
    		else if (dx != 1)
    		{
    			this.type |= TYPE_UNIFORM_SCALE;
    		}
    		
    		if ((m00 == 0 && m11 == 0) || (m10 == 0 && m01 == 0 && (m00 < 0 || m11 < 0)))
    		{
    			this.type |= TYPE_QUADRANT_ROTATION;
    		}
    		else if (m01 != 0 || m10 != 0)
    		{
    			this.type |= TYPE_GENERAL_ROTATION;
    		}
        }
    }
    
    /**
     * Returns a transform representing a translation transformation.
     * @param tx The distance by which coordinates are translated in the X axis direction.
     * @param ty The distance by which coordinates are translated in the Y axis direction.
     * @return An AffineTransform object that represents a translation transformation, created with the specified vector.
     */
	public static AffineTransform createTranslation(float tx, float ty)
	{
		AffineTransform t = new AffineTransform();
        t.setToTranslation(tx, ty);
        return t;
	}
	
	/**
	 * Returns a transform representing a scaling transformation.
	 * @param sx The factor by which coordinates are scaled along the X axis direction.
	 * @param sy The factor by which coordinates are scaled along the Y axis direction.
	 * @return An AffineTransform object that scales coordinates by the specified factors.
	 */
	public static AffineTransform createScale(float sx, float sy)
	{
        AffineTransform t = new AffineTransform();
        t.setToScale(sx, sy);
        return t;
    }
	
	/**
	 *  Concatenates an AffineTransform Tx to this AffineTransform Cx in the most commonly useful way to provide a new user space that is mapped to the former user space 
	 *  by Tx.
	 * @param Tx The AffineTransform object to be concatenated with this AffineTransform object.
	 */
	public void concatenate(AffineTransform Tx)
	{
        set(multiply(Tx, this));
    }
	
	/** 
     * Multiply matrix of two AffineTransform objects 
     * @param t1 - the AffineTransform object is a multiplicand
     * @param t2 - the AffineTransform object is a multiplier
     * @return an AffineTransform object that is a result of t1 multiplied by matrix t2. 
     */
    AffineTransform multiply(AffineTransform t1, AffineTransform t2)
	{
    	//Original code from java.awt.geom.AffineTransform
    	return new AffineTransform(
                t1.m00 * t2.m00 + t1.m10 * t2.m01,          // m00
                t1.m00 * t2.m10 + t1.m10 * t2.m11,          // m01
                t1.m01 * t2.m00 + t1.m11 * t2.m01,          // m10
                t1.m01 * t2.m10 + t1.m11 * t2.m11,          // m11
                t1.m02 * t2.m00 + t1.m12 * t2.m01 + t2.m02, // m02
                t1.m02 * t2.m10 + t1.m12 * t2.m11 + t2.m12);// m12
    	
    	//Could be optimized based on Matrix.type
	}

	/**
	 * Stores the scalar component of this matrix in the specified vector.
	 * @param scale A vector to receive the scale.
	 */
	public void getScale(XYPointFloat scale)
	{
		scale.x = m00;
		scale.y = m11;
	}
	
	/**
	 * Inverts this matrix and stores the result in dst.
	 * @param dst A matrix to store the invert of this matrix in.
	 * @return true if the the matrix can be inverted, false otherwise.
	 */
	public boolean invert(AffineTransform dst)
	{
		float det = getDeterminant();
        if (Math.abs(det) < ZERO)
        {
        	return false;
        }
        
        det = 1f / det; //Just to speed up processing
        
        dst.type = TYPE_UNKNOWN;
        dst.m00 = m11 * det;
        dst.m10 = -m10 * det;
        dst.m01 = -m01 * det;
        dst.m11 = m00 * det;
        dst.m02 = (m01 * m12 - m11 * m02) * det;
        dst.m12 = (m10 * m02 - m00 * m12) * det;
        return true;
	}
	
	/**
	 * Sets the values of this matrix to those of the specified matrix.
	 * @param m The source matrix.
	 */
	public void set(AffineTransform m)
	{
		this.type = m.type;
        this.m00 = m.m00;
        this.m10 = m.m10;
        this.m01 = m.m01;
        this.m11 = m.m11;
        this.m02 = m.m02;
        this.m12 = m.m12;
	}
	
	/**
	 * Sets this matrix to the identity matrix.
	 */
	public void setIdentity()
	{
		type = TYPE_IDENTITY;
        m00 = m11 = 1;
        m10 = m01 = m02 = m12 = 0;
	}
	
	/**
	 * Concatenates this transform with a translation transformation.
	 * @param tx The distance by which coordinates are translated in the X axis direction.
	 * @param ty The distance by which coordinates are translated in the Y axis direction.
	 */
	public void translate(float tx, float ty)
	{
        concatenate(AffineTransform.createTranslation(tx, ty));
    }
	
	/**
	 * Concatenates this transform with a scaling transformation.
	 * @param sx The factor by which coordinates are scaled along the X axis direction.
	 * @param sy The factor by which coordinates are scaled along the Y axis direction.
	 */
	public void scale(float sx, float sy)
	{
        concatenate(AffineTransform.createScale(sx, sy));
    }
	
	/**
	 * Returns the determinant of the matrix representation of the transform.
	 * @return The determinant of the matrix used to transform the coordinates.
	 */
	public float getDeterminant()
	{
        return m00 * m11 - m01 * m10;
    }
	
	/**
	 * Sets this transform to a translation transformation.
	 * @param tx The distance by which coordinates are translated in the X axis direction.
	 * @param ty The distance by which coordinates are translated in the Y axis direction.
	 */
	public void setToTranslation(float tx, float ty)
	{
        m00 = m11 = 1;
        m01 = m10 = 0;
        m02 = tx;
        m12 = ty;
        if (tx == 0 && ty == 0)
        {
            type = TYPE_IDENTITY;
        }
        else
        {
            type = TYPE_TRANSLATION;
        }
    }
	
	/**
	 * Sets this transform to a scaling transformation.
	 * @param sx The factor by which coordinates are scaled along the X axis direction.
	 * @param sy The factor by which coordinates are scaled along the Y axis direction.
	 */
	public void setToScale(float sx, float sy)
	{
        m00 = sx;
        m11 = sy;
        m10 = m01 = m02 = m12 = 0;
        if (sx != 1 || sy != 1)
        {
            type = TYPE_UNKNOWN;
        }
        else
        {
            type = TYPE_IDENTITY;
        }
    }
	
	/**
	 * Transforms the specified ptSrc and stores the result in ptDst.
	 * @param ptSrc The specified Point2D to be transformed.
	 * @param ptDst The specified Point2D that stores the result of transforming ptSrc.
	 * @return The ptDst after transforming ptSrc and storing the result in ptDst.
	 */
	public XYPointFloat transformPoint(XYPointFloat ptSrc, XYPointFloat ptDst)
	{
        if (ptDst == null)
        {
        	ptDst = new XYPointFloat();
        }
        
        float x = ptSrc.x;
        float y = ptSrc.y;
        
        ptDst.x = x * m00 + y * m01 + m02;
        ptDst.y = x * m10 + y * m11 + m12;
        return ptDst;
    }
	
	/**
	 * Transforms the relative distance vector specified by ptSrc and stores the result in ptDst.
	 * @param ptSrc The distance vector to be delta transformed.
	 * @param ptDst The resulting transformed distance vector.
	 * @return ptDst, which contains the result of the transformation.
	 */
	public XYPointFloat transformNormal(XYPointFloat ptSrc, XYPointFloat ptDst)
	{
		if (ptDst == null)
        {
        	ptDst = new XYPointFloat();
        }
        
        float x = ptSrc.x;
        float y = ptSrc.y;
        
        ptDst.x = x * m00 + y * m01;
        ptDst.y = x * m10 + y * m11;
        return ptDst;
    }
	
	/**
	 * Transforms an array of floating point coordinates by this transform.
	 * @param srcPts The array containing the source point coordinates. Each point is stored as a pair of x, y coordinates.
	 * @param srcOff The array into which the transformed point coordinates are returned. Each point is stored as a pair of x, y coordinates.
	 * @param dstPts The offset to the first point to be transformed in the source array.
	 * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
	 * @param numPts The number of points to be transformed.
	 */
	public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts)
	{
		int step = 2;
        if (srcPts == dstPts && srcOff < dstOff && dstOff < srcOff + numPts * 2)
        {
            srcOff = srcOff + numPts * 2 - 2;
            dstOff = dstOff + numPts * 2 - 2;
            step = -2;
        }
        while (--numPts >= 0)
        {
            float x = srcPts[srcOff + 0];
            float y = srcPts[srcOff + 1];
            dstPts[dstOff + 0] = (x * m00 + y * m01 + m02);
            dstPts[dstOff + 1] = (x * m10 + y * m11 + m12);
            srcOff += step;
            dstOff += step;
        }
	}
	
	public String toString()
	{
        return getClass().getName() + "[[" + m00 + ", " + m01 + ", " + m02 + "], [" + m10 + ", " + m11 + ", " + m12 + "]]";
    }
    
    public int hashCode()
    {
    	//Not at all efficient but no other real choice
    	int hash = new Float(m00).hashCode();
    	hash += new Float(m01).hashCode();
    	hash += new Float(m02).hashCode();
    	hash += new Float(m10).hashCode();
    	hash += new Float(m11).hashCode();
    	hash += new Float(m12).hashCode();
        return hash;
    }
    
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (obj instanceof AffineTransform)
        {
            AffineTransform t = (AffineTransform)obj;
            return m00 == t.m00 && m01 == t.m01 && m02 == t.m02 && m10 == t.m10 && m11 == t.m11 && m12 == t.m12;
        }
        return false;
    }
}
