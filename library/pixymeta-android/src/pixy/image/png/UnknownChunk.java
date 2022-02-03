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

import pixy.io.IOUtils;

/**
 * Special chunk to handle PNG ChunkType.UNKNOWN.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/30/2012
 */
public class UnknownChunk extends Chunk {

	private final int chunkValue;
	
	public UnknownChunk(long length, int chunkValue, byte[] data, long crc) {
		super(ChunkType.UNKNOWN,length, data, crc);
		this.chunkValue = chunkValue;
	}
	
	public int getChunkValue(){
		return chunkValue;
	}
	
	@Override public boolean isValidCRC() {				 
		return (calculateCRC(chunkValue, getData()) == getCRC());
	}
	
	@Override public void write(OutputStream os) throws IOException{
		IOUtils.writeIntMM(os, (int)getLength());
		IOUtils.writeIntMM(os, this.chunkValue);
		IOUtils.write(os, getData());
		IOUtils.writeIntMM(os, (int)getCRC());
	}
	
	@Override public String toString() {
		return super.toString() + "[Chunk type value: 0x"+ Integer.toHexString(chunkValue)+"]";
	}	
}
