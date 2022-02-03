/*
 * Copyright (c) 2014-2021 by Wen Yu
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */

package pixy.image.png;

import java.io.IOException;
import java.io.OutputStream;

import pixy.util.zip.CRC32;
import pixy.io.IOUtils;
import pixy.util.ArrayUtils;
import pixy.util.LangUtils;

/**
 * Class for PNG chunks
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 11/07/2012
 */
public class Chunk implements Comparable<Chunk> {

	private final long length;
	private final ChunkType chunkType;
	private final byte[] data;
	private final long crc;	
	
	/**
	 * Compare different chunks according to their Attribute ranking.
	 *  
	 * This is intended to be used for comparing chunks with different
	 * chunk types rather than chunks of the same chunkType which will always 
	 * have the same ranking.
	 */
	public int compareTo(Chunk that) {
    	return this.chunkType.getRanking() - that.chunkType.getRanking();
    }
	
	public Chunk(ChunkType chunkType, long length, byte[] data, long crc) {
		this.length = length;
		this.chunkType = chunkType;
		this.data = data;
		this.crc = crc;
	}
	
	public ChunkType getChunkType() {
		return chunkType;
	}	
	
	public long getLength() {
		return this.length;
	}
	
	public byte[] getData() {
		return this.data.clone();
	}	
	
	public long getCRC() {
		return this.crc;
	}
	
	public boolean isValidCRC() {
		return (calculateCRC(chunkType.getValue(), data) == crc);
	}
	
	public void write(OutputStream os) throws IOException {
		IOUtils.writeIntMM(os, (int)length);
		IOUtils.writeIntMM(os, chunkType.getValue());
		IOUtils.write(os, data);
		IOUtils.writeIntMM(os, (int)crc);		
	}
	
	@Override public String toString() {
		return this.chunkType.toString();
	}
	
	public boolean equals(Object that) {
		
		if (! (that instanceof Chunk)) {
			return false;
		}
		
		Chunk other = (Chunk)that;
		
		long thisCRC = calculateCRC(this.getChunkType().getValue(), this.getData());
		long otherCRC = calculateCRC(other.getChunkType().getValue(), other.getData());

		return thisCRC == otherCRC;
	}
	
	public int hashCode() {
		return LangUtils.longToIntHashCode(calculateCRC(this.getChunkType().getValue(), this.getData()));
	}
	
	public static long calculateCRC(int chunkValue, byte[] data)
	{
		CRC32 crc32 = new CRC32();
		 
		crc32.update(ArrayUtils.toByteArrayMM(chunkValue));
		crc32.update(data);
		 
		return crc32.getValue();
	}
	
	public static long calculateCRC(int chunkValue, byte[] data, int offset, int length)
	{
		CRC32 crc32 = new CRC32();
		 
		crc32.update(ArrayUtils.toByteArrayMM(chunkValue));
		crc32.update(data, offset, length);
		 
		return crc32.getValue();
	}
}
