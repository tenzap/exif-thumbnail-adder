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
import java.util.ArrayList;
import java.util.List;

import pixy.io.IOUtils;
import pixy.util.Reader;

/**
 * JPEG DQT segment reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/11/2013
 */
public class DQTReader implements Reader {

	private Segment segment;
	private List<QTable> qTables = new ArrayList<QTable>(4);
	
	public DQTReader(Segment segment) throws IOException {
		//
		if(segment.getMarker() != Marker.DQT) {
			throw new IllegalArgumentException("Not a valid DQT segment!");
		}
		
		this.segment = segment;
		read();
	}
	
	public List<QTable> getTables() {
		return qTables;
	}
	
	public void read() throws IOException {
		//
		byte[] data = segment.getData();		
		int len = segment.getLength();
		len -= 2;//
		
		int offset = 0;
		
	  	int[] de_zig_zag_order = JPGConsts.getDeZigzagMatrix();
		  
		while(len > 0)
		{
			int QT_info = data[offset++];
			len--;
		    int QT_precision = (QT_info>>4)&0x0f;
		    int QT_index=(QT_info&0x0f);
		    int numOfValues = 64 << QT_precision;
		    
		    int[] out = new int[64];
		   
		    // Read QT tables
    	    // 8 bit For precision value of 0
		   	if(QT_precision == 0) {
				for (int j = 0; j < 64; j++) {
					out[j] = data[de_zig_zag_order[j] + offset]&0xff;			
			    }
			} else { // 16 bit big-endian for precision value of 1								
				for (int j = 0; j < 64; j++) {
					out[j] = (IOUtils.readUnsignedShortMM(data, offset + de_zig_zag_order[j]<<1));	
				}				
			}
		   	
		   	qTables.add(new QTable(QT_precision, QT_index, out));
		
			len -= numOfValues;
			offset += numOfValues;
		}
	}	
}
