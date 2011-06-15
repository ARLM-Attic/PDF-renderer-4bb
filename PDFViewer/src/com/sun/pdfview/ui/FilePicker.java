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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.FocusChangeListener;
import net.rim.device.api.ui.Graphics;
//#endif
//#ifdef BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import net.rim.device.api.ui.TouchEvent;
//#endif
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.NullField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * Imitation of 5.0 FilePicker.
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
		
		private static class FilePickerUI extends PopupScreen implements FieldChangeListener
		{
			private FilePickerImpl fp;
			public String selectedFile;
			private VerticalFieldManager list;
			private FileConnection file;
			private int viewMode;
			
			private static final int MODE_THUMBNAIL = 0;
			private static final int MODE_TITLE = MODE_THUMBNAIL + 1;
			private static final int MODE_LIST = MODE_TITLE + 1;
			
			public FilePickerUI(FilePickerImpl fp)
			{
				super(new VerticalFieldManager(), PopupScreen.DEFAULT_MENU | PopupScreen.DEFAULT_CLOSE | PopupScreen.USE_ALL_HEIGHT | PopupScreen.USE_ALL_WIDTH);
				this.fp = fp;
				
				viewMode = MODE_LIST;
				setupUI();
			}
			
			public void close()
			{
				if(file != null && file.isOpen())
				{
					try
					{
						file.close();
					}
					catch (IOException e)
					{
					}
				}
				super.close();
			}
			
			protected void makeMenu(Menu menu, int instance)
			{
				// TODO Auto-generated method stub
				super.makeMenu(menu, instance);
			}
			
			private void setupUI()
			{
				//Title
				add(new LabelField("Select"));
				
				add(new NullField()); //Temp place holder
				configurePath();
				
				add(new SeparatorField());
				
				add(list = new VerticalFieldManager(VerticalFieldManager.USE_ALL_WIDTH));
				populateList();
			}
			
			private void configurePath()
			{
				if(this.fp.rootPath == null && file == null)
				{
					deleteRange(1, 1);
					insert(new LabelField("Explore"), 1);
				}
				else
				{
					//Memory and path
					HorizontalFieldManager horz = new HorizontalFieldManager();
					
					//XXX Add memory icon
					
					String path = file.getURL();
					int pathIndex = path.indexOf('/', 1);
					horz.add(new LabelField('/' + friendlyName(path.substring(1, pathIndex)) + path.substring(pathIndex), LabelField.FIELD_LEFT | LabelField.ELLIPSIS));
					
					insert(horz, 1);
				}
			}
			
			private void populateList()
			{
				SelectField select;
				LabelField lab;
				int len;
				list.deleteAll();
				if(this.fp.rootPath == null && file == null)
				{
					String[] roots = getRoots();
					len = roots.length;
					for(int i = 0; i < len; i++)
					{
						//In keeping with the original manner the FilePicker works
						if(roots[i].equals("system"))
						{
							//Skip "system"
							continue;
						}
						
						select = new SelectField();
						select.setChangeListener(this);
						
						//XXX Icon
						
						select.add(new LabelField(friendlyName(roots[i]), LabelField.FIELD_LEFT | LabelField.ELLIPSIS | LabelField.NON_FOCUSABLE));
						
						list.add(select);
					}
				}
				else
				{
					Vector files = new Vector();
					Vector folders = new Vector();
					FileConnection tmp;
					try
					{
						Enumeration en = file.list();
						String url = file.getURL();
						while(en.hasMoreElements())
						{
							tmp = (FileConnection)Connector.open(url + (String)en.nextElement(), Connector.READ);
							if(tmp.isDirectory())
							{
								folders.addElement(tmp.getURL());
							}
							else
							{
								if(passFilter(tmp.getName()))
								{
									files.addElement(tmp.getURL());
								}
							}
							tmp.close();
						}
						if(folders.size() > 0)
						{
							len = folders.size();
							for(int i = 0; i < len; i++)
							{
								select = new SelectField();
								select.setChangeListener(this);
								
								//XXX Icon
								
								tmp = (FileConnection)Connector.open((String)folders.elementAt(i), Connector.READ);
								String name = tmp.getName();
								select.add(new LabelField(name.substring(0, name.length() - 1), LabelField.FIELD_LEFT | LabelField.ELLIPSIS | LabelField.NON_FOCUSABLE));
								tmp.close();
								
								list.add(select);
							}
							if(files.size() > 0)
							{
								list.add(new SeparatorField());
							}
						}
						len = files.size();
						if(this.viewMode == MODE_THUMBNAIL)
						{
							for(int i = 0; i < len; i += 4)
							{
								HorizontalFieldManager man = new HorizontalFieldManager(HorizontalFieldManager.USE_ALL_WIDTH); //TODO: Need to be evenly spaced
								
								int max = Math.max(4, len - i);
								for(int k = i; k < max; k++)
								{
									tmp = (FileConnection)Connector.open((String)folders.elementAt(i), Connector.READ);
									BitmapField bf = new BitmapField(null); //TODO: Need icon
									bf.setCookie(tmp.getName());
									man.add(bf);
									tmp.close();
								}
								
								list.add(man);
							}
						}
						else
						{
							for(int i = 0; i < len; i++)
							{
								select = new SelectField();
								select.setChangeListener(this);
								
								//XXX Icon
								
								tmp = (FileConnection)Connector.open((String)folders.elementAt(i), Connector.READ);
								select.add(new LabelField(tmp.getName(), LabelField.FIELD_LEFT | LabelField.ELLIPSIS | LabelField.NON_FOCUSABLE));
								
								if(this.viewMode == MODE_LIST)
								{
									long size = tmp.fileSize();
									if(size >= 0)
									{
										String sizeMark;
										if(size == 0)
										{
											sizeMark = "0 KB";
										}
										else if(size < 1024)
										{
											sizeMark = "1 KB";
										}
										else
										{
											size >>= 10; //Equivilant of dividing by 1024
											if(size < 1024)
											{
												sizeMark = size + " KB";
											}
											else
											{
												size >>= 10;
												if(size < 1024)
												{
													sizeMark = size + " MB";
												}
												else
												{
													size >>= 10;
													sizeMark = size + " GB";
												}
											}
										}
										select.add(new LabelField(sizeMark, LabelField.FIELD_RIGHT | LabelField.NON_FOCUSABLE));
									}
								}
								tmp.close();
								
								list.add(select);
							}
						}
					}
					catch(Exception e)
					{
					}
				}
			}
			
			private String friendlyName(String name)
			{
				if(name.equals("SDCard"))
				{
					return "Media Card";
				}
				else if(name.equals("store"))
				{
					return "Device Memory";
				}
				return name;
			}
			
			private String removePrefix(String path)
			{
				return path.substring(7);
			}
			
			private boolean passFilter(String name)
			{
				if(this.fp.filters != null)
				{
					for(int i = this.fp.filters.length - 1; i >= 0; i--)
					{
						if(name.endsWith(this.fp.filters[i]))
						{
							return true;
						}
					}
					return false;
				}
				return true;
			}
			
			private String[] getRoots()
			{
				Enumeration en = FileSystemRegistry.listRoots();
				Vector v = new Vector();
				while(en.hasMoreElements())
				{
					v.addElement(en.nextElement());
				}
				String[] str = new String[v.size()];
				v.copyInto(str);
				int l = v.size();
				for(int i = 0; i < l; i++)
				{
					str[i] = str[i].substring(0, str[i].length() - 1);
				}
				return str;
			}
			
			public void fieldChanged(Field field, int context)
			{
				if(context != FieldChangeListener.PROGRAMMATIC)
				{
					System.out.println("Stuff");
					// TODO Set the fileConnection (unless it user went to memory selection)
				}
			}
		}
		
		//In order for this field to work, all added fields can't be focusable
		private static class SelectField extends HorizontalFieldManager implements FocusChangeListener
		{
			private boolean click;
			
			public SelectField()
			{
				this(0);
			}
			
			public SelectField(long style)
			{
				super(style);
				
				NullField field = new NullField();
				field.setFocusListener(this);
				add(field);
			}
			
			protected void paint(Graphics graphics)
			{
				if(this.getField(0).isFocus())
				{
					int tc = graphics.getColor();
					graphics.setColor(Color.RED);
					graphics.fillRect(0, 0, this.getWidth(), this.getHeight());
					graphics.setColor(tc);
				}
				super.paint(graphics);
			}
			
			public void focusChanged(Field field, int eventType)
			{
				if(eventType == FocusChangeListener.FOCUS_LOST)
				{
					click = false;
				}
				invalidate();
			}
			
			protected boolean navigationClick(int status, int time)
			{
				if(this.getField(0).isFocus())
				{
					click = true;
					return true;
				}
				return super.navigationClick(status, time);
			}
			
			protected boolean navigationUnclick(int status, int time)
			{
				if(this.getField(0).isFocus() && click)
				{
					click = false;
					invokeAction(0);
					return true;
				}
				return super.navigationUnclick(status, time);
			}
			
			protected boolean trackwheelClick(int status, int time)
			{
				if(this.getField(0).isFocus())
				{
					click = true;
					return true;
				}
				return super.trackwheelClick(status, time);
			}
			
			protected boolean trackwheelUnclick(int status, int time)
			{
				if(this.getField(0).isFocus() && click)
				{
					click = false;
					invokeAction(0);
					return true;
				}
				return super.trackwheelUnclick(status, time);
			}
			
//#endif
//#ifdef BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
			protected boolean touchEvent(TouchEvent message)
			{
				switch(message.getEvent())
				{
					case TouchEvent.CLICK:
						invokeAction(0);
						return true;
				}
				return super.touchEvent(message);
			}
//#endif
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
		}
	}
}
//#endif
