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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import pixy.io.IOUtils;
import pixy.util.Reader;

/**
 * PNG IDAT chunk reader
 * <p>
 * All the IDAT chunks must be merged together before using this reader, as
 * per PNG specification, the compressed data stream is the concatenation of
 * the contents of all the IDAT chunks.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/26/2013
 */
public class IDATReader implements Reader {

	private byte[] rawData;
	private ByteArrayOutputStream byteOutput = null;
	
	public IDATReader() {
		this(8192); // 8K buffer
	}
	
	public IDATReader(int bufLen) {
		byteOutput = new ByteArrayOutputStream(bufLen);
	}
	
	public IDATReader addChunk(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");

		if (chunk.getChunkType() != ChunkType.IDAT) {
			throw new IllegalArgumentException("Not a valid IDAT chunk.");
		}		
		
		try {
			byteOutput.write(chunk.getData());
		} catch (IOException e) {
			throw new RuntimeException("IDATReader: error adding new chunk");
		}
		
		return this;
	}
	
	public byte[] getData() throws IOException {
		if(rawData == null)
			read();
		return rawData;
	}

	public void read() throws IOException {		
		// Inflate compressed data
		BufferedInputStream bin = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(byteOutput.toByteArray())));
		this.rawData = IOUtils.inputStreamToByteArray(bin);
		bin.close();
	}
}
