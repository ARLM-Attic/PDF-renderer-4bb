//#preprocessor

/*
 * File: CmapTable.java
 * Version: 1.3
 * Initial Creation: May 19, 2010 1:59:51 PM
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
package com.sun.pdfview.font.ttf;

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.ByteBuffer;
//#else
import com.sun.pdfview.helper.nio.ByteBuffer;
//#endif
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.sun.pdfview.helper.PDFUtil;

/**
 * Represents the TTF "cmap" table
 *
 * @author  jkaplan
 */
public class CmapTable extends TrueTypeTable
{
	/** Holds value of property version. */
    private short version;
    
    /**
     * Holds the CMap subtables, sorted properly
     */
    private Hashtable subtables;
    
    /** Creates a new instance of CmapTable */
    protected CmapTable()
    {
        super(TrueTypeTable.CMAP_TABLE);
        
        setVersion((short)0x0);
        
        subtables = PDFUtil.synchronizedTable(new Hashtable());
    }
    
    /**
     * Add a CMap
     */
    public void addCMap(short platformID, short platformSpecificID, CMap cMap)
    {
        CmapSubtable key = new CmapSubtable(platformID, platformSpecificID);
        subtables.put(key, cMap);
    }
    
    /**
     * Get a CMap by platform and specific ID
     */
    public CMap getCMap(short platformID, short platformSpecificID)
    {
        CmapSubtable key = new CmapSubtable(platformID, platformSpecificID);
        return (CMap)subtables.get(key);
    }
    
    /**
     * Get all CMaps
     */
    public CMap[] getCMaps()
    {
        CMap[] maps = new CMap[subtables.size()];
        
        int k = 0;
        for(Enumeration i = subtables.elements(); i.hasMoreElements();)
        {
        	maps[k++] = (CMap)i.nextElement();
        }
        
        return maps;
    }
    
    /**
     * Remove a CMap
     */
    public void removeCMap(short platformID, short platformSpecificID)
    {
        CmapSubtable key = new CmapSubtable(platformID, platformSpecificID);
        subtables.remove(key);
    }
    
    public void setData(ByteBuffer data)
    {
        setVersion(data.getShort());
        
        short numberSubtables = data.getShort();
        
        for (int i = 0; i < numberSubtables; i++)
        {
            short platformID = data.getShort();
            short platformSpecificID = data.getShort();
            int offset = data.getInt();
            
            int pos = data.position();
            
            // get the position from the start of this buffer 
            data.position(offset);
            
            ByteBuffer mapData = data.slice();
            
            data.position(pos);
            
            try
            {
                CMap cMap = CMap.getMap(mapData);
                if (cMap != null)
                {
                    addCMap(platformID, platformSpecificID, cMap);
                }
            }
            catch (Exception ex)
            {
                System.out.println("Error reading map.  PlatformID=" + platformID + ", PlatformSpecificID=" + platformSpecificID);
                System.out.println("Reason: " + ex);
            }
        }
    }
    
    public ByteBuffer getData()
    {
        ByteBuffer buf = ByteBuffer.allocateDirect(getLength());
        
        // write the table header
        buf.putShort(getVersion());
        buf.putShort((short)subtables.size());
        
        // the current offset to write to, starts at the end of the
        // subtables
        int curOffset = 4 + (subtables.size() * 8);
        
        // write the subtables
        for (Enumeration i = subtables.keys(); i.hasMoreElements();)
        {
            CmapSubtable cms = (CmapSubtable)i.nextElement();
            CMap map = (CMap)subtables.get(cms);
            
            buf.putShort(cms.platformID);
            buf.putShort(cms.platformSpecificID);
            buf.putInt(curOffset);
            
            curOffset += map.getLength();
        }
        
        // write the tables
        for (Enumeration i = subtables.elements(); i.hasMoreElements();)
        {
            CMap map = (CMap)i.nextElement();
            buf.put(map.getData());
        }
        
        // reset the position to the start of the buffer
        buf.flip();
        
        return buf;
    }
    
    public int getLength()
    {
        // start with the size of the fixed data
        int length = 4;
        
        // add the size of the subtables 
        length += subtables.size() * 8;
        
        // add the size of the dynamic data
        for (Enumeration i = subtables.elements(); i.hasMoreElements();)
        {     
            // add the size of the subtable data
            CMap map = (CMap)i.nextElement();
            length += map.getLength();
        }
        
        return length;
    }
    
    /** Getter for property version.
     * @return Value of property version.
     *
     */
    public short getVersion()
    {
        return this.version;
    }
    
    /** Setter for property version.
     * @param version New value of property version.
     *
     */
    public void setVersion(short version)
    {
        this.version = version;
    }

    /**
     * Get the number of tables
     */
    public short getNumberSubtables()
    {
        return (short)subtables.size();
    }
    
    /** Print a pretty string */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        String indent = "    ";
        
        buf.append(indent + "Version: " + this.getVersion() + "\n");
        buf.append(indent + "NumMaps: " + this.getNumberSubtables() + "\n");
        
        for (Enumeration i = subtables.keys(); i.hasMoreElements();)
        {
            CmapSubtable key = (CmapSubtable)i.nextElement();
            
            buf.append(indent + "Map: platformID: " + key.platformID + " PlatformSpecificID: " + key.platformSpecificID + "\n");
            
            CMap map = (CMap)subtables.get(key);
            
            buf.append(map.toString());
        }
        
        return buf.toString();
    }
    
    class CmapSubtable
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
    	implements Comparable
//#endif
    {
        /**
         * The platformID for this subtable
         */
        short platformID;
        
        /**
         * The platform-specific id
         */
        short platformSpecificID;
        
        /** 
         * Create a Cmap subtable
         */
        protected CmapSubtable(short platformID, short platformSpecificID)
        {
            this.platformID = platformID;
            this.platformSpecificID = platformSpecificID;
        }
        
        /**
         * Compare two subtables
         */
        public boolean equals(Object obj)
        {
            return (compareTo(obj) == 0);
        }
        
        /**
         * Sort ascending by platform ID and then specific ID
         */
        public int compareTo(Object obj)
        {
            if (!(obj instanceof CmapSubtable))
            {
                return -1;
            }
            
            CmapSubtable cms = (CmapSubtable) obj;
            if (platformID < cms.platformID)
            {
                return -1;
            }
            else if (platformID > cms.platformID)
            {
                return 1;
            }
            else
            {
                if (platformSpecificID < cms.platformSpecificID)
                {
                    return -1;
                }
                else if (platformSpecificID > cms.platformSpecificID)
                {
                    return 1;
                }
                else
                {
                    return 0;
                }
            }
        }
    }
}
