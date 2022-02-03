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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.util.Reader;

/**
 * PNG IEND chunk reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/29/2013
 */
public class IENDReader implements Reader {

	private Chunk chunk;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(IENDReader.class);
	
	public IENDReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		if (chunk.getChunkType() != ChunkType.IEND) {
			throw new IllegalArgumentException("Not a valid IEND chunk.");
		}
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("IENDReader: error reading chunk");
		}
	}

	public void read() throws IOException {
		if(chunk.getData().length != 0) {
			LOGGER.warn("Warning: IEND data field is not empty!");
		}
	}
}
