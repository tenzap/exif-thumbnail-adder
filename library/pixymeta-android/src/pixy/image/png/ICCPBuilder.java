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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.DeflaterOutputStream;

import pixy.util.Builder;

/**
 * PNG iCCP chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/23/2014
 */
public class ICCPBuilder extends ChunkBuilder implements Builder<Chunk> {
	
	private String profileName;
	private byte[] profileData;

	public ICCPBuilder() {
		super(ChunkType.ICCP);
	}
	
	public ICCPBuilder data(byte[] data) {
		this.profileData = data;		
		return this;
	}
	
	public ICCPBuilder name(String name) {
		this.profileName = name.trim().replaceAll("\\s+", " ");
		return this;		
	}

	@Override
	protected byte[] buildData() {
		StringBuilder sb = new StringBuilder(this.profileName);
		sb.append('\0'); // Null separator
		sb.append('\0'); // Compression method	
		ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);	
		try {
			bo.write(sb.toString().getBytes("iso-8859-1"));		
			DeflaterOutputStream ds = new DeflaterOutputStream(bo);
			BufferedOutputStream bout = new BufferedOutputStream(ds);
			bout.write(profileData);
			bout.flush();
			bout.close();
		} catch(Exception ex) { 
			ex.printStackTrace();
		}
		
		return bo.toByteArray();				
	}
}
