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

import pixy.util.Reader;

/**
 * JPEG COM segment reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/11/2013
 */
public class COMReader implements Reader {

	private Segment segment;
	private String comment;
	
	public COMReader(Segment segment) throws IOException {
		//
		if(segment.getMarker() != Marker.COM) {
			throw new IllegalArgumentException("Not a valid COM segment!");
		}
		
		this.segment = segment;
		read();
	}
	
	public String getComment() {
		return this.comment;
	}
	
	public void read() throws IOException {
		this.comment = new String(segment.getData()).trim();
	}
}
