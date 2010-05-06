/*
 * File: HexDump.java
 * Version: 1.3
 * Initial Creation: May 5, 2010 7:11:08 PM
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
package com.sun.pdfview;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

public class HexDump
{
	public static void printData(byte[] data)
	{
        char[] parts = new char[17];
        int partsloc = 0;
        for (int i = 0; i < data.length; i++)
        {
            int d = ((int) data[i]) & 0xff;
            if (d == 0)
            {
                parts[partsloc++] = '.';
            }
            else if (d < 32 || d >= 127)
            {
                parts[partsloc++] = '?';
            }
            else
            {
                parts[partsloc++] = (char)d;
            }
            if (i % 16 == 0)
            {
                int start = Integer.toHexString(data.length).length();
                int end = Integer.toHexString(i).length();

                for (int j = start; j > end; j--)
                {
                    System.out.print("0");
                }
                System.out.print(Integer.toHexString(i) + ": ");
            }
            if (d < 16)
            {
                System.out.print("0" + Integer.toHexString(d));
            }
            else
            {
                System.out.print(Integer.toHexString(d));
            }
            if ((i & 15) == 15 || i == data.length - 1)
            {
                System.out.println("      " + new String(parts));
                partsloc = 0;
            }
            else if ((i & 7) == 7)
            {
                System.out.print("  ");
                parts[partsloc++] = ' ';
            }
            else if ((i & 1) == 1)
            {
                System.out.print(" ");
            }
        }
        System.out.println();
    }
	
	public static void main(String[] args)
	{
        if (args.length != 1)
        {
            System.out.println("Usage: ");
            System.out.println("    HexDump <filename>");
            System.exit(-1);
        }
        
        FileConnection file = null;
        try
        {
        	file = (FileConnection)Connector.open(args[0], Connector.READ);

            int size = (int)file.fileSize();
            byte[] data = new byte[size];
            
            InputStream in = file.openInputStream();
            in.read(data, 0, size);
            in.close();
            
            printData(data);
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
        finally
        {
        	if(file != null)
        	{
        		try
        		{
        			file.close();
        		}
        		catch(IOException ioe)
        		{
        			ioe.printStackTrace();
        		}
        	}
        }
    }
}
