//#preprocessor

//#ifndef NATIVE_SOFTREFERENCE

/*
 * File: SoftReference.java
 * Version: 1.0
 * Initial Creation: May 5, 2010 8:43:30 PM
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
package com.sun.pdfview.helper;

import java.util.Vector;

import net.rim.device.api.lowmemory.LowMemoryListener;
import net.rim.device.api.lowmemory.LowMemoryManager;
import net.rim.device.api.system.RuntimeStore;

/**
 * Similar and compatible with java.lang.ref.SoftReference only BlackBerry specific. Requires signing.
 * @author Vincent Simonetti
 */
public class SoftReference
{
	private static Object mem;
	
	/**
	 * The low memory listener that should execute to remove any references that are being contained in it.
	 */
	private class SoftReferenceMemoryManager implements LowMemoryListener
	{
		public boolean freeStaleObject(int priority)
		{
			//Could use priority but it doesn't seem to change
			return freeReferences(this);
		}
	}
	
	private static boolean freeReferences(LowMemoryListener man)
	{
		//Manually retrieve the reference so that it doesn't try to create a new array if one is not avalible. Shouldn't be a problem since the LowMemoryListsner is 
		//only called if the vector had to be created.
		RuntimeStore store = RuntimeStore.getRuntimeStore();
		
		Object obj;
		boolean free = false;
		if((obj = store.get(SOFT_REFERENCE_ID)) != null)
		{
			synchronized(obj)
			{
				store.remove(SOFT_REFERENCE_ID);
				//free = freeVector((Vector)obj); //Originally freed each object in the array... For a 4MB PDF, pre-rendering then freeing it took about 6min. Way to 
				//		long, though this is what RIM's sample code had they also say in their documents that when arrays are freed each element in the array is also 
				//		freed. Let them do the work.
				LowMemoryManager.markAsRecoverable(obj);
				free = true;
				obj = null;
				LowMemoryManager.removeLowMemoryListener(man); //The only reason for the listener has been executed, no need to have it called again.
			}
		}
		return free;
	}
	
	/*
	private static boolean freeVector(Vector v)
	{
		//RIM's samples free each element in a Vector so do it just because they know better on the internals of BlackBerry's JVM and how it frees memory.
		boolean free = false;
		int size = v.size();
		for(int i = size - 1; i >= 0; i--)
		{
			Object obj = v.elementAt(i);
			v.removeElementAt(i);
			LowMemoryManager.markAsRecoverable(obj);
			obj = null;
			free = true;
		}
		return free;
	}
	*/
	
	private static final long SOFT_REFERENCE_ID = 0xB334695D1A6DB320L;
	private int index;
	
	/**
	 * Get a singleton vector that contains the references.
	 */
	private synchronized static Vector getReferences(SoftReference refS, boolean createIfNull)
	{
		RuntimeStore store = RuntimeStore.getRuntimeStore();
		
		Object obj;
		if((obj = store.get(SOFT_REFERENCE_ID)) == null && createIfNull)
		{
			SoftReferenceMemoryManager ref = refS.new SoftReferenceMemoryManager();
			mem = ref;
			LowMemoryManager.addLowMemoryListener(ref);
			store.put(SOFT_REFERENCE_ID, obj = new Vector());
		}
		//Could end up with having old references when an application is closed.
		return (Vector)obj;
	}
	
	/**
	 * A new soft reference, if memory is needed this reference will be released.
	 * @param referent The reference to store.
	 */
	public SoftReference(Object referent)
	{
		if(referent == null)
		{
			//Null, nothing todo
			return;
		}
		boolean cont = true;
		if(referent instanceof String[])
		{
			//Little hack to cleanup memory
			String[] ary = (String[])referent;
			if(ary[0].equals("CLEANUP_REFRENCES_CALLBACK") && ary[1].equals("PSW:00221133")) //Yes this is open source but hopefully no one will want to clean all the references unless the app is closing, in which case there is a nice helper function for that in PDFFile.
			{
				cont = false;
				this.index = -1;
				if(mem != null)
				{
					freeReferences((LowMemoryListener)mem);
				}
			}
		}
		if(cont)
		{
			Vector vect = getReferences(this, true);
			synchronized(vect)
			{
				int len = vect.size();
				int ind = -1;
				for(int i = 0; i < len; i++)
				{
					//See if a space is available so the array doesn't need to be resized.
					if(vect.elementAt(i) == null)
					{
						ind = i;
						break;
					}
				}
				//Set/add the reference
				if(ind == -1)
				{
					this.index = len;
					vect.addElement(referent);
				}
				else
				{
					this.index = ind;
					vect.setElementAt(referent, ind);
				}
			}
		}
	}
	
	/**
	 * Remove the reference.
	 */
	public void clear()
	{
		if(this.index >= 0)
		{
			Vector vect = getReferences(this, false);
			if(vect != null)
			{
				synchronized(vect)
				{
					if(vect.size() > this.index)
					{
						//Remove the reference from the array and mark it as recoverable
						vect.setElementAt(null, this.index);
					}
					this.index = -1;
				}
			}
			else
			{
				this.index = -1;
			}
		}
	}
	
	/**
	 * Get the object that this reference is referring to.
	 * @return The object this reference is referring to, if the reference has been freed or cleared then it will return null.
	 */
	public Object get()
	{
		if(index >= 0)
		{
			//Get the reference
			Vector vect = getReferences(this, false);
			if(vect != null)
			{
				synchronized(vect)
				{
					if(vect.size() > this.index)
					{
						return vect.elementAt(this.index);
					}
					else
					{
						//Element has been freed, clear it for easier processing later
						this.index = -1;
					}
				}
			}
			else
			{
				//Element has been freed, clear it for easier processing later
				this.index = -1;
			}
		}
		return null;
	}
}

//#endif