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

import pixy.meta.iptc.IPTCEnvelopeTag;
import pixy.meta.iptc.IPTCTag;
import pixy.string.StringUtils;

/**
 * Defines DataSet tags for IPTC Envelope Record - Record number 1.
 * * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public enum IPTCEnvelopeTag implements IPTCTag {
	 RECORD_VERSION(0, "EnvelopeRecordVersion"),
	 DESTINATION(5, "Destination") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 FILE_FORMAT(20, "FileFormat"),
	 FILE_VERSION(22, "FileVersion"),
	 SERVICE_IDENTIFIER(30, "ServiceIdentifier"),
	 ENVELOPE_NUMBER(40, "EnvelopeNumber"),
	 PRODUCT_ID(50, "ProductID") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 ENVELOPE_PRIORITY(60, "EnvelopePriority"),
	 DATE_SENT(70, "DateSent"),
	 TIME_SENT(80, "TimeSent"),
	 CODED_CHARACTER_SET(90, "CodedCharacterSet"),
	 UNIQUE_OBJECT_NAME(100, "UniqueObjectName"),
	 ARM_IDENTIFIER(120, "ARMIdentifier"),
	 ARM_VERSION(122, "ARMVersion"),
	 	 
	 UNKNOWN(999, "Unknown");
	 
	 private IPTCEnvelopeTag(int tag, String name) {
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
		 return IPTCRecord.ENVELOP.getRecordNumber();
	 }
	 
	 public int getTag() { return tag; }
	 
	 public static IPTCEnvelopeTag fromTag(int value) {
      	IPTCEnvelopeTag record = recordMap.get(value);
	   	if (record == null)
	   		return UNKNOWN;
   		return record;
	 }
  
	 @Override public String toString() {
	   return name;
	 }
  
	 private static final Map<Integer, IPTCEnvelopeTag> recordMap = new HashMap<Integer, IPTCEnvelopeTag>();
   
	 static
	 {
		 for(IPTCEnvelopeTag record : values()) {
			 recordMap.put(record.getTag(), record);
		 }
	 }	    
 
	 private final int tag;
	 private final String name;
}
