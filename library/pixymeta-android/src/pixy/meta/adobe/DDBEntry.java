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
 * DDBEntry.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    24Jul2015  initial creation
 */

package pixy.meta.adobe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.io.ReadStrategy;
import pixy.meta.MetadataEntry;
import pixy.string.StringUtils;

//Building block for DDB
public class DDBEntry {
	private int type;
	private int size;
	protected byte[] data;
	protected ReadStrategy readStrategy;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(DDBEntry.class);

	public DDBEntry(DataBlockType etype, int size, byte[] data, ReadStrategy readStrategy) {
		this(etype.getValue(), size, data, readStrategy);
	}
	
	public DDBEntry(int type, int size, byte[] data, ReadStrategy readStrategy) {
		this.type = type;
		if(size < 0) throw new IllegalArgumentException("Input size is negative");
		this.size = size;
		this.data = data;
		if(readStrategy == null) throw new IllegalArgumentException("Input readStrategy is null");
		this.readStrategy = readStrategy;
	}

	public void print() {
		DataBlockType etype = getTypeEnum();
		if(etype != DataBlockType.UNKNOWN)
			LOGGER.info("Type: {} ({})", etype, etype.getDescription());
		else
			LOGGER.info("Type: Unknown (value 0x{})", Integer.toHexString(type));
		LOGGER.info("Size: {}", size);	
	}
	
	public int getType() {
		return type;
	}
	
	public DataBlockType getTypeEnum() {
		return DataBlockType.fromInt(type);
	}
	
	protected MetadataEntry getMetadataEntry() {
		//	
		DataBlockType eType  = DataBlockType.fromInt(type);
		
		if (eType == DataBlockType.UNKNOWN) {
			return new MetadataEntry("UNKNOWN [" + StringUtils.intToHexStringMM(type) + "]:", eType.getDescription());
		} else {
			return new MetadataEntry("" + eType, eType.getDescription());
		}
	}
	
	public int getSize() {
		return size;
	}
	
	public byte[] getData() {
		return data.clone();
	}
}
