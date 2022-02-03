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

import pixy.util.Builder;

/**
 * Base builder for PNG chunks.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/30/2012
 */
public abstract class ChunkBuilder implements Builder<Chunk> {

	private final ChunkType chunkType;
	
	public ChunkBuilder(ChunkType chunkType) {
		this.chunkType = chunkType;
	}
	
	protected ChunkType getChunkType() {
		return chunkType;
	}
	
	public final Chunk build() {
		byte[] data = buildData();
		
		long crc = Chunk.calculateCRC(chunkType.getValue(), data);
	    
		return new Chunk(chunkType, data.length, data, crc);
	}
	
	protected abstract byte[] buildData();
}
