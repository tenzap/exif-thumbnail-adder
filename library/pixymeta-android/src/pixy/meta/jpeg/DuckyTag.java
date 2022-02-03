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
 * DuckyTag.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package pixy.meta.jpeg;

import java.util.HashMap;
import java.util.Map;

public enum DuckyTag {
	//
	QUALITY(1, "Quality"),
	COMMENT(2, "Comment"),
	COPYRIGHT(3, "Copyright"),
	 
	UNKNOWN(999, "Unknown");
	 
	private static final Map<Integer, DuckyTag> recordMap = new HashMap<Integer, DuckyTag>();
	 
	static {
		for(DuckyTag record : values()) {
			recordMap.put(record.getTag(), record);
		}
	}
	 
	public static DuckyTag fromTag(int value) {
		 DuckyTag record = recordMap.get(value);
		 if (record == null)
			 return UNKNOWN;
		 return record;
 	}
	 
	private final int tag;
   
	private final String name;
   
	private DuckyTag(int tag, String name) {
		this.tag = tag;
		this.name = name;
	}
    
	public String getName() {
		return name;
	}	    
  
	public int getTag() { 
		return tag;
	}
	 
	@Override public String toString() {
		return name;
	}	
}
