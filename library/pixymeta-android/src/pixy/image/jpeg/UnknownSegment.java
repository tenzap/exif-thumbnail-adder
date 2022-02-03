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

package pixy.image.jpeg;

import java.io.IOException;
import java.io.OutputStream;

import pixy.io.IOUtils;

/**
 * Special segment to handle JPEG Marker.UNKNOWN.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/22/2013
 */
public class UnknownSegment extends Segment {

	private short markerValue;
	
	public UnknownSegment(short markerValue, int length, byte[] data) {
		super(Marker.UNKNOWN, length, data);
		this.markerValue = markerValue;
	}
	
	public short getMarkerValue() {
		return markerValue;
	}
	
	@Override public void write(OutputStream os) throws IOException{
		IOUtils.writeIntMM(os, getLength());
		IOUtils.writeIntMM(os, this.markerValue);
		IOUtils.write(os, getData());
	}
	
	@Override public String toString() {
		return super.toString() + "[Marker value: 0x"+ Integer.toHexString(markerValue&0xffff)+"]";
	}
}
