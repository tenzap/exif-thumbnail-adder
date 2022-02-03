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
 * DDB.java - Adobe Photoshop Document Data Block
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    23Jul2015  Initial creation
 */

package pixy.meta.adobe;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.meta.Metadata;
import pixy.meta.MetadataEntry;
import pixy.meta.MetadataType;
import pixy.io.IOUtils;
import pixy.io.ReadStrategy;
import pixy.util.ArrayUtils;

public class DDB extends Metadata {
	private ReadStrategy readStrategy;
	private Map<Integer, DDBEntry> entries = new HashMap<Integer, DDBEntry>();
	// DDB unique ID
	public static final String DDB_ID = "Adobe Photoshop Document Data Block\0";
	public static final int _8BIM = 0x3842494d; // "8BIM"
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(DDB.class);
	
	public static void showDDB(byte[] data, ReadStrategy readStrategy) {
		if(data != null && data.length > 0) {
			DDB ddb = new DDB(data, readStrategy);
			try {
				ddb.read();
				ddb.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void showDDB(InputStream is, ReadStrategy readStrategy) {
		try {
			showDDB(IOUtils.inputStreamToByteArray(is), readStrategy);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public DDB(byte[] data, ReadStrategy readStrategy) {
		super(MetadataType.PHOTOSHOP_DDB, data);
		if(readStrategy == null) throw new IllegalArgumentException("Input readStategy is null");
		this.readStrategy = readStrategy;
	}
	
	public Map<Integer, DDBEntry> getEntries() {
		return Collections.unmodifiableMap(entries);
	}
	
	public Iterator<MetadataEntry> iterator() {
		ensureDataRead();
		List<MetadataEntry> entries = new ArrayList<MetadataEntry>();

		for(DDBEntry entry : this.entries.values())
			entries.add(entry.getMetadataEntry());
		
		return Collections.unmodifiableCollection(entries).iterator();
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			if(!new String(data, i, DDB_ID.length()).equals(DDB_ID)) {
				throw new RuntimeException("Invalid Photoshop Document Data Block");
			}
			i += DDB_ID.length();
			while((i+4) < data.length) {
				int signature = readStrategy.readInt(data, i);
				i += 4;
				if(signature ==_8BIM) {
					int type = readStrategy.readInt(data, i);
					i += 4;
					int size = readStrategy.readInt(data, i);
					i += 4;
					DataBlockType etype = DataBlockType.fromInt(type);
					switch(etype) {
						case Layr:
							entries.put(type, new LayerData(size, ArrayUtils.subArray(data, i, size), readStrategy));
							break;
						case LMsk:
							entries.put(type, new UserMask(size, ArrayUtils.subArray(data, i, size), readStrategy));
							break;
						case FMsk:
							entries.put(type, new FilterMask(size, ArrayUtils.subArray(data, i, size), readStrategy));
							break;
						default:
							entries.put(type, new DDBEntry(type, size, ArrayUtils.subArray(data, i, size), readStrategy));
					}
					i += ((size + 3)>>2)<<2;// Skip data with padding bytes (padded to a 4 byte offset)
				}
			}
			isDataRead = true;
		}
	}
	
	public void showMetadata() {
		ensureDataRead();
		LOGGER.info("<<Adobe DDB information starts>>");
		for(DDBEntry entry : entries.values()) {
			entry.print();
		}
		LOGGER.info("<<Adobe DDB information ends>>");
	}
}
