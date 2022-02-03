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

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

import pixy.util.Builder;

/**
 * PNG IDAT chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/26/2013
 */
public class IDATBuilder extends ChunkBuilder implements Builder<Chunk> {

	private ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
	private Deflater deflater = new Deflater(5);
		
	public IDATBuilder() {
		super(ChunkType.IDAT);		
	}
	
	public IDATBuilder(int compressionLevel) {
		this();
		deflater = new Deflater(compressionLevel);
	}
	
	public IDATBuilder data(byte[] data, int offset, int length) {
		// Caches the bytes
		bout.write(data, offset, length);
		
		return this;
	}
	
	public IDATBuilder data(byte[] data) {
		return data(data, 0, data.length);
	}

	@Override
	protected byte[] buildData() {
		// Compresses raw data
		deflater.setInput(bout.toByteArray());
		
		bout.reset();
		byte buffer[] = new byte[4096];
		
		if(finish)
			// This is to make sure we get all the input data compressed
			deflater.finish();
		
		while(!deflater.finished()) {
			int bytesCompressed = deflater.deflate(buffer);
			if(bytesCompressed <= 0) break;
			bout.write(buffer, 0, bytesCompressed);
		}		 
		
		byte temp[] = bout.toByteArray();
			
		bout.reset();
		
		return temp;
	}
	
	public void setFinish(boolean finish) {
		this.finish = finish;
	}
	
	private boolean finish;
}
