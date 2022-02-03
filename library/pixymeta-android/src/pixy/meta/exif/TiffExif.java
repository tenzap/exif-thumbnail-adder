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
 * TiffExif.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    13Nar2015  Initial creation
 */

package pixy.meta.exif;

import java.io.IOException;
import java.io.OutputStream;

import pixy.meta.exif.Exif;
import pixy.image.tiff.IFD;

public class TiffExif extends Exif {

	public TiffExif() {
		;
	}
	
	public TiffExif(IFD imageIFD) {
		super(imageIFD);		
	}
	
	/** 
	 * Write the EXIF data to the OutputStream
	 * 
	 * @param os OutputStream
	 * @throws Exception 
	 */
	@Override
	public void write(OutputStream os) throws IOException {
		ensureDataRead();
		; // We won't write anything here
	}
}
