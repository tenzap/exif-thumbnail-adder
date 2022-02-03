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
import java.util.EnumSet;

import pixy.io.IOUtils;
import pixy.util.Reader;

/**
 * JPEG SOF segment reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/09/2013
 */
public class SOFReader implements Reader {

	private int precision;
	private int frameHeight;
	private int frameWidth;
	private int numOfComponents;
	private Component[] components;
	private static final EnumSet<Marker> SOFS = 
			EnumSet.of(Marker.SOF0, Marker.SOF1, Marker.SOF2, Marker.SOF3, Marker.SOF5, 
		               Marker.SOF6, Marker.SOF7, Marker.SOF9, Marker.SOF10, Marker.SOF11,
		               Marker.SOF13, Marker.SOF14, Marker.SOF15)
	        ;
	
	private Segment segment;
	
	public SOFReader(Segment segment) throws IOException {
		//
		if(!SOFS.contains(segment.getMarker())) {
			throw new IllegalArgumentException("Not a valid SOF segment: " + segment.getMarker());
		}
		
		this.segment = segment;
		read();
	}
	
	public int getLength() {
		return segment.getLength();
	}
	
	public int getPrecision() {
		return precision;
	}
	
	public int getFrameHeight() {
		return frameHeight;
	}
	
	public int getFrameWidth() {
		return frameWidth;
	}
	
	public int getNumOfComponents() {
		return numOfComponents;
	}
	
	public Component[] getComponents() {
		return components.clone();
	}
	
	public void read() throws IOException {
		//
		byte[] data = segment.getData();
		// This is in bits/sample, usually 8, (12 and 16 not supported by most software). 
		precision = data[0]; // Usually 8, for baseline JPEG
		// Image frame width and height
		frameHeight = IOUtils.readUnsignedShortMM(data, 1);
		frameWidth = IOUtils.readUnsignedShortMM(data, 3);
		 // Number of components
		// Usually 1 = grey scaled, 3 = color YCbCr or YIQ, 4 = color CMYK 
        // JFIF uses either 1 component (Y, greyscaled) or 3 components (YCbCr, sometimes called YUV, color).
		numOfComponents = data[5];
		components = new Component[numOfComponents];
	
		int offset = 6;
		
		for (int i = 0; i < numOfComponents; i++) {
			byte componentId = data[offset++];
			// Sampling factors (1byte) (bit 0-3 horizontal, 4-7 vertical).
			byte sampleFactor = data[offset++];
			byte hSampleFactor = (byte)((sampleFactor>>4)&0x0f);
			byte vSampleFactor = (byte)((sampleFactor&0x0f));
			byte qTableNumber = data[offset++];
					
			components[i] = new Component(componentId, hSampleFactor, vSampleFactor, qTableNumber);
		}
	}
}
