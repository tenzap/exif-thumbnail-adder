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

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import pixy.meta.iptc.IPTCPreObjectDataTag;
import pixy.meta.iptc.IPTCTag;
import pixy.string.StringUtils;

/**
 * Defines DataSet tags for IPTC PreObjectData Record - Record number 7.
 * * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public enum IPTCPreObjectDataTag implements IPTCTag {
	 SIZE_MODE(10, "SizeMode"),
	 MAX_SUBFILE_SIZE(20, "MaxSubfileSize"),
	 OBJECT_SIZE_ANNOUNCED(90, "ObjectSizeAnnounced"),
	 MAXIMUM_OBJECT_SIZE(95, "MaximumObjectSize"),
	 	 	 
	 UNKNOWN(999, "Unknown");
	 
	 private IPTCPreObjectDataTag(int tag, String name) {
		 this.tag = tag;
		 this.name = name;
	 }
	 
	 public boolean allowMultiple() {
		 return false;
	 }
	 
	 // Default implementation. Could be replaced by individual ENUM
	 public String getDataAsString(byte[] data) {
		 try {
			 String strVal = new String(data, "UTF-8").trim();
			 if(strVal.length() > 0) return strVal;
		 } catch (UnsupportedEncodingException e) {
			 e.printStackTrace();
		 }
		 // Hex representation of the data
		 return StringUtils.byteArrayToHexString(data, 0, IPTCTag.MAX_STRING_REPR_LEN);
	 }
	 
	 public String getName() {
		 return name;
	 }
	 
	 public int getRecordNumber() {
	 	return IPTCRecord.PRE_OBJECTDATA.getRecordNumber();
	 }
	 
	 public int getTag() {
		 return tag;
	 }
	 
	 public static IPTCPreObjectDataTag fromTag(int value) {
		 IPTCPreObjectDataTag record = recordMap.get(value);
	   	if (record == null)
	   		return UNKNOWN;
     	return record;
	 }
  
	 @Override public String toString() {
	   return name;
	 }
  
	 private static final Map<Integer, IPTCPreObjectDataTag> recordMap = new HashMap<Integer, IPTCPreObjectDataTag>();
   
	 static
	 {
		 for(IPTCPreObjectDataTag record : values()) {
			 recordMap.put(record.getTag(), record);
		 }
	 }	    
 
	 private final int tag;
	 private final String name;
}
