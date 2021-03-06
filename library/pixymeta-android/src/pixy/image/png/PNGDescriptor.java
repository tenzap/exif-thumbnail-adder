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

public class PNGDescriptor {
	//
	public static String getCompressionTypeDescrition(int compressionType) {
		//
		String description = "";
		
		switch(compressionType) {
			case 0:
				description = "Deflate/inflate compression with a 32K sliding window";
				break;
			default:
				description = "Invalid compression value";
				break;
		}
		 
		return description;			 
	}
	
	public static String getFilterDescription(int filter) {
		//
		String description = "";
		
		switch(filter) {
			case 0:
				description = "No filter";
				break;
			case 1:
				description = "SUB filter";
				break;
			case 2:
				description = "UP filter";
				break;
			case 3:
				description = "AVERAGE filter";
				break;
			case 4:
				description = "PAETH filter";
				break;
			default:
				description = "Invalid filter type";
				break;
		}
	
		return description;		
	}
	
	public static String getFilterTypeDescription(int filterType) {
		//
		String description = "";
		
		switch(filterType) {
			case 0:
				description = "Adaptive filtering with five basic filter types";
				break;
			default:
				description = "Invalid filter type";
				break;
		}
		 
		return description;		
	}
	
	public static String getInterlaceTypeDescription(int interlaceType) {
		//
		String description = "";
		
		switch(interlaceType) {
			case 0:
				description = "No interlace";
				break;
			case 1:
				description = "Adam7 interlace";
				break;
			default:
				description = "Invalid interlace type";
				break;
		}

		return description;		
	}
	
	private PNGDescriptor() {}
}
