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

import pixy.util.Reader;

/**
 * Special chunk reader for UnknownChunk.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/01/2013
 */
public class UnknownChunkReader implements Reader {

	private int chunkValue;
	private byte[] data;
	private Chunk chunk;
		
	public UnknownChunkReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("UnknownChunkReader: error reading chunk");
		}
	}
	
	public int getChunkValue() {
		return this.chunkValue;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void read() throws IOException {       
   		if (chunk instanceof UnknownChunk) {
   			UnknownChunk unknownChunk = (UnknownChunk)chunk;
   			this.chunkValue = unknownChunk.getChunkValue();
   			this.data = unknownChunk.getData();
   		} else
   		    throw new IllegalArgumentException("Expect UnknownChunk.");
     }
}
