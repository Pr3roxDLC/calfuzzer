//******************************************************************************
//
// File:    SharedCharacterArrayReductionBuf_1.java
// Package: benchmarks.determinism.pj.edu.ritmp.buf
// Unit:    Class benchmarks.determinism.pj.edu.ritmp.buf.SharedCharacterArrayReductionBuf_1
//
// This Java source file is copyright (C) 2007 by Alan Kaminsky. All rights
// reserved. For further information, contact the author, Alan Kaminsky, at
// ark@cs.rit.edu.
//
// This Java source file is part of the Parallel Java Library ("PJ"). PJ is free
// software; you can redistribute it and/or modify it under the terms of the GNU
// General Public License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// PJ is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// A copy of the GNU General Public License is provided in the file gpl.txt. You
// may also obtain a copy of the GNU General Public License on the World Wide
// Web at http://www.gnu.org/licenses/gpl.html.
//
//******************************************************************************

package benchmarks.determinism.pj.edu.ritmp.buf;

import benchmarks.determinism.pj.edu.ritmp.Buf;

import benchmarks.determinism.pj.edu.ritpj.reduction.CharacterOp;
import benchmarks.determinism.pj.edu.ritpj.reduction.Op;
import benchmarks.determinism.pj.edu.ritpj.reduction.SharedCharacterArray;

import benchmarks.determinism.pj.edu.ritutil.Range;

import java.nio.ByteBuffer;

/**
 * Class SharedCharacterArrayReductionBuf_1 provides a reduction buffer for
 * class {@linkplain SharedCharacterArrayBuf_1}.
 *
 * @author  Alan Kaminsky
 * @version 26-Oct-2007
 */
class SharedCharacterArrayReductionBuf_1
	extends SharedCharacterArrayBuf_1
	{

// Hidden data members.

	CharacterOp myOp;

// Exported constructors.

	/**
	 * Construct a new shared character array reduction buffer.
	 *
	 * @param  theArray  Shared array.
	 * @param  theRange  Range of array elements to include in the buffer. The
	 *                   stride is assumed to be 1.
	 * @param  op        Binary operation.
	 *
	 * @exception  NullPointerException
	 *     (unchecked exception) Thrown if <TT>op</TT> is null.
	 */
	public SharedCharacterArrayReductionBuf_1
		(SharedCharacterArray theArray,
		 Range theRange,
		 CharacterOp op)
		{
		super (theArray, theRange);
		if (op == null)
			{
			throw new NullPointerException
				("SharedCharacterArrayReductionBuf_1(): op is null");
			}
		myOp = op;
		}

// Exported operations.

	/**
	 * Store the given item in this buffer.
	 * <P>
	 * The <TT>put()</TT> method must not block the calling thread; if it does,
	 * all message I/O in MP will be blocked.
	 *
	 * @param  i     Item index in the range 0 .. <TT>length()</TT>-1.
	 * @param  item  Item to be stored at index <TT>i</TT>.
	 */
	public void put
		(int i,
		 char item)
		{
		myArray.reduce (myArrayOffset+i, item, myOp);
		}

	/**
	 * Create a buffer for performing parallel reduction using the given binary
	 * operation. The results of the reduction are placed into this buffer.
	 *
	 * @param  op  Binary operation.
	 *
	 * @exception  ClassCastException
	 *     (unchecked exception) Thrown if this buffer's element data type and
	 *     the given binary operation's argument data type are not the same.
	 */
	public Buf getReductionBuf
		(Op op)
		{
		throw new UnsupportedOperationException();
		}

// Hidden operations.

	/**
	 * Receive as many items as possible from the given byte buffer to this
	 * buffer.
	 * <P>
	 * The <TT>receiveItems()</TT> method must not block the calling thread; if
	 * it does, all message I/O in MP will be blocked.
	 *
	 * @param  i       Index of first item to receive, in the range 0 ..
	 *                 <TT>length</TT>-1.
	 * @param  num     Maximum number of items to receive.
	 * @param  buffer  Byte buffer.
	 *
	 * @return  Number of items received.
	 */
	protected int receiveItems
		(int i,
		 int num,
		 ByteBuffer buffer)
		{
		int index = i;
		int off = myArrayOffset + i;
		int max = Math.min (i + num, myLength);
		while (index < max && buffer.remaining() >= 2)
			{
			myArray.reduce (off, buffer.getChar(), myOp);
			++ index;
			++ off;
			}
		return index - i;
		}

	}
