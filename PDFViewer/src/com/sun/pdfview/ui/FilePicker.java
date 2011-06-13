//#preprocessor

//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
/*
 * File: FilePicker.java
 * Version: 1.0
 * Initial Creation: Jun 13, 2011 11:30:27 AM
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
package com.sun.pdfview.ui;

import java.util.Vector;

import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * Really basic file picker, based on 5.0 version of FilePicker.
 */
public abstract class FilePicker
{
	public static interface Listener
	{
		void selectionDone(String selected);
	}
	
	FilePicker()
	{
	}
	
	public static final FilePicker getInstance()
	{
		return new FilePickerImpl();
	}
	
	public abstract void cancel();
	
	public abstract void setFilter(String filterString);
	
	public abstract void setListener(Listener listener);
	
	public abstract void setPath(String defaultPath);
	
	public abstract String show();
	
	private static class FilePickerImpl extends FilePicker
	{
		private String rootPath;
		private String[] filters;
		private Listener listener;
		private FilePickerUI currentFPui;
		
		public void cancel()
		{
			if(this.currentFPui != null)
			{
				this.currentFPui.selectedFile = null;
				this.currentFPui.close();
			}
		}
		
		public void setFilter(String filterString)
		{
			if(filterString == null)
			{
				this.filters = null;
			}
			else
			{
				Vector v = new Vector();
				while(filterString.length() > 0)
				{
					int index = filterString.indexOf(':');
					if(index == -1)
					{
						v.addElement(filterString);
						filterString = "";
					}
					else
					{
						v.addElement(filterString.substring(0, index));
						filterString = filterString.substring(index);
					}
				}
				filters = new String[v.size()];
				v.copyInto(filters);
			}
		}
		
		public void setListener(Listener listener)
		{
			this.listener = listener;
		}
		
		public void setPath(String defaultPath)
		{
			//Could do checks for validity but the value that will be passed in will be the same as the value returned to the listener, so it will always be valid.
			this.rootPath = defaultPath;
		}
		
		public String show()
		{
			//Create picker
			final FilePickerUI fpui = this.currentFPui = new FilePickerUI(this);
			//Get UI application
			final UiApplication app = UiApplication.getUiApplication();
			//Push screen on display
			if(UiApplication.isEventDispatchThread())
			{
				app.pushModalScreen(fpui);
			}
			else
			{
				app.invokeAndWait(new Runnable()
				{
					public void run()
					{
						app.pushModalScreen(fpui);
					}
				});
			}
			//Process results
			this.currentFPui = null;
			if(this.listener != null)
			{
				this.listener.selectionDone(fpui.selectedFile);
			}
			return fpui.selectedFile;
		}
		
		private static class FilePickerUI extends PopupScreen
		{
			private FilePickerImpl fp;
			public String selectedFile;
			
			public FilePickerUI(FilePickerImpl fp)
			{
				super(new VerticalFieldManager());
				this.fp = fp;
			}
			
			//TODO
		}
	}
}
//#endif
