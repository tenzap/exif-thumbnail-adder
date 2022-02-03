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
 * Segment.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    16Mar2015  Changed write() to work with stand-alone segments
 */

package pixy.image.jpeg;

import java.io.IOException;
import java.io.OutputStream;

import pixy.io.IOUtils;

/**
 * JPEG segment.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/21/2013
 */
public class Segment {

	private Marker marker;
	private int length;
	private byte[] data;
	
	public Segment(Marker marker, int length, byte[] data) {
		this.marker = marker;
		this.length = length;
		this.data = data;
	}
	
	public Marker getMarker() {
		return marker;
	}
	
	public int getLength() {
		return length;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void write(OutputStream os) throws IOException {
		IOUtils.writeShortMM(os, marker.getValue());
		// If this is not a stand-alone segment, write the content as well
		if(length > 0) {
			IOUtils.writeShortMM(os, length);
			IOUtils.write(os, data);
		}
	}
	
	@Override public String toString() {
		return this.marker.toString();
	}
}
