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
 * MetadataEntry.java
 */

package pixy.meta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MetadataEntry {
	
	private String key;
	private String value;
	private boolean isMetadataEntryGroup;
	
	private Collection<MetadataEntry> entries = new ArrayList<MetadataEntry>();

	public MetadataEntry(String key, String value) {
		this(key, value, false);
	}
	
	public MetadataEntry(String key, String value, boolean isMetadataEntryGroup) {
		this.key = key;
		this.value = value;
		this.isMetadataEntryGroup = isMetadataEntryGroup;
	}
	
	public void addEntry(MetadataEntry entry) {
		entries.add(entry);
	}
	
	public void addEntries(Collection<MetadataEntry> newEntries) {
		entries.addAll(newEntries);
	}
	
	public String getKey() {
		return key;
	}
	
	public boolean isMetadataEntryGroup()  {
		return isMetadataEntryGroup;
	}
	
	public Collection<MetadataEntry> getMetadataEntries() {
		return Collections.unmodifiableCollection(entries);
	}
	
	public String getValue() {
		return value;
	}
}
