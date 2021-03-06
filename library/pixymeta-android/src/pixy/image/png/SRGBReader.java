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
 * PNG sRGB chunk reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/16/2013
 */
public class SRGBReader implements Reader {

	private Chunk chunk;
	private byte renderingIntent;
	
	public SRGBReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		if (chunk.getChunkType() != ChunkType.SRGB) {
			throw new IllegalArgumentException("Not a valid sRGB chunk.");
		}
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("SRGBReader: error reading chunk");
		}
	}
	
	/**
	 * sRGB rendering intent:
	 * <p>
	 * 0 - Perceptual:
	 * for images preferring good adaptation to the output device gamut at the expense of
	 * colorimetric accuracy, such as photographs.
	 * <p>
	 * 1 - Relative colorimetric:
	 * for images requiring colour appearance matching (relative to the output device white point),
	 * such as logos.
	 * <p>
	 * 2 - Saturation:
	 * for images preferring preservation of saturation at the expense of hue and lightness,
	 * such as charts and graphs.
	 * <p>
	 * 3 - Absolute colorimetric:
	 * for images requiring preservation of absolute colorimetry, such as previews of images destined
	 * for a different output device (proofs).
	 */
	public byte getRenderingIntent() {
		return renderingIntent;
	}

	public void read() throws IOException {
		byte[] data = chunk.getData();
		if(data.length > 0)
			renderingIntent = data[0]; 
		else renderingIntent = -1;
	}
}
