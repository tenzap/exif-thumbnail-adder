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
 * IRBThumbnail.java
 *
 * Who   Date       Description
 * ====  =========  ===========================================================
 * WY    27Apr2015  Added copy constructor
 * WY    10Apr2015  Implemented base class Thumbnail abstract method write()
 * WY    13Mar2015  Initial creation for IRBReader to encapsulate IRB thumbnail
 */

package pixy.meta.adobe;

import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import pixy.meta.Thumbnail;

/** 
 * Photoshop Image Resource Block thumbnail wrapper.
 *
 * @author Wen Yu, yuwen_66@yahoo.com 
 * @version 1.0 01/10/2015   
 */
public class IRBThumbnail extends Thumbnail {
	
	public IRBThumbnail() { ; }
	
	public IRBThumbnail(Bitmap thumbnail) {
		super(thumbnail);
	}
	
	public IRBThumbnail(int width, int height, int dataType, byte[] compressedThumbnail) {
		super(width, height, dataType, compressedThumbnail);
	}
	
	public IRBThumbnail(IRBThumbnail other) { // Copy constructor
		this.dataType = other.dataType;
		this.height = other.height;
		this.width = other.width;
		this.thumbnail = other.thumbnail;
		this.compressedThumbnail = other.compressedThumbnail;
	}

	@Override
	public void write(OutputStream os) throws IOException {
		if(getDataType() == Thumbnail.DATA_TYPE_KJpegRGB) { // Compressed old-style JPEG format
			os.write(getCompressedImage());
		} else if(getDataType() == Thumbnail.DATA_TYPE_KRawRGB) {
			Bitmap thumbnail = getRawImage();
			if(thumbnail == null) throw new IllegalArgumentException("Expected raw data thumbnail does not exist!");
			try {
				thumbnail.compress(Bitmap.CompressFormat.JPEG, writeQuality, os);
			} catch (Exception e) {
				throw new RuntimeException("Unable to compress thumbnail as JPEG");
			}			
		}
	}
 }
