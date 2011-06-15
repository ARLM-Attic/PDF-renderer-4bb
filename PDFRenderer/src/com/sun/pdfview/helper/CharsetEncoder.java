//#preprocessor

/*
 * File: CharsetEncoder.java
 * Version: 1.0
 * Initial Creation: May 9, 2010 8:09:34 PM
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.pdfview.helper;

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0 | BlackBerrySDK4.7.1
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
//#else
import com.sun.pdfview.helper.nio.Buffer;
import com.sun.pdfview.helper.nio.ByteBuffer;
import com.sun.pdfview.helper.nio.ShortBuffer;
import com.sun.pdfview.helper.nio.BufferOverflowException;
import com.sun.pdfview.helper.nio.BufferUnderflowException;
//#endif

import com.sun.pdfview.i18n.ResourcesResource;
import com.sun.pdfview.ResourceManager;


import net.rim.device.api.math.Fixed32;

/**
 * Modified implementation of java.nio.charset.CharsetEncoder. Only necessary components from the JDK source code are implemented. Other smaller minor classes are 
 * consolidated into this class.
 * @author Vincent Simonetti
 */
public abstract class CharsetEncoder
{
	// internal status consts
	private static final int INIT = 0;
	private static final int ONGOING = INIT + 1;
	private static final int END = ONGOING + 1;
	private static final int FLUSH = END + 1;
	
	// the Charset which creates this encoder
	//private Object cs;
	
	//TODO: Modify so it uses USE_FIXED32 to determine if Fixed32 should be used
	// average bytes per character created by this encoder
	private int averBytes;
	
	// maximum bytes per character can be created by this encoder, in Fixed32 format
	private int maxBytes;
	
	// replacement byte array
	private byte[] replace;
	
	// internal status
	private int status;
	
	// action for malformed input
	private int malformAction;
	
	// action for unmapped char input
	private int unmapAction;
	
	// CodingErrorAction replacement
	
	/** Action indicating that a coding error is to be handled by dropping the erroneous input and resuming the coding operation.*/
	public static final int ERROR_ACTION_IGNORE = 0;
	/** Action indicating that a coding error is to be handled by dropping the erroneous input, appending the coder's replacement value to the output buffer, and resuming the coding operation.*/
	public static final int ERROR_ACTION_REPLACE = ERROR_ACTION_IGNORE + 1;
	/** Action indicating that a coding error is to be reported, either by returning a <code>RESULT_</code> object or by throwing a CharacterCodingException, whichever is appropriate for the method implementing the coding process.*/
	public static final int ERROR_ACTION_REPORT = ERROR_ACTION_REPLACE + 1;
	
	// CoderResult replacement
	
	private static final int RESULT_OFFSET = 24;
	private static final int RESULT_VALUE_MASK = 0x0FFFFFFF;
	private static final int RESULT_IN_NO_ERROR = 0;
	private static final int RESULT_IN_OVERFLOW = RESULT_IN_NO_ERROR + 1;
	private static final int RESULT_IN_UNDERFLOW = RESULT_IN_OVERFLOW + 1;
	private static final int RESULT_IN_MALFORMED = RESULT_IN_UNDERFLOW + 1;
	private static final int RESULT_IN_UNMAPPABLE = RESULT_IN_MALFORMED + 1;
	private static final int RESULT_IN_INVALID = RESULT_IN_UNMAPPABLE + 3;
	
	/** Result object indicating overflow, meaning that there is insufficient room in the output buffer.*/
	public static final int RESULT_UNDERFLOW = RESULT_IN_OVERFLOW << RESULT_OFFSET;
	/** Result object indicating underflow, meaning that either the input buffer has been completely consumed or, if the input buffer is not yet empty, that additional input is required.*/
	public static final int RESULT_OVERFLOW = RESULT_IN_UNDERFLOW << RESULT_OFFSET;
	private static final int RESULT_MALFORMED = RESULT_IN_MALFORMED << RESULT_OFFSET;
	private static final int RESULT_UNMAPPABLE = RESULT_IN_UNMAPPABLE << RESULT_OFFSET;
	private static final int RESULT_INVALID = RESULT_IN_INVALID << RESULT_OFFSET;
	
	/**
	 * Static factory method that returns the unique object describing a malformed-input error of the given length.
	 */
	public static int resultMalformedForLength(int length)
	{
		if(length < 0 || length > RESULT_VALUE_MASK)
		{
			return RESULT_INVALID;
		}
		else if(length == 0)
		{
			return RESULT_IN_NO_ERROR;
		}
		else
		{
			return RESULT_MALFORMED | length;
		}
	}
	
	/**
	 * Static factory method that returns the unique result object describing an unmappable-character error of the given length.
	 */
	public static int resultUnmappableForLength(int length)
	{
		if(length < 0 || length > RESULT_VALUE_MASK)
		{
			return RESULT_INVALID;
		}
		else if(length == 0)
		{
			return RESULT_IN_NO_ERROR;
		}
		else
		{
			return RESULT_UNMAPPABLE | length;
		}
	}
	
	/**
	 * Returns the length of the erroneous input described by a result.
	 * @param result The result to check.
	 * @return The length of the erroneous input, a positive integer.
	 * @throws UnsupportedOperationException If the result does not describe an error condition, that is, if the {@link #resultIsError(int) resultIsError} does not return true.
	 */
	public static int resultLength(int result)
	{
		if(resultIsError(result))
		{
			return result & CharsetEncoder.RESULT_VALUE_MASK;
		}
		throw new UnsupportedOperationException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getString(ResourcesResource.HELPER_CHAR_ENC_BAD_RESULT));
	}
	
	/**
	 * Tells whether or not the result describes an error condition.
	 * @param result The result to check.
	 * @return <code>true</code> if, and only if, this object denotes either a malformed-input error or an unmappable-character error.
	 */
	public static boolean resultIsError(int result)
	{
		int type = result >>> CharsetEncoder.RESULT_OFFSET;
		return (type == RESULT_IN_MALFORMED || type == RESULT_IN_UNMAPPABLE);
	}
	
	/**
	 * Constructs a new <code>CharsetEncoder</code> using the given * <code>Charset</code>, average number and maximum number of bytes created by this encoder for one input 
	 * character.
	 * 
	 * @param cs the <code>Charset</code> to be used by this encoder.
	 * @param averageBytesPerChar average number of bytes created by this encoder for one input character, must be positive and in Fixed32 format.
	 * @param maxBytesPerChar maximum number of bytes which can be created by this encoder for one input character, must be positive and in Fixed32 format.
	 * @throws IllegalArgumentException if <code>maxBytesPerChar</code> or <code>averageBytesPerChar</code> is negative.
	 */
	protected CharsetEncoder(Object cs, int averageBytesPerChar, int maxBytesPerChar)
	{
		this(cs, averageBytesPerChar, maxBytesPerChar, new byte[] {(byte)'?'});
	}
	
	/**
	 * Constructs a new <code>CharsetEncoder</code> using the given * <code>Charset</code>, replacement byte array, average number and maximum number of bytes created by 
	 * this encoder for one input character.
	 * 
	 * @param cs the <code>Charset</code> to be used by this encoder.
	 * @param averageBytesPerChar average number of bytes created by this encoder for one single input character, must be positive and in Fixed32 format.
	 * @param maxBytesPerChar maximum number of bytes which can be created by this encoder for one single input character, must be positive and in Fixed32 format.
	 * @param replacement the replacement byte array, cannot be null or empty, its length cannot be larger than <code>maxBytesPerChar</code>, and must be a legal 
	 * replacement, which can be justified by {@link #isLegalReplacement(byte[]) isLegalReplacement}.
	 * @throws IllegalArgumentException if any parameters are invalid.
	 */
	protected CharsetEncoder(Object cs, int averageBytesPerChar, int maxBytesPerChar, byte[] replacement)
	{
		if(averageBytesPerChar <= 0 || maxBytesPerChar <= 0)
		{
			throw new IllegalArgumentException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getString(ResourcesResource.HELPER_CHAR_ENC_NEG_BYTE_NUM));
		}
		if(averageBytesPerChar > maxBytesPerChar)
		{
			throw new IllegalArgumentException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getString(ResourcesResource.HELPER_CHAR_ENC_AVG_BYTE_GRT_MAX_BYTE));
		}
		//this.cs = cs;
		this.averBytes = averageBytesPerChar;
		this.maxBytes = maxBytesPerChar;
		this.status = INIT;
		this.malformAction = CharsetEncoder.ERROR_ACTION_REPORT;
		this.unmapAction = CharsetEncoder.ERROR_ACTION_REPORT;
		replaceWith(replacement);
	}
	
	/**
	 * Gets the average number of bytes created by this encoder for a single input character.
	 * 
	 * @return the average number of bytes created by this encoder for a single  input character.
	 */
	public final int averageBytesPerChar()
	{
		return this.averBytes;
	}
	
	//canEncode and implCanEncode are unnecessary in the context of this program.
	
	/**
	 * Gets the <code>Charset</code> which this encoder uses.
	 * 
	 * @return the <code>Charset</code> which this encoder uses.
	 */
	public final Object charset()
	{
		//return this.cs;
		return null;
	}
	
	/**
	 * This is a facade method for the encoding operation.
	 * <p>
	 * This method encodes the remaining character sequence of the given character buffer into a new byte buffer. This method performs a complete encoding operation, 
	 * resets at first, then encodes, and flushes at last.
	 * <p>
	 * This method should not be invoked if another encode operation is ongoing.
	 * 
	 * @param in the input buffer.
	 * @return a new <code>ByteBuffer</code> containing the bytes produced by this encoding operation. The buffer's limit will be the position of the last byte in the 
	 * buffer, and the position will be zero.
	 * @throws IllegalStateException if another encoding operation is ongoing.
	 * @throws MalformedInputException if an illegal input character sequence for this charset is encountered, and the action for malformed error is 
	 * {@link CharsetEncoder#ERROR_ACTION_REPORT CharsetEncoder.ERROR_ACTION_REPORT}
	 * @throws UnmappableCharacterException if a legal but unmappable input character sequence for this charset is encountered, and the action for unmappable character 
	 * error is {@link CharsetEncoder#ERROR_ACTION_REPORT CharsetEncoder.ERROR_ACTION_REPORT}. Unmappable means the Unicode character sequence at the input buffer's 
	 * current position cannot be mapped to a equivalent byte sequence.
	 * @throws CharacterCodingException if other exception happened during the encode operation.
	 */
	public final ByteBuffer encode(ShortBuffer in) throws CharacterCodingException
	{
		if(in.remaining() == 0)
		{
			return ByteBuffer.allocateDirect(0);
		}
		reset();
		int length = Fixed32.toInt(in.remaining() * averBytes);
		ByteBuffer output = ByteBuffer.allocateDirect(length);
		int result = CharsetEncoder.RESULT_IN_INVALID << CharsetEncoder.RESULT_OFFSET;
		while(true)
		{
			result = encode(in, output, false);
			if (result == CharsetEncoder.RESULT_UNDERFLOW)
			{
				break;
			}
			else if (result == CharsetEncoder.RESULT_OVERFLOW)
			{
				output = allocateMore(output);
				continue;
			}
			checkCoderResult(result);
		}
		result = encode(in, output, true);
		checkCoderResult(result);
		
		while(true)
		{
			result = flush(output);
			if (result == CharsetEncoder.RESULT_UNDERFLOW)
			{
				output.flip();
				break;
			}
			else if (result == CharsetEncoder.RESULT_OVERFLOW)
			{
				output = allocateMore(output);
				continue;
			}
			checkCoderResult(result);
			output.flip();
			switch(result >>> CharsetEncoder.RESULT_OFFSET)
			{
				case CharsetEncoder.RESULT_IN_MALFORMED:
					throw new MalformedInputException(CharsetEncoder.resultLength(result));
				case CharsetEncoder.RESULT_UNMAPPABLE:
					throw new UnmappableCharacterException(CharsetEncoder.resultLength(result));
			}
			break;
		}
		status = FLUSH;
		return output;
	}
	
	/**
	 * checks the result whether it needs to throw CharacterCodingException.
	 */
	private void checkCoderResult(int result) throws CharacterCodingException
	{
		int type = result >>> CharsetEncoder.RESULT_OFFSET;
		if (this.malformAction == CharsetEncoder.ERROR_ACTION_REPORT && type == CharsetEncoder.RESULT_IN_MALFORMED)
		{
			throw new MalformedInputException(CharsetEncoder.resultLength(result));
		}
		else if (this.unmapAction == CharsetEncoder.ERROR_ACTION_REPORT && type == CharsetEncoder.RESULT_IN_UNMAPPABLE)
		{
			throw new UnmappableCharacterException(CharsetEncoder.resultLength(result));
		}
	}
	
	// allocate more spaces to the given ByteBuffer
	private ByteBuffer allocateMore(ByteBuffer output)
	{
		 if(output.capacity() == 0)
		 {
			 return ByteBuffer.allocateDirect(1);
		 }
		 ByteBuffer result = ByteBuffer.allocateDirect(output.capacity() * 2);
		 output.flip();
		 result.put(output);
		 return result;
	}
	
	/**
	 * Encodes characters starting at the current position of the given input buffer, and writes the equivalent byte sequence into the given output buffer from its current 
	 * position.
	 * <p>
	 * The buffers' position will be changed with the reading and writing operation, but their limits and marks will be kept intact.
	 * <p>
	 * A <code>CoderResult</code> instance will be returned according to following rules:
	 * <ul>
	 * <li>A {@link CoderResult#malformedForLength(int) malformed input} result indicates that some malformed input error was encountered, and the erroneous characters 
	 * start at the input buffer's position and their number can be got by result's {@link CoderResult#length() length}. This kind of result can be returned only if the 
	 * malformed action is {@link CharsetEncoder#ERROR_ACTION_REPORT CharsetEncoder.ERROR_ACTION_REPORT}.</li>
	 * <li>{@link CharsetEncoder#RESULT_IN_UNDERFLOW CharsetEncoder.RESULT_IN_UNDERFLOW} indicates that as many characters as possible in the input buffer have been 
	 * encoded. If there is no further input and no characters left in the input buffer then this task is complete. If this is not the case then the client should call 
	 * this method again supplying some more input characters.</li>
	 * <li>{@link CharsetEncoder#RESULT_OVERFLOW CharsetEncoder.RESULT_OVERFLOW} indicates that the output buffer has been filled, while there are still some characters 
	 * remaining in the input buffer. This method should be invoked again with a non-full output buffer.</li>
	 * <li>A {@link CharsetEncoder#resultUnmappableForLength(int) unmappable character} result indicates that some unmappable character error was encountered, and the 
	 * erroneous characters start at the input buffer's position and their number can be got by result's {@link CharsetEncoder#resultLength(int) length}. This kind of result 
	 * can be returned only on {@link CharsetEncoder#ERROR_ACTION_REPORT CharsetEncoder.ERROR_ACTION_REPORT}.</li>
	 * </ul>
	 * <p>
	 * The <code>endOfInput</code> parameter indicates if the invoker can provider further input. This parameter is true if and only if the characters in the current input 
	 * buffer are all inputs for this encoding operation. Note that it is common and won't cause an error if the invoker sets false and then has no more input available, 
	 * while it may cause an error if the invoker always sets true in several consecutive invocations. This would make the remaining input to be treated as malformed input.
	 * <p>
	 * This method invokes the {@link #encodeLoop(ShortBuffer, ByteBuffer) encodeLoop} method to implement the basic encode logic for a specific charset.
	 * 
	 * @param in the input buffer.
	 * @param out the output buffer.
	 * @param endOfInput true if all the input characters have been provided.
	 * @return a <code>RESULT_</code> indicating the result.
	 * @throws IllegalStateException if the encoding operation has already started or no more input is needed in this encoding process.
	 * @throws CoderMalfunctionError If the {@link #encodeLoop(ShortBuffer, ByteBuffer) encodeLoop} method threw an <code>BufferUnderflowException</code> or 
	 * <code>BufferUnderflowException</code>.
	 */
	public final int encode(ShortBuffer in, ByteBuffer out, boolean endOfInput)
	{
		if ((status == FLUSH) || (!endOfInput && status == END))
		{
			throw new IllegalStateException();
		}
		
		int result;
		while (true)
		{
			try
			{
				result = encodeLoop(in, out);
			}
			catch (BufferOverflowException e)
			{
				throw new CoderMalfunctionError(e);
			}
			catch (BufferUnderflowException e)
			{
				throw new CoderMalfunctionError(e);
			}
			if(result == CharsetEncoder.RESULT_UNDERFLOW)
			{
				status = endOfInput ? END : ONGOING;
				if (endOfInput)
				{
					int remaining = in.remaining();
					if (remaining > 0)
					{
						result = CharsetEncoder.resultMalformedForLength(remaining);
					}
					else
					{
						return result;
					}
				}
				else
				{
					return result;
				}
			}
			else if
			(result == CharsetEncoder.RESULT_OVERFLOW)
			{
				status = endOfInput ? END : ONGOING;
				return result;
			}
			int action = malformAction;
			if ((result >>> CharsetEncoder.RESULT_OFFSET) == CharsetEncoder.RESULT_IN_UNMAPPABLE)
			{
				action = unmapAction;
			}
			// If the action is IGNORE or REPLACE, we should continue encoding.
			if (action == CharsetEncoder.ERROR_ACTION_REPLACE)
			{
				if (out.remaining() < replace.length)
				{
					return CharsetEncoder.RESULT_OVERFLOW;
				}
				out.put(replace);
			}
			else
			{
				if (action != CharsetEncoder.ERROR_ACTION_IGNORE)
				{
					return result;
				}
			}
			in.position(in.position() + CharsetEncoder.resultLength(result));
		}
	}
	
	/**
	 * Encodes characters into bytes. This method is called by {@link #encode(ShortBuffer, ByteBuffer, boolean) encode}.
	 * <p>
	 * This method will implement the essential encoding operation, and it won't stop encoding until either all the input characters are read, the output buffer is filled, 
	 * or some exception is encountered. Then it will return a <code>RESULT_</code> value indicating the result of the current encoding operation. The rule to 
	 * construct the <code>RESULT_</code> is the same as for {@link #encode(ShortBuffer, ByteBuffer, boolean) encode}. When an exception is encountered in the encoding 
	 * operation, most implementations of this method will return a relevant result object to the {@link #encode(ShortBuffer, ByteBuffer, boolean) encode} method, and some 
	 * performance optimized implementation may handle the exception and implement the error action itself.
	 * <p>
	 * The buffers are scanned from their current positions, and their positions will be modified accordingly, while their marks and limits will be intact. At most 
	 * {@link ShortBuffer#remaining() in.remaining()} characters will be read, and {@link ByteBuffer#remaining() out.remaining()} bytes will be written.
	 * <p>
	 * Note that some implementations may pre-scan the input buffer and return <code>CharsetEncoder.RESULT_UNDERFLOW</code> until it receives sufficient input.
	 * <p>
	 * @param in the input buffer.
	 * @param out the output buffer.
	 * @return a <code>RESULT_</code> indicating the result.
	 */
	protected abstract int encodeLoop(ShortBuffer in, ByteBuffer out);
	
	/**
	 * Flushes this encoder.
	 * <p>
	 * This method will call {@link #implFlush(ByteBuffer) implFlush}. Some encoders may need to write some bytes to the output buffer when they have read all input 
	 * characters, subclasses can overridden {@link #implFlush(ByteBuffer) implFlush} to perform writing action.
	 * <p>
	 * The maximum number of written bytes won't larger than {@link ByteBuffer#remaining() out.remaining()}. If some encoder wants to write more bytes than the output 
	 * buffer's available remaining space, then <code>CharsetEncoder.RESULT_OVERFLOW</code> will be returned, and this method must be called again with a byte buffer that 
	 * has free space. Otherwise this method will return <code>CharsetEncoder.RESULT_UNDERFLOW</code>, which means one encoding process has been completed successfully.
	 * <p>
	 * During the flush, the output buffer's position will be changed accordingly, while its mark and limit will be intact.
	 * 
	 * @param out the given output buffer.
	 * @return <code>CharsetEncoder.RESULT_UNDERFLOW</code> or <code>CharsetEncoder.RESULT_OVERRFLOW</code>.
	 *  @throws IllegalStateException  if this encoder hasn't read all input characters during one encoding process, which means neither after calling 
	 *  {@link #encode(ShortBuffer) encode(ShortBuffer)} nor after  calling {@link #encode(ShortBuffer, ByteBuffer, boolean) encode(ShortBuffer, ByteBuffer, boolean)} 
	 *  with {@code true} for the last boolean parameter.
	 */
	public final int flush(ByteBuffer out)
	{
		if(status != END && status != INIT)
		{
			throw new IllegalStateException();
		}
		int result = implFlush(out);
		if((result >>> CharsetEncoder.RESULT_OFFSET) == CharsetEncoder.RESULT_IN_UNDERFLOW)
		{
			status = FLUSH;
		}
		return result;
	}
	
	/**
	 * Flushes this encoder. The default implementation does nothing and always returns <code>CoderResult.UNDERFLOW</code>; this method can be overridden if needed.
	 * 
	 * @param out the output buffer.
	 * @return <code>CharsetEncoder.RESULT_UNDERFLOW</code> or <code>CharsetEncoder.RESULT_OVERFLOW</code>.
	 */
	protected int implFlush(ByteBuffer out)
	{
		return CharsetEncoder.RESULT_UNDERFLOW;
	}
	
	/**
	 * Notifies that this encoder's <code>CodingErrorAction</code> specified for malformed input error has been changed. The default implementation does nothing; this 
	 * method can be overridden if needed.
	 * 
	 * @param newAction the new action.
	 */
	protected void implOnMalformedInput(int newAction)
	{
		// default implementation is empty
	}
	
	/**
     * Notifies that this encoder's <code>CodingErrorAction</code> specified for unmappable character error has been changed. The default implementation does nothing; 
     * this method can be overridden if needed.
     * 
     * @param newAction the new action. One of the <code>ERROR_ACTION_</code> options.
     */
    protected void implOnUnmappableCharacter(int newAction)
    {
    	// default implementation is empty
    }
	
	/**
	 * Notifies that this encoder's replacement has been changed. The default
	 * implementation does nothing; this method can be overridden if needed.
	 * 
	 * @param newReplacement the new replacement string.
	 */
	protected void implReplaceWith(byte[] newReplacement)
	{
		// default implementation is empty
	}
	
	/**
	 * Resets this encoder's charset related state. The default implementation does nothing; this method can be overridden if needed.
	 */
	protected void implReset()
	{
		// default implementation is empty
	}
	
	/**
	 * Checks if the given argument is legal as this encoder's replacement byte array.
	 * 
	 * The given byte array is legal if and only if it can be decode into sixteen bits Unicode characters.
	 * 
	 * This method can be overridden for performance improvement.
	 * 
	 * @param repl the given byte array to be checked.
	 * @return true if the the given argument is legal as this encoder's replacement byte array.
	 */
	public boolean isLegalReplacement(byte[] repl)
	{
		//The classes that use this (Identity8BitCharsetEncoder, PDFDocCharsetEncoder) both override this so it doesn't do replacement checks. Since this unnecessary
		//there is no need to implement it.
		return true;
	}
	
	/**
	 *  Gets this encoder's <code>CodingErrorAction</code> when a malformed input error occurred during the encoding process.
	 *  
	 *  @return this encoder's <code>CodingErrorAction</code> when a malformed input error occurred during the encoding process.
	 */
	public int malformedInputAction()
	{
		return this.malformAction;
	}
	
	/**
	 * Gets the maximum number of bytes which can be created by this encoder for one input character, must be positive.
	 * 
	 * @return the maximum number of bytes which can be created by this encoder for one input character, must be positive and in Fixed32 format.
	 */
	public final int maxBytesPerChar()
	{
		return this.maxBytes;
	}
	
	/**
	 * Sets this encoder's action on malformed input error.
	 * 
	 * This method will call the {@link #implOnMalformedInput(int) implOnMalformedInput} method with the given new action as argument.
	 * 
	 * @param newAction the new action on malformed input error.
	 * @return this encoder.
	 * @throws IllegalArgumentException if the given newAction is invalid.
	 */
	public final CharsetEncoder onMalformedInput(int newAction)
	{
		if(newAction < CharsetEncoder.ERROR_ACTION_IGNORE || newAction > CharsetEncoder.ERROR_ACTION_REPORT)
		{
			throw new IllegalArgumentException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getString(ResourcesResource.HELPER_CHAR_ENC_MALFORM_INVALID_ACTION));
		}
		this.malformAction = newAction;
		implOnMalformedInput(newAction);
		return this;
	}
	
	/**
	 * Sets this encoder's action on unmappable character error.
	 * 
	 * This method will call the {@link #implOnUnmappableCharacter(CodingErrorAction) implOnUnmappableCharacter} method with the given new action as argument.
	 * 
	 * @param newAction the new action on unmappable character error. One of the <code>ERROR_ACTION_</code> options.
	 * @return this encoder.
	 * @throws IllegalArgumentException if the given newAction is invalid.
	 */
	public final CharsetEncoder onUnmappableCharacter(int newAction)
	{
		if (newAction < ERROR_ACTION_IGNORE || newAction > ERROR_ACTION_REPLACE)
		{
			throw new IllegalArgumentException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getString(ResourcesResource.HELPER_CHAR_ENC_UNMAP_INVALID_ACTION));
		}
		unmapAction = newAction;
		implOnUnmappableCharacter(newAction);
		return this;
	}
	
	/**
	 * Gets the replacement byte array, which is never null or empty.
	 * 
	 * @return the replacement byte array, cannot be null or empty.
	 */
	public final byte[] replacement()
	{
		return this.replace;
	}
	
	/**
	 * Sets the new replacement value.
	 * 
	 * This method first checks the given replacement's validity, then changes the replacement value and finally calls the {@link #implReplaceWith(byte[]) implReplaceWith} 
	 * method with the given new replacement as argument.
	 * 
	 * @param replacement the replacement byte array, cannot be null or empty, its length cannot be larger than <code>maxBytesPerChar</code>, and it must be legal 
	 * replacement, which can be justified by calling <code>isLegalReplacement(byte[] repl)</code>.
	 * @return this encoder.
	 * @throws IllegalArgumentException if the given replacement cannot satisfy the requirement mentioned above.
	 */
	public final CharsetEncoder replaceWith(byte[] replacement)
	{
		if (null == replacement || 0 == replacement.length || maxBytes < Fixed32.toFP(replacement.length) || !isLegalReplacement(replacement))
		{
			throw new IllegalArgumentException(ResourceManager.getResource(ResourceManager.LOCALIZATION).getString(ResourcesResource.HELPER_CHAR_ENC_ILLEGAL_REPLACEMENT));
		}
		replace = replacement;
		implReplaceWith(replacement);
		return this;
	}
	
	/**
	 * Resets this encoder. This method will reset the internal status and then calls <code>implReset()</code> to reset any status related to the specific charset.
	 * 
	 * @return this encoder.
	 */
	public final CharsetEncoder reset()
	{
		status = INIT;
		implReset();
		return this;
	}
	
	/**
	 * Gets this encoder's <code>CodingErrorAction</code> when unmappable character occurred during encoding process.
	 * 
	 * @return this encoder's <code>ERROR_ACTION_</code> when unmappable character occurred during encoding process.
	 */
	public int unmappableCharacterAction()
	{
		return this.unmapAction;
	}
}
