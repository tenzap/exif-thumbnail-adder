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

package pixy.meta.icc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.meta.icc.ICCProfile;
import pixy.meta.icc.ProfileTag;
import pixy.io.IOUtils;

/**
 * ICC Profile Tag Table
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public class ProfileTagTable {
	private int tagCount;
	private Map<Integer, TagEntry> tagEntries = new HashMap<Integer, TagEntry>();
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileTagTable.class);
		
	public static class TagEntry implements Comparable<TagEntry> {
		private int profileTag;
		private int dataOffset;
		private int dataLength;
		private byte[] data;
		
		public TagEntry(int profileTag, int dataOffset, int dataLength, byte[] data) {
			this.profileTag  = profileTag;
			this.dataOffset = dataOffset;
			this.dataLength = dataLength;
			this.data = data;
		}
		
		public int compareTo(TagEntry o) {
			return (int)((this.profileTag&0xffffffffL) - (o.profileTag&0x0ffffffffL));
		}
		
		public int getProfileTag() {
			return profileTag;
		}
		
		public int getDataOffset() {
			return dataOffset;
		}
		
		public int getDataLength() {
			return dataLength;
		}
		
		public byte[] getData() {
			return data;
		}		
	}
	
	public ProfileTagTable() {}
	
	public void addTagEntry(TagEntry tagEntry) {
		tagEntries.put(tagEntry.getProfileTag(), tagEntry);
	}
	
	public void read(byte[] data) {
		int offset = ICCProfile.TAG_TABLE_OFFSET;
		tagCount = IOUtils.readIntMM(data, offset);
		offset += 4;
		// Read each tag
		for(int i = 0; i < tagCount; i++) {
			int tagSignature = IOUtils.readIntMM(data, offset);
			offset += 4;
			ProfileTag tag = ProfileTag.fromInt(tagSignature);
			int dataOffset = IOUtils.readIntMM(data, offset);
			offset += 4;
			int dataLength = IOUtils.readIntMM(data, offset);
			offset += 4;
			
			byte[] temp = new byte[dataLength];
			System.arraycopy(data, dataOffset, temp, 0, temp.length);
			
			tagEntries.put(tagSignature, new TagEntry(tag.getValue(), dataOffset, dataLength, temp));
		}
	}
	
	public int getTagCount() {
		return tagCount;
	}
	
	public TagEntry getTagEntry(ProfileTag profileTag) {
		return tagEntries.get(profileTag.getValue());
	}
	
	public List<TagEntry> getTagEntries() {
		return new ArrayList<TagEntry>(tagEntries.values());
	}
	
	public void showTable() {
		StringBuilder profileTable = new StringBuilder();
		profileTable.append("*** Start of ICC_Profile Tag Table ***\n");
		profileTable.append("Tag Count: " + tagCount + "\n");
		
		List<TagEntry> list = getTagEntries();
		Collections.sort(list);
		int count = 0;
	
		for(TagEntry tagEntry:list) {
			profileTable.append("Tag# " + count++);
			profileTable.append(", Tag Name: " + ProfileTag.fromInt(tagEntry.getProfileTag()));
			profileTable.append(", Data Offset: " + tagEntry.getDataOffset());
			profileTable.append(", Data Length: " + tagEntry.getDataLength() + "\n");
		}
		profileTable.append("*** End of ICC_Profile Tag Table ***\n");
		
		LOGGER.info("\n{}", profileTable);
	}
}
