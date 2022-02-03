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
 * PNG PLTE chunk reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/26/2013
 */
public class PLTEReader implements Reader {

	private byte[] redMap;
	private byte[] greenMap;
	private byte[] blueMap;
	private Chunk chunk;
	
	public PLTEReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		if (chunk.getChunkType() != ChunkType.PLTE) {
			throw new IllegalArgumentException("Not a valid PLTE chunk.");
		}
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("PLTEReader: error reading chunk");
		}
	}
	
	public byte[] getRedMap() { return redMap; }
	public byte[] getGreenMap() { return greenMap; }
	public byte[] getBlueMap() { return blueMap; }
	
	public void read() throws IOException {	
		
		byte[] colorMap = chunk.getData();
		int mapLen = colorMap.length;
		
		if ((mapLen % 3) != 0) {
			throw new IllegalArgumentException("Invalid colorMap length: " + mapLen);
		}
		
		redMap = new byte[mapLen/3];
		greenMap = new byte[mapLen/3];
		blueMap = new byte[mapLen/3];
		
		for (int i = mapLen - 1, j = redMap.length - 1; j >= 0; j--) {
			blueMap[j]  = colorMap[i--];
			greenMap[j] = colorMap[i--];
			redMap[j] 	= colorMap[i--];			
		}		
	}
}
