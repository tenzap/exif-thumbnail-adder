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

package pixy.meta.exif;

import java.util.HashMap;
import java.util.Map;

import pixy.meta.exif.InteropTag;
import pixy.image.tiff.FieldType;
import pixy.image.tiff.Tag;
import pixy.image.tiff.TiffTag;
import pixy.string.StringUtils;

/**
 * Defines Interoperability tags
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public enum InteropTag implements Tag {
	// EXIF InteropSubIFD tags
	INTEROPERABILITY_INDEX("InteroperabilityIndex", (short)0x0001) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	INTEROPERABILITY_VERSION("InteroperabilityVersion", (short)0x0002) {
		public FieldType getFieldType() {
			return FieldType.UNDEFINED;
		}
	},
	RELATED_IMAGE_FILE_FORMAT("RelatedImageFileFormat", (short)0x1000) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	RELATED_IMAGE_WIDTH("RelatedImageWidth", (short)0x1001) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	RELATED_IMAGE_LENGTH("RelatedImageLength", (short)0x1002) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	// unknown tag
	UNKNOWN("Unknown",  (short)0xffff); 
	// End of IneropSubIFD tags
		
	private InteropTag(String name, short value)
	{
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	
	public short getValue() {
		return value;
	}
	
	@Override
    public String toString() {
		if (this == UNKNOWN)
			return name;
		return name + " [Value: " + StringUtils.shortToHexStringMM(value) +"]";
	}
	
    public static Tag fromShort(short value) {
       	InteropTag tag = tagMap.get(value);
    	if (tag == null)
    	   return TiffTag.UNKNOWN;
   		return tag;
    }
    
    private static final Map<Short, InteropTag> tagMap = new HashMap<Short, InteropTag>();
       
    static
    {
      for(InteropTag tag : values()) {
           tagMap.put(tag.getValue(), tag);
      }
    }
    
    /**
     * Intended to be overridden by certain tags to provide meaningful string
     * representation of the field value such as compression, photo metric interpretation etc.
     * 
	 * @param value field value to be mapped to a string
	 * @return a string representation of the field value or empty string if no meaningful string
	 * 	representation exists.
	 */
    public String getFieldAsString(Object value) {
    	return "";
	}
    
    public boolean isCritical() {
    	return true;
    }
	
	public FieldType getFieldType() {
		return FieldType.UNKNOWN;
	}
	
	private final String name;
	private final short value;
}
