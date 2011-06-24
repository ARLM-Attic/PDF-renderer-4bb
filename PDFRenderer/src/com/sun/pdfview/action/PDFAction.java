/*
 * File: PDFAction.java
 * Version: 1.2
 * Initial Creation: May 8, 2010 11:12:04 PM
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
package com.sun.pdfview.action;

import java.io.IOException;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;

/**
 * The common super-class of all PDF actions.
 */
public class PDFAction
{
	/** the type of this action */
    private String type;
    
    /** the next action or array of actions */
    private PDFObject next;
    
    /** Creates a new instance of PDFAction */
    public PDFAction(String type)
    {
        this.type = type;
    }
    
    /**
     * Get an action of the appropriate type from a PDFObject
     *
     * @param obj the PDF object containing the action to parse
     * @param root the root of the PDF object tree
     */
    public static PDFAction getAction(PDFObject obj, PDFObject root) throws IOException
    {
        // figure out the action type
        PDFObject typeObj = obj.getDictRef("S");
        if (typeObj == null)
        {
            throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.ACTION_PDF_NO_TYPE, new Object[]{obj.toString()}));
        }
        
        // create the action based on the type
        PDFAction action = null;
        String type = typeObj.getStringValue();
        if (type.equals("GoTo"))
        {
            action = new GoToAction(obj, root);
        }
        else
        {
            /** [JK FIXME: Implement other action types! ] */
            throw new PDFParseException(com.sun.pdfview.ResourceManager.getResource(com.sun.pdfview.ResourceManager.LOCALIZATION).getFormattedString(com.sun.pdfview.i18n.ResourcesResource.ACTION_PDF_UNK_TYPE, new Object[]{type}));
        }
        
        // figure out if there is a next action
        PDFObject nextObj = obj.getDictRef("Next");
        if (nextObj != null)
        {
            action.setNext(nextObj);
        }
        
        // return the action
        return action;
    }
    
    /**
     * Get the type of this action
     */
    public String getType()
    {
        return type;
    }
    
    /**
     * Get the next action or array of actions
     */
    public PDFObject getNext()
    {
        return next;
    }
    
    /**
     * Set the next action or array of actions
     */
    public void setNext(PDFObject next)
    {
        this.next = next;
    }
}
