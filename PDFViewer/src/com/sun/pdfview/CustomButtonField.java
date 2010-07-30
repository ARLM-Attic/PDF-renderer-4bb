/*
 * File: CustomButtonField
 * Version: 1.0
 * Initial Creation: May 29, 2010 6:18:52 PM
 */
package com.sun.pdfview;

import net.rim.device.api.math.Fixed32;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.system.PNGEncodedImage;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.XYDimension;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.ui.component.ButtonField;

/**
 * Customizable button
 * @author Vincent Simonetti
 */
//Originally created for personal helper library called RebuildLib.
public class CustomButtonField extends ButtonField
{
	//DrawStyle is not used because some undesired/unsupported flags are available.
	/**
	 * Put the text on the bottom of the button.
	 */
    public static final int BOTTOM = 1;
    /**
     * Put the text on the top of the button.
     */
    public static final int TOP = 2;
    /**
     * Put the text on the left of the button.
     */
    public static final int LEFT = 4;
    /**
     * Put the text on the right of the button.
     */
    public static final int RIGHT = 8;
    /**
     * Put the text in the center (vertical) of the button.
     */
    public static final int VCENTER = BOTTOM | TOP;
    /**
     * Put the text in the center (horizontal) of the button.
     */
    public static final int HCENTER = LEFT | RIGHT;
    
    /**
     * Normal icon. This is the main icon to display.
     */
    protected EncodedImage nIcon;
    /**
     * Focus icon for when the button is focused.
     */
    protected EncodedImage fIcon;
    /**
     * Click icon for when the button is clicked.
     */
    protected EncodedImage cIcon;
    /**
     * Flag that tells where the text is located.
     */
    protected int textLocation;
    /**
     * The gap between the icon and the text, used when the text is not centered.
     */
    protected int gap;
    /**
     * The scale of the icon if one exists. Scale is in the Fixed32 format.
     */
    protected int scale;
    /**
     * The maximum width of the button, used only for text.
     */
    protected int maxW;
    
    /**
     * Create a new CustomButtonField.
     */
    public CustomButtonField()
    {
        super();
        textLocation = VCENTER | HCENTER;
        gap = 3;
        scale = 655400;
        maxW = -1;
    }
    
    /**
     * Create a new CustomButtonField.
     * @param style The button style, use the standard Field style flags.
     */
    public CustomButtonField(long style)
    {
        super(style);
        textLocation = VCENTER | HCENTER;
        gap = 3;
        scale = 655400;
        maxW = -1;
    }
    
    /**
     * Create a new CustomButtonField.
     * @param label The text to use for the field.
     */
    public CustomButtonField(String label)
    {
        super(label);
        textLocation = VCENTER | HCENTER;
        gap = 3;
        scale = 655400;
        maxW = -1;
    }
    
    /**
     * Create a new CustomButtonField.
     * @param label The text to use for the field.
     * @param style The button style, use the standard Field style flags.
     */
    public CustomButtonField(String label, long style)
    {
        super(label, style);
        textLocation = VCENTER | HCENTER;
        gap = 3;
        scale = 655400;
        maxW = -1;
    }
    
    private XYDimension getLargestImageSize()
    {
        int mW = (nIcon == null) ? 0 : nIcon.getWidth();
        int mH = (nIcon == null) ? 0 : nIcon.getHeight();
        int temp = 0;
        if(fIcon != null)
        {
            temp = fIcon.getWidth();
            if(mW < temp)
            {
                mW = temp;
            }
            temp = fIcon.getHeight();
            if(mH < temp)
            {
                mH = temp;
            }
        }
        if(cIcon != null)
        {
            temp = cIcon.getWidth();
            if(mW < temp)
            {
                mW = temp;
            }
            temp = cIcon.getHeight();
            if(mH < temp)
            {
                mH = temp;
            }
        }
        return scaleDem(new XYDimension(mW, mH));
    }
    
    private XYDimension scaleDem(XYDimension dem)
    {
        XYDimension tDem = new XYDimension(Fixed32.mul(Fixed32.toFP(dem.width), scale), Fixed32.mul(Fixed32.toFP(dem.height), scale));
        tDem.width = Fixed32.div(tDem.width, 6553600);
        tDem.height = Fixed32.div(tDem.height, 6553600);
        tDem.width = Fixed32.toInt(tDem.width);
        tDem.height = Fixed32.toInt(tDem.height);
        //XYDimension tDem = new XYDimension(dem.width * scale, dem.height * scale);
        //tDem.width = tDem.width / 100;
        //tDem.height = tDem.height / 100;
        return tDem;
    }
    
    /**
     * Retrieves this field's preferred height.
     * @return Preferred height for this field in pixels.
     */
    public int getPreferredHeight()
    {
        int size = this.getFont().getHeight();
        if(nIcon != null)
        {
            //icon
            XYDimension icoS = getLargestImageSize();
            if((this.textLocation & HCENTER) == HCENTER)
            {
                if(icoS.height > size)
                {
                    size = icoS.height;
                }
            }
            else
            {
                size += icoS.height + ((this.getLabel().length() == 0) ? 0 : gap);
            }
        }
        if((this.getLabel() == null) || (this.getLabel().length() == 0))
        {
            return 0;
        }
        return size;
    }
    
    /**
     * Retrieves this field's preferred width.
     * @return Preferred width for this field in pixels.
     */
    public int getPreferredWidth()
    {
        int size = this.getFont().getAdvance(this.getLabel()) + gap;
        if(nIcon != null)
        {
            //icon
            XYDimension icoS = getLargestImageSize();
            if((this.textLocation & VCENTER) == VCENTER)
            {
                if(icoS.width > size)
                {
                    size = icoS.width;
                }
            }
            else
            {
                if(size != 0)
                {
                    if(icoS.width > size)
                    {
                        if(icoS.width > (size + gap))
                        {
                            size = icoS.width;
                        }
                    }
                }
                else
                {
                    size = icoS.width;
                }
            }
        }
        if(maxW == -1)
        {
            return size;
        }
        else
        {
            if(size > maxW)
            {
                return size;
            }
            else
            {
                return maxW;
            }
        }
    }
    
    /**
     * Invoked by the framework to redraw a portion of this field.
     * @param graphics Graphics context for drawing in this field.
     */
    protected void paint(Graphics graphics)
    {
        if((nIcon == null) && ((this.getLabel() == null) ? true : this.getLabel().length() == 0))
        {
            return;
        }
        if(!graphics.isDrawingStyleSet(Graphics.DRAWSTYLE_AALINES))
        {
            graphics.setDrawingStyle(Graphics.DRAWSTYLE_AALINES, true);
        }
        XYRect clip = graphics.getClippingRect();
        if(nIcon == null)
        {
            //Draw text only
            drawText(graphics, 0, clip.width);
        }
        else
        {
            EncodedImage scal = getScaledImage();
            XYRect icon = center(new XYDimension(scal.getScaledWidth(), scal.getScaledHeight()), clip);
            if((this.getLabel() == null) ? true : this.getLabel().length() == 0)
            {
                //Draw icon only
                drawIcon(graphics, icon.x, icon.y, scal);
            }
            else
            {
                //Draw icon and text
                int tY = 0;
                int tH = this.getFont().getHeight();
                if((textLocation & VCENTER) == VCENTER)
                {
                    tY = ((clip.height) / 2) - (tH / 2);
                }
                else if((textLocation & BOTTOM) == BOTTOM)
                {
                    tY = clip.height - tH;
                    icon.y -= gap;
                }
                else
                {
                    icon.y += gap;
                }
                graphics.drawImage(icon, scal, 0, 0, 0);
                drawText(graphics, tY, clip.width);
            }
        }
    }
    
    private XYRect center(XYDimension icon, XYRect clip)
    {
        return new XYRect((clip.width / 2) - (icon.width / 2), (clip.height / 2) - (icon.height / 2), icon.width, icon.height);
    }
    
    private int convertDrawFlags()
    {
        int draw = (int)(this.getStyle() & DrawStyle.ELLIPSIS);
        if((textLocation & HCENTER) == HCENTER)
        {
            draw |= DrawStyle.HCENTER;
        }
        else if((textLocation & LEFT) == LEFT)
        {
            draw |= DrawStyle.LEFT;
        }
        else
        {
            draw |= DrawStyle.RIGHT;
        }
        return draw;
    }
    
    private void drawText(Graphics graphics, int y, int width)
    {
        graphics.setFont(this.getFont());
        graphics.setColor(Color.WHITE);
        graphics.drawText(this.getLabel(), gap, y, convertDrawFlags(), width);
    }
    
    private EncodedImage getScaledImage()
    {
        int scaledScale = getScaledScale(nIcon.getWidth(), nIcon.getHeight());
        EncodedImage scal = null;
        boolean drawNorm = true;
        if(this.isFocus())
        {
            if(fIcon != null)
            {
                drawNorm = false;
                scal = fIcon.scaleImage32(scaledScale, scaledScale);
            }
        }
        else if(this.isSelecting())
        {
            if(cIcon != null)
            {
                drawNorm = false;
                scal = cIcon.scaleImage32(scaledScale, scaledScale);
            }
        }
        if(drawNorm)
        {
            scal = nIcon.scaleImage32(scaledScale, scaledScale);
        }
        return scal;
    }
    
    private void drawIcon(Graphics graphics, int x, int y, EncodedImage scal)
    {
        graphics.drawImage(x, y, scal.getScaledWidth(), scal.getScaledHeight(), scal, 0, 0, 0);
    }
    
    private int getScaledScale(int width, int height)
    {
        int w = Fixed32.div(Fixed32.toFP(width), Fixed32.div(Fixed32.mul(Fixed32.toFP(width), scale), 6553600));
        int h = Fixed32.div(Fixed32.toFP(height), Fixed32.div(Fixed32.mul(Fixed32.toFP(height), scale), 6553600));
        return Fixed32.div(w + h, 131072); //2
    }
    
    //Normal icon
    
    /**
     * Set the normal icon using a Bitmap.
     * @param ico The icon to set or null if no normal icon should be set.
     */
    public void setNormalIcon(Bitmap ico)
    {
    	setNormalIcon(ico == null ? null : PNGEncodedImage.encode(ico));
    }
    
    /**
     * Set the normal icon using a EncodedImage.
     * @param ico The icon to set or null if no normal icon should be set.
     */
    public void setNormalIcon(EncodedImage ico)
    {
        if(ico == null)
        {
            if(fIcon != null)
            {
                nIcon = fIcon;
            }
            else if(cIcon != null)
            {
                nIcon = cIcon;
            }
            else
            {
                nIcon = null;
            }
            return;
        }
        nIcon = ico;
    }
    
    /**
     * Get the normal icon as a Bitmap.
     * @return The icon as a Bitmap or null if one does not exist.
     */
    public Bitmap getNormalIconBitmap()
    {
        if(nIcon == null)
        {
            return null;
        }
        return nIcon.getBitmap();
    }
    
    /**
     * Get the normal icon as a EncodedImage.
     * @return The icon as a EncodedImage or null if one does not exist.
     */
    public EncodedImage getNormalIconEncoded()
    {
        if(nIcon == null)
        {
            return null;
        }
        return nIcon;
    }
    
    //Focus icon
    
    /**
     * Set the focus icon using a Bitmap.
     * @param ico The icon to set or null if no focus icon should be set.
     */
    public void setFocusIcon(Bitmap ico)
    {
    	setFocusIcon(ico == null ? null : PNGEncodedImage.encode(ico));
    }
    
    /**
     * Set the focus icon using a EncodedImage.
     * @param ico The icon to set or null if no focus icon should be set.
     */
    public void setFocusIcon(EncodedImage ico)
    {
        fIcon = ico;
        if(fIcon != null)
        {
            if(nIcon == null)
            {
                nIcon = fIcon;
            }
        }
    }
    
    /**
     * Get the focus icon as a Bitmap.
     * @return The icon as a Bitmap or null if one does not exist.
     */
    public Bitmap getFocusIconBitmap()
    {
        if(fIcon == null)
        {
            return null;
        }
        return fIcon.getBitmap();
    }
    
    /**
     * Get the focus icon as a EncodedImage.
     * @return The icon as a EncodedImage or null if one does not exist.
     */
    public EncodedImage getFocusIconEncoded()
    {
        if(fIcon == null)
        {
            return null;
        }
        return fIcon;
    }
    
    //Click icon
    
    /**
     * Set the click icon using a Bitmap.
     * @param ico The icon to set or null if no focus icon should be set.
     */
    public void setClickIcon(Bitmap ico)
    {
    	setClickIcon(ico == null ? null : PNGEncodedImage.encode(ico));
    }
    
    /**
     * Set the click icon using a EncodedImage.
     * @param ico The icon to set or null if no focus icon should be set.
     */
    public void setClickIcon(EncodedImage ico)
    {
        cIcon = ico;
        if(cIcon != null)
        {
            if(nIcon == null)
            {
                nIcon = cIcon;
            }
        }
    }
    
    /**
     * Get the click icon as a Bitmap.
     * @return The icon as a Bitmap or null if one does not exist.
     */
    public Bitmap getClickIconBitmap()
    {
        if(cIcon == null)
        {
            return null;
        }
        return cIcon.getBitmap();
    }
    
    /**
     * Get the click icon as a EncodedImage.
     * @return The icon as a EncodedImage or null if one does not exist.
     */
    public EncodedImage getClickIconEncoded()
    {
        if(cIcon == null)
        {
            return null;
        }
        return cIcon;
    }
    
    //Text location
    
    /**
     * Set the text location.
     * @param loc Use either BOTTOM, TOP, LEFT, RIGHT, VCENTER, HCENTER.
     */
    public void setTextLocation(int loc)
    {
        textLocation = loc;
    }
    
    /**
     * Get the text location.
     * @return One of the text locations location. One of the BOTTOM, TOP, LEFT, RIGHT, VCENTER, HCENTER.
     */
    public int getTextLocation()
    {
        return textLocation;
    }
    
    //Text gap
    
    /**
     * Set the gap between the icon and the text.
     * @param g The gap in pixels.
     */
    public void setTextGap(int g)
    {
        gap = Math.max(g, 0);
    }
    
    /**
     * Get the gap between the icon and the text.
     * @return The gap in pixels.
     */
    public int getTextGap()
    {
        return gap;
    }
    
    //Max width
    
    /**
     * Set the maximum width of the button when displaying text.
     * @param w The maximum width, anything <= 0 means that there is no maximum width.
     */
    public void setMaxWidth(int w)
    {
        maxW = w;
        if(maxW <= 0)
        {
            maxW = -1;
        }
    }
    
    /**
     * Get the maximum width of the button when displaying text.
     * @return The maximum width, anything <= 0 means that there is no maximum width.
     */
    public int getMaxWidth()
    {
        return maxW;
    }
    
    //Icon scale
    
    /**
     * Set the scale of the icon. Scale must be a valid Fixed32 number.
     * @param fix32scale The scale the icon/s should be drawn at. Must be a valid Fixed32 number and must be in the format of 0 &lt; fix32scale &lt;= 1. 0 is equal to a 0x0 px icon, 1 is equal to the same size as the icon loaded, etc..
     */
    public void setIconScale(int fix32scale)
    {
        scale = Fixed32.mul(fix32scale, 6553600); //100% == 6553600
    }
    
    /**
     * Get the scale that the icon/s should be drawn at as a Fixed32 number.
     * @return The scale of the icons.
     */
    public int getIconScale()
    {
        return Fixed32.div(scale, 6553600);
    }
}
