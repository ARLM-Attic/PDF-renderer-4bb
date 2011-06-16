//#preprocessor

/*
 * File: PDFViewer.java
 * Version: 1.0
 * Initial Creation: May 28, 2010 4:24:22 PM
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

import java.io.InputStream;
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.ByteBuffer;
import net.rim.device.api.io.URI;
import net.rim.device.api.ui.picker.FilePicker;
//#else
import com.sun.pdfview.helper.nio.ByteBuffer;
import com.sun.pdfview.ui.FilePicker;
//#endif

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import com.sun.pdfview.decrypt.PDFAuthenticationFailureException;

import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.math.Fixed32;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.KeypadUtil;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.UiEngine;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * A demo PDF Viewer application.
 * @author Vincent Simonetti
 */
public class PDFViewer extends UiApplication
{
    public PDFViewer()
    {
        pushScreen(new PDFViewerScreen());
    }
    
    public static void main(String[] args)
	{
    	PDFViewer viewer = new PDFViewer();
    	viewer.enterEventDispatcher();
	}
    
	public class PDFViewerScreen extends MainScreen implements FilePicker.Listener
	{
		private static final String TITLE = "PDF Viewer";
		private static final int FP_TWO = Fixed32.ONE * 2;
		
		private PDFFile file;
		
		//Various menu items, loaded as needed
		private MenuItem openPDF, closePDF, zoomIn, zoomOut, fit2screen, viewOutline;
		private LabelField title;
		private Manager pageManager;
		private BasicEditField pageTextField;
		private ThumbnailPopup thumbNails;
		private TreeField treeField;
		private FullPageField pageField;
		
		private boolean hasOutline;
		private String lastDir;
		private int currentpage;
		private int pageCount;
		
		public PDFViewerScreen()
		{
			super(MainScreen.NO_VERTICAL_SCROLL | MainScreen.NO_HORIZONTAL_SCROLL);
			setTitle(title = new LabelField(TITLE, LabelField.NON_FOCUSABLE | LabelField.FIELD_HCENTER | LabelField.ELLIPSIS));
			
			file = null;
			lastDir = null;
			hasOutline = false;
			
			//Initialize standard menu items
			openPDF = new MenuItem("Open PDF", 0x197F36F, 800)
			{
				public void run()
				{
					openPDF();
				}
			};
			
			//Setup screen
			
			//First setup the page changer
			Manager man = new HorizontalButtonFieldSet(HorizontalButtonFieldSet.NO_HORIZONTAL_SCROLL | HorizontalButtonFieldSet.NO_HORIZONTAL_SCROLLBAR | HorizontalButtonFieldSet.USE_ALL_WIDTH);
			CustomButtonField click = new CustomButtonField(CustomButtonField.CONSUME_CLICK | CustomButtonField.NEVER_DIRTY);
			click.setNormalIcon(EncodedImage.getEncodedImageResource("first.gif"));
			click.setIconScale(0x20000);
			click.setChangeListener(new FieldChangeListener() {
				public void fieldChanged(Field field, int context)
				{
					gotoPage(1);
				}
			});
			man.add(click);
			click = new CustomButtonField(CustomButtonField.CONSUME_CLICK | CustomButtonField.NEVER_DIRTY);
			click.setNormalIcon(EncodedImage.getEncodedImageResource("prev.gif"));
			click.setIconScale(0x20000);
			click.setChangeListener(new FieldChangeListener() {
				public void fieldChanged(Field field, int context)
				{
					gotoPage(currentpage - 1);
				}
			});
			man.add(click);
			
			man.add(pageTextField = new BasicEditField(null, "1", 1, BasicEditField.FILTER_NUMERIC | BasicEditField.CONSUME_INPUT | BasicEditField.FIELD_VCENTER | BasicEditField.FIELD_HCENTER){
				protected boolean keyDown(int keycode, int time)
				{
					if(KeypadUtil.getKeyChar(keycode, KeypadUtil.MODE_UI_CURRENT_LOCALE) == Characters.ENTER)
					{
						String text = pageTextField.getText();
						if(text != null && text.length() > 0)
						{
							int value = Integer.parseInt(text);
							if(currentpage != value)
							{
								gotoPage(value);
							}
						}
						return true;
					}
					return super.keyDown(keycode, time);
				}
			});
			
			click = new CustomButtonField(CustomButtonField.CONSUME_CLICK | CustomButtonField.NEVER_DIRTY);
			click.setNormalIcon(EncodedImage.getEncodedImageResource("next.gif"));
			click.setIconScale(0x20000);
			click.setChangeListener(new FieldChangeListener() {
				public void fieldChanged(Field field, int context)
				{
					gotoPage(currentpage + 1);
				}
			});
			man.add(click);
			click = new CustomButtonField(CustomButtonField.CONSUME_CLICK | CustomButtonField.NEVER_DIRTY);
			click.setNormalIcon(EncodedImage.getEncodedImageResource("last.gif"));
			click.setIconScale(0x20000);
			click.setChangeListener(new FieldChangeListener() {
				public void fieldChanged(Field field, int context)
				{
					gotoPage(pageCount);
				}
			});
			man.add(click);
			pageManager = man;
			
			//Setup thumbnail system
			thumbNails = (ThumbnailPopup)ThumbnailPopup.createThumbnailPopup(this.file, this);
			
			//Setup page screen
			VerticalFieldManager vert = new VerticalFieldManager(VerticalFieldManager.NO_VERTICAL_SCROLL | VerticalFieldManager.NO_HORIZONTAL_SCROLLBAR);
			vert.add(pageField = new FullPageField(this));
			this.add(vert);
		}
		
		public void openPDF()
		{
			FilePicker picker = FilePicker.getInstance();
			picker.setFilter("pdf");
			picker.setListener(this);
			if(lastDir != null)
			{
				picker.setPath(lastDir);
			}
			picker.show();
		}
		
		public void selectionDone(String selected)
		{
			if(selected == null || selected.length() <= 0)
			{
				return;
			}
			
			//create a PDFFile from the data
			PDFFile nFile = null;
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
			URI uri = null;
//#else
			String uri = null;
//#endif
			boolean error = false;
			
			FileConnection file = null;
			InputStream in = null;
			try
			{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
				uri = URI.create(selected); //Never going to give an error but needs to be in try/catch anyway.
//#else
				uri = selected;
//#endif
				lastDir = uri.toString();
				lastDir = lastDir.substring(0, lastDir.length() - getFileName(uri).length());
				
				file = (FileConnection)Connector.open(uri.toString(), Connector.READ);
				in = file.openInputStream();
				ByteBuffer buf = ByteBuffer.wrap(IOUtilities.streamToBytes(in));
				
				nFile = new PDFFile(buf);
			}
			catch(PDFAuthenticationFailureException e)
			{
				Dialog.alert("PDF requires a password, not supported in current PDF Viewer. Sorry.");
				error = true;
			}
			catch(Exception e)
			{
				Dialog.alert(selected + " doesn't appear to be a PDF file.\n: " + e.getMessage());
				error = true;
			}
			finally
			{
				if(file != null)
				{
					try
					{
						if(in != null)
						{
							in.close();
						}
						file.close();
					}
					catch(Exception e)
					{
					}
				}
			}
			
			if(error)
			{
				return;
			}
			
			//Now that we're reasonably sure this document is real, close the old one.
			if(this.file != null)
			{
				closePDF();
			}
			
			//set up our document
			if(nFile != null)
			{
				this.file = nFile;
			}
			this.title.setText(TITLE + ": " + getFileName(uri));
			
			//Go to page
			int pageCount = (this.pageCount = nFile.getNumPages()) / 10;
			int count = 1;
			while(pageCount > 0)
			{
				count++;
				pageCount /= 10;
			}
			pageTextField.setMaxSize(count);
			//gotoPage(1); //Thumbnail Thread will call page 1 when it is finished parsing, thus preventing two parsers from running at once
			this.setStatus(pageManager);
			
			//Setup thumbnails
			thumbNails.setPDF(this.file);
			thumbNails.startProcess();
			pushGlobalScreen(thumbNails, 0, UiEngine.GLOBAL_QUEUE); //TODO: Just want button added, not whole screen.
			
			//if the PDF has an outline, set it up.
			try
			{
				OutlineNode node = this.file.getOutline();
				if(node != null)
				{
					if(treeField == null)
					{
						treeField = new TreeField(new TreeFieldCallback()
						{
							public void drawTreeItem(TreeField treeField, Graphics graphics, int node, int y, int width, int indent)
							{
								Object cookie = treeField.getCookie(node);
								graphics.drawText(cookie.toString(), indent, y, Graphics.ELLIPSIS, width);
							}
						}, TreeField.FOCUSABLE);
					}
					else
					{
						treeField.deleteAll();
					}
					node.getChildAt(0).loadTree(treeField); //The root is actually a child node.
					hasOutline = true;
				}
				else if(treeField != null)
				{
					treeField = null; //Remove it to free up memory
				}
			}
			catch (Exception e)
			{
				treeField = null;
				hasOutline = true;
			}
		}
		
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
		private String getFileName(URI uri)
		{
			String path = uri.getPath();
			return path.substring(path.lastIndexOf('/') + 1);
		}
//#else
		private String getFileName(String uri)
		{
			return uri.substring(uri.lastIndexOf('/') + 1);
		}
//#endif
		
		protected void makeMenu(Menu menu, int instance)
		{
			super.makeMenu(menu, instance);
			
			//Add "Open PDF"
			menu.add(openPDF);
			if(file != null)
			{
				//Add "Close PDF"
				if(closePDF == null)
				{
					closePDF = new MenuItem("Close PDF", 0x197F370, 700)
					{
						public void run()
						{
							closePDF();
						}
					};
				}
				menu.add(closePDF);
				
				//Add "Zoom In"
				if(zoomIn == null)
				{
					zoomIn = new MenuItem("Zoom In", 0x195F36A, 1200)
					{
						public void run()
						{
							zoom(true, false);
						}
					};
				}
				menu.add(zoomIn);
				
				//Add "Zoom Out"
				if(zoomOut == null)
				{
					zoomOut = new MenuItem("Zoom Out", 0x195F36B, 1100)
					{
						public void run()
						{
							zoom(false, false);
						}
					};
				}
				menu.add(zoomOut);
				
				//Add "Fit to Screen"
				if(fit2screen == null)
				{
					fit2screen = new MenuItem("Fit to Screen", 0x195F36C, 1000)
					{
						public void run()
						{
							zoom(false, true);
						}
					};
				}
				menu.add(fit2screen);
				
				if(hasOutline)
				{
					//Add "Show Outline"
					if(viewOutline == null)
					{
						viewOutline = new MenuItem("Show Outline", 0x196F36E, 900)
						{
							public void run()
							{
								showOutline();
							}
						};
					}
					menu.add(viewOutline);
				}
			}
		}
		
		public int getCurrentPage()
		{
			return this.currentpage;
		}
		
		public void gotoPage(int page)
		{
			if(page == currentpage)
			{
				//Nothing to do
				return;
			}
			if(page < 1)
			{
				page = 1;
			}
			if(page > pageCount)
			{
				page = pageCount;
			}
			currentpage = page;
			pageTextField.setText(Integer.toString(page));
			pageField.showPage(file.getPage(currentpage));
			thumbNails.showThumbnail(currentpage);
		}
		
		private void showOutline()
		{
			PopupScreen outline = new PopupScreen(new VerticalFieldManager(VerticalFieldManager.VERTICAL_SCROLL));
			HorizontalFieldManager horz = new HorizontalFieldManager(HorizontalFieldManager.HORIZONTAL_SCROLL)
			{
				public void sublayout(int maxWidth, int maxHeight)
				{
					int pWidth = treeField.getPreferredWidth();
					if(pWidth > 0)
					{
						maxWidth = Math.min(maxWidth, pWidth); //This is to prevent the "massive" possible width (usually well over 1 million pixels)
					}
					super.sublayout(maxWidth, maxHeight);
				}
			};
			
			horz.add(this.treeField);
			outline.add(horz);
			
			pushGlobalScreen(outline, 0, UiEngine.GLOBAL_QUEUE);
		}
		
		private void zoom(boolean in, boolean fit)
		{
			if(fit)
			{
				pageField.fitToField();
			}
			else
			{
				if(in)
				{
					pageField.zoom(FP_TWO);
				}
				else
				{
					pageField.zoom(-FP_TWO);
				}
			}
		}
		
		private void closePDF()
		{
			this.setStatus(null);
			this.pageField.showPage(null);
			
			//Do this to reset the title and it's position
			this.title.setText(TITLE);
			Manager man = this.title.getManager();
			man.delete(this.title);
			man.add(this.title);
			
			this.thumbNails.reset();
			//TODO: Pop thumbnails off stack if PDF is closed but thumbnails is visible.
			PDFFile.appClosing();
			file = null;
		}
		
		public void close()
		{
			if(file != null)
			{
				closePDF();
			}
	        PDFFile.appClosing(); //Call a second time just as a precaution if there are any remnants.
			super.close();
		}
	}
}
