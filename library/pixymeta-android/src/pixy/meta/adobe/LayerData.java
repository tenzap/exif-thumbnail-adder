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
 *
 * Change History - most recent changes go on top of previous changes
 *
 * LayerData.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    27Jul2015  initial creation
 */

package pixy.meta.adobe;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.io.ReadStrategy;

public class LayerData extends DDBEntry {
	private int layerCount;
	private List<Channel> channels = new ArrayList<Channel>();
		
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(LayerData.class);
	
	public LayerData(int size, byte[] data, ReadStrategy readStrategy) {
		super(DataBlockType.Layr, size, data, readStrategy);
		read();
	}

	public void print() {
		super.print();
		LOGGER.info("Number of layers: {}", layerCount);
	}
	
	@SuppressWarnings("unused")
	private void read() {
		int i = 0;
		layerCount = readStrategy.readUnsignedShort(data, i);
		i += 2;
		for(int j = 0; j < layerCount; j++) { // For each layer
			int topCoord = readStrategy.readInt(data, i);
			i += 4;
			int leftCoord = readStrategy.readInt(data, i);
			i += 4;
			int bottomCoord = readStrategy.readInt(data, i);
			i += 4;
			int rightCoord = readStrategy.readInt(data, i);
			i += 4;
			int channelCount = readStrategy.readUnsignedShort(data, i);
			i += 2;
			for(int k = 0; k < channelCount; k++) {
				int id = readStrategy.readShort(data, i);
				i += 2;
				int len = readStrategy.readInt(data, i);
				i += 4;
				channels.add(new Channel(id, len));
			}
			int blendModeSignature = readStrategy.readInt(data, i);
			i += 4;
			int blendMode = readStrategy.readInt(data, i);
			i += 4;
			int opacity = data[i++]&0xff;
			int clipping = data[i++]&0xff;
			int flags = data[i++]&0xff;
			int filler = data[i++]&0xff;
			int extraLen = readStrategy.readInt(data, i);
			i += 4;
			i += extraLen; // Skip the extra data for now
			// TODO: read the following structure:
			//Layer mask data
			//Layer blending ranges
			//Layer name: Pascal string, padded to a multiple of 4 bytes
			//Additional layer information (optional)
		}	
	}
}
