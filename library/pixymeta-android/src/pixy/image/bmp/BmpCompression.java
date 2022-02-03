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

package pixy.image.bmp;

import java.util.HashMap;
import java.util.Map;

/**
 * BMP compression type.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/09/2014
 */
public enum BmpCompression {
	//
	BI_RGB("No Compression", 0),
	BI_RLE8("8 bit RLE Compression (8 bit only)", 1),
	BI_RLE4("4 bit RLE Compression (4 bit only)", 2),
	BI_BITFIELDS("No compression (16 & 32 bit only)", 3),
	
	UNKNOWN("Unknown", 9999);
	
	private BmpCompression(String description, int value) {
		this.description = description;
		this.value = value;
	}
	
	public String getDescription() {
		return description;
	}
	
	public int getValue() {
		return value;
	}
	
	@Override
    public String toString() {
		return description;
	}
	
	public static BmpCompression fromInt(int value) {
       	BmpCompression compression = typeMap.get(value);
    	if (compression == null)
    	   return UNKNOWN;
      	return compression;
    }
    
    private static final Map<Integer, BmpCompression> typeMap = new HashMap<Integer, BmpCompression>();
       
    static
    {
      for(BmpCompression compression : values())
    	  typeMap.put(compression.getValue(), compression);
    } 

	private String description;
	private int value;
}
