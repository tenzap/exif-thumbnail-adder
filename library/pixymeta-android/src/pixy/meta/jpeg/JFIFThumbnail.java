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
 * JFIFThumbnail.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    14Jul2015  Added copy constructor
 * WY    12Jul2015  Initial creation
 */

package pixy.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import pixy.meta.Thumbnail;

public class JFIFThumbnail extends Thumbnail {

	public JFIFThumbnail(Bitmap thumbnail) {
		super(thumbnail);
	}
	
	public JFIFThumbnail(JFIFThumbnail other) { // Copy constructor
		this.dataType = other.dataType;
		this.height = other.height;
		this.width = other.width;
		this.thumbnail = other.thumbnail;
		this.compressedThumbnail = other.compressedThumbnail;
	}

	@Override
	public void write(OutputStream os) throws IOException {
		Bitmap thumbnail = getRawImage();
		if(thumbnail == null) throw new IllegalArgumentException("Expected raw data thumbnail does not exist!");
		int thumbnailWidth = thumbnail.getWidth();
		int thumbnailHeight = thumbnail.getHeight();
		int[] pixels = new int[thumbnailWidth*thumbnailHeight];
		thumbnail.getPixels(pixels, 0, thumbnailWidth, 0, 0, thumbnailWidth, thumbnailHeight);
		for(int pixel : pixels) {
			os.write(pixel >> 16); // Red
			os.write(pixel >> 8); // Green
			os.write(pixel); // Blue
		}
	}
}
