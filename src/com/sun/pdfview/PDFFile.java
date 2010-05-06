/*
 * File: PDFFile.java
 * Version: 1.15
 * Initial Creation: May 6, 2010 7:04:57 AM
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
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Vector;

/**
 * An encapsulation of a .pdf file.  The methods of this class
 * can parse the contents of a PDF file, but those methods are
 * hidden.  Instead, the public methods of this class allow
 * access to the pages in the PDF file.  Typically, you create
 * a new PDFFile, ask it for the number of pages, and then
 * request one or more PDFPages.
 * @author Mike Wessler
 */
public class PDFFile
{
	public final static int NUL_CHAR = 0;
    public final static int FF_CHAR = 12;

    private String versionString = "1.1";
    private int majorVersion = 1;
    private int minorVersion = 1;
    /** the end of line character */
    /** the comment text to begin the file to determine it's version */
    private final static String VERSION_COMMENT = "%PDF-";
    /**
     * A ByteBuffer containing the file data
     */
    ByteBuffer buf;
    /**
     * the cross reference table mapping object numbers to locations
     * in the PDF file
     */
    PDFXref[] objIdx;
    /** the root PDFObject, as specified in the PDF file */
    PDFObject root = null;
    /** the Encrypt PDFObject, from the trailer */
    PDFObject encrypt = null;

    /** The Info PDFPbject, from the trailer, for simple metadata */
    PDFObject info = null;

    /** a mapping of page numbers to parsed PDF commands */
    Cache cache;
    /**
     * whether the file is printable or not (trailer -> Encrypt -> P & 0x4)
     */
    private boolean printable = true;
    /**
     * whether the file is saveable or not (trailer -> Encrypt -> P & 0x10)
     */
    private boolean saveable = true;

    /**
     * The default decrypter for streams and strings. By default, no
     * encryption is expected, and thus the IdentityDecrypter is used.
     */
    private PDFDecrypter defaultDecrypter = IdentityDecrypter.getInstance();

    /**
     * get a PDFFile from a .pdf file.  The file must me a random access file
     * at the moment.  It should really be a file mapping from the nio package.
     * <p>
     * Use the getPage(...) methods to get a page from the PDF file.
     * @param buf the RandomAccessFile containing the PDF.
     * @throws IOException if there's a problem reading from the buffer
     * @throws PDFParseException if the document appears to be malformed, or
     *  its features are unsupported. If the file is encrypted in a manner that
     *  the product or platform does not support then the exception's {@link
     *  PDFParseException#getCause() cause} will be an instance of {@link
     *  UnsupportedEncryptionException}.
     * @throws PDFAuthenticationFailureException if the file is password
     *  protected and requires a password
     */
    public PDFFile(ByteBuffer buf) throws IOException
    {
    	this(buf, null);
    }

    /**
     * get a PDFFile from a .pdf file.  The file must me a random access file
     * at the moment.  It should really be a file mapping from the nio package.
     * <p>
     * Use the getPage(...) methods to get a page from the PDF file.
     * @param buf the RandomAccessFile containing the PDF.
     * @param password the user or owner password
     * @throws IOException if there's a problem reading from the buffer
     * @throws PDFParseException if the document appears to be malformed, or
     *  its features are unsupported. If the file is encrypted in a manner that
     *  the product or platform does not support then the exception's {@link
     *  PDFParseException#getCause() cause} will be an instance of {@link
     *  UnsupportedEncryptionException}.
     * @throws PDFAuthenticationFailureException if the file is password
     *  protected and the supplied password does not decrypt the document
     */
    public PDFFile(ByteBuffer buf, PDFPassword password) throws IOException
    {
        this.buf = buf;

        cache = new Cache();

        parseFile(password);
    }
    
    /**
     * Gets whether the owner of the file has given permission to print
     * the file.
     * @return true if it is okay to print the file
     */
    public boolean isPrintable()
    {
        return printable;
    }

    /**
     * Gets whether the owner of the file has given permission to save
     * a copy of the file.
     * @return true if it is okay to save the file
     */
    public boolean isSaveable()
    {
        return saveable;
    }

    /**
     * get the root PDFObject of this PDFFile.  You generally shouldn't need
     * this, but we've left it open in case you want to go spelunking.
     */
    public PDFObject getRoot()
    {
        return root;
    }

    /**
     * return the number of pages in this PDFFile.  The pages will be
     * numbered from 1 to getNumPages(), inclusive.
     */
    public int getNumPages()
    {
        try
        {
            return root.getDictRef("Pages").getDictRef("Count").getIntValue();
        }
        catch (Exception ioe)
        {
            return 0;
        }
    }
    
    /**
     * Get metadata (e.g., Author, Title, Creator) from the Info dictionary
     * as a string.
     * @param name the name of the metadata key (e.g., Author)
     * @return the info
     * @throws IOException if the metadata cannot be read
     */
    public String getStringMetadata(String name) throws IOException
    {
        if (info != null)
        {
            final PDFObject meta = info.getDictRef(name);
            return meta != null ? meta.getTextStringValue() : null;
        }
        else
        {
            return null;
        }
    }

    /**
     * Get the keys into the Info metadata, for use with
     * {@link #getStringMetadata(String)}
     * @return the keys present into the Info dictionary
     * @throws IOException if the keys cannot be read
     */
    public Enumeration getMetadataKeys() throws IOException
    {
        if (info != null)
        {
            return info.getDictKeys();
        }
        else
        {
            return new Vector().elements();
        }
    }


    /**
     * Used internally to track down PDFObject references.  You should never
     * need to call this.
     * <p>
     * Since this is the only public method for tracking down PDF objects,
     * it is synchronized.  This means that the PDFFile can only hunt down
     * one object at a time, preventing the file's location from getting
     * messed around.
     * <p>
     * This call stores the current buffer position before any changes are made
     * and restores it afterwards, so callers need not know that the position
     * has changed.
     *
     */
    public synchronized PDFObject dereference(PDFXref ref, PDFDecrypter decrypter) throws IOException
    {
        int id = ref.getID();
        
        // make sure the id is valid and has been read
        if (id >= objIdx.length || objIdx[id] == null)
        {
            return PDFObject.nullObj;
        }
        
        // check to see if this is already dereferenced
        PDFObject obj = objIdx[id].getObject();
        if (obj != null)
        {
            return obj;
        }
        
        int loc = objIdx[id].getFilePos();
        if (loc < 0)
        {
            return PDFObject.nullObj;
        }
        
        // store the current position in the buffer
        int startPos = buf.position();
        
        // move to where this object is
        buf.position(loc);
        
        // read the object and cache the reference
        obj= readObject(ref.getID(), ref.getGeneration(), decrypter);
        if (obj == null)
        {
            obj = PDFObject.nullObj;
        }
        
        objIdx[id].setObject(obj);
        
        // reset to the previous position
        buf.position(startPos);
        
        return obj;
    }
    
    /**
     * Is the argument a white space character according to the PDF spec?.
     * ISO Spec 32000-1:2008 - Table 1
     */
    public static boolean isWhiteSpace(int c)
    {
        switch (c)
        {
            case NUL_CHAR:  // Null (NULL)
            case '\t':      // Horizontal Tab (HT)
            case '\n':      // Line Feed (LF)
            case FF_CHAR:   // Form Feed (FF)
            case '\r':      // Carriage Return (CR)
            case ' ':       // Space (SP)
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Is the argument a delimiter according to the PDF spec?<p>
     *
     * ISO 32000-1:2008 - Table 2
     *
     * @param c the character to test
     */
    public static boolean isDelimiter(int c)
    {
        switch (c)
        {
            case '(':   // LEFT PARENTHESIS
            case ')':   // RIGHT PARENTHESIS
            case '<':   // LESS-THAN-SIGN
            case '>':   // GREATER-THAN-SIGN
            case '[':   // LEFT SQUARE BRACKET
            case ']':   // RIGHT SQUARE BRACKET
            case '{':   // LEFT CURLY BRACKET
            case '}':   // RIGHT CURLY BRACKET
            case '/':   // SOLIDUS
            case '%':   // PERCENT SIGN
                return true;
            default:
                return false;
        }
    }

    /**
     * return true if the character is neither a whitespace or a delimiter.
     *
     * @param c the character to test
     * @return boolean
     */
    public static boolean isRegularCharacter (int c)
    {
        return !(isWhiteSpace(c) || isDelimiter(c));
    }
    
    /**
     * read the next object from the file
     * @param objNum the object number of the object containing the object
     *  being read; negative only if the object number is unavailable (e.g., if
     *  reading from the trailer, or reading at the top level, in which
     *  case we can expect to be reading an object description)
     * @param objGen the object generation of the object containing the object
     *  being read; negative only if the objNum is unavailable
     * @param decrypter the decrypter to use
     */
    private PDFObject readObject(int objNum, int objGen, PDFDecrypter decrypter) throws IOException
    {
    	return readObject(objNum, objGen, false, decrypter);
    }

    /**
     * read the next object with a special catch for numbers
     * @param numscan if true, don't bother trying to see if a number is
     *  an object reference (used when already in the middle of testing for
     *  an object reference, and not otherwise)
     * @param objNum the object number of the object containing the object
     *  being read; negative only if the object number is unavailable (e.g., if
     *  reading from the trailer, or reading at the top level, in which
     *  case we can expect to be reading an object description)
     * @param objGen the object generation of the object containing the object
     *  being read; negative only if the objNum is unavailable
     * @param decrypter the decrypter to use
     */
    private PDFObject readObject(int objNum, int objGen, boolean numscan, PDFDecrypter decrypter) throws IOException
    {
        // skip whitespace
        int c;
        PDFObject obj = null;
        while (obj == null)
        {
            while (isWhiteSpace(c = buf.get()));
            // check character for special punctuation:
            if (c == '<')
            {
                // could be start of <hex data>, or start of <<dictionary>>
                c = buf.get();
                if (c == '<')
                {
                    // it's a dictionary
                	obj = readDictionary(objNum, objGen, decrypter);
                }
                else
                {
                    buf.position(buf.position() - 1);
                    obj = readHexString(objNum, objGen, decrypter);
                }
            }
            else if (c == '(')
            {
            	obj = readLiteralString(objNum, objGen, decrypter);
            }
            else if (c == '[')
            {
                // it's an array
            	obj= readArray(objNum, objGen, decrypter);
            }
            else if (c == '/')
            {
                // it's a name
                obj = readName();
            }
            else if (c == '%')
            {
                // it's a comment
                readLine();
            }
            else if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.')
            {
                // it's a number
                obj = readNumber((char) c);
                if (!numscan)
                {
                    // It could be the start of a reference.
                    // Check to see if there's another number, then "R".
                    //
                    // We can't use mark/reset, since this could be called
                    // from dereference, which already is using a mark
                    int startPos = buf.position();

                    PDFObject testnum= readObject(-1, -1, true, decrypter);
                    if (testnum != null && testnum.getType() == PDFObject.NUMBER)
                    {
                    	PDFObject testR= readObject(-1, -1, true, decrypter);
                        if (testR != null && testR.getType() == PDFObject.KEYWORD && testR.getStringValue().equals("R"))
                        {
                            // yup.  it's a reference.
                            PDFXref xref = new PDFXref(obj.getIntValue(), testnum.getIntValue());
                            // Create a placeholder that will be dereferenced
                            // as needed
                            obj = new PDFObject(this, xref);
                        }
                        else if (testR != null && testR.getType() == PDFObject.KEYWORD && testR.getStringValue().equals("obj")) {
                            // it's an object description
                        	obj= readObjectDescription(obj.getIntValue(), testnum.getIntValue(), decrypter);
                        }
                        else
                        {
                            buf.position(startPos);
                        }
                    }
                    else
                    {
                        buf.position(startPos);
                    }
                }
            }
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
            {
                // it's a keyword
                obj = readKeyword((char)c);
            }
            else
            {
                // it's probably a closing character.
                // throwback
                buf.position(buf.position() - 1);
                break;
            }
        }
        return obj;
    }
    
    /**
     * requires the next few characters (after whitespace) to match the
     * argument.
     * @param match the next few characters after any whitespace that
     * must be in the file
     * @return true if the next characters match; false otherwise.
     */
    private boolean nextItemIs(String match) throws IOException
    {
        // skip whitespace
        int c;
        while (isWhiteSpace(c = buf.get()));
        for (int i = 0; i < match.length(); i++)
        {
            if (i > 0)
            {
                c = buf.get();
            }
            if (c != match.charAt(i))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * process a version string, to determine the major and minor versions
     * of the file.
     * 
     * @param versionString
     */
    private void processVersion(String versionString)
    {
        try
        {
        	int index = versionString.indexOf('.');
            majorVersion = Integer.parseInt(versionString.substring(0, index));
            minorVersion = Integer.parseInt(versionString.substring(index + 1));
            this.versionString = versionString;
        }
        catch (Exception e)
        {
            // ignore
        }
    }
    
    /**
     * return the major version of the PDF header.
     * 
     * @return int
     */
    public int getMajorVersion()
    {
        return majorVersion;
    }

    /**
     * return the minor version of the PDF header.
     * 
     * @return int
     */
    public int getMinorVersion()
    {
        return minorVersion;
    }

    /**
     * return the version string from the PDF header.
     * 
     * @return String
     */
    public String getVersionString()
    {
        return versionString;
    }
    
    //TODO
}
