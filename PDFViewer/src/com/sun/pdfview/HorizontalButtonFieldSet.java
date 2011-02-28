//#preprocessor

/*
 * File: HorizontalButtonFieldSet.java
 * Version: 1.0
 * Initial Creation: May 29, 2010 7:11:32 PM
 *
 * Research In Motion Limited proprietary and confidential
 * Copyright Research In Motion Limited, 2009-2009
 */
package com.sun.pdfview;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Manager;
//#ifndef BlackBerrySDK4.1.0 | BlackBerrySDK4.2.0 | BlackBerrySDK4.2.1 | BlackBerrySDK4.3.0 | BlackBerrySDK4.5.0
import net.rim.device.api.ui.decor.Border;
//#endif

/**
 * VerticalButtonFieldSet aligns a group of buttons vertically
 * so that they have equal widths
 * 
 * This Manager does not respect Horizontal style bits on the child nodes
 * since they're all fully justified
 * 
 * If USE_ALL_WIDTH is passed into this manager then the children 
 * will also use all available width
 */
public class HorizontalButtonFieldSet extends Manager 
{
    public static final int AXIS_SEQUENTIAL = 0;
    public static final int AXIS_VERTICAL   = 1 << 1;
    
    public HorizontalButtonFieldSet()
    {
        this( 0 );
    } 
    
    public HorizontalButtonFieldSet( long style ) 
    {
        super( style );
    }
    
    protected void sublayout( int width, int height )
    {
        int availableWidth = width;

        int numFields = getFieldCount();
        int maxPreferredWidth = 0;
        int maxHeight = 0;


        // There may be a few remaining pixels after dividing up the space
        // we must split up the space between the first and last buttons
        int fieldWidth = width / numFields;
        int firstFieldExtra = 0;
        int lastFieldExtra = 0;
        
        int unUsedWidth = width - fieldWidth * numFields;
        if( unUsedWidth > 0 ) {
            firstFieldExtra = unUsedWidth / 2;
            lastFieldExtra = unUsedWidth - firstFieldExtra;
        }
        
        int prevRightMargin = 0;
        
        // Layout the child fields, and calculate the max height
        for( int i = 0; i < numFields; i++ ) {
            
            int nextLeftMargin = 0;
            if( i < numFields - 1 ) {
                Field nextField = getField( i );
                nextLeftMargin = nextField.getMarginLeft();
            }
            
            Field currentField = getField( i );
            
            int widthForButton = fieldWidth;
            
            int leftMargin  = Math.max( prevRightMargin, currentField.getMarginLeft() ) / 2;
            int rightMargin = Math.max( nextLeftMargin, currentField.getMarginRight() ) / 2;
            if( i == 0 ) {
                widthForButton = fieldWidth + firstFieldExtra;
                leftMargin = currentField.getMarginLeft();
            } else if( i == numFields -1 ) {
                widthForButton = fieldWidth + lastFieldExtra;
                rightMargin = currentField.getMarginRight();
            }
            
            int currentVerticalMargins = currentField.getMarginTop() + currentField.getMarginBottom();
            int currentHorizontalMargins = leftMargin + rightMargin;
            
            widthForButton -= currentHorizontalMargins;
            
            int currentPreferredWidth = currentField.getPreferredWidth() + getBorderWidth( currentField );
            if( currentPreferredWidth < widthForButton ) {
                int newPadding = ( widthForButton - currentPreferredWidth ) / 2; 
                currentField.setPadding( currentField.getPaddingTop(), newPadding, currentField.getPaddingBottom(), newPadding );
            }
            layoutChild( currentField, widthForButton, height );
            maxHeight = Math.max( maxHeight, currentField.getHeight() + currentVerticalMargins );
   
            prevRightMargin = rightMargin;
            nextLeftMargin = 0;
        }

        // Now position the fields, respecting the Vertical style bits
        int usedWidth = 0;
        int y;
        prevRightMargin = 0;
        for( int i = 0; i < numFields; i++ ) {
            
            Field currentField = getField( i );
            int marginTop = currentField.getMarginTop();
            int marginBottom = currentField.getMarginBottom();
            int marginLeft = Math.max( currentField.getMarginLeft(), prevRightMargin );
            int marginRight = currentField.getMarginRight();
            
            if( currentField.isStyle( FIELD_BOTTOM ) ) {
                    y = maxHeight - currentField.getHeight() - currentField.getMarginBottom();
            } else if( currentField.isStyle( FIELD_VCENTER ) ) {
                    y = marginTop + ( maxHeight - marginTop - currentField.getHeight() - marginBottom ) >> 1;
            } else {
            	y = marginTop;
            }
            setPositionChild( currentField, usedWidth + marginLeft, y );
            usedWidth += currentField.getWidth() + marginLeft;
            prevRightMargin = marginRight;
        }
        setExtent( width, maxHeight );
    }
    
    public static int getBorderWidth(Field field)
    {
        int width = 0;
        
//#ifdef BlackBerrySDK4.1.0 | BlackBerrySDK4.2.0 | BlackBerrySDK4.2.1 | BlackBerrySDK4.3.0 | BlackBerrySDK4.5.0
        width = field.getWidth() - field.getContentWidth() - field.getPaddingLeft() - field.getPaddingRight();
//#else
        Border border = field.getBorder();
        if( border != null )
        {
            width = border.getLeft() + border.getRight();
        }
//#endif
        return width;
    }
}
