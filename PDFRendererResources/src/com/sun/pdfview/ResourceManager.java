/*
 * File: ResourceManager.java
 * Version: 1.0
 * Initial Creation: Jun 30, 2010 2:21:31 PM
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
import java.util.Vector;

import net.rim.device.api.system.RuntimeStore;
import net.rim.device.api.util.LongVector;

/**
 * Resource manager for PDF Renderer.
 * @author Vincent Simonetti
 */
public final class ResourceManager
{
	private static final long SINGLETON_STORAGE_ID = 0x67C95E44C4049000L;
	private static final long RESOURCE_CACHE_ID = 0x2F3B9A41ADBB0DC2L;
	
	//There are some "extras" that are coded but not used, thus they can be removed if the developer desires. They are there in case anyone wants to make their own extensions.
	
	/**
	 * Get a Resources object that represents resources.
	 * @param name The name of the resource to get. This is a relative namespace of where the resources are located. So if the resource is located in 
	 * "com.sun.pdfview.myresource.res" then this should be "myresource.res".
	 * @return The resource system.
	 */
	public static Resources getResource(String name)
	{
		Vector resourceCache;
		Object obj;
		if((obj = ResourceManager.singletonStorageGet(RESOURCE_CACHE_ID)) != null)
		{
			resourceCache = (Vector)obj;
		}
		else
		{
			resourceCache = new Vector();
			ResourceManager.singletonStorageSet(RESOURCE_CACHE_ID, resourceCache);
		}
		
		int c = resourceCache.size();
		for(int i = 0; i < c; i++)
		{
			Resources res = (Resources)resourceCache.elementAt(i);
			if(res.getName().equals(name))
			{
				//Found it
				return res;
			}
		}
		
		boolean abs = name.startsWith("/");
		
		try
		{
			//See if a specific type exists for this resource
			Class clazz = Class.forName((abs ? name.substring(1) : "com.sun.pdfview." + name) + ".ResourceImpl");
			obj = clazz.newInstance();
			if(obj instanceof Resources)
			{
				resourceCache.addElement(obj);
				return (Resources)obj;
			}
		}
		catch(Exception e)
		{
		}
		
		//Create default resource implementation since no specific or cached version exists
		Resources res = new ResourcesDefaultImpl(name, abs);
		resourceCache.addElement(res);
		return res;
	}
	
	/**
	 * Get a singleton object. This is not the same as just calling RuntimeStore and is managed for memory usage. It will be cleaned up when 
	 * {@link #singltonStorageCleanup()} is called.
	 * @param uid The ID of the object to get.
	 * @return The object (if it was set using {@link #singletonStorageSet(long, Object)}) or null if it doesn't exist or was not set using {@link #singletonStorageSet(long, Object)}.
	 */
	public synchronized static Object singletonStorageGet(long uid)
	{
		RuntimeStore store = RuntimeStore.getRuntimeStore();
		Object obj;
		if((obj = store.get(SINGLETON_STORAGE_ID)) != null)
		{
			LongVector v = (LongVector)obj;
			if(v.contains(uid))
			{
				return store.get(uid);
			}
		}
		return null;
	}
	
	/**
	 * Set a singleton object.
	 * @param uid The ID of the object to set. If this happens to be an object that already exists but was not set using this function then an exception will be thrown.
	 * @param obj The singleton object to set or null if the current object should be removed.
	 * @return The previous object (if it was set using {@link #singletonStorageSet(long, Object)}) or null if it didn't exist or was not set using {@link #singletonStorageSet(long, Object)}.
	 */
	public synchronized static Object singletonStorageSet(long uid, Object obj)
	{
		RuntimeStore store = RuntimeStore.getRuntimeStore();
		Object objS;
		LongVector v;
		if((objS = store.get(SINGLETON_STORAGE_ID)) != null) //Singleton list exists
		{
			v = (LongVector)objS;
			if(v.contains(uid))
			{
				objS = store.get(uid); //Get previous value
				if(obj != null)
				{
					store.replace(uid, obj); //Replace the current object
				}
				else
				{
					store.remove(uid); //Remove the object
					v.removeElement(uid);
				}
				return objS; //Return previous object
			}
		}
		if(obj != null) //If the function hasn't returned yet and the object is not null then the Singleton list doesn't exist yet
		{
			v = new LongVector(); //Create the list and add the object
			store.put(SINGLETON_STORAGE_ID, v);
			store.put(uid, obj); //Will throw an exception if already there
			v.addElement(uid);
		}
		return null;
	}
	
	/**
	 * Remove all singleton objects.
	 */
	public synchronized static void singltonStorageCleanup()
	{
		RuntimeStore store = RuntimeStore.getRuntimeStore();
		Object obj;
		if((obj = store.get(SINGLETON_STORAGE_ID)) != null)
		{
			LongVector v = (LongVector)obj;
			store.remove(SINGLETON_STORAGE_ID);
			int len = v.size();
			for(int i = 0; i < len; i++)
			{
				store.remove(v.elementAt(i));
			}
		}
	}
	
	private static class ResourcesDefaultImpl implements Resources
	{
		private static final String ORG_PATH = "/com/sun/pdfview/";
		
		private String path;
		private int index;
		
		public ResourcesDefaultImpl(String relPath, boolean abs)
		{
			this.path = abs ? relPath : ORG_PATH + relPath;
			this.index = abs ? 0 : ORG_PATH.length();
			if((!relPath.endsWith(".")) || (!relPath.endsWith("/")))
			{
				this.path += "/";
				this.index++;
			}
			this.path = this.path.replace('.', '/');
		}
		
		public String getName()
		{
			return this.path.substring(this.index);
		}
		
		public InputStream getStream(String name)
		{
			return getClass().getResourceAsStream(path + name);
		}
		
		public String getString(long ID)
		{
			throw new UnsupportedOperationException();
		}
		
		public String[] getStringArray(long ID)
		{
			throw new UnsupportedOperationException();
		}
	}
}
