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

package pixy.image.png;

import java.util.HashMap;
import java.util.Map;

/**
 * Define PNG image color types
 * 
 * @author Wen Yu, yuwen_66@yahoo.com 
 * @version 1.0 07/29/2013
 */
public enum ColorType {	
	// Image color formats
	GRAY_SCALE(0, "Gray-scale: each pixel is a grayscale sample."),
	TRUE_COLOR(2, "True-color: each pixel is a R,G,B triple."),
	INDEX_COLOR(3, "Index-color: each pixel is a palette index; a PLTE chunk must appear."), 
	GRAY_SCALE_WITH_ALPHA(4, "Gray-scale-with-alpha: each pixel is a grayscale sample, followed by an alpha sample."), 
	TRUE_COLOR_WITH_ALPHA(6, "True-color-with-alpha: each pixel is a R,G,B triple, followed by an alpha sample."), 
    
	UNKNOWN(999, "UNKNOWN"); // We don't know this color format
	
	private ColorType(int value, String description)
    {
    	this.value = value;
        this.description = description;	
    }    
    
    public String getDescription()
    {
    	return this.description;
    }
    
    public int getValue()
    {
    	return this.value;
    }
      
    @Override
    public String toString() {return "Image color format: " + getValue() + " - " + description;}
    
    public static ColorType fromInt(int value) {
       	ColorType colorType = intMap.get(value);
    	if (colorType == null)
    	   return UNKNOWN;
   		return colorType;
    }
    
    private static final Map<Integer, ColorType> intMap = new HashMap<Integer, ColorType>();
    
    static
    {
      for(ColorType color : values()) {
          intMap.put(color.getValue(), color);
      }
    }   
    
    private final String description;
    private final int value;
}
