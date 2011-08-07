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
import java.util.Stack;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Graphics;
//#endif
//#ifdef BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import net.rim.device.api.ui.TouchEvent;
//#endif
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.Menu;
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
				this.currentFPui.UIClose();
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
			if(fpui.good)
			{
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
			private FieldListField folderList, fileList;
			private VerticalFieldManager listManager;
			private FileConnection curDirectory;
			private int viewMode;
			private Stack pathStack;
			
			public boolean good;
			
			private static final int MODE_THUMBNAIL = 0;
			private static final int MODE_TITLE = MODE_THUMBNAIL + 1;
			private static final int MODE_LIST = MODE_TITLE + 1;
			
			public FilePickerUI(FilePickerImpl fp)
			{
				super(new VerticalFieldManager(), PopupScreen.DEFAULT_MENU | PopupScreen.DEFAULT_CLOSE | PopupScreen.USE_ALL_HEIGHT | PopupScreen.USE_ALL_WIDTH);
				this.good = true;
				this.fp = fp;
				if(this.fp.rootPath != null)
				{
					try
					{
						this.curDirectory = (FileConnection)Connector.open(this.fp.rootPath, Connector.READ);
					}
					catch(Exception ex)
					{
						Dialog.alert("Can't open default directory.");
						this.good = false;
						return;
					}
					this.pathStack = new Stack();
				}
				
				viewMode = MODE_LIST;
				setupUI();
			}
			
			public int getViewMode()
			{
				return this.viewMode;
			}
			
			public void UIClose()
			{
				if(curDirectory != null && curDirectory.isOpen())
				{
					try
					{
						curDirectory.close();
					}
					catch (IOException e)
					{
					}
				}
				super.close();
			}
			
			public void close()
			{
				if(closeUI())
				{
					UIClose();
				}
				else
				{
					goBack(true);
				}
			}
			
			private boolean closeUI()
			{
				if(this.fp.rootPath == null && this.curDirectory == null)
				{
					return true;
				}
				else if(this.fp.rootPath != null && this.curDirectory != null)
				{
					return this.fp.rootPath.equals(this.curDirectory.getURL());
				}
				return false;
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
				
				add(this.curDirectory == null ? new LabelField("Explore") : (Field)new HorizontalFieldManager());
				configurePath();
				
				add(new SeparatorField());
				
				this.listManager = new VerticalFieldManager(VerticalFieldManager.VERTICAL_SCROLL);
				add(this.listManager);
				this.folderList = new FieldListField(this);
				this.fileList = new FieldListField(this);
				this.folderList.setChangeListener(this);
				this.fileList.setChangeListener(this);
				populateList();
			}
			
			private void configurePath()
			{
				if(this.curDirectory == null)
				{
					if(!(getField(1) instanceof LabelField))
					{
						//Change the field if not the correct type
						replace(getField(1), new LabelField("Explore"));
					}
				}
				else
				{
					if(!(getField(1) instanceof HorizontalFieldManager))
					{
						//Change the field if not the correct type
						replace(getField(1), new HorizontalFieldManager());
					}
					
					//Memory and path
					HorizontalFieldManager horz = (HorizontalFieldManager)getField(1);
					
					//XXX Add memory icon
					
					String path = this.curDirectory.getURL();
					int pathIndex = path.indexOf('/', 8);
					
					StringBuffer buffer = new StringBuffer();
					buffer.append('/');
					buffer.append(friendlyName(path.substring(8, pathIndex)));
					if(pathIndex + 1 < path.length())
					{
						buffer.append(path.substring(pathIndex, path.length() - 1));
					}
					
					if(horz.getFieldCount() < 1) //XXX Change to 2 when icon is added (and make sure that "else" gets updated as well
					{
						horz.add(new LabelField(buffer.toString(), LabelField.FIELD_LEFT | LabelField.ELLIPSIS));
					}
					else
					{
						((LabelField)horz.getField(0)).setText(buffer.toString());
					}
				}
			}
			
			private void populateList()
			{
				HorizontalFieldManager row;
				int len;
				this.folderList.deleteAll();
				this.fileList.deleteAll();
				this.listManager.deleteAll();
				if(this.curDirectory == null)
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
						
						row = this.folderList.getNewRow(false);
						
						//XXX Icon
						
						row.add(new LabelField(friendlyName(roots[i]), LabelField.FIELD_LEFT | LabelField.ELLIPSIS | LabelField.NON_FOCUSABLE));
						row.setCookie(roots[i]);
						
						this.folderList.commitRow();
					}
					this.listManager.add(this.folderList);
				}
				else
				{
					Vector files = new Vector();
					Vector folders = new Vector();
					FileConnection tmp;
					try
					{
						Enumeration en = this.curDirectory.list();
						String url = this.curDirectory.getURL();
						if(this.fp.rootPath != null)
						{
							folders.addElement("@Up");
						}
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
								row = this.folderList.getNewRow(false);
								
								//XXX Icon
								
								String urlStr = (String)folders.elementAt(i);
								if(urlStr.charAt(0) == '@')
								{
									//Special folder option
									row.add(new LabelField("Up", LabelField.FIELD_LEFT | LabelField.ELLIPSIS | LabelField.NON_FOCUSABLE));
									row.setCookie("..");
								}
								else
								{
									tmp = (FileConnection)Connector.open(urlStr, Connector.READ);
									String name = tmp.getName();
									row.add(new LabelField(name.substring(0, name.length() - 1), LabelField.FIELD_LEFT | LabelField.ELLIPSIS | LabelField.NON_FOCUSABLE));
									row.setCookie(name);
									tmp.close();
								}
								
								this.folderList.commitRow();
							}
							this.listManager.add(this.folderList);
							if(files.size() > 0)
							{
								this.listManager.add(new SeparatorField());
							}
						}
						len = files.size();
						if(len > 0)
						{
							if(this.viewMode == MODE_THUMBNAIL)
							{
								for(int i = 0; i < len; i += 4)
								{
									row = this.fileList.getNewRow(true);
									
									int max = Math.max(4, len - i);
									for(int k = i; k < max; k++)
									{
										tmp = (FileConnection)Connector.open((String)files.elementAt(i), Connector.READ);
										BitmapField bf = new BitmapField(null); //TODO: Need icon
										bf.setCookie(tmp.getName());
										row.add(bf);
										tmp.close();
									}
									
									this.fileList.commitRow();
								}
							}
							else
							{
								for(int i = 0; i < len; i++)
								{
									row = this.fileList.getNewRow(false);
									
									//XXX Icon
									
									tmp = (FileConnection)Connector.open((String)files.elementAt(i), Connector.READ);
									row.add(new LabelField(tmp.getName(), LabelField.FIELD_LEFT | LabelField.ELLIPSIS | LabelField.NON_FOCUSABLE));
									row.setCookie(tmp.getName());
									
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
											row.add(new LabelField(sizeMark, LabelField.FIELD_RIGHT | LabelField.NON_FOCUSABLE));
										}
									}
									tmp.close();
									
									this.fileList.commitRow();
								}
							}
							this.listManager.add(this.fileList);
						}
					}
					catch(Exception e)
					{
						Dialog.alert("Can't open path.");
						this.selectedFile = null;
						this.UIClose();
					}
				}
				this.folderList.commitHeight();
				this.fileList.commitHeight();
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
					ListField list = (ListField)field;
					Field row = (Field)list.getCallback().get(list, list.getSelectedIndex());
					String url = (String)row.getCookie();
					if(url.equals(".."))
					{
						if(this.pathStack != null)
						{
							boolean push = true;
							if(this.pathStack.size() > 0)
							{
								String path = (String)this.pathStack.peek();
								if(path.equals(".."))
								{
									this.pathStack.pop();
									push = false;
								}
							}
							if(push)
							{
								String path = this.curDirectory.getName();
								if(path.length() == 0)
								{
									path = this.curDirectory.getURL().substring(8);
									path = path.substring(0, path.length() - 1);
								}
								this.pathStack.push(path);
							}
						}
						goBack(false);
					}
					else
					{
						if(this.pathStack != null)
						{
							if(this.pathStack.size() > 0)
							{
								String path = (String)this.pathStack.peek();
								if(path.equals(url))
								{
									this.pathStack.pop();
								}
								else
								{
									this.pathStack.push("..");
								}
							}
							else
							{
								this.pathStack.push("..");
							}
						}
						openPath(url);
					}
				}
			}
			
			private void openPath(String relPath)
			{
				if(this.curDirectory == null)
				{
					try
					{
						this.curDirectory = (FileConnection)Connector.open("file:///" + relPath + '/', Connector.READ);
						configurePath();
						populateList();
					}
					catch(Exception e)
					{
						Dialog.alert("Can't open root.");
						this.selectedFile = null;
						this.UIClose();
					}
				}
				else
				{
					if(!relPath.endsWith("/"))
					{
						//File
						this.selectedFile = this.curDirectory.getURL() + relPath;
						this.UIClose();
					}
					else
					{
						try
						{
							String path = this.curDirectory.getURL() + relPath;
							
							this.curDirectory.close();
							
							this.curDirectory = (FileConnection)Connector.open(path, Connector.READ);
							configurePath();
							populateList();
						}
						catch(Exception e)
						{
							Dialog.alert("Can't open path.");
							this.selectedFile = null;
							this.UIClose();
						}
					}
				}
			}
			
			private void goBack(boolean processStack)
			{
				if(processStack && this.pathStack != null && this.pathStack.size() > 0)
				{
					String path = (String)this.pathStack.pop();
					if(path.equals(".."))
					{
						goBackProcess();
					}
					else
					{
						openPath(path);
					}
				}
				else
				{
					goBackProcess();
				}
			}
			
			private void goBackProcess()
			{
				try
				{
					int len = this.curDirectory.getName().length();
					
					this.curDirectory.close();
					
					if(len > 0)
					{
						String path = this.curDirectory.getURL();
						path = path.substring(0, path.length() - len);
						this.curDirectory = (FileConnection)Connector.open(path, Connector.READ);
					}
					else
					{
						this.curDirectory = null;
					}
				}
				catch(Exception e)
				{
					Dialog.alert("Can't go back.");
					this.selectedFile = null;
					this.UIClose();
				}
				configurePath();
				populateList();
			}
		}
		
		private static class FieldListField extends ListField implements ListFieldCallback
		{
			private Vector data;
			private FLFHorizontalFieldManager chorz;
			private FilePickerUI ui;
			
			public FieldListField(FilePickerUI ui)
			{
				setSearchable(false);
				this.data = new Vector();
				this.ui = ui;
			}
			
			public int getSelectedIndex()
			{
				int index = super.getSelectedIndex();
				if(this.ui.getViewMode() == FilePickerUI.MODE_THUMBNAIL)
				{
					index |= 0x80000000;
					//TODO: If in grid mode then return an "encoded" index
				}
				return index;
			}
			
			protected void fieldChangeNotify(int context)
			{
				if(context != FieldChangeListener.PROGRAMMATIC)
				{
					super.fieldChangeNotify(context);
				}
			}
			
			protected boolean navigationClick(int status, int time)
			{
				if(((status & KeypadListener.STATUS_FOUR_WAY) != 0) || ((status & KeypadListener.STATUS_TRACKWHEEL) != 0))
				{
					this.fieldChangeNotify(0);
					return true;
				}
				return super.navigationClick(status, time);
			}
			
			public void commitHeight()
			{
				this.setRowHeight(getPreferredHeight(this));
			}
			
			public void drawListRow(ListField listField, Graphics graphics, int index, int y, int width)
			{
				FLFHorizontalFieldManager horz = (FLFHorizontalFieldManager)((FieldListField)listField).data.elementAt(index);
				graphics.pushContext(0, y, width, listField.getRowHeight(), 0, y);
				horz.paint(graphics);
				graphics.popContext();
			}
			
			protected void layout(int width, int height)
			{
				super.layout(width, height);
				
				int rowWidth = this.getWidth();
				int rowHeight = this.getRowHeight();
				for(int i = data.size() - 1; i >= 0; i--)
				{
					((FLFHorizontalFieldManager)data.elementAt(i)).layoutIn(rowWidth, rowHeight);
				}
			}
			
			public Object get(ListField listField, int index)
			{
				if((index & 0x80000000) == 0x80000000)
				{
					//TODO: Encoded index
				}
				return ((FieldListField)listField).data.elementAt(index);
			}
			
			public int getPreferredWidth(ListField listField)
			{
				int mWidth = 0;
				for(int i = ((FieldListField)listField).data.size() - 1; i >= 0; i--)
				{
					int w = ((FLFHorizontalFieldManager)((FieldListField)listField).data.elementAt(i)).getPreferredWidth();
					if(w > mWidth)
					{
						mWidth = w;
					}
				}
				return mWidth;
			}
			
			public int getPreferredHeight(ListField listField)
			{
				int mHeight = listField.getRowHeight(); //Get the default
				for(int i = ((FieldListField)listField).data.size() - 1; i >= 0; i--)
				{
					int h = ((FLFHorizontalFieldManager)((FieldListField)listField).data.elementAt(i)).getPreferredHeight();
					if(h > mHeight)
					{
						mHeight = h;
					}
				}
				return mHeight;
			}
			
			public int indexOfList(ListField listField, String prefix, int start)
			{
				//Unused
				return -1;
			}
			
			public void deleteAll()
			{
				//Make sure the field knows that no elements exist
				for(int i = this.getSize() - 1; i >= 0; i--)
				{
					this.delete(i);
				}
				//Actually remove the elements
				data.removeAllElements();
			}
			
			public HorizontalFieldManager getNewRow(boolean fieldsSelectable)
			{
				if(chorz != null)
				{
					throw new IllegalStateException();
				}
				return chorz = new FLFHorizontalFieldManager(fieldsSelectable);
			}
			
			public void commitRow()
			{
				chorz.commit();
				commitRow(chorz);
				chorz = null;
			}
			
			private void commitRow(Field field)
			{
				int index = this.getSize();
				this.insert(index);
				data.insertElementAt(field, index);
			}
			
			private static class FLFHorizontalFieldManager extends HorizontalFieldManager
			{
				private boolean fieldsSelectable, commited;
				
				public FLFHorizontalFieldManager(boolean fieldsSelectable)
				{
					super(fieldsSelectable ? 0 : FLFHorizontalFieldManager.NON_FOCUSABLE);
					this.fieldsSelectable = fieldsSelectable;
				}
				
				public void commit()
				{
					commited = true;
				}
				
				public void add(Field field)
				{
					if(commited)
					{
						throw new IllegalStateException();
					}
					if(!fieldsSelectable && field.isFocusable())
					{
						throw new IllegalArgumentException();
					}
					super.add(field);
				}
				
				public void insert(Field field, int index)
				{
					if(commited)
					{
						throw new IllegalStateException();
					}
					if(!fieldsSelectable && field.isFocusable())
					{
						throw new IllegalArgumentException();
					}
					super.insert(field, index);
				}
				
				public void delete(Field field)
				{
					if(commited)
					{
						throw new IllegalStateException();
					}
					super.delete(field);
				}
				
				public void deleteAll()
				{
					if(commited)
					{
						throw new IllegalStateException();
					}
					super.deleteAll();
				}
				
				public void deleteRange(int start, int count)
				{
					if(commited)
					{
						throw new IllegalStateException();
					}
					super.deleteRange(start, count);
				}
				
				public void paint(Graphics graphics)
				{
					super.paint(graphics);
				}
				
				public void layoutIn(int width, int height)
				{
					super.layout(width, height);
				}
			}
		}
	}
}
//#endif
