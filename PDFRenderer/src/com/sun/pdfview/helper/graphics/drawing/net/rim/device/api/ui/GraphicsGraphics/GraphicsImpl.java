/*
 * File: GraphicsImpl.java
 * Version: 1.0
 * Initial Creation: Jun 25, 2011 11:41:01 PM
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
package com.sun.pdfview.helper.graphics.drawing.net.rim.device.api.ui.GraphicsGraphics;

/**
 * PDFgraphics implementation of Graphics. Note, this is a "just in case" class since it is unlikely that a non-GraphicsInternal type Graphics object will be
 * used (though if you create a LCDUI Image, then a Graphics object, then look at "peer" inside the Graphics object, it is a non-GraphicsInternal type Graphics object.
 */
public final class GraphicsImpl extends com.sun.pdfview.helper.graphics.drawing.net.rim.device.api.ui.GraphicsInternalGraphics.GraphicsImpl
{
}
