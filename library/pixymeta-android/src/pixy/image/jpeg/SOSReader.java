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

public class SOSReader implements Reader {
	//
	private Segment segment;
	private SOFReader reader;
	
	int Ss, Se, Ah_Al, Ah, Al;
	
	public SOSReader(Segment segment) throws IOException {
		//
		if(segment.getMarker() != Marker.SOS) {
			throw new IllegalArgumentException("Not a valid SOS segment!");
		}
		
		this.segment = segment;
		read();
	}
	
	public SOSReader(Segment segment, SOFReader reader) throws IOException {
		//
		if(segment.getMarker() != Marker.SOS) {
			throw new IllegalArgumentException("Not a valid SOS segment!");
		}
		
		this.segment = segment;
		this.reader = reader;
		read();
	}
	
	public void read() throws IOException {
		//
		byte[] data = segment.getData();		
		int count = 0;
		
		byte numOfComponents = data[count++];
		Component[] components = reader.getComponents();		
		
		for(int i = 0; i < numOfComponents; i++) {
			byte id = data[count++];
			byte tbl_no = data[count++];			
		
			for(Component component : components) {
				if(component.getId() == id) {					
					component.setACTableNumber((byte)(tbl_no&0x0f));
					component.setDCTableNumber((byte)((tbl_no>>4)&0x0f));
					break;
				}
			}
		}
		
		//Start of spectral or predictor selection
		Ss = data[count++];
	    //End of spectral selection
		Se = data[count++];
		//Ah: Successive approximation bit position high
		//Al: Successive approximation bit position low or point transform
		Ah_Al = data[count++];
	    Ah = (Ah_Al>>4)&0x0f;
		Al = Ah_Al&0x0f;
	}
	
	public void setSOFReader(SOFReader reader) {
		this.reader = reader;
	}
}
