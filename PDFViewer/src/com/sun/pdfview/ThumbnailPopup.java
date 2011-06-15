//#preprocessor

/*
 * File: ThumbnailPopup.java
 * Version: 1.0
 * Initial Creation: Jun 5, 2010 3:39:23 PM
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

import net.rim.device.api.system.Application;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.ScrollChangeListener;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * Display the Thumbnails of a PDF.
 */
public class ThumbnailPopup extends PopupScreen implements ScrollChangeListener, Runnable
{
	private static final int DISTANCE_FROM_EDGE = 3;
	static final int THUMB_BUFFER = 3;
	private static final int THUMB_HEIGHT = 96 + THUMB_BUFFER;
	private static final int THUMB_DEFAULT_WIDTH = (THUMB_HEIGHT * 4) / 3;
	
	/** The PDFFile being displayed */
	private PDFFile file;
	/** Thread that renders each thumbnail in turn */
	private Thread anim;
	/**
     * Which thumbnail needs to be drawn next, or -1 if the previous
     * needy thumbnail is being processed.
     */
    private int needdrawn = -1;
    private int lastThumb = 0;
    
    private Tracker tracker;
    private PDFViewer.PDFViewerScreen viewer;
    
    private class Tracker extends PageTracker
    {
		public void pageClick(int index)
		{
			thumbClick(index);
		}
    }
    
    private ThumbnailPopup(VerticalFieldManager man, PDFViewer.PDFViewerScreen pdfviewer)
    {
    	super(man, PopupScreen.DEFAULT_CLOSE);
    	man.setScrollListener(this);
    	this.tracker = new Tracker();
    	this.viewer = pdfviewer;
    }
    
    protected void sublayout(int width, int height)
	{
    	super.sublayout(width, height);
    	super.setPosition(DISTANCE_FROM_EDGE, getContentTop());
    }
    
	public void scrollChanged(Manager manager, int newHorizontalScroll, int newVerticalScroll)
	{
		//Custom drawing will cause GFX problems, invalidate everything to fix this.
		manager.invalidate();
	}
	
	public static PopupScreen createThumbnailPopup(PDFFile file, PDFViewer.PDFViewerScreen pdfviewer)
	{
		ThumbnailPopup pop = new ThumbnailPopup(new VerticalFieldManager(VerticalFieldManager.VERTICAL_SCROLL), pdfviewer);
		if(file != null)
		{
			pop.setPDF(file);
			pop.startProcess();
		}
		return pop;
	}
	
	//TODO: Add button for sliding.
	
	private void thumbClick(int index)
	{
		this.getDelegate().invalidate();
		this.viewer.gotoPage(index + 1);
	}
	
	public void startProcess()
	{
		anim = new Thread(this);
        anim.start();
	}
	
	public void reset()
	{
		stop();
		synchronized(tracker)
		{
			this.tracker.reset();
		}
		this.file = null;
		this.deleteAll();
	}
	
	public void stop()
	{
		if(anim != null)
		{
			if(anim.isAlive())
			{
				anim = null; //This should cause the thread to stop looping and return
				/*
				try
				{
					anim.join();
				}
				catch (InterruptedException e)
				{
				}
				*/
			}
		}
	}
	
	public void showThumbnail(int page)
	{
		//TODO
		System.out.println("ThumbnailPopup: " + page);
	}

	public void setPDF(PDFFile file)
	{
		reset();
		this.file = file;
		if(file != null)
		{
			int c;
			PageField[] fields = new PageField[c = file.getNumPages()];
			for(int i = 0; i < c; i++)
			{
				fields[i] = new PageField(null, THUMB_DEFAULT_WIDTH, THUMB_HEIGHT, this.tracker);
			}
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
			this.getDelegate().addAll(fields);
//#else
			Manager man = this.getDelegate();
			for(int i = 0; i < c; i++)
			{
				man.add(fields[i]);
			}
//#endif
		}
	}
	
	public void close()
	{
		stop();
		super.close();
	}

	public void run()
	{
		int workingon = 0;	// the thumbnail we'll be rendering next.
		
		while (anim == Thread.currentThread())
		{
			if (needdrawn >= 0)
			{
                workingon = needdrawn;
                needdrawn = -1;
            }
			
            // find an unfinished page
            int loop;
            PageField pageField;
            for (loop = this.getFieldCount(); loop > 0; loop--)
            {
            	pageField = (PageField)this.getField(workingon);
                if (pageField.getPage() == null)
                {
                    break;
                }
                workingon++;
                if (workingon >= this.getFieldCount())
                {
                    workingon = 0;
                }
            }
            if (loop == 0)
            {
                // done all pages.
                break;
            }
            
            // build the page
            try
            {
                int pagetoread = workingon + 1;
                //int pagetoread = 1;
                //System.out.println("Read page: " + pagetoread);
                PDFPage p = file.getPage(pagetoread, true);
                if(workingon == 0)
                {
                	synchronized(this.tracker)
                	{
                		this.tracker.click(workingon);
                	}
                }
                
                int hi = THUMB_HEIGHT - THUMB_BUFFER;
                int wid = (int)Math.ceil(hi * p.getAspectRatio());
                //if (!p.isFinished())
                //{
                //	System.out.println("Page not finished!");
                //	p.waitForFinish();
                //}
                //int pagetowrite = 0;
                int pagetowrite = workingon;
                
                Bitmap i = p.getImage(wid, hi, null, true, true);
                
                // images[0] = i;
                synchronized (Application.getEventLock())
                {
                	pageField = (PageField)this.getField(pagetowrite);
                	//if(pageField.getMaxWidth() > wid || pageField.getMaxHeight() > hi)
                	//{
                		pageField.setMax(wid, hi);
                	//}
                	pageField.setPage(i);
                }
                
                invalidate();
            }
            catch (Exception e)
            {
                int size = THUMB_HEIGHT - THUMB_BUFFER;
                synchronized (Application.getEventLock())
                {
                	pageField = (PageField)this.getField(workingon);
                	//if(pageField.getMaxWidth() > size || pageField.getMaxHeight() > size)
                	//{
                		pageField.setMax(size, size);
                	//}
                	pageField.setPage(new Bitmap(size, size));
                }
                if(workingon == 0)
                {
                	synchronized(this.tracker)
                	{
                		this.tracker.click(workingon);
                	}
                }
            }
		}
	}
}
