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

import pixy.util.Builder;

/**
 * PNG PLTE chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/26/2013
 */
public class PLTEBuilder extends ChunkBuilder implements Builder<Chunk> {

	private byte[] redMap;
	private byte[] greenMap;
	private byte[] blueMap;
	
	public PLTEBuilder() {
		super(ChunkType.PLTE);		
	}

	public PLTEBuilder redMap(byte[] redMap) {
		this.redMap = redMap;		
		return this;
	}
	
	public PLTEBuilder greenMap(byte[] greenMap) {
		this.greenMap = greenMap;
		return this;
	}
	
	public PLTEBuilder blueMap(byte[] blueMap) {
		this.blueMap = blueMap;
		return this;
	}
	
	@Override
	protected byte[] buildData() {
		// Converts to PNG RGB PLET format
		int mapLen = redMap.length;
		byte[] colorMap = new byte[3*mapLen];
		
		for (int i = mapLen - 1, j = colorMap.length - 1; i >= 0; i--) {			
			colorMap[j--] = blueMap[i];
			colorMap[j--] = greenMap[i];
			colorMap[j--] = redMap[i];
		}

		return colorMap;
	}
}
