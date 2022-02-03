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

package pixy.meta.iptc;

import java.util.HashMap;
import java.util.Map;

import pixy.meta.iptc.IPTCRecord;

/**
 * Defines IPTC data set record number
 * * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public enum IPTCRecord {
	ENVELOP(1, "Envelop"), APPLICATION(2, "Application"), NEWSPHOTO(3, "NewsPhoto"),
	PRE_OBJECTDATA(7, "PreObjectData"), OBJECTDATA(8, "ObjectData"), POST_OBJECTDATA(9, "PostObjectData"),
	FOTOSTATION(240, "FotoStation"), UNKNOWN(999, "Unknown");	
	
	private IPTCRecord(int recordNumber, String name) {
		this.recordNumber = recordNumber;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public int getRecordNumber() {
		return recordNumber;
	}
	
	@Override public String toString() {
		return name;
	}
	
	public static IPTCRecord fromRecordNumber(int value) {
      	IPTCRecord record = recordMap.get(value);
	   	if (record == null)
	   		return UNKNOWN;
   		return record;
	}
	
	private static final Map<Integer, IPTCRecord> recordMap = new HashMap<Integer, IPTCRecord>();
	   
	static
	{
		for(IPTCRecord record : values()) {
			recordMap.put(record.getRecordNumber(), record);
		}
	}	    
 	
	private final int recordNumber;
	private final String name;
}
