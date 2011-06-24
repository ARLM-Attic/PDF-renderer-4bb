//#preprocessor

/*
 * File: PDFImage.java
 * Version: 1.12
 * Initial Creation: May 14, 2010 12:56:10 PM
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

//As a precaution since some of the code used in the sub-classes, preprocessData, and readData is based off OpenJDK code.
/*
 * Copyright (c) 1995, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.pdfview;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.ByteBuffer;
//#else
import com.sun.pdfview.helper.nio.ByteBuffer;
//#endif

import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.math.Fixed32;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.Color;

import com.sun.pdfview.colorspace.IndexedColor;
import com.sun.pdfview.colorspace.PDFColorSpace;
import com.sun.pdfview.decode.PDFDecoder;
import com.sun.pdfview.helper.ColorSpace;
import com.sun.pdfview.helper.graphics.color.ICC_ColorSpace;
import com.sun.pdfview.helper.PDFUtil;

/**
 * Encapsulates a PDF Image
 */
public class PDFImage
{
	public static void dump(PDFObject obj) throws IOException
	{
        p("dumping PDF object: " + obj);
        if (obj == null) {
            return;
        }
        Hashtable dict = obj.getDictionary();
        p("   dict = " + dict);
        for (Enumeration keys = dict.keys(); keys.hasMoreElements();)
        {
        	Object key = keys.nextElement();
            p("key = " + key + " value = " + dict.get(key));
        }
    }
	
    public static void p(String string)
    {
        System.out.println(string);
    }
    
    private int[] GREY_TO_ARGB = null;
    
    private int[] getGreyToArgbMap(int numBits, ColorSpace cs)
    {
    	if(GREY_TO_ARGB == null)
    	{
    		GREY_TO_ARGB = createGreyToArgbMap(numBits, cs);
    	}
    	return GREY_TO_ARGB;
    }
    /*
    private static int[][] GREY_TO_ARGB = new int[8][];
    
    private static int[] getGreyToArgbMap(int numBits)
    {
    	PDFUtil.assert(numBits <= 8, "numBits <= 8");
        int[] argbVals = GREY_TO_ARGB[numBits - 1];
        if (argbVals == null)
        {
            argbVals = createGreyToArgbMap(numBits);
        }
        return argbVals;
    }
    */
    
    /**
     * Create a map from all bit-patterns of a certain depth greyscale to the
     * corresponding sRGB values via the ICC colorr converter.
     * @param numBits the number of greyscale bits
     * @return a 2^bits array of standard 32-bit ARGB fits for each greyscale value
     *  at that bitdepth
     */
    /*
    private static int[] createGreyToArgbMap(int numBits)
    {
    	final ColorSpace greyCs = PDFColorSpace.getColorSpace(PDFColorSpace.COLORSPACE_GRAY).getColorSpace();
    */
    private static int[] createGreyToArgbMap(int numBits, final ColorSpace greyCs)
    {
    	int len;
    	byte[] greyVals = new byte[len = 1 << numBits];
        for (int i = 0; i < len; i++)
        {
            greyVals[i] = (byte)(i & 0xFF);
        }
        
        final int[] argbVals = new int[len];
        final int mask = (1 << numBits) - 1;
        final float[] norm = new float[1];
        final float min = greyCs.getMinValue(0);
        final float scale = 1f / mask;
        float[] res;
        
        //Instead of doing the color conversion for an entire image, just do it for the color range
        for(int i = 0; i < len; i++)
        {
        	norm[0] = min + (float)(greyVals[i] & 0xFF) * scale;
        	res = greyCs.toRGB(norm);
        	argbVals[i] = 0xFF000000 | rgb2int(res[0], res[1], res[2]);
        }
        
        //GREY_TO_ARGB[numBits - 1] = argbVals;
        return argbVals;
    }
    
    /** color key mask. Array of start/end pairs of ranges of color components to
     *  mask out. If a component falls within any of the ranges it is clear. */
    private int[] colorKeyMask = null;
    /** the width of this image in pixels */
    private int width;
    /** the height of this image in pixels */
    private int height;
    /** the colorspace to interpret the samples in */
    private PDFColorSpace colorSpace;
    /** the number of bits per sample component */
    private int bpc;
    /** whether this image is a mask or not */
    private boolean imageMask = false;
    /** the SMask image, if any */
    private PDFImage sMask;
    /** the decode array */
    private float[] decode;
    private float[] decodeMins;
    private float[] decodeCoefficients;
    /** the actual image data */
    private PDFObject imageObj;
    
    /** 
     * Create an instance of a PDFImage
     */
    protected PDFImage(PDFObject imageObj)
    {
        this.imageObj = imageObj;
    }
    
    /**
     * Read a PDFImage from an image dictionary and stream
     *
     * @param obj the PDFObject containing the image's dictionary and stream
     * @param resources the current resources
     */
    public static PDFImage createImage(PDFObject obj, Hashtable resources) throws IOException
    {
        // create the image
        PDFImage image = new PDFImage(obj);
        
        // get the width (required)
        PDFObject widthObj = obj.getDictRef("Width");
        if (widthObj == null)
        {
            throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.IMAGE_CANT_READ_WIDTH, new Object[]{obj.toString()}));
        }
        image.setWidth(widthObj.getIntValue());
        
        // get the height (required)
        PDFObject heightObj = obj.getDictRef("Height");
        if (heightObj == null)
        {
            throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.IMAGE_CANT_READ_HEIGHT, new Object[]{obj.toString()}));
        }
        image.setHeight(heightObj.getIntValue());
        
        // figure out if we are an image mask (optional)
        PDFObject imageMaskObj = obj.getDictRef("ImageMask");
        if (imageMaskObj != null)
        {
            image.setImageMask(imageMaskObj.getBooleanValue());
        }
        
        // read the bpc and colorspace (required except for masks) 
        if (image.isImageMask())
        {
            image.setBitsPerComponent(1);
            
            // create the indexed color space for the mask
            // [PATCHED by michal.busta@gmail.com] - default value of Decode according to PDF spec. is [0, 1]
        	// so the color arry should be:  
            int[] colors = {Color.BLACK, Color.WHITE};
            
            PDFObject imageMaskDecode = obj.getDictRef("Decode");
            if (imageMaskDecode != null)
            {
                PDFObject[] array = imageMaskDecode.getArray();
                float decode0 = array[0].getFloatValue();
                if (decode0 == 1.0f)
                {
                    colors = new int[]{Color.WHITE, Color.BLACK};
                }
            }
            image.setColorSpace(new IndexedColor(colors));
        }
        else
        {
            // get the bits per component (required)
            PDFObject bpcObj = obj.getDictRef("BitsPerComponent");
            if (bpcObj == null)
            {
                throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.IMAGE_CANT_READ_BPC, new Object[]{obj.toString()}));
            }
            image.setBitsPerComponent(bpcObj.getIntValue());
            
            // get the color space (required)
            PDFObject csObj = obj.getDictRef("ColorSpace");
            if (csObj == null)
            {
                throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.IMAGE_CANT_READ_COLORSPACE, new Object[]{obj.toString()}));
            }
            
            PDFColorSpace cs = PDFColorSpace.getColorSpace(csObj, resources);
            image.setColorSpace(cs);
        }
        
        // read the decode array
        PDFObject decodeObj = obj.getDictRef("Decode");
        if (decodeObj != null)
        {
            PDFObject[] decodeArray = decodeObj.getArray();
            
            int len;
            float[] decode = new float[len = decodeArray.length];
            for (int i = 0; i < len; i++)
            {
                decode[i] = decodeArray[i].getFloatValue();
            }
            
            image.setDecode(decode);
        }
        
        // read the soft mask.
        // If ImageMask is true, this entry must not be present.
        // (See implementation note 52 in Appendix H.)
        if (imageMaskObj == null)
        {
            PDFObject sMaskObj = obj.getDictRef("SMask");
            if (sMaskObj == null)
            {
                // try the explicit mask, if there is no SoftMask
                sMaskObj = obj.getDictRef("Mask");
            }
            
            if (sMaskObj != null)
            {
                if (sMaskObj.getType() == PDFObject.STREAM)
                {
                    try
                    {
                        PDFImage sMaskImage = PDFImage.createImage(sMaskObj, resources);
                        image.setSMask(sMaskImage);
                    }
                    catch (IOException ex)
                    {
                        p("ERROR: there was a problem parsing the mask for this object");
                        dump(obj);
                        ex.printStackTrace();
                    }
                }
                else if (sMaskObj.getType() == PDFObject.ARRAY)
                {
                    // retrieve the range of the ColorKeyMask
                    // colors outside this range will not be painted.
                    try
                    {
                        image.setColorKeyMask(sMaskObj);
                    }
                    catch (IOException ex)
                    {
                        p("ERROR: there was a problem parsing the color mask for this object");
                        dump(obj);
                        ex.printStackTrace();
                    }
                }
            }
        }
        
        return image;
    }
    
    /**
     * Get the image that this PDFImage generates.
     *
     * @return a buffered image containing the decoded image data
     */
    public Bitmap getImage()
    {
        try
        {
        	Bitmap bi = (Bitmap)imageObj.getCache();
        	
            if (bi == null)
            {
            	byte[] data = null;
                ByteBuffer jpegBytes = null;
                final boolean jpegDecode = PDFDecoder.isLastFilter(imageObj, PDFDecoder.DCT_FILTERS);
                if (jpegDecode)
                {
                    // if we're lucky, the stream will have just the DCT
                    // filter applied to it, and we'll have a reference to
                    // an underlying mapped file, so we'll manage to avoid
                    // a copy of the encoded JPEG bytes
                    jpegBytes = imageObj.getStreamBuffer(PDFDecoder.DCT_FILTERS);
                }
                else
                {
                    data = imageObj.getStream();
                }
                // parse the stream data into an actual image
                bi = parseData(data, jpegBytes);
                imageObj.setCache(bi);
            }
            /* //XX X Disable when done
            if(bi != null)
            {
            	EncodedImage ei = net.rim.device.api.system.PNGEncodedImage.encode(bi);
            	try
            	{
            		javax.microedition.io.file.FileConnection file = (javax.microedition.io.file.FileConnection)javax.microedition.io.Connector.open("file:///SDCard/BlackBerry/pictures/" + ei.hashCode() + ".png", javax.microedition.io.Connector.READ_WRITE);
            		if(file.exists())
            		{
            			file.truncate(0);
            		}
            		else
            		{
            			file.create();
            		}
            		java.io.OutputStream out = file.openOutputStream();
            		out.write(ei.getData());
            		out.close();
            		file.close();
            	}
            	catch(Exception e)
            	{
            	}
            }
            //*/
            return bi;
        }
        catch (IOException ioe)
        {
            System.out.println("Error reading image");
            ioe.printStackTrace();
            return null;
        }
    }
    
    /**
     * <p>Parse the image stream into a Bitmap. Note that this is
     * guaranteed to be called after all the other setXXX methods have been 
     * called.</p>
     *
     * <p>NOTE: the color convolving is extremely slow on large images.
     * It would be good to see if it could be moved out into the rendering
     * phases, where we might be able to scale the image down first.</p>
     * 
     * @param data the data when already completely filtered and uncompressed
     * @param jpegData a byte buffer if data still requiring the DCDTecode filter
     *  is being used
     */
    protected Bitmap parseData(byte[] data, ByteBuffer jpegData) throws IOException
    {
//    	String hex;
//    	String name;
//    	synchronized (System.out)
//    	{
//    		int len = data.length;
//    		System.out.println("\n\n" + name + ": " + len);
//    		for (int i = 0; i < len; i++)
//    		{
//    			hex = "0x" + Integer.toHexString(0xFF & data[i]);
//    			System.out.print(hex);
//    			if (i < data.length - 1)
//    			{
//    				System.out.print(", ");
//    			}
//    			if ((i + 1) % 25 == 0)
//    			{
//    				System.out.print("\n");
//    			}
//    		}
//   		System.out.println("\n");
//    		System.out.flush();
//    	}
        
        Bitmap bi = loadImage(data, jpegData);
        
        // add in the alpha data supplied by the SMask, if any
        PDFImage sMaskImage = getSMask();
        if (sMaskImage != null)
        {
            Bitmap si = sMaskImage.getImage();

            Bitmap outImage = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, getWidth(), getHeight());
            outImage.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
            
            int[] srcArray = new int[width];
            int[] maskArray = new int[width];
            
            for (int i = 0; i < height; i++)
            {
                bi.getARGB(srcArray, 0, width, 0, i, width, 1);
                si.getARGB(maskArray, 0, width, 0, i, width, 1);
                
                for (int j = 0; j < width; j++)
                {
                    int ac = 0xFF000000;
                    
                    maskArray[j] = ((maskArray[j] & 0xff) << 24) | (srcArray[j] & ~ac);
                }
                
                outImage.setARGB(maskArray, 0, width, 0, i, width, 1);
            }
            
            bi = outImage;
        }
        
        return (bi);
    }
    
    /**
     * Get the image's width
     */
    public int getWidth()
    {
        return width;
    }
    
    /**
     * Set the image's width
     */
    protected void setWidth(int width)
    {
        this.width = width;
    }
    
    /**
     * Get the image's height
     */
    public int getHeight()
    {
        return height;
    }
    
    /**
     * Set the image's height
     */
    protected void setHeight(int height)
    {
        this.height = height;
    }
    
    /**
     * set the color key mask. It is an array of start/end entries
     * to indicate ranges of color indicies that should be masked out.
     * 
     * @param maskArrayObject
     */
    private void setColorKeyMask(PDFObject maskArrayObject) throws IOException
    {
        PDFObject[] maskObjects = maskArrayObject.getArray();
        colorKeyMask = null;
        int len;
        int[] masks = new int[len = maskObjects.length];
        for (int i = 0; i < len; i++)
        {
            masks[i] = maskObjects[i].getIntValue();
        }
        colorKeyMask = masks;
    }
    
    /**
     * Get the colorspace associated with this image, or null if there
     * isn't one
     */
    protected PDFColorSpace getColorSpace()
    {
        return colorSpace;
    }
    
    /**
     * Set the colorspace associated with this image
     */
    protected void setColorSpace(PDFColorSpace colorSpace)
    {
        this.colorSpace = colorSpace;
    }
    
    /**
     * Get the number of bits per component sample
     */
    protected int getBitsPerComponent()
    {
        return bpc;
    }
    
    /**
     * Set the number of bits per component sample
     */
    protected void setBitsPerComponent(int bpc)
    {
        this.bpc = bpc;
    }
    
    /**
     * Return whether or not this is an image mask
     */
    public boolean isImageMask()
    {
        return imageMask;
    }
    
    /**
     * Set whether or not this is an image mask
     */
    public void setImageMask(boolean imageMask)
    {
        this.imageMask = imageMask;
    }
    
    /** 
     * Return the soft mask associated with this image
     */
    public PDFImage getSMask()
    {
        return sMask;
    }
    
    /**
     * Set the soft mask image
     */
    protected void setSMask(PDFImage sMask)
    {
        this.sMask = sMask;
    }
    
    /**
     * Get the decode array
     */
    protected float[] getDecode()
    {
        return decode;
    }
    
    /**
     * Set the decode array
     */
    protected void setDecode(float[] decode)
    {
    	float max = (1 << getBitsPerComponent()) - 1;
        this.decode = decode;
        this.decodeCoefficients = new float[decode.length / 2];
        this.decodeMins = new float[decode.length / 2];
        int len = decode.length;
        for (int i = 0; i < len; i += 2)
        {
            decodeMins[i/2] = decode[i];
            decodeCoefficients[i/2] = (decode[i + 1] - decode[i]) / max;
        }
    }
    
    /**
     * Refactor part of parseData and consolidate some functions.
     */
    private Bitmap loadImage(byte[] data, ByteBuffer jpegData)
    {
    	Bitmap bi = null;
    	
    	if(jpegData != null)
    	{
    		// Use EncodedImage to decode the JPEG into a Bitmap
    		
            // TODO - strictly speaking, the application of the YUV->RGB
            // transformation when reading JPEGs does not adhere to the spec.
            // We're just going to let JAI read this in - as it is, JAI looks
            // for the specific Adobe marker header so that it may apply
            // the transform, so that's good. If that marker isn't present,
            // then it also applies a number of other heuristics to determine
            // whether the transform should be applied.
            // (http://java.sun.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html)
            // In practice, it probably almost always does the right thing here,
            // though note that the present or default value of the ColorTransform
            // dictionary entry is not being observed, so there is scope for
            // error
    		// NOTE - It's not entirely clear what EncodedImage does but it works and produces an RGB image
    		ByteBufferInputStream in = null;
    		try
    		{
	    		in = new ByteBufferInputStream(jpegData);
	    		EncodedImage ei = EncodedImage.createEncodedImage(IOUtilities.streamToBytes(in), 0, -1, "image/jpeg");
	    		if(ei.getWidth() == this.width && ei.getHeight() == this.height)
	    		{
	    			//Image is correct size, return it
	    			bi = ei.getBitmap();
	    		}
	    		else
	    		{
	    			//Image is not the correct size, resize it
	    			int xScale = Fixed32.div(Fixed32.toFP(ei.getWidth()), Fixed32.toFP(this.width));
	    			int yScale = Fixed32.div(Fixed32.toFP(ei.getHeight()), Fixed32.toFP(this.height));
	    			ei = ei.scaleImage32(xScale, yScale);
	    			ei.setDecodeMode(EncodedImage.DECODE_READONLY);
	    			bi = ei.getBitmap();
	    		}
    		}
    		catch(Exception e)
    		{
    		}
    		finally
    		{
    			if(in != null)
    			{
    				try
    				{
    					in.close();
    				}
    				catch(IOException e)
    				{
    				}
    			}
    		}
    		jpegData = null;
    	}
    	else
    	{
    		int[] pixData = new int[getWidth() * getHeight()];
    		
    		bi = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, getWidth(), getHeight());
    		if(readData(data, pixData))
    		{
    			bi.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
    		}
    		
    		//Set the data
    		bi.setARGB(pixData, 0, getWidth(), 0, 0, getWidth(), getHeight());
    	}
    	
    	/*
    	//XXX OLD-Kept until testing of this class is complete
    	InputStream seek = null;
    	try
    	{
    		seek = new ByteArrayInputStream(data);
    		
    		//Create the image data itself
    		int[] pixData = new int[getWidth() * getHeight()];
    		boolean alpha = false;
    		
    		//Create the pixel data
    		//TODO: This can be made much more efficient.
    		PDFColorSpace cs = getColorSpace();
    		byte[] bData = readData(seek, cs);
            int imgLen = bData.length;
    		
    		if (cs instanceof IndexedColor)
            {
    			//IndexedColorModel
                IndexedColor ics = (IndexedColor)cs;
                
                byte[] components = ics.getColorComponents();
                int num = ics.getCount();
                
                // process the decode array
                if (decode != null)
                {
                    byte[] normComps = new byte[components.length];
                    
                    // move the components array around
                    for (int i = 0; i < num; i++)
                    {
                        byte[] orig = new byte[1];
                        orig[0] = (byte)i;
                        
                        float[] res = normalize(orig, null, 0);
                        int idx = (int) res[0];
                        
                        normComps[i * 3] = components[idx * 3];
                        normComps[(i * 3) + 1] = components[(idx * 3) + 1];
                        normComps[(i * 3) + 2] = components[(idx * 3) + 2];
                    }
                    
                    components = normComps;
                }
                
                // make sure the size of the components array is 2 ^ numBits
                // since if it's not, Java will complain
                int correctCount = 1 << getBitsPerComponent();
                if (correctCount < num)
                {
                    byte[] fewerComps = new byte[correctCount * 3];
                    
                    System.arraycopy(components, 0, fewerComps, 0, correctCount * 3);
                    
                    components = fewerComps;
                    num = correctCount;
                }
                if (colorKeyMask == null || colorKeyMask.length == 0)
                {
                	alpha = false;
                	for(int i = 0; i < imgLen; i++)
                	{
                		int pos = bData[i];
                		pixData[i] = (components[pos + 0] << 16) | (components[pos + 1] << 8) | components[pos + 2];
                	}
                }
                else
                {
                    byte[] aComps = new byte[num * 4];
                    int idx = 0;
                    for (int i = 0; i < num; i++)
                    {
                        aComps[idx++] = components[(i * 3)];
                        aComps[idx++] = components[(i * 3) + 1];
                        aComps[idx++] = components[(i * 3) + 2];
                        aComps[idx++] = (byte)0xFF;
                    }
                    int len = colorKeyMask.length;
                    for (int i = 0; i < len; i += 2)
                    {
                        for (int j = colorKeyMask[i]; j <= colorKeyMask[i + 1]; j++)
                        {
                            aComps[(j * 4) + 3] = 0;    // make transparent
                        }
                    }
                    alpha = true;
                    for(int i = 0; i < imgLen; i++)
                	{
                		int pos = bData[i];
                		pixData[i] = (aComps[pos + 3] << 24) | (aComps[pos + 0] << 16) | (aComps[pos + 1] << 8) | aComps[pos + 2];
                	}
                }
            }
            else
            {
            	//ComponentColorModel, most code is based off of ComponentColorModel and ColorModel.
            	ColorSpace jcs = cs.getColorSpace(); //Might need to convert values to RGB
            	ComponentDecoder dec = new ComponentDecoder(jcs, getBitsPerComponent());
            	alpha = false;
            	
            	dec.decode(bData, imgLen, pixData, pixData.length);
            }
    		
    		bi = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, getWidth(), getHeight());
    		if(alpha)
    		{
    			bi.createAlpha(Bitmap.ALPHA_BITDEPTH_8BPP);
    		}
    		
    		//Set the data
    		bi.setARGB(pixData, 0, getWidth(), 0, 0, getWidth(), getHeight());
    	}
    	catch(Exception e)
    	{
    	}
    	finally
    	{
    		if(seek != null)
    		{
    			try
    			{
    				seek.close();
    			}
    			catch(IOException ioe)
    			{
    			}
    		}
    	}
    	*/
    	
    	return bi;
    }
    
    //If you want an easy explanation: data is, well, data. preprocessData is the equivalent of a SampleModel. readData (after preprocessData) is the equivalent of ColorModel.
    /**
     * Convert the data to A/RGB data.
     * @param data The data to convert.
     * @param bmpData The converted data.
     * @return If alpha exists or not.
     */
    private boolean readData(byte[] data, int[] argbData)
    {
    	PDFColorSpace cs = getColorSpace();
    	boolean alpha = false;
    	
    	byte[] pdata = preprocessData(data, cs);
    	int dataLen = pdata.length;
    	
    	if (cs instanceof IndexedColor)
        {
			//IndexedColorModel
            IndexedColor ics = (IndexedColor)cs;
            
            byte[] components = ics.getColorComponents();
            int num = ics.getCount();
            
            // process the decode array
            if (decode != null)
            {
                byte[] normComps = new byte[components.length];
                
                // move the components array around
                for (int i = 0; i < num; i++)
                {
                    byte[] orig = new byte[1];
                    orig[0] = (byte)i;
                    
                    float[] res = normalize(orig, null, 0);
                    int idx = (int) res[0];
                    
                    normComps[i * 3] = components[idx * 3];
                    normComps[(i * 3) + 1] = components[(idx * 3) + 1];
                    normComps[(i * 3) + 2] = components[(idx * 3) + 2];
                }
                
                components = normComps;
            }
            
            // make sure the size of the components array is 2 ^ numBits
            // since if it's not, Java will complain
            int correctCount = 1 << getBitsPerComponent();
            if (correctCount < num)
            {
                byte[] fewerComps = new byte[correctCount * 3];
                
                System.arraycopy(components, 0, fewerComps, 0, correctCount * 3);
                
                components = fewerComps;
                num = correctCount;
            }
            if (colorKeyMask == null || colorKeyMask.length == 0)
            {
            	//alpha = false;
            	for(int i = 0; i < dataLen; i++)
            	{
            		int pos = pdata[i] & 0xFF;
            		argbData[i] = (components[pos + 0] << 16) | (components[pos + 1] << 8) | components[pos + 2];
            	}
            }
            else
            {
                byte[] aComps = new byte[num * 4];
                int idx = 0;
                for (int i = 0; i < num; i++)
                {
                    aComps[idx++] = components[(i * 3)];
                    aComps[idx++] = components[(i * 3) + 1];
                    aComps[idx++] = components[(i * 3) + 2];
                    aComps[idx++] = (byte)0xFF;
                }
                int len = colorKeyMask.length;
                for (int i = 0; i < len; i += 2)
                {
                    for (int j = colorKeyMask[i]; j <= colorKeyMask[i + 1]; j++)
                    {
                        aComps[(j * 4) + 3] = 0;    // make transparent
                    }
                }
                alpha = true;
                for(int i = 0; i < dataLen; i++)
            	{
            		int pos = pdata[i] & 0xFF;
            		argbData[i] = (aComps[pos + 3] << 24) | (aComps[pos + 0] << 16) | (aComps[pos + 1] << 8) | aComps[pos + 2];
            	}
            }
        }
        else
        {
        	//Component-based images
            if(decode != null)
            {
            	int com = cs.getNumComponents();
            	float[] norm = new float[com];
            	byte[] pix = new byte[com];
            	int k;
            	for(int i = 0, d = 0; i < dataLen; i += com) //Since 
            	{
            		for(k = 0; k < com; k++)
            		{
            			pix[k] = pdata[k + i];
            		}
            		normalize(pix, norm, 0);
            		argbData[d++] = 0xFF000000 | rgb2int(norm[0], norm[1], norm[2]);
            	}
            }
            else
            {
            	ColorSpace jcs = cs.getColorSpace();
                ColorSpace rgbCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                if (isGreyscale(jcs) && bpc <= 8)
                {
                	//Speed up processing
                	convertGreyscaleToArgb(pdata, argbData);
                }
//#ifdef FORCE_IMG_DECODE
                else
//#else
                else if (!isImageMask() && jcs instanceof ICC_ColorSpace && !jcs.equals(rgbCS))
//#endif
                {
                	//This is very slow, hopefully it will never come to this.
                	
	            	//ComponentColorModel, most code is based off of ComponentColorModel and ColorModel.
	            	ComponentDecoder dec = new ComponentDecoder(jcs, getBitsPerComponent());
	            	
	            	dec.decode(pdata, dataLen, argbData, argbData.length); //Might need to convert values to RGB
                }
                //XXX What about if the image is RGB? and FORCE_IMG_DECODE is disabled
            }
            
            //Don't need to handle color space conversion: IndexedColor is already in RGB, JPEG is handled for us, the last 3 possibilities already convert to RGB if not already in it.
        }
    	return alpha;
    }
    
    private static int rgb2int(float r, float g, float b)
	{
		return (((int)(r * 255.0f + 0.5f)) << 16) | (((int)(g * 255.0f + 0.5f)) << 8) | ((int)(b * 255.0f + 0.5f));
	}
    
    private boolean isGreyscale(ColorSpace aCs)
    {
        return aCs.getType() == ColorSpace.TYPE_GRAY;
    }
    
    private void convertGreyscaleToArgb(byte[] data, int[] argb)
    {
    	// we use an optimised greyscale colour conversion, as with scanned
        // greyscale/mono documents consisting of nothing but page-size
        // images, using the ICC converter is perhaps 15 times slower than this
        // method. Using an example scanned, mainly monochrome document, on this
        // developer's machine pages took an average of 3s to render using the
        // ICC converter filter, and around 115ms using this method. We use
        // pre-calculated tables generated using the ICC converter to map between
        // each possible greyscale value and its desired value in sRGB.
        // We also try to avoid going through SampleModels, WritableRasters or
        // BufferedImages as that takes about 3 times as long.
    	int i = 0;
        final int[] greyToArgbMap = getGreyToArgbMap(bpc, getColorSpace().getColorSpace());
        final int width = getWidth();
        final int height = getHeight();
        if (bpc == 1)
        {
            int calculatedLineBytes = (width + 7) / 8;
            int rowStartByteIndex;
            // avoid hitting the WritableRaster for the common 1 bpc case
            if (greyToArgbMap[0] == 0 && greyToArgbMap[1] == 0xFFFFFFFF)
            {
                // optimisation for common case of a direct map to full white
                // and black, using bit twiddling instead of consulting the
                // greyToArgb map
                for (int y = 0; y < height; ++y)
                {
                    // each row is byte-aligned
                    rowStartByteIndex = y * calculatedLineBytes;
                    for (int x = 0; x < width; ++x)
                    {
                        final byte b = data[rowStartByteIndex + x / 8];
                        final int white = b >> (7 - (x & 7)) & 1;
                        // if white == 0, white - 1 will be 0xFFFFFFFF,
                        //  which when xored with 0xFFFFFF will produce 0
                        // if white == 1, white - 1 will be 0,
                        //  which when xored with 0xFFFFFF will produce 0xFFFFFF
                        //  (ignoring the top two bytes, which are always set high anyway)
                        argb[i] = 0xFF000000 | ((white - 1) ^ 0xFFFFFF);
                        ++i;
                    }
                }
            }
            else
            {
                // 1 bpc case where we can't bit-twiddle and need to consult
                // the map
                for (int y = 0; y < height; ++y)
                {
                    rowStartByteIndex = y * calculatedLineBytes;
                    for (int x = 0; x < width; ++x)
                    {
                        final byte b = data[rowStartByteIndex + x / 8];
                        final int val = b >> (7 - (x & 7)) & 1;
                        argb[i] = greyToArgbMap[val];
                        ++i;
                    }
                }
            }
        }
        else
        {
            for (int y = 0; y < height; ++y)
            {
                for (int x = 0; x < width; ++x)
                {
                    final int greyscale = data[x + y * width] & 0xFF;
                    argb[i] = greyToArgbMap[greyscale];
                    ++i;
                }
            }
        }
    }
    
    /**
     * Preprocess the data so bytes aren't packed together.
     * @param data The data to "preprocess"
     * @param cs The colorspace to use.
     * @return The preprocessed data
     */
    private byte[] preprocessData(byte[] data, PDFColorSpace cs)
    {
    	int comCount = cs.getNumComponents();
    	int bpc = getBitsPerComponent();
    	byte[] ndata;
    	int len = data.length;
    	if(comCount == 1 && bpc < 8)
    	{
    		//Packed
    		
    		int finalShift = 8 - bpc;
    		int finalMask = ((byte)(0xF << finalShift)) & 0xFF;
    		int pixPerByte = 8 / bpc;
    		len *= pixPerByte;
    		ndata = new byte[len];
    		for(int d = 0, s = 0; d < len; s++)
    		{
    			int shift = finalShift;
    			int mask = finalMask;
    			for(int i = 0; i < pixPerByte; i++)
    			{
    				ndata[d++] = (byte)((data[s] & mask) >>> shift);
    				shift -= finalShift;
    				mask >>= bpc;
    			}
    		}
    	}
    	else
    	{
    		int pixelSize = comCount * bpc;
    		if(bpc < 8 && pixelSize != 1 && pixelSize != 2 && pixelSize != 4)
    		{
    			PDFUtil.assert(bpc < 8, "bpc < 8", "This is designed just for use with per-component sizes of less than 8 bits; you should probably use PixelInterleavedSampleModel");
    			PDFUtil.assert(bpc == 1 || bpc == 2 || bpc == 4, "bpc == 1 || bpc == 2 || bpc == 4", "we don't want to grab components across byte boundaries");
    			
    			//PdfSubByteSampleModel
    			int w = getWidth();
    			int h = getHeight();
    			ndata = new byte[comCount * w * getHeight()];
    			
    			int bitsPerLine = 8 * ((pixelSize * w + 7) / 8);
    			int componentMask = (1 << bpc) - 1;
    			int ignoredBitsPerComponentPerByte = 8 - bpc;
    			
    			//Do this the slow-and-steady way, going through each component.
    			for(int x = 0, d = 0; x < w; x++)
    			{
    				for(int y = 0; y < h; y++)
    				{
    					for(int i = 0; i < comCount; i++)
    					{
    						int pos = y * bitsPerLine + pixelSize * x + bpc * i;
    						ndata[d++] = (byte)((data[pos >> 3] >>> (ignoredBitsPerComponentPerByte - (pos & 7))) & componentMask); //Note: An odd gap occurs when changing height
    					}
    				}
    			}
    		}
    		else
    		{
	    		//Component
	    		
	    		//PDFImage does everything with bytes so no need to worry about SHORT/USHORT/etc. sized items.
	    		ndata = data; //ComponentSampleModel simply reads the data as it is.
    		}
    	}
    	return ndata;
    }
    
    /* XXX OLD-Kept until testing of this class is complete
    /**
     * Read in component based/packed pixel based images.
     * @return The image data.
     * /
    private byte[] readData(InputStream in, PDFColorSpace cs) throws IOException
    {
    	int comCount = cs.getNumComponents();
    	int len = in.available();
    	byte[] data;
    	if(comCount == 1 && getBitsPerComponent() < 8)
    	{
    		//Packed
    		
    		int bpc = getBitsPerComponent();
    		int finalShift = 8 - bpc;
    		int finalMask = ((byte)(0xF << finalShift)) & 0xFF;
    		int pixPerByte = 8 / bpc;
    		len *= pixPerByte;
    		data = new byte[len];
    		int offset = len - in.available();
    		in.read(data, offset, in.available());
    		for(int d = 0, s = offset; d < len; s++)
    		{
    			int shift = finalShift;
    			int mask = finalMask;
    			for(int i = 0; i < pixPerByte; i++)
    			{
    				data[d++] = (byte)((data[s] & mask) >>> shift);
    				shift -= finalShift;
    				mask >>= bpc;
    			}
    		}
    	}
    	else
    	{
    		//Component
    		
    		//PDFImage does everything with bytes so no need to worry about SHORT/USHORT/etc. sized items.
    		data = new byte[len];
    		in.read(data, 0, len); //ComponentSampleModel simply reads the data as it is.
    	}
    	return data;
    }
    */
    
    /**
     * Normalize an array of values to match the decode array
     */
    private float[] normalize(byte[] pixels, float[] normComponents, int normOffset)
    {
        if (normComponents == null)
        {
            normComponents = new float[normOffset + pixels.length];
        }
        
        // trivial loop unroll - saves a little time
        switch (pixels.length)
        {
	        case 4:
	            normComponents[normOffset + 3] = decodeMins[3] + (float)(pixels[3] & 0xFF) * decodeCoefficients[3];
	        case 3:
	            normComponents[normOffset + 2] = decodeMins[2] + (float)(pixels[2] & 0xFF) * decodeCoefficients[2];
	        case 2:
	            normComponents[normOffset + 1] = decodeMins[1] + (float)(pixels[1] & 0xFF) * decodeCoefficients[1];
	        case 1:
	            normComponents[normOffset ] = decodeMins[0] + (float)(pixels[0] & 0xFF) * decodeCoefficients[0];
	            break;
	        default:
	            throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.IMAGE_NORMALIZE_NEED_MORE_THEN_4_COM_IMPL));
        }
        
        return normComponents;
    }
    
    /**
     * Decoder for component based images. DOES NOT HANDLE ALPHA!
     */
    class ComponentDecoder
    {
    	//Based off of java.awt.image.ComponentColorModel and java.awt.image.ColorModel.
    	
    	private ColorSpace cs;
    	private boolean is_sRGB;
    	private int numComponents;
    	private int bitsPerCom;
    	private byte[][] colorLUTs;
    	private float scaleFactor;
    	
		public ComponentDecoder(ColorSpace cs, int bitsPerComponent)
		{
			this.cs = cs;
			this.is_sRGB = cs.isCS_sRGB();
			this.numComponents = cs.getNumComponents();
			this.bitsPerCom = bitsPerComponent;
			
			int maxValue = (1 << bitsPerCom) - 1;
			scaleFactor = 1.0f / maxValue;
			
            for (int i = 0; i < numComponents; i++)
            {
                if (cs.getMinValue(i) != 0.0f || cs.getMaxValue(i) != 1.0f)
                {
                	throw new IllegalArgumentException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getString(com.sun.pdfview.i18n.ResourcesResource.IMAGE_COLORSPACE_NOMALIZED_ONLY));
                }
            }
			
			this.colorLUTs = new byte[3][];
			if (is_sRGB)
			{
                for (int i = 0; i < numComponents; i++)
                {
                    if (bitsPerCom != 8)
                    {
                        colorLUTs[i] = new byte[maxValue + 1];
                        for (int j = 0; j <= maxValue; j++)
                        {
                            colorLUTs[i][j] = (byte)(scaleFactor * j + 0.5f);
                        }
                    }
                }
            }
		}
		
		public int decode(byte[] input, int iLen, int[] output, int oLen)
		{
			int o, i;
			byte[] in = new byte[numComponents];
			float[] normComp = null;
			for(o = 0, i = 0; o < oLen && i < iLen; o++, i += numComponents)
        	{
				System.arraycopy(input, i, in, 0, numComponents);
        		if (is_sRGB)
        		{
                    int comp1 = in[0] & 0xff;
                    int comp2 = in[1] & 0xff;
                    int comp3 = in[2] & 0xff;
                    if (bitsPerCom != 8)
                    {
                    	comp1 = colorLUTs[0][comp1] & 0xff;
                    	comp2 = colorLUTs[1][comp2] & 0xff;
                    	comp3 = colorLUTs[2][comp3] & 0xff;
                    }
                    output[o] = 0xFF000000 | (comp1 << 16) | (comp2 << 8) | comp3;
                }
        		else
        		{
	                normComp = getNormalizedComponents(in, normComp, 0);
	                float[] rgbComp = cs.toRGB(normComp);
	                output[o] = 0xFF000000 | rgb2int(rgbComp[0], rgbComp[1], rgbComp[2]);
        		}
        	}
			return o;
		}
		
		public float[] getNormalizedComponents(byte[] pixel, float[] normComponents, int normOffset)
        {
            if (getDecode() == null)
            {
            	if (normComponents == null)
            	{
                    normComponents = new float[numComponents + normOffset];
                }
            	for (int i = 0, idx = normOffset; i < numComponents; i++, idx++)
            	{
                    normComponents[idx] = (pixel[i] & 0xff) * scaleFactor;
                }
            	return normComponents;
            }
            
            return normalize(pixel, normComponents, normOffset);
        }
    }
}
