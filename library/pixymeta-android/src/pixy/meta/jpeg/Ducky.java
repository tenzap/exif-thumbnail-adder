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
 * Ducky.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package pixy.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.io.IOUtils;
import pixy.meta.Metadata;
import pixy.meta.MetadataEntry;
import pixy.meta.MetadataType;

public class Ducky extends Metadata {

	private Map<DuckyTag, DuckyDataSet> datasetMap;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Ducky.class);
		
	public Ducky() {
		super(MetadataType.JPG_DUCKY);
		datasetMap =  new EnumMap<DuckyTag, DuckyDataSet>(DuckyTag.class);
		isDataRead = true;
	}
	
	public Ducky(byte[] data) {
		super(MetadataType.JPG_DUCKY, data);
	}
	
	public void addDataSet(DuckyDataSet dataSet) {
		if(datasetMap != null) {
			datasetMap.put(DuckyTag.fromTag(dataSet.getTag()), dataSet);				
		}
	}
	
	public void addDataSets(Collection<? extends DuckyDataSet> dataSets) {
		if(datasetMap != null) {
			for(DuckyDataSet dataSet: dataSets) {
				datasetMap.put(DuckyTag.fromTag(dataSet.getTag()), dataSet);				
			}
		}
	}
	
	public Map<DuckyTag, DuckyDataSet> getDataSets() {
		ensureDataRead();
		return Collections.unmodifiableMap(datasetMap);
	}
	
	public Iterator<MetadataEntry> iterator() {
		ensureDataRead();
		
		List<MetadataEntry> entries = new ArrayList<MetadataEntry>();
			
		for(DuckyDataSet dataset : datasetMap.values()) {
			entries.add(dataset.getMetadataEntry());
		}
		
		return Collections.unmodifiableCollection(entries).iterator();
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			datasetMap = new EnumMap<DuckyTag, DuckyDataSet>(DuckyTag.class);
			
			for(;;) {
				if(i + 4 > data.length) break;
				int tag = IOUtils.readUnsignedShortMM(data, i);
				i += 2;
				int size = IOUtils.readUnsignedShortMM(data, i);
				i += 2;
				DuckyTag etag = DuckyTag.fromTag(tag);
				datasetMap.put(etag, new DuckyDataSet(tag, size, data, i));
				i += size;
			}
			
		    isDataRead = true;
		}
	}

	public void showMetadata() {
		ensureDataRead();
		LOGGER.info("JPEG Ducky output starts =>");
		// Print DuckyDataSet
		for(DuckyDataSet dataset : datasetMap.values()) {
			dataset.print();
		}
		LOGGER.info("<= JPEG Ducky output ends");
	}
	
	public void write(OutputStream os) throws IOException {
		ensureDataRead();
		for(DuckyDataSet dataset : getDataSets().values())
			dataset.write(os);
	}
}
