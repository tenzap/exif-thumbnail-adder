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

import pixy.io.IOUtils;
import pixy.util.Reader;

/**
 * PNG IHDR chunk reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/25/2013
 */
public class IHDRReader implements Reader {

	private int width = 0;
	private int height = 0;
	private byte bitDepth = 0;
	private byte colorType = 0;
	private byte compressionMethod = 0;
	private byte filterMethod = 0;
	private byte interlaceMethod = 0;
	private Chunk chunk;
	
	public IHDRReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		if (chunk.getChunkType() != ChunkType.IHDR) {
			throw new IllegalArgumentException("Not a valid IHDR chunk.");
		}
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("IHDRReader: error reading chunk");
		}
	}
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public byte getBitDepth() { return bitDepth; }
	public byte getColorType() { return colorType; }
	public byte getCompressionMethod() { return compressionMethod; }
	public byte getFilterMethod() { return filterMethod; }
	public byte getInterlaceMethod() { return interlaceMethod; }

	public void read() throws IOException {	
		//
		byte[] data = chunk.getData();
		
		this.width = IOUtils.readIntMM(data, 0);
		this.height = IOUtils.readIntMM(data, 4);
		this.bitDepth = data[8];
		this.colorType = data[9];
		this.compressionMethod = data[10];
		this.filterMethod = data[11];
		this.interlaceMethod = data[12];
	}
}
