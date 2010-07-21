/*
 * File: TrueTypeFont.java
 * Version: 1.6
 * Initial Creation: May 18, 2010 10:31:45 PM
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.FontFamily;
import net.rim.device.api.ui.FontManager;
import net.rim.device.api.util.MathUtilities;

import com.sun.pdfview.helper.PDFUtil;

/**
*
* @author  jkaplan
*/
public class TrueTypeFont
{
	private int type;
    // could be a ByteBuffer or a TrueTypeTable
	
    private Hashtable tables;
    
    /** Creates a new instance of TrueTypeParser */
    public TrueTypeFont(int type)
    {
        this.type = type;

        tables = PDFUtil.synchronizedTable(new Hashtable());
    }
    
    /**
     * Parses a TrueType font from a byte array
     */
    public static TrueTypeFont parseFont(byte[] orig)
    {
        ByteBuffer inBuf = ByteBuffer.wrap(orig);
        return parseFont (inBuf);
    }

    /**
     * Parses a TrueType font from a byte buffer
     */
    public static TrueTypeFont parseFont(ByteBuffer inBuf)
    {
        int type = inBuf.getInt();
        short numTables = inBuf.getShort();
        short searchRange = inBuf.getShort();
        short entrySelector = inBuf.getShort();
        short rangeShift = inBuf.getShort();
        
        TrueTypeFont font = new TrueTypeFont(type);
        parseDirectories(inBuf, numTables, font);
        
        return font;
    }
    
    /**
     * Get the type of this font
     */
    public int getType()
    {
        return type;
    }
    
    /**
     * Add a table to the font
     *
     * @param tagString the name of this table, as a 4 character string
     *        (i.e. cmap or head)
     * @param data the data for this table, as a byte buffer
     */
    public void addTable (String tagString, ByteBuffer data)
    {
        tables.put(tagString, data);
    }
    
    /**
     * Add a table to the font
     *
     * @param tagString the name of this table, as a 4 character string
     *        (i.e. cmap or head)
     * @param table the table
     */
    public void addTable(String tagString, TrueTypeTable table)
    {
        tables.put(tagString, table);
    }
    
    /**
     * Get a table by name.  This command causes the table in question
     * to be parsed, if it has not already been parsed.
     *
     * @param tagString the name of this table, as a 4 character string
     *        (i.e. cmap or head)
     */
    public TrueTypeTable getTable(String tagString)
    {
        Object tableObj = tables.get(tagString);
        
        TrueTypeTable table = null;
        
        if (tableObj instanceof ByteBuffer)
        {
            // the table has not yet been parsed.  Parse it, and add the
            // parsed version to the map of tables.
            ByteBuffer data = (ByteBuffer)tableObj;
            
            table = TrueTypeTable.createTable(this, tagString, data);
            addTable(tagString, table);
        }
        else
        {
            table = (TrueTypeTable)tableObj;
        }
        
        return table;
    }
    
    /**
     * Remove a table by name
     *
     * @param tagString the name of this table, as a 4 character string
     *        (i.e. cmap or head)
     */
    public void removeTable(String tagString)
    {
        tables.remove(tagString);
    }
    
    /**
     * Get the number of tables
     */
    public short getNumTables()
    {
        return (short)tables.size ();
    }
    
    /**
     * Get the search range
     */
    public short getSearchRange()
    {
        double pow2 = Math.floor (MathUtilities.log(getNumTables()) / MathUtilities.log(2));
        double maxPower = MathUtilities.pow(2, pow2);
        
        return (short)(16 * maxPower);
    }

    /**
     * Get the entry selector
     */
    public short getEntrySelector()
    {
        double pow2 = Math.floor (MathUtilities.log(getNumTables()) / MathUtilities.log(2));
        double maxPower = MathUtilities.pow(2, pow2);
        
        return (short)(MathUtilities.log(maxPower) / MathUtilities.log(2));
    }

    /**
     * Get the range shift
     */
    public short getRangeShift()
    {
        double pow2 = Math.floor(MathUtilities.log(getNumTables()) / MathUtilities.log(2));
        double maxPower = MathUtilities.pow(2, pow2);

        return (short)((maxPower * 16) - getSearchRange());
    }
    
    /**
     * Write a font given the type and an array of Table Directory Entries
     */
    public byte[] writeFont()
    {
        // allocate a buffer to hold the font
        ByteBuffer buf = ByteBuffer.allocateDirect(getLength());
        
        // write the font header
        buf.putInt(getType());
        buf.putShort(getNumTables());
        buf.putShort(getSearchRange());
        buf.putShort(getEntrySelector());
        buf.putShort(getRangeShift());
        
        // first offset is the end of the table directory entries
        int curOffset = 12 + (getNumTables() * 16);
        
        // write the tables
        for (Enumeration i = tables.keys(); i.hasMoreElements();)
        {
            String tagString = (String) i.nextElement();
            int tag = TrueTypeTable.stringToTag(tagString);
            
            ByteBuffer data = null;
            
            Object tableObj = tables.get(tagString);
            if (tableObj instanceof TrueTypeTable)
            {
                data = ((TrueTypeTable)tableObj).getData();
            }
            else
            {
                data = (ByteBuffer)tableObj;
            }
            
            int dataLen = data.remaining ();
            
            // write the table directory entry
            buf.putInt(tag);
            buf.putInt(calculateChecksum(tagString, data));
            buf.putInt(curOffset);
            buf.putInt(dataLen);
            
            // save the current position
            int pos = buf.position();
            
            // move to the current offset and write the data
            buf.position(curOffset);
            buf.put(data);
            
            // reset the data start pointer
            data.flip();
            
            // return to the table directory entry
            buf.position(pos);
            
            // udate the offset
            curOffset += dataLen;
            
            // don't forget the padding
            while ((curOffset % 4) > 0)
            {
                curOffset++;
            }
        }
        
        buf.position(curOffset);
        buf.flip();
        
        // adjust the checksum
        updateChecksumAdj(buf);
        
        return buf.array();
    }
    
    /**
     * Calculate the checksum for a given table
     * 
     * @param tagString the name of the data
     * @param data the data in the table
     */
    private static int calculateChecksum (String tagString, ByteBuffer data)
    {
        int sum = 0;
        
        int pos = data.position();
        
        // special adjustment for head table
        if (tagString.equals("head"))
        {
            data.putInt(8, 0);
        }
        
        int nlongs = (data.remaining() + 3) / 4;
        
        while (nlongs-- > 0)
        {
            if (data.remaining() > 3)
            {
                sum += data.getInt();
            }
            else
            {
                byte b0 = (data.remaining() > 0) ? data.get() : 0;
                byte b1 = (data.remaining() > 0) ? data.get() : 0;
                byte b2 = (data.remaining() > 0) ? data.get() : 0;
                
                sum += ((0xff & b0) << 24) | ((0xff & b1) << 16) | ((0xff & b2) << 8);
            }
        }
        
        data.position(pos);
        
        return sum;
    }
    
    /**
     * Get directory entries from a font
     */
    private static void parseDirectories(ByteBuffer data, int numTables, TrueTypeFont ttf)
    {
        for (int i = 0; i < numTables; i++)
        {
            int tag = data.getInt();
            String tagString = TrueTypeTable.tagToString(tag);
//            System.out.println ("TTFFont.parseDirectories: " + tagString);
            int checksum = data.getInt();
            int offset = data.getInt();
            int length = data.getInt();
            
            // read the data
//            System.out.println ("TTFFont.parseDirectories: checksum: " + checksum + ", offset: " + offset + ", length: " + length);
            int pos = data.position();
            data.position(offset);
            
            ByteBuffer tableData = data.slice();
            tableData.limit(length);
            
            int calcChecksum = calculateChecksum(tagString, tableData);
            
            if (calcChecksum == checksum)
            {
                ttf.addTable (tagString, tableData);
            }
            else
            {
                // System.out.println("Mismatched checksums on table " + tagString + ": " + calcChecksum + " != " + checksum);
            	
                ttf.addTable (tagString, tableData);
            }
            data.position(pos);
        }
    }
    
    /**
     * Get the length of the font
     *
     * @return the length of the entire font, in bytes
     */
    private int getLength()
    {
        // the size of all the table directory entries
        int length = 12 + (getNumTables () * 16);
        
        // for each directory entry, get the size,
        // and don't forget the padding!
        for (Enumeration i = tables.elements(); i.hasMoreElements();)
        {
            Object tableObj = i.nextElement();
            
            // add the length of the entry
            if (tableObj instanceof TrueTypeTable)
            {
                length += ((TrueTypeTable)tableObj).getLength ();
            }
            else
            {
                length += ((ByteBuffer)tableObj).remaining ();
            }
            
            // pad
            if ((length % 4) != 0)
            {
                length += (4 - (length % 4));
            }
        }
        
        return length;
    }
    
    /**
     * Update the checksumAdj field in the head table
     */
    private void updateChecksumAdj(ByteBuffer fontData)
    {
        int checksum = calculateChecksum("", fontData);
        int checksumAdj = 0xb1b0afba - checksum;
        
        // find the head table
        int offset = 12 + (getNumTables() * 16);
        
        // find the head table
        for (Enumeration i = tables.keys(); i.hasMoreElements();)
        {
            String tagString = (String)i.nextElement();
            
            // adjust the checksum
            if (tagString.equals ("head"))
            {
                fontData.putInt(offset + 8, checksumAdj);
                return;
            }
            
            // add the length of the entry 
            Object tableObj = tables.get (tagString);
            if (tableObj instanceof TrueTypeTable)
            {
                offset += ((TrueTypeTable)tableObj).getLength ();
            }
            else
            {
                offset += ((ByteBuffer)tableObj).remaining ();
            }
            
            // pad
            if ((offset % 4) != 0)
            {
                offset += (4 - (offset % 4));
            }
        }
    }
    
    /**
     * Write the font to a pretty string
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer ();
        
        System.out.println("Type         : " + getType ());
        System.out.println("NumTables    : " + getNumTables ());
        System.out.println("SearchRange  : " + getSearchRange ());
        System.out.println("EntrySelector: " + getEntrySelector ());
        System.out.println("RangeShift   : " + getRangeShift ());
        
        for (Enumeration i = tables.keys(); i.hasMoreElements();)
        {
        	Object e = i.nextElement();
        	
            TrueTypeTable table = null;
            Object v = tables.get(e);
            if (v instanceof ByteBuffer)
            {
                table = getTable((String)e);
            }
            else
            {
                table = (TrueTypeTable)v;
            }
            
            System.out.println(table);
        }
        
        return buf.toString ();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main (String[] args)
    {
        if (args.length != 1)
        {
            System.out.println ("Usage: ");
            System.out.println ("    TrueTypeParser <filename>");
            System.exit (-1);
        }
        
        try
        {
        	FileConnection con = (FileConnection)Connector.open(args[0], Connector.READ);
            InputStream raf = con.openInputStream();
            
            int size = (int)con.fileSize();
            byte[] data = new byte[size];
            
            raf.read(data, 0, size);
            raf.close();
            con.close();
            
            TrueTypeFont ttp = TrueTypeFont.parseFont(data);
            
            System.out.println(ttp);
            
            InputStream fontStream = new ByteArrayInputStream(ttp.writeFont());
            
            String name = "TFONT" + System.currentTimeMillis();
            FontManager man = FontManager.getInstance();
            int status = man.load(fontStream, name, FontManager.APPLICATION_FONT);
            if(status == FontManager.SUCCESS)
            {
            	Font f = FontFamily.forName(name).getFont(Font.PLAIN, 10);
            	System.out.println("Font loaded");
            	man.unload(name);
            }
            else
            {
            	System.out.println("Font load error: " + status);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
