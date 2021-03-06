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
 * ImageMetadata.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    13Mar2015  Initial creation
*/

package pixy.meta.image;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import pixy.meta.Metadata;
import pixy.meta.MetadataEntry;
import pixy.meta.MetadataType;
import pixy.meta.Thumbnail;

public class ImageMetadata extends Metadata {
	private Map<String, Thumbnail> thumbnails;
	private Collection<MetadataEntry> entries = new ArrayList<MetadataEntry>();

	public ImageMetadata() {
		super(MetadataType.IMAGE);
	}
	
	public ImageMetadata(Map<String, Thumbnail> thumbnails) {
		super(MetadataType.IMAGE);
		this.thumbnails = thumbnails;
	}
	
	public void addMetadataEntry(MetadataEntry entry) {
		entries.add(entry);
	}
	
	public void addMetadataEntries(Collection<MetadataEntry> entries) {
		entries.addAll(entries);
	}
	
	public boolean containsThumbnail() {
		return thumbnails != null && thumbnails.size() > 0;
	}
	
	public Map<String, Thumbnail> getThumbnails() {
		return thumbnails;
	}
	
	public Iterator<MetadataEntry> iterator() {
		if(containsThumbnail()) { // We have thumbnail
			Iterator<Map.Entry<String, Thumbnail>> mapEntries = thumbnails.entrySet().iterator();
			entries.add(new MetadataEntry("Total number of thumbnails", "" + thumbnails.size()));
			int i = 0;
			while (mapEntries.hasNext()) {
			    Map.Entry<String, Thumbnail> entry = mapEntries.next();
			    MetadataEntry e = new MetadataEntry("Thumbnail " + i, entry.getKey(), true);
			    Thumbnail thumbnail = entry.getValue();
			    e.addEntry(new MetadataEntry("Thumbnail width", ((thumbnail.getWidth() < 0)? " Unavailable": ""+ thumbnail.getWidth())));
				e.addEntry(new MetadataEntry("Thumbnail height", ((thumbnail.getHeight() < 0)? " Unavailable": "" + thumbnail.getHeight())));
				e.addEntry(new MetadataEntry("Thumbnail data type", thumbnail.getDataTypeAsString()));
				entries.add(e);
				i++;
			}
		}		
		return Collections.unmodifiableCollection(entries).iterator();
	}
	
	public void read() throws IOException {
		if(!isDataRead)
			// No implementation
			isDataRead = true;
	}	
}
