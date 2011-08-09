//#preprocessor

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0

/*
 * File: GraphicsImplOpenVG.java
 * Version: 1.0
 * Initial Creation: Aug 7, 2011 7:52:43 PM
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
package com.sun.pdfview.helper.graphics.drawing.net.rim.device.api.ui.GraphicsInternalGraphics;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import net.rim.device.api.egl.EGL12;
import net.rim.device.api.openvg.VG;
import net.rim.device.api.openvg.VGUtils;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.XYRect;

import com.sun.pdfview.ResourceManager;
import com.sun.pdfview.helper.AffineTransform;
import com.sun.pdfview.helper.PDFGraphics;
import com.sun.pdfview.helper.graphics.BasicStroke;
import com.sun.pdfview.helper.graphics.Composite;
import com.sun.pdfview.helper.graphics.Geometry;
import com.sun.pdfview.helper.graphics.Paint;
import com.sun.pdfview.i18n.ResourcesResource;

/**
 * PDFgraphics implementation of Graphics, this is the native Graphics system only.
 */
final class GraphicsImplOpenVG extends PDFGraphics implements GraphicsImpl.InnerAccess
{
	private EGL10 egl;
	private EGLDisplay display;
    private EGLConfig config;
    private EGLContext context;
    private EGLSurface surface;
    private int surfaceType;
    private VG vg;
    
    private int[] version = new int[2];
    private Bitmap pixmap;
    private int bindDepth;
    
    private Graphics destination;
    
    private String[] properties;
    private PDFGraphics gfx;
	
	public static PDFGraphics tryAndGet()
	{
		if(VGUtils.isSupported())
		{
			try
			{
				//TODO: Do a better check to make sure that OpenVG is actually supported and would work.
				return new GraphicsImplOpenVG();
			}
			catch(RuntimeException re)
			{
			}
		}
		return null;
	}
	
	public GraphicsImplOpenVG()
	{
		bindDepth = 0;
		
		// Get EGL
		egl = (EGL10)EGLContext.getEGL();
		
		// Get the display from the primary DisplayInstance
		display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (egl.eglGetError() != EGL10.EGL_SUCCESS)
        {
            throw new RuntimeException();
        }
        
        // Initialize EGL
        egl.eglInitialize(display, version);
        if (egl.eglGetError() != EGL10.EGL_SUCCESS)
        {
            throw new RuntimeException();
        }
        
        if(version[1] < 2)
        {
        	throw new RuntimeException(); //OpenVG needs EGL 1.2 or higher
        }
	}
	
	protected void onFinished()
	{
		PDFGraphics.finishGraphics(this.gfx);
		
		egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroyContext(display, context);
        egl.eglDestroySurface(display, surface);
	}
	
	public void setDrawingDeviceIn(Object device)
	{
		setDrawingDevice(device);
	}
	
	protected void setDrawingDevice(Object device)
	{
		if(device == null)
		{
			throw new NullPointerException();
		}
		this.destination = (Graphics)device;
		
		//Set the surface type
		surfaceType = EGL10.EGL_PBUFFER_BIT; //This is a safe bet since it isn't drawing to the native graphics system
		/* XXX Need to figure out how to tell native (from Field/Screen/native GUI) Graphics object from one returned by a Bitmap or "other" item
		Class drawClass = null;
		try
		{
			drawClass = Class.forName("net.rim.device.api.ui.GraphicsInternal"); //This is the Graphics returned by the native system and not by a image
		}
		catch (ClassNotFoundException e)
		{
		}
		if(this.destination.getClass().equals(drawClass))
		{
			surfaceType = EGL10.EGL_WINDOW_BIT; //Use the native window
		}
		*/
		
		// Describe our config criteria attributes.
        int[] attribs =
        {
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
            EGL10.EGL_RED_SIZE,			8,
            EGL10.EGL_GREEN_SIZE,		8,
            EGL10.EGL_BLUE_SIZE,		8,
            /*
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0
            EGL10.EGL_RED_SIZE,			5,
            EGL10.EGL_GREEN_SIZE,		6,
            EGL10.EGL_BLUE_SIZE,		5,
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
            */
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0
            EGL10.EGL_ALPHA_SIZE,		8,
            EGL10.EGL_SURFACE_TYPE,		surfaceType,
            EGL12.EGL_RENDERABLE_TYPE,	EGL12.EGL_OPENVG_BIT,
            EGL10.EGL_NONE
        };
        
        // Choose the first config found.
        EGLConfig[] configs = new EGLConfig[1];
        int[] num_configs = new int[1];
        egl.eglChooseConfig(display, attribs, configs, 1, num_configs);
        if (egl.eglGetError() != EGL10.EGL_SUCCESS)
        {
            throw new RuntimeException();
        }
        config = configs[0];
        
        //Create the VG context
        createContext();
        
        this.gfx = PDFGraphics.createGraphics(this.vg);
        if(!this.gfx.isValid())
        {
        	this.properties = this.gfx.getSupportedProperties();
        	this.gfx.setProperty(this.properties[0], new Runnable() //BIND
        	{
				public void run()
				{
					bindTarget();
				}
			});
        	this.gfx.setProperty(this.properties[1], new Runnable() //RELEASE
        	{
				public void run()
				{
					releaseTarget();
				}
			});
        }
	}
	
	private void createContext()
    {
        ((EGL12)egl).eglBindAPI(EGL12.EGL_OPENVG_API);
        if (egl.eglGetError() != EGL10.EGL_SUCCESS)
        {
            throw new RuntimeException();
        }
        
        context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, null);
        if (egl.eglGetError() != EGL10.EGL_SUCCESS)
        {
            throw new RuntimeException();
        }
        vg = VGUtils.getVG(context);
        if(this.gfx != null)
        {
        	this.gfx.setProperty(this.properties[2], vg); //VG
        }
    }
	
	private void contextLost()
    {
        createContext();
        if (!egl.eglMakeCurrent(display, surface, surface, context))
        {
            throw new RuntimeException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getFormattedString(ResourcesResource.HELPER_GRAPHICS_DRAW_RIM_GRAPHICS_SUB_VG_CONTEXT_LOST, new Object[]{new Integer(egl.eglGetError())}));
        }
    }
	
	private void bindTarget()
    {
		bindDepth++;
		if(bindDepth == 1)
		{
	        if (surface == null)
	        {
	            switch(surfaceType)
	            {
		            case EGL10.EGL_WINDOW_BIT:
		                surface = egl.eglCreateWindowSurface(display, config, this.destination, null);
		                if (egl.eglGetError() != EGL10.EGL_SUCCESS)
		                {
		                	throw new RuntimeException();
		                }
		                break;
		            case EGL10.EGL_PBUFFER_BIT:
		            	XYRect clipRect = this.destination.getClippingRect();
		                int[] surfaceAttribs = new int[]
		                {
		                    EGL11.EGL_WIDTH,	clipRect.width,
		                    EGL11.EGL_HEIGHT,	clipRect.height,
		                    EGL11.EGL_NONE
		                };
		                surface = egl.eglCreatePbufferSurface(display, config, surfaceAttribs);
		                if (egl.eglGetError() != EGL10.EGL_SUCCESS)
		                {
		                	throw new RuntimeException();
		                }
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
		                pixmap = new Bitmap(Bitmap.ROWWISE_32BIT_XRGB8888, clipRect.width, clipRect.height);
		                /*
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0
		                pixmap = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, clipRect.width, clipRect.height);
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0
		                */
//#endif
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1 | BlackBerrySDK5.0.0
		                break;
	            }
	        }
	        
	        if (!egl.eglMakeCurrent(display, surface, surface, context))
	        {
	            if (egl.eglGetError() == EGL11.EGL_CONTEXT_LOST)
	            {
	                contextLost();
	            }
	            else
	            {
	                throw new RuntimeException("eglMakeCurrent error");
	            }
	        }
	        
	        if (surfaceType == EGL10.EGL_WINDOW_BIT || surfaceType == EGL10.EGL_PIXMAP_BIT)
	        {
	            egl.eglWaitNative(EGL10.EGL_CORE_NATIVE_ENGINE, this.destination);
	        }
		}
    }
	
	private void releaseTarget()
    {
		bindDepth--;
		if(bindDepth == 0)
		{
	        switch(surfaceType)
	        {
		        case EGL10.EGL_WINDOW_BIT:
		            ((EGL12)egl).eglWaitClient();
		            break;
		        case EGL10.EGL_PBUFFER_BIT:
		            // You can either copy the surface to Graphics or a mutable Bitmap/Image here.
		            egl.eglCopyBuffers(display, surface, pixmap);
		            if (pixmap != null)
		            {
		               this.destination.drawBitmap(0, 0, pixmap.getWidth(), pixmap.getHeight(), pixmap, 0, 0);
		            }
		            break;
	        }
		}
    }
	
	public void clear(int x, int y, int width, int height)
	{
		this.gfx.clear(x, y, width, height);
	}
	
	public void draw(Geometry s)
	{
		this.gfx.draw(s);
	}
	
	public boolean drawImage(Bitmap img, AffineTransform xform)
	{
		return this.gfx.drawImage(img, xform);
	}
	
	public void fill(Geometry s)
	{
		this.gfx.fill(s);
	}
	
	public Geometry getClip()
	{
		return this.gfx.getClip();
	}
	
	public AffineTransform getTransform()
	{
		return this.gfx.getTransform();
	}
	
	public void setBackgroundColor(int c)
	{
		this.gfx.setBackgroundColor(c);
	}
	
	public void setClipIn(Geometry s, boolean direct)
	{
		setClip(s, direct);
	}
	
	protected void setClip(Geometry s, boolean direct)
	{
		if(direct)
		{
			this.gfx.setClip(s);
		}
		else
		{
			this.gfx.clip(s);
		}
	}
	
	public void setColor(int c)
	{
		this.gfx.setColor(c);
	}
	
	public void setComposite(Composite comp)
	{
		this.gfx.setComposite(comp);
	}
	
	public void setPaint(Paint paint)
	{
		this.gfx.setPaint(paint);
	}
	
	public void setRenderingHint(int hintKey, int hintValue)
	{
		this.gfx.setRenderingHint(hintKey, hintValue);
	}
	
	public void setStroke(BasicStroke s)
	{
		this.gfx.setStroke(s);
	}
	
	public void setTransformIn(AffineTransform Tx, boolean direct)
	{
		setTransform(Tx, direct);
	}
	
	protected void setTransform(AffineTransform Tx, boolean direct)
	{
		if(direct)
		{
			this.gfx.setTransform(Tx);
		}
		else
		{
			this.gfx.transform(Tx);
		}
	}
	
	public void translate(int x, int y)
	{
		this.gfx.translate(x, y);
	}
}

//#endif
