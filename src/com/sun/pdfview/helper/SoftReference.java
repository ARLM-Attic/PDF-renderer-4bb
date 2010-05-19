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
	/**
	 * The low memory listener that should execute to remove any references that are being contained in it.
	 */
	private class SoftReferenceMemoryManager implements LowMemoryListener
	{
		public boolean freeStaleObject(int priority)
		{
			//Manually retrieve the reference so that it doesn't try to create a new array if one is not avalible. Shouldn't be a problem since the LowMemoryListsner is 
			//only called if the vector had to be created.
			RuntimeStore store = RuntimeStore.getRuntimeStore();
			
			Object obj;
			if((obj = store.get(SOFT_REFERENCE_ID)) != null)
			{
				synchronized(obj)
				{
					store.remove(SOFT_REFERENCE_ID);
					LowMemoryManager.markAsRecoverable(obj);
					LowMemoryManager.removeLowMemoryListener(this); //The only reason for the listener has been executed, no need to have it called again.
					return true;
				}
			}
			return false;
		}
	}
	
	private static final long SOFT_REFERENCE_ID = 0xB334695D1A6DB320L;
	private int index;
	
	/**
	 * Get a singleton vector that contains the refrences.
	 */
	private synchronized static Vector getReferences()
	{
		RuntimeStore store = RuntimeStore.getRuntimeStore();
		
		Object obj;
		if((obj = store.get(SOFT_REFERENCE_ID)) == null)
		{
			SoftReferenceMemoryManager ref = new SoftReference(null).new SoftReferenceMemoryManager();
			LowMemoryManager.addLowMemoryListener(ref);
			store.put(SOFT_REFERENCE_ID, obj = new Vector());
		}
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
		Vector vect = getReferences();
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
	
	/**
	 * Remove the reference.
	 */
	public void clear()
	{
		Vector vect = getReferences();
		synchronized(vect)
		{
			//Remove the reference from the array and mark it as recoverable
			//Object obj = vect.elementAt(this.index);
			//LowMemoryManager.markAsRecoverable(obj);
			vect.setElementAt(null, this.index);
			this.index = -1;
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
			Vector vect = getReferences();
			synchronized(vect)
			{
				return vect.elementAt(this.index);
			}
		}
		return null;
	}
}

//#endif